import { register } from 'node:module';
import { pathToFileURL } from 'node:url';

register('./scripts/workbench-test-loader.mjs', pathToFileURL('./'));
