# PulsePlay Arena Website

Static gaming landing page built with HTML, CSS, and JavaScript.

## Local preview

Run a local server from the repository root:

```bash
python3 -m http.server 4173
```

Open: `http://127.0.0.1:4173/index.html`

## GitHub Pages deployment

This repo includes an Actions workflow at `.github/workflows/deploy-pages.yml` that deploys the site to GitHub Pages.

### One-time setup

1. Push this repository to GitHub.
2. Go to **Settings → Pages**.
3. Under **Build and deployment**, set **Source** to **GitHub Actions**.

### Deploy

- Automatic deploy runs on every push to `main`.
- You can also run it manually from **Actions → Deploy static site to GitHub Pages → Run workflow**.

After a successful run, your public site URL appears in the workflow summary.
