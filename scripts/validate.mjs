import { readFileSync, existsSync } from "node:fs";

const requiredFiles = [
  "index.html", "styles.css", "app.js", "favicon.svg", "site.webmanifest",
  "vercel.json", "netlify.toml", "robots.txt"
];

for (const file of requiredFiles) {
  if (!existsSync(file)) throw new Error(`Missing deploy artifact: ${file}`);
}

const html = readFileSync("index.html", "utf8");
const script = readFileSync("app.js", "utf8");
const styles = readFileSync("styles.css", "utf8");

for (const asset of ["styles.css", "app.js", "favicon.svg", "site.webmanifest"]) {
  if (!html.includes(asset)) throw new Error(`index.html does not reference ${asset}`);
}

for (const id of [
  "helpButton", "startInvestigation", "filterButton", "refreshButton", "showAllButton",
  "riskDetailsButton", "graphResetButton", "simulateButton", "scopeButton", "menuButton"
]) {
  if (!html.includes(`id="${id}"`)) throw new Error(`Missing UI control: ${id}`);
  if (!script.includes(`$("#${id}").addEventListener`)) throw new Error(`Unwired UI control: ${id}`);
}

for (const capability of ["cloneGraph", "findPaths", "evaluateCandidate", "applyAction", "sourceEdgeSignature"]) {
  if (!script.includes(capability)) throw new Error(`Missing simulation safety capability: ${capability}`);
}

if (!styles.includes("prefers-reduced-motion")) throw new Error("Missing reduced-motion support");
JSON.parse(readFileSync("site.webmanifest", "utf8"));
JSON.parse(readFileSync("vercel.json", "utf8"));

console.log("Static deployment validation passed.");
