'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { isNewerVersion, checkForUpdate, installUpdate } = require('../updater');

test('compares stable semantic versions without accepting malformed values', () => {
  assert.equal(isNewerVersion('1.0.3', '1.0.2'), true);
  assert.equal(isNewerVersion('1.1.0', '1.0.9'), true);
  assert.equal(isNewerVersion('1.0.2', '1.0.2'), false);
  assert.equal(isNewerVersion('1.0.1', '1.0.2'), false);
  assert.equal(isNewerVersion('1.0.3-beta.1', '1.0.2'), false);
  assert.equal(isNewerVersion('01.0.3', '1.0.2'), false);
});

test('checks public npm metadata and reports a newer release', async () => {
  let request;
  const result = await checkForUpdate('1.0.2', async (url, options) => {
    request = { url, options };
    return { ok: true, json: async () => ({ version: '1.0.3' }) };
  });

  assert.match(request.url, /registry\.npmjs\.org/);
  assert.equal(request.options.method, 'GET');
  assert.equal(result.latest, '1.0.3');
  assert.equal(result.updateAvailable, true);
});

test('rejects invalid registry versions and invalid install targets', async () => {
  await assert.rejects(
    checkForUpdate('1.0.2', async () => ({ ok: true, json: async () => ({ version: 'latest' }) })),
    /invalid stable version/,
  );
  await assert.rejects(installUpdate('1.0.3; malicious'), /invalid version string/);
});

test('delegates a valid installation target to npm', async () => {
  let installed;
  await installUpdate('1.0.3', async version => { installed = version; });
  assert.equal(installed, '1.0.3');
});
