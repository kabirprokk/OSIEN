'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { COMMANDS } = require('../tui');

test('slash-command suggestions include the provider model-list command', () => {
  assert.ok(COMMANDS.some(([command]) => command === '/models'));
  assert.ok(COMMANDS.some(([command]) => command === '/model'));
  assert.ok(COMMANDS.some(([command]) => command === '/signin'));
  assert.ok(COMMANDS.some(([command]) => command === '/api'));
  assert.ok(COMMANDS.some(([command]) => command === '/test-model'));
  assert.ok(COMMANDS.some(([command]) => command === '/update'));
});
