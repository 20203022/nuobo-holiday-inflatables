import {build} from 'esbuild';
import {mkdir} from 'node:fs/promises';
await mkdir('../vendor', {recursive:true});
await build({entryPoints:['ui-runtime.js'], bundle:true, format:'iife', minify:true, outfile:'../vendor/ui-runtime.js', define:{'process.env.NODE_ENV':'"production"'}, target:['es2020'], logLevel:'info'});
await build({entryPoints:{'globe-runtime':'../globe-effects.js','hero-runtime':'../hero-effects.js'}, bundle:true, format:'esm', splitting:true, minify:true, outdir:'../vendor', chunkNames:'three-shared-[hash]', target:['es2020'], logLevel:'info'});
