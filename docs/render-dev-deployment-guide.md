# Render deployment guide (dev branch)

This project deploys the `dev` branch to Render using GitHub Actions.

## 1) One-time setup in Render

1. Open your Render dashboard.
2. Open the target service (or create one if this is your first deployment).
3. In the service settings, find **Deploy Hook** and create/copy the hook URL.
4. Confirm the Render service is connected to this repository and uses the right branch/environment.

> Tip: If you have separate environments, keep a dedicated service for `dev`.

## 2) One-time setup in GitHub

1. Go to this repository on GitHub.
2. Open **Settings** → **Secrets and variables** → **Actions**.
3. Add a new repository secret named:
   - `RENDER_DEPLOY_HOOK_URL`
4. Paste the deploy hook URL from Render.

## 3) What happens in CI/CD

Workflow file: `.github/workflows/cd-render-dev.yml`

On every push to `dev` (or manual run):

1. GitHub Actions checks out the code.
2. Java 21 + Maven cache are configured.
3. `mvn -B test` runs.
4. If tests pass, GitHub POSTs to Render deploy hook.

If tests fail (or the secret is missing), deployment is blocked.

## 4) First deployment checklist

- [ ] `dev` branch exists and contains the latest code.
- [ ] `RENDER_DEPLOY_HOOK_URL` secret is set in GitHub.
- [ ] Render service is healthy and points to expected runtime settings.
- [ ] Required runtime env vars are set in Render (DB URL, API keys, etc.).

## 5) Trigger your first deployment

### Option A: Push to `dev`

```bash
git checkout dev
git pull
git merge <your-feature-branch>
git push origin dev
```

### Option B: Manual run

1. Go to **Actions** tab in GitHub.
2. Open **CD - Render (dev)**.
3. Click **Run workflow** and choose `dev`.

## 6) Verify deployment

1. In GitHub Actions, open the latest workflow run.
2. Ensure all steps pass, especially **Trigger Render deploy hook**.
3. In Render, open the service logs and confirm a new deploy started/completed.
4. Hit your health endpoint (for example `/actuator/health`) once deploy is live.

## 7) Troubleshooting

### `RENDER_DEPLOY_HOOK_URL secret is not set`
Add the secret in GitHub Actions secrets with exact name.

### Tests fail in GitHub Actions
Fix test/build failures first; deploy is intentionally gated by tests.

### Render did not deploy after workflow passed
- Verify the deploy hook URL belongs to the correct Render service.
- Regenerate hook in Render and update GitHub secret.
- Check Render service event logs for rejected deploys.
