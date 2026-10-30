'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { isUnsupportedModelError, isUnsupportedCodexModelError, buildCodexPrompt, extractModelNames } = require('../codex');

test('only explicit unsupported-model responses classify a model as unavailable', () => {
  assert.equal(isUnsupportedModelError(400, 'model_not_found: unknown model'), true);
  assert.equal(isUnsupportedModelError(404, 'The model does not exist'), true);
  assert.equal(isUnsupportedModelError(429, 'rate limit exceeded'), false);
  assert.equal(isUnsupportedModelError(500, 'upstream failure'), false);
  assert.equal(isUnsupportedModelError(401, 'invalid api key'), false);
});

test('Codex errors mark only explicit unknown-model responses unsupported', () => {
  assert.equal(isUnsupportedCodexModelError('unknown model gpt-x'), true);
  assert.equal(isUnsupportedCodexModelError('model gpt-x is not available'), true);
  assert.equal(isUnsupportedCodexModelError('authentication required'), false);
  assert.equal(isUnsupportedCodexModelError('rate limit reached'), false);
});

test('Codex model catalogue extracts only visible named models', () => {
  assert.deepEqual(extractModelNames({ data: [
    { id: 'gpt-codex', displayName: 'GPT Codex', hidden: false },
    { id: 'secret-preview', displayName: 'Preview', hidden: true },
    { id: '', displayName: 'Empty' },
  ] }), ['gpt-codex']);
});

test('Codex prompt preserves bounded conversation context and asks for text output only', () => {
  const prompt = buildCodexPrompt([
    { role: 'system', content: 'You are OSIN.' },
    { role: 'user', content: 'Question one' },
    { role: 'assistant', content: 'Answer one' },
    { role: 'user', content: 'Question two' },
  ]);
  assert.match(prompt, /Question one/);
  assert.match(prompt, /Answer one/);
  assert.match(prompt, /Question two/);
  assert.match(prompt, /Do not modify files/);
});
