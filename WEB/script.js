'use strict';

const demoResponses = {
  '/help': {
    prompt: 'Show me the available slash commands.',
    response: 'In OSIN, /help shows command help. Other available commands include /new, /clear, /undo, /attach <path>, /files, /detach N, /copy, /usage, /settings, /model NAME, /support, and /exit.',
  },
  '/attach src/app.js': {
    prompt: 'How would I bring a source file into this conversation?',
    response: 'In the real CLI, use /attach src/app.js to attach a UTF-8 text file, then ask your question. The file is read locally and included with your next message. This webpage demo does not read files.',
  },
  '/usage': {
    prompt: 'What does /usage show?',
    response: 'The real /usage command shows request count and input, output, and total token counts reported by the configured provider. It is not a dollar balance. This demonstration has no live usage data.',
  },
};

const menuButton = document.querySelector('.menu-toggle');
const navigation = document.querySelector('#site-nav');

menuButton.addEventListener('click', () => {
  const isOpen = menuButton.getAttribute('aria-expanded') === 'true';
  menuButton.setAttribute('aria-expanded', String(!isOpen));
  menuButton.setAttribute('aria-label', isOpen ? 'Open navigation' : 'Close navigation');
  navigation.classList.toggle('is-open', !isOpen);
});

navigation.addEventListener('click', event => {
  if (event.target.closest('a')) {
    navigation.classList.remove('is-open');
    menuButton.setAttribute('aria-expanded', 'false');
    menuButton.setAttribute('aria-label', 'Open navigation');
  }
});

document.querySelectorAll('.command-chip').forEach(button => {
  button.addEventListener('click', () => {
    const exchange = demoResponses[button.dataset.command];
    const messages = document.querySelector('#demo-messages');
    if (!exchange || !messages) return;
    document.querySelectorAll('.command-chip').forEach(chip => chip.setAttribute('aria-pressed', 'false'));
    button.setAttribute('aria-pressed', 'true');
    messages.innerHTML = '';
    const user = document.createElement('article');
    user.className = 'message';
    const userRole = document.createElement('div');
    userRole.className = 'message-role role-you';
    userRole.textContent = '› YOU';
    const userText = document.createElement('p');
    userText.textContent = exchange.prompt;
    user.append(userRole, userText);
    const answer = document.createElement('article');
    answer.className = 'message message-response';
    const answerRole = document.createElement('div');
    answerRole.className = 'message-role role-osin';
    answerRole.textContent = '◈ OSIN';
    const answerText = document.createElement('p');
    answerText.textContent = exchange.response;
    const note = document.createElement('div');
    note.className = 'token-note';
    note.textContent = 'Illustrative response · no model call was made';
    answer.append(answerRole, answerText, note);
    messages.append(user, answer);
  });
});
