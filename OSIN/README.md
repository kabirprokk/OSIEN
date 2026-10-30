# OSIN CLI

OSIN is a terminal-based AI coding assistant with a mouse-enabled chat
workspace, text-file attachments, large-paste handling, and slash commands.
It sends requests to an OpenAI-compatible chat-completions API. It does not
run shell commands or directly edit project files.

## Install globally

Once the first public npm release is available, install OSIN globally with:

```sh
npm install --global @kabirprokk/osin-cli
```

The `osin` command is then available from any directory. Node.js 18 or newer
and npm are required. To install and test from a local checkout before
publication, run `npm install --global .` from this directory.

Until the npm release is published, clone the repository, change to `OSIN`,
and run `npm install --global .` there:

```sh
git clone https://github.com/kabirprokk/OSIEN.git
cd OSIEN/OSIN
npm install --global .
```

## Configure

Create per-user configuration files:

```sh
osin init
```

Edit the generated `.env` file and set the API key, OpenAI-compatible API base
URL, and model ID for your provider:

```dotenv
OSIN_API_KEY=your_provider_key
OSIN_BASE_URL=https://api.openai.com/v1
OSIN_MODEL=gpt-4o-mini
```

OSIN currently sends OpenAI-compatible Chat Completions requests. Use `/models`
to query model IDs exposed by that provider to your key; a returned model may
not necessarily support chat completions. Legacy `GROQ_*` and `OPENAI_*`
environment variables are recognized. Existing process environment variables
take precedence over values in the config file.

- Windows: `%APPDATA%\OSIN\.env`
- macOS and Linux: `$XDG_CONFIG_HOME/osin/.env`, or `~/.config/osin/.env`

Then start the workspace:

```sh
osin
```

Use `osin --help` for command-line options. Inside the workspace, type `/` to
see available commands. Use `/attach <path>` to attach a UTF-8 text file.

The config directory also stores optional `sponsors.json` and local aggregate
`metrics.json` data. Metrics are not uploaded. Keep API keys private and do
not attach secret files.

## Develop and test

```sh
npm test
node osin.js --version
```

The project has no runtime dependencies.
