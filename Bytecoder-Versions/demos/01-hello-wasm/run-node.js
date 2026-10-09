#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const scriptDir = __dirname;
const runtimePath = path.join(scriptDir, 'dist', 'hello_runtime.js');
const wasmPath = path.join(scriptDir, 'dist', 'hello_wasmclasses.wasm');

if (!fs.existsSync(runtimePath) || !fs.existsSync(wasmPath)) {
    console.error(`Error: Build artifacts not found in ${path.join(scriptDir, 'dist')}. Please run build.sh first.`);
    process.exit(1);
}

let runtimeCode = fs.readFileSync(runtimePath, 'utf8');
// Expose bytecoder onto global scope for Node.js
runtimeCode = runtimeCode.replace('const bytecoder = {', 'globalThis.bytecoder = {');
eval(runtimeCode);

const wasmBuffer = fs.readFileSync(wasmPath);
WebAssembly.instantiate(wasmBuffer, bytecoder.imports)
    .then(({ module, instance }) => {
        bytecoder.init(module, instance);
        bytecoder.bootstrap();
        bytecoder.initializeFileIO();
        console.log('[Bytecoder Runner] WasmGC Module instantiated and bootstrapped.');
        
        if (typeof instance.exports.main === 'function') {
            console.log('[Bytecoder Runner] Invoking main(null, null)...');
            instance.exports.main(null, null);
        } else {
            console.error('[Bytecoder Runner] Export "main" not found in WebAssembly exports!');
        }
    })
    .catch(err => {
        console.error('[Bytecoder Runner] Execution failed:', err);
        process.exit(1);
    });
