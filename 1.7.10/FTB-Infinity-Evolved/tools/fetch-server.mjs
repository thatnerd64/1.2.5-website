#!/usr/bin/env node
// Downloads an FTB Infinity Evolved 1.7 3.1.0 server matching the browser build, into a directory:
//
//   node tools/fetch-server.mjs <server-dir> [--jobs=8]
//
// Then, in <server-dir>: java -Xmx4G -jar forge-1.7.10-10.13.4.1614-1.7.10-universal.jar nogui
// (Java 8; set online-mode=false in server.properties: browser players have no Mojang session).
//
// The mod set is the browser build's: the pack's files minus its client-only ones and minus the mods the browser
// build leaves out (fastcraft, Patcher, CustomMainMenu; see buildtools Prepare.addPack), so Forge's mod list check
// passes when a browser player joins. Libraries go where the Forge jar's Class-Path expects them.
// Only node (18+) is needed: run it on the server machine itself.

import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile, stat, rename } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';

const argv = process.argv.slice(2);
const dir = resolve(argv.find(a => !a.startsWith('--')) || 'server');
const args = Object.fromEntries(argv.filter(a => a.startsWith('--')).map(a => a.replace(/^--/, '').split('=')));
const jobs = Number(args.jobs ?? 8);

const MC = '1.7.10';
const FORGE = '1.7.10-10.13.4.1614-1.7.10';
const PACK = { id: 23, version: 99 }; // FTB Infinity Evolved 1.7, 3.1.0
const LEFT_OUT = /^(fastcraft|Patcher|CustomMainMenu)/i; // as in the browser build

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

async function minecraftServer() {
  console.log('Minecraft server', MC);
  const manifest = JSON.parse(await get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'));
  const version = JSON.parse(await get(manifest.versions.find(v => v.id === MC).url));
  const s = version.downloads.server;
  await download(s.url, join(dir, `minecraft_server.${MC}.jar`), { sha: s.sha1, size: s.size });
}

async function forge() {
  console.log('Forge', FORGE);
  const name = `forge-${FORGE}-universal.jar`;
  await download(`https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/${name}`, join(dir, name));
  // [path the Forge jar's Class-Path names, where to download it]
  const central = 'https://repo1.maven.org/maven2/', mojang = 'https://libraries.minecraft.net/';
  const libs = [
    ['net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar', mojang],
    ['org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar', central],
    ['com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.jar', central],
    ['com/typesafe/config/1.2.1/config-1.2.1.jar', central],
    ['org/scala-lang/scala-actors-migration_2.11/1.1.0/scala-actors-migration_2.11-1.1.0.jar', central],
    ['org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar', central],
    ['org/scala-lang/plugins/scala-continuations-library_2.11/1.0.2/scala-continuations-library_2.11-1.0.2.jar', central],
    ['org/scala-lang/plugins/scala-continuations-plugin_2.11.1/1.0.2/scala-continuations-plugin_2.11.1-1.0.2.jar', central],
    ['org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar', central],
    ['org/scala-lang/scala-parser-combinators_2.11/1.0.1/scala-parser-combinators_2.11-1.0.1.jar', central,
      'org/scala-lang/modules/scala-parser-combinators_2.11/1.0.1/scala-parser-combinators_2.11-1.0.1.jar'],
    ['org/scala-lang/scala-reflect/2.11.1/scala-reflect-2.11.1.jar', central],
    ['org/scala-lang/scala-swing_2.11/1.0.1/scala-swing_2.11-1.0.1.jar', central,
      'org/scala-lang/modules/scala-swing_2.11/1.0.1/scala-swing_2.11-1.0.1.jar'],
    ['org/scala-lang/scala-xml_2.11/1.0.2/scala-xml_2.11-1.0.2.jar', central,
      'org/scala-lang/modules/scala-xml_2.11/1.0.2/scala-xml_2.11-1.0.2.jar'],
    ['lzma/lzma/0.0.1/lzma-0.0.1.jar', mojang],
    ['net/sf/jopt-simple/jopt-simple/4.5/jopt-simple-4.5.jar', central],
    ['com/google/guava/guava/17.0/guava-17.0.jar', central],
    ['org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar', central],
  ];
  await pool(libs, ([path, base, from]) => download(base + (from || path), join(dir, 'libraries', path)));
}

async function pack() {
  console.log('FTB Infinity Evolved 1.7', `v${PACK.version}`);
  const manifest = JSON.parse(await get(`https://api.modpacks.ch/public/modpack/${PACK.id}/${PACK.version}`));
  const files = manifest.files
    .filter(f => !f.clientonly && !(f.type === 'mod' && LEFT_OUT.test(f.name)) && !f.path.startsWith('./resources'))
    .map(f => ({ url: encodeURI(decodeURI(f.url)), dest: join(dir, f.path, f.name), sha: f.sha1, size: f.size }));
  const fetched = await pool(files, f => download(f.url, f.dest, f));
  console.log(`  ${files.length} files (${fetched} downloaded)`);
}

await mkdir(dir, { recursive: true });
await minecraftServer();
await forge();
await pack();
console.log('done:', dir);
