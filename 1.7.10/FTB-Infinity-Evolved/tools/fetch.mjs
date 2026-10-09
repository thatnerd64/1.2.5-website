#!/usr/bin/env node
// Downloads everything the build needs into input/ from the official public sources.
//
//   node tools/fetch.mjs [--only=minecraft|forge|libs|pack] [--jobs=8]
//
// Layout produced (all git-ignored, nothing from Mojang or the mod authors is ever committed):
//   input/minecraft/1.7.10.jar            vanilla client jar            (Mojang, piston-data)
//   input/minecraft/1.7.10.json           version manifest
//   input/assets/indexes + objects        sounds, languages (Mojang's asset index; sounds are the bulk, 112 MB)
//   input/libraries/<maven path>.jar      every library the 1.7.10 launcher puts on the classpath
//   input/forge/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar          (Forge maven)
//   input/pack/mods/*.jar                 the 140 mods of FTB Infinity Evolved 1.7 3.1.0     (CurseForge CDN)
//   input/pack/config/**, input/pack/modpack/**, input/pack/resources/**  the pack's own files (FTB dist)
//   input/pack/manifest.json              the FTB modpack API manifest the above came from
//
// Files are verified against the sha1 the source publishes and skipped when already present.

import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile, stat, rename } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const input = join(root, 'input');
const args = Object.fromEntries(process.argv.slice(2).map(a => a.replace(/^--/, '').split('=')).map(([k, v]) => [k, v ?? true]));
const only = args.only;
const jobs = Number(args.jobs ?? 8);

const MC = '1.7.10';
const FORGE = '1.7.10-10.13.4.1614-1.7.10';
const PACK = { id: 23, version: 99 }; // FTB Infinity Evolved 1.7, 3.1.0

const sha1 = buf => createHash('sha1').update(buf).digest('hex');

async function exists(path, size) {
  try {
    const s = await stat(path);
    return size === undefined || s.size === size;
  } catch {
    return false;
  }
}

async function get(url, tries = 4) {
  let err;
  for (let i = 0; i < tries; i++) {
    try {
      const r = await fetch(url, { headers: { 'user-agent': 'mc-web-fetch/1.0' } });
      if (!r.ok) throw new Error(`${r.status} ${r.statusText}`);
      return Buffer.from(await r.arrayBuffer());
    } catch (e) {
      err = e;
      await new Promise(r => setTimeout(r, 1000 * 2 ** i));
    }
  }
  throw new Error(`${url}: ${err.message}`);
}

/** Downloads url to dest unless it is already there with the right hash. */
async function download(url, dest, { sha, size } = {}) {
  if (await exists(dest, size)) {
    if (!sha || sha1(await readFile(dest)) === sha) return false;
  }
  const buf = await get(url);
  if (sha && sha1(buf) !== sha) throw new Error(`sha1 mismatch for ${url}`);
  await mkdir(dirname(dest), { recursive: true });
  await writeFile(dest + '.part', buf);
  await rename(dest + '.part', dest);
  return true;
}

async function pool(items, fn) {
  let next = 0, done = 0, fetched = 0;
  await Promise.all(Array.from({ length: jobs }, async () => {
    while (next < items.length) {
      const item = items[next++];
      if (await fn(item)) fetched++;
      if (++done % 50 === 0 || done === items.length) process.stdout.write(`  ${done}/${items.length}\n`);
    }
  }));
  return fetched;
}

async function minecraft() {
  console.log('Minecraft', MC);
  await mkdir(join(input, 'minecraft'), { recursive: true });
  const versionPath = join(input, 'minecraft', `${MC}.json`);
  let version;
  if (await exists(versionPath)) {
    version = JSON.parse(await readFile(versionPath)); // already fetched: no need to ask Mojang again
  } else {
    const manifest = JSON.parse(await get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'));
    const entry = manifest.versions.find(v => v.id === MC);
    version = JSON.parse(await get(entry.url));
    await writeFile(versionPath, JSON.stringify(version, null, 2));
  }
  const c = version.downloads.client;
  await download(c.url, join(input, 'minecraft', `${MC}.jar`), { sha: c.sha1, size: c.size });

  console.log('Libraries');
  const libs = [];
  for (const lib of version.libraries) {
    const a = lib.downloads?.artifact;
    if (a && !lib.natives) libs.push({ url: a.url, dest: join(input, 'libraries', a.path), sha: a.sha1, size: a.size });
  }
  await pool(libs, l => download(l.url, l.dest, l));
}

async function assets() {
  console.log('Assets');
  const version = JSON.parse(await readFile(join(input, 'minecraft', `${MC}.json`)));
  const ai = version.assetIndex;
  const indexPath = join(input, 'assets', 'indexes', `${ai.id}.json`);
  await download(ai.url, indexPath, { sha: ai.sha1, size: ai.size });
  const index = JSON.parse(await readFile(indexPath));
  const objects = [...new Map(Object.values(index.objects).map(o => [o.hash, o])).values()].map(o => ({
    url: `https://resources.download.minecraft.net/${o.hash.slice(0, 2)}/${o.hash}`,
    dest: join(input, 'assets', 'objects', o.hash.slice(0, 2), o.hash),
    sha: o.hash,
    size: o.size,
  }));
  await pool(objects, o => download(o.url, o.dest, o));
}

async function forge() {
  console.log('Forge', FORGE);
  const name = `forge-${FORGE}-universal.jar`;
  const url = `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/${name}`;
  await download(url, join(input, 'forge', name));
  // Libraries Forge adds on top of the vanilla list (see the universal jar's version.json).
  const extra = [
    ['net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar', 'https://libraries.minecraft.net/'],
    ['org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar', 'https://repo1.maven.org/maven2/'],
    ['com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.jar', 'https://repo1.maven.org/maven2/'],
    ['com/typesafe/config/1.2.1/config-1.2.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/scala-actors-migration_2.11/1.1.0/scala-actors-migration_2.11-1.1.0.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/plugins/scala-continuations-library_2.11/1.0.2/scala-continuations-library_2.11-1.0.2.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/plugins/scala-continuations-plugin_2.11.1/1.0.2/scala-continuations-plugin_2.11.1-1.0.2.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/modules/scala-parser-combinators_2.11/1.0.1/scala-parser-combinators_2.11-1.0.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/scala-reflect/2.11.1/scala-reflect-2.11.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/modules/scala-swing_2.11/1.0.1/scala-swing_2.11-1.0.1.jar', 'https://repo1.maven.org/maven2/'],
    ['org/scala-lang/modules/scala-xml_2.11/1.0.2/scala-xml_2.11-1.0.2.jar', 'https://repo1.maven.org/maven2/'],
    ['lzma/lzma/0.0.1/lzma-0.0.1.jar', 'https://libraries.minecraft.net/'],
    ['net/sf/jopt-simple/jopt-simple/4.5/jopt-simple-4.5.jar', 'https://repo1.maven.org/maven2/'],
    ['com/google/guava/guava/17.0/guava-17.0.jar', 'https://repo1.maven.org/maven2/'],
    ['org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar', 'https://repo1.maven.org/maven2/'],
  ];
  await pool(extra, ([path, base]) => download(base + path, join(input, 'libraries', path)).catch(e => {
    console.warn('  (optional) ' + e.message);
    return false;
  }));
}

async function pack() {
  console.log('FTB Infinity Evolved 1.7', `v${PACK.version}`);
  const url = `https://api.modpacks.ch/public/modpack/${PACK.id}/${PACK.version}`;
  const manifest = JSON.parse(await get(url));
  await mkdir(join(input, 'pack'), { recursive: true });
  await writeFile(join(input, 'pack', 'manifest.json'), JSON.stringify(manifest, null, 2));
  // Mods go to pack/mods; everything else keeps its path relative to the game directory.
  const files = manifest.files
    .filter(f => !f.serveronly)
    .map(f => ({
      url: encodeURI(decodeURI(f.url)),
      dest: join(input, 'pack', f.path, f.name),
      sha: f.sha1,
      size: f.size,
    }));
  const fetched = await pool(files, f => download(f.url, f.dest, f));
  console.log(`  ${files.length} files (${fetched} downloaded)`);
}

if (!only || only === 'minecraft') await minecraft();
if (!only || only === 'forge' || only === 'libs') await forge();
if (!only || only === 'assets') await assets();
if (!only || only === 'pack') await pack();
console.log('done');
