# GhostAccess

GhostAccess is a polished, browser-based hackathon MVP for discovering forgotten access paths and testing the smallest safe remediation in a **synthetic** permission graph.

## Run it

No dependency install is needed. From this folder, serve the static files with any local web server, for example:

```bash
python3 -m http.server 4173
```

Then open `http://localhost:4173`.

## Deploy

This is a dependency-free static site and is ready to deploy to Vercel or Netlify.

- **Vercel:** import the repository and use the included `vercel.json`; the build command is `npm run build` and the output directory is `.`.
- **Netlify:** import the repository; `netlify.toml` supplies the build, publish directory, and response headers.

Both configurations include restrictive browser security headers while allowing the two Google Font origins used by the interface.

## What the prototype demonstrates

- Prioritized stale-access findings with explainable, weighted risk signals.
- A focused identity -> group -> role -> permission -> resource graph.
- Clickable graph nodes with modeled relationship details.
- Three remediation candidates for the main demo path, ranked by scope and collateral impact.
- A simulation state that proves the selected candidate closes the modeled path, while the source graph remains unchanged.
- Clear “synthetic,” “simulated,” and “verified in simulation” boundaries throughout the interface.
- A deterministic refresh action that re-evaluates every finding and candidate.
- Every visible interactive control is wired; there are no placeholder account or overflow controls.

## Demo flow

1. Open **Analytics Contractor** (the default critical finding).
2. Inspect the visualized path to **Customer Database** and the five risk inputs.
3. Select **Remove from Project Exporters**.
4. Click **Simulate selected change** to show that the path becomes unreachable with zero other identities affected.

## Scope boundaries

This is a deterministic frontend prototype built from synthetic data. It does not connect to AWS, Azure, Google Cloud, or any live identity provider; it does not revoke access; and it is not a production-grade IAM policy evaluator.
