# Webhook Configuration Guide

This document explains how to configure webhooks to trigger CI/CD pipelines for this project.

## Overview

Webhooks allow external services to trigger CI/CD pipelines automatically when specific events occur (e.g., code push, pull request, manual trigger).

## GitHub Actions Webhook Setup

### Automatic Triggers

GitHub Actions automatically triggers on:
- **Push events**: When code is pushed to `main`, `master`, or `develop` branches
- **Pull requests**: When PRs are opened/updated against `main`, `master`, or `develop`
- **Manual trigger**: Via GitHub UI (Actions tab → Run workflow)

### Webhook Configuration

1. **Repository Settings**:
   - Go to your repository on GitHub
   - Navigate to **Settings** → **Webhooks**
   - Click **Add webhook**

2. **Webhook Settings**:
   - **Payload URL**: `https://api.github.com/repos/{owner}/{repo}/dispatches`
   - **Content type**: `application/json`
   - **Secret**: (Optional) Generate a secret token
   - **Events**: Select "Let me select individual events"
     - Check: `Repository dispatch`
   - **Active**: ✓

3. **Trigger via API**:
   ```bash
   curl -X POST \
     -H "Accept: application/vnd.github.v3+json" \
     -H "Authorization: token YOUR_GITHUB_TOKEN" \
     https://api.github.com/repos/{owner}/{repo}/dispatches \
     -d '{"event_type":"webhook-trigger"}'
   ```

4. **Using GitHub CLI**:
   ```bash
   gh api repos/{owner}/{repo}/dispatches \
     -f event_type='webhook-trigger'
   ```

### Testing Webhook

```bash
# Test webhook trigger
curl -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  https://api.github.com/repos/{owner}/{repo}/dispatches \
  -d '{"event_type":"manual-trigger","client_payload":{"ref":"main"}}'
```

## GitLab CI/CD Webhook Setup

### Automatic Triggers

GitLab CI automatically triggers on:
- **Push events**: When code is pushed to protected branches
- **Merge requests**: When MRs are created/updated
- **Tags**: When tags are created

### Webhook Configuration

1. **Project Settings**:
   - Go to your GitLab project
   - Navigate to **Settings** → **Webhooks**
   - Click **Add webhook**

2. **Webhook Settings**:
   - **URL**: `https://gitlab.com/api/v4/projects/{project_id}/trigger/pipeline`
   - **Trigger**: Select "Pipeline events"
   - **Token**: Generate a trigger token
   - **Enable SSL verification**: ✓

3. **Trigger Token Setup**:
   - Go to **Settings** → **CI/CD** → **Pipeline triggers**
   - Click **Add trigger**
   - Copy the **Trigger token**

4. **Trigger via API**:
   ```bash
   curl -X POST \
     -F token=YOUR_TRIGGER_TOKEN \
     -F ref=main \
     https://gitlab.com/api/v4/projects/{project_id}/trigger/pipeline
   ```

5. **Trigger via Webhook**:
   ```bash
   curl -X POST \
     -H "X-Gitlab-Token: YOUR_WEBHOOK_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"ref":"main"}' \
     https://gitlab.com/api/v4/projects/{project_id}/trigger/pipeline
   ```

### Testing Webhook

```bash
# Test pipeline trigger
curl -X POST \
  -F token=YOUR_TRIGGER_TOKEN \
  -F ref=main \
  https://gitlab.com/api/v4/projects/{project_id}/trigger/pipeline
```

## Jenkins Webhook Setup

### GitHub Webhook Configuration

1. **Jenkins Plugin**:
   - Install "GitHub plugin" if not already installed
   - Go to **Manage Jenkins** → **Configure System**
   - Under **GitHub**, add GitHub server
   - Configure credentials

2. **Job Configuration**:
   - Open your Jenkins job
   - Go to **Build Triggers**
   - Check **GitHub hook trigger for GITScm polling**
   - Save

3. **GitHub Repository Settings**:
   - Go to GitHub repository → **Settings** → **Webhooks**
   - Click **Add webhook**
   - **Payload URL**: `http://your-jenkins-server/github-webhook/`
   - **Content type**: `application/json`
   - **Events**: Select "Just the push event"
   - **Active**: ✓

### Generic Webhook Trigger

1. **Install Plugin**:
   - Install "Generic Webhook Trigger" plugin

2. **Job Configuration**:
   - Open your Jenkins job
   - Go to **Build Triggers**
   - Check **Generic Webhook Trigger**
   - Configure:
     - **Post content parameters**: Add parameters as needed
     - **Token**: Generate a unique token
   - Save

3. **Trigger URL**:
   ```
   http://your-jenkins-server/generic-webhook-trigger/invoke?token=YOUR_TOKEN
   ```

4. **Trigger via cURL**:
   ```bash
   curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"ref":"refs/heads/main"}' \
     http://your-jenkins-server/generic-webhook-trigger/invoke?token=YOUR_TOKEN
   ```

## Webhook Testing

### Test GitHub Actions Webhook

```bash
# Using GitHub CLI
gh workflow run ci-cd.yml

# Using API
curl -X POST \
  -H "Accept: application/vnd.github.v3+json" \
  -H "Authorization: token YOUR_TOKEN" \
  https://api.github.com/repos/{owner}/{repo}/actions/workflows/ci-cd.yml/dispatches \
  -d '{"ref":"main"}'
```

### Test GitLab Webhook

```bash
# Trigger pipeline
curl -X POST \
  -F token=YOUR_TRIGGER_TOKEN \
  -F ref=main \
  https://gitlab.com/api/v4/projects/{project_id}/trigger/pipeline
```

### Test Jenkins Webhook

```bash
# Generic webhook trigger
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"ref":"refs/heads/main"}' \
  http://your-jenkins-server/generic-webhook-trigger/invoke?token=YOUR_TOKEN
```

## Security Best Practices

1. **Use Secrets**: Store webhook tokens and API keys as secrets
2. **HTTPS Only**: Always use HTTPS for webhook URLs
3. **Token Rotation**: Regularly rotate webhook tokens
4. **IP Whitelisting**: Restrict webhook access to known IPs (if possible)
5. **Signature Verification**: Verify webhook signatures when provided
6. **Rate Limiting**: Implement rate limiting to prevent abuse

## Troubleshooting

### GitHub Actions Not Triggering

1. Check workflow file syntax
2. Verify branch names match trigger conditions
3. Check repository settings for workflow permissions
4. Review Actions tab for error messages

### GitLab Pipeline Not Triggering

1. Verify `.gitlab-ci.yml` syntax
2. Check pipeline triggers configuration
3. Verify trigger token is correct
4. Check GitLab CI/CD settings

### Jenkins Not Triggering

1. Check Jenkins logs
2. Verify webhook URL is accessible
3. Check GitHub/Jenkins plugin configuration
4. Verify credentials are correct

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitLab CI/CD Documentation](https://docs.gitlab.com/ee/ci/)
- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Webhook Security Best Practices](https://webhooks.fyi/best-practices/security)

