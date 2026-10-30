# OCM Skill

Browse free, community-maintained skills, agents, and commands for OpenCode.

**Live site:** https://ocmskills.vercel.app

**Registry:** https://github.com/kabirprokk/opencode-registry

**CLI:** https://github.com/kabirprokk/opencode-marketplace

Independent community project. It is not affiliated with or endorsed by Anomaly.
Packages are community submissions; review their source and permissions before installing.

## Local development

Requirements: Node.js 20.9 or newer and npm.

```sh
npm ci
npm run dev
```

Open http://localhost:3000. The catalog is rendered from the public registry on
the server and revalidated every five minutes. Search and category filters run
in the browser without another network request.

## Checks

```sh
npm run lint
npm run build
```

## Contribute

Submit package metadata to the registry repository. Keep package names unique,
link to the source repository, include a license, and document setup or
permissions required. Marketplace inclusion is not a security review.

## License

MIT. See [LICENSE](./LICENSE).
