"use strict";

const fs = require("node:fs");
const path = require("node:path");

const registryPath = path.join(__dirname, "..", "registry.json");
const schemaPath = path.join(__dirname, "..", "schema.json");
const registry = JSON.parse(fs.readFileSync(registryPath, "utf8"));
const schema = JSON.parse(fs.readFileSync(schemaPath, "utf8"));

if (schema.$schema !== "https://json-schema.org/draft/2020-12/schema") throw new Error("Unexpected schema draft");
const schemaPathPattern = new RegExp(schema.$defs.package.properties.path.pattern);
if (schemaPathPattern.test("skills/../outside") || schemaPathPattern.test("skills\\..\\outside")) {
  throw new Error("Schema path pattern must reject slash and backslash traversal");
}
if (registry.version !== 1 || !/^\d{4}-\d{2}-\d{2}$/.test(registry.updated) || !Array.isArray(registry.packages)) {
  throw new Error("Registry header is invalid");
}

const names = new Set();
for (const item of registry.packages) {
  if (!item || typeof item !== "object") throw new Error("Package entry must be an object");
  if (typeof item.name !== "string" || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(item.name)) throw new Error(`Invalid package name: ${item.name}`);
  if (names.has(item.name.toLowerCase())) throw new Error(`Duplicate package name: ${item.name}`);
  names.add(item.name.toLowerCase());
  if (!["skill", "agent", "command", "skill-pack", "mcp"].includes(item.type)) throw new Error(`Unsupported type for ${item.name}`);
  if ([item.version, item.description, item.author, item.license].some((value) => typeof value !== "string" || !value.trim()) ||
    item.description.length > 300) {
    throw new Error(`Missing or invalid metadata for ${item.name}`);
  }
  if (item.install !== "git") throw new Error(`Unsupported install method for ${item.name}`);
  if (typeof item.repo !== "string") throw new Error(`Invalid repository URL for ${item.name}`);
  const repo = new URL(item.repo);
  if (repo.protocol !== "https:" || repo.hostname !== "github.com" || repo.username || repo.password || repo.search || repo.hash ||
    !/^\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+(?:\.git)?\/?$/.test(repo.pathname)) {
    throw new Error(`Repository must be a public GitHub HTTPS URL: ${item.name}`);
  }
  if (item.type !== "skill-pack" && (typeof item.path !== "string" || !item.path)) throw new Error(`Missing source path for ${item.name}`);
  if (item.path !== undefined && typeof item.path !== "string") throw new Error(`Invalid source path for ${item.name}`);
  if (item.path) {
    const segments = item.path.replaceAll("\\", "/").split("/");
    if (item.path.startsWith("/") || segments.some((segment) => !segment || segment === "." || segment === "..")) {
      throw new Error(`Unsafe source path for ${item.name}`);
    }
  }
  if (!Array.isArray(item.tags) || item.tags.some((tag) => typeof tag !== "string" || !tag || tag.length > 40) || new Set(item.tags).size !== item.tags.length) {
    throw new Error(`Invalid tags for ${item.name}`);
  }
}

console.log(`Registry valid: ${registry.packages.length} packages; schema parses successfully.`);
