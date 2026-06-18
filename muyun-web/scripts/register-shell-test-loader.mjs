import { register } from 'node:module';
import { pathToFileURL } from 'node:url';

register('./scripts/shell-test-loader.mjs', pathToFileURL('./'));
