# GitHub Actions Secrets & Configuration Setup

## Quick Start Checklist

- [ ] Create Docker Hub account/token
- [ ] Generate Kubernetes kubeconfig
- [ ] Create Slack webhooks
- [ ] Setup SonarQube token
- [ ] Add GitHub secrets
- [ ] Create GitHub environments
- [ ] Setup branch protection rules
- [ ] Configure required status checks

---

## Step 1: Docker Hub Setup

### Create Docker Hub Account

```bash
# If you don't have a Docker Hub account
1. Go to https://hub.docker.com/
2. Sign up for a free account
3. Verify email
```

### Generate Docker Access Token

```bash
# Recommended: Use access token instead of password
1. Login to Docker Hub
2. Click profile icon → Account Settings
3. Click "Security" in left sidebar
4. Click "New Access Token"
5. Enter name: "ci-cd-bot"
6. Select access scope: "Read, Write, Delete"
7. Click "Generate"
8. Copy token (you won't see it again!)
```

---

## Step 2: Kubernetes Setup

### Export kubeconfig (if using Kubernetes cluster)

```bash
# Get current kubeconfig
kubectl config view --raw > kubeconfig.yaml

# Or if using cloud provider
# AWS EKS
aws eks update-kubeconfig --region us-east-1 --name leadflow-prod

# Azure AKS
az aks get-credentials --resource-group rg-name --name cluster-name

# Encode to base64 for GitHub secret
cat kubeconfig.yaml | base64 > kubeconfig.b64
```

### Verify kubeconfig

```bash
# Test connectivity
kubectl cluster-info
kubectl get nodes

# Should output cluster info without errors
```

---

## Step 3: Slack Webhook Setup

### Create Incoming Webhook

```bash
# Option 1: For #deployments channel
1. Go to Slack workspace settings
2. Search for "Incoming Webhooks"
3. Click "Add New Webhook"
4. Select channel: #deployments
5. Click "Add Webhook"
6. Copy webhook URL

# Option 2: For #security-alerts channel (separate webhook)
1. Repeat steps 1-5
2. Select channel: #security-alerts
3. Copy webhook URL
```

### Test Slackbot

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"text":"Test message"}' \
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# You should see message in Slack
```

---

## Step 4: SonarQube Setup (Optional)

### Deploy SonarQube

```bash
# Using Docker
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:lts

# Access at http://localhost:9000
# Default login: admin/admin
```

### Generate SonarQube Token

```bash
# In SonarQube UI
1. Go to http://sonarqube:9000
2. Login as admin
3. Click admin icon → My Account → Security
4. Generate New Token
5. Name: "CI-CD"
6. Copy token
```

---

## Step 5: GitHub Repository Secrets

### Add Repository Secrets

```bash
# Go to GitHub repository
# Settings → Secrets and variables → Actions → New repository secret

# Add each secret:
```

**Secret 1: DOCKER_REGISTRY**
```
Value: docker.io
or: ghcr.io
or: <your-private-registry>
```

**Secret 2: DOCKER_USERNAME**
```
Value: <your-docker-username>
```

**Secret 3: DOCKER_PASSWORD**
```
Value: <docker-access-token>
```

**Secret 4: KUBE_CONFIG**
```
Value: <contents of kubeconfig.b64>

# Get this from:
cat kubeconfig.b64 | pbcopy  # Mac
cat kubeconfig.b64 | xclip -selection clipboard  # Linux
type kubeconfig.b64 | clip  # Windows
```

**Secret 5: SLACK_WEBHOOK_URL**
```
Value: https://hooks.slack.com/services/YOUR_TEAM_ID/YOUR_BOT_ID/YOUR_SECRET_TOKEN
Description: Slack webhook for deployment notifications
```

**Secret 6: SLACK_SECURITY_WEBHOOK_URL**
```
Value: https://hooks.slack.com/services/YOUR_TEAM_ID/YOUR_BOT_ID/YOUR_SECURITY_TOKEN
Description: Slack webhook for security scan notifications
```

**Secret 7: SONARQUBE_HOST_URL** (if using SonarQube)
```
Value: https://sonarqube.example.com
```

**Secret 8: SONARQUBE_TOKEN** (if using SonarQube)
```
Value: <sonarqube-token>
```

**Secret 9: STAGING_URL**
```
Value: https://api-staging.leadflow.io
```

---

## Step 6: Create GitHub Environments

### Staging Environment

```bash
# Go to: Settings → Environments → Create environment

Name: staging
Deployment branches and tags:
  - Include all branches
  
Environment secrets: (optional, none for staging)
Protected branches: (unchecked - staging is permissive)
```

### Production Environment

```bash
# Go to: Settings → Environments → Create environment

Name: production
Deployment branches and tags:
  - Include: main, master
  
Environment secrets: (optional)

Required reviewers: (check box)
  - Add @security-team
  - Add @devops-team
  
Protected branches: (optional)
  - Restrict to main, master
  
Wait timer: 0 minutes (or set to 5 for safety)
```

---

## Step 7: Branch Protection Rules

### Protect Main Branch

```bash
# Go to: Settings → Branches → Add rule

Branch name pattern: main

Rule Configuration:
  ✅ Require a pull request before merging
    - Required approvals: 2
    - Require approval from code owners: Yes
    ✅ Require status checks to pass before merging
    - Require branches to be up to date before merging: Yes
    - Check required:
      - build (Build job)
      - test (Test job)
      - security-scan (Security job)
    ✅ Require code review before merging
    ✅ Include administrators: No
```

### Protect Master Branch

```bash
# Repeat above for: master
```

---

## Step 8: Required Status Checks

### Configure Required Checks

```bash
# In branch protection rule above, ensure these are selected:
✅ build
✅ test
✅ security-scan

# These will prevent merge if any workflow fails
```

---

## Step 9: GitHub Actions Settings

### Enable Actions

```bash
# Go to: Settings → Actions → General

Actions permissions:
  ○ Disable all
  ○ Allow all
  ✓ Allow enterprise, and select non-enterprise, actions and reusable workflows

Workflow permissions:
  ○ Read and write permissions
  ✓ Read repository contents and packages permissions
  
Allow GitHub Actions to create and approve pull requests:
  ✓ Checked (for automated PRs)
```

---

## Verification Steps

### Test Each Secret

```bash
# Build Workflow
git push origin feature-branch
# Monitor: GitHub Actions → build workflow should trigger

# Test Workflow
# Should run automatically after build

# Security Scans
# Should complete without errors

# Docker Push
# Check Docker Hub: Repositories → leadflow-backend
# Tag should exist: latest, <commit-sha>

# Slack Notifications
# Should see message in #deployments channel

# Kubernetes Deployment
# kubectl get deployment -n staging
# Should show latest image
```

### Troubleshooting Secret Issues

```bash
# Error: "Authentication failed"
Solution: Verify secret value is correct (no extra spaces)

# Docker: "Manifest unknown"
Solution: Ensure image was pushed to registry
           docker images | grep leadflow

# Kubernetes: "Connection refused"  
Solution: Verify KUBE_CONFIG is valid
          kubectl auth can-i get deployments --as=system:serviceaccount:...

# Slack: "No response from server"
Solution: Verify webhook URL
          curl -I https://hooks.slack.com/...
```

---

## Security Best Practices

### Secret Rotation

```bash
# Monthly rotation recommended for:
- DOCKER_PASSWORD
- SLACK_WEBHOOK_URL
- SONARQUBE_TOKEN

# Steps:
1. Generate new secret/token in original system
2. Update GitHub secret
3. Test with manual workflow trigger
4. Delete old secret from original system
```

### Access Control

```bash
# Restrict secret access to:
- Branch: main, master
- Environment: staging, production
- Approvers: @security-team, @devops-team
```

### Audit Trail

```bash
# Monitor in GitHub:
Settings → Audit log → Filter by:
- Secret access
- Deployment
- Actions enabled/disabled

# Review quarterly
```

---

## Manual Workflow Trigger

### Deploy to Production

```bash
# Option 1: Via GitHub UI
1. Go to: Actions → Deploy
2. Click "Run workflow"
3. Select "production" from dropdown
4. Click green "Run workflow" button
5. Wait for approval (if configured)

# Option 2: Via GitHub CLI
gh workflow run deploy.yml \
  -f environment=production
```

### Debug Workflow

```bash
# Enable debug logging
Settings → Secrets and variables → Actions
Add new secret:
  Name: ACTIONS_STEP_DEBUG
  Value: true

# Next workflow run will show detailed logs
# Disable after debugging (contains sensitive data)
```

---

## Common Configurations

### Different Docker Registries

**Docker Hub (default):**
```
DOCKER_REGISTRY: docker.io
DOCKER_USERNAME: <your-username>
DOCKER_PASSWORD: <docker-token>
```

**GitHub Container Registry (ghcr.io):**
```
DOCKER_REGISTRY: ghcr.io
DOCKER_USERNAME: <github-username>
DOCKER_PASSWORD: <github-pat-token>
```

**AWS Elastic Container Registry:**
```
DOCKER_REGISTRY: <account-id>.dkr.ecr.<region>.amazonaws.com
DOCKER_USERNAME: AWS
DOCKER_PASSWORD: <aws-access-key-secret>
```

**Private Registry (e.g., Harbor):**
```
DOCKER_REGISTRY: registry.example.com
DOCKER_USERNAME: <registry-username>
DOCKER_PASSWORD: <registry-password>
```

---

## Integration with External Systems

### PagerDuty Integration

```bash
# In PagerDuty:
1. Create integration key for "GitHub Actions"
2. Copy integration key

# In GitHub:
Add secret: PAGERDUTY_INTEGRATION_KEY
         Value: <integration-key>

# In deploy.yml, add:
- name: Trigger PagerDuty
  if: failure()
  run: |
    curl -X POST https://events.pagerduty.com/v2/enqueue \
      -H 'Content-Type: application/json' \
      -d '{
        "routing_key": "${{ secrets.PAGERDUTY_INTEGRATION_KEY }}",
        "event_action": "trigger",
        "payload": {
          "summary": "Deployment failed",
          "severity": "critical",
          "source": "GitHub Actions"
        }
      }'
```

### Jira Integration

```bash
# In Jira:
1. Create API token (Profile → Security → API tokens)
2. Copy token

# In GitHub:
Add secrets:
  JIRA_HOST: https://your-domain.atlassian.net
  JIRA_USERNAME: your-email@example.com
  JIRA_API_TOKEN: <api-token>

# In workflow, add:
- name: Update Jira
  run: |
    curl -X POST ${{ secrets.JIRA_HOST }}/rest/api/3/issues/PROJ-123/transitions \
      -H 'Authorization: Basic ...' \
      -H 'Content-Type: application/json'
```

---

## Reference URLs

- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- [GitHub Branch Protection](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [Docker Hub Access Tokens](https://docs.docker.com/docker-hub/access-tokens/)
- [Slack Webhooks](https://api.slack.com/messaging/webhooks)
- [Kubernetes kubeconfig](https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/)

---

**Last Updated:** 2024
**Version:** 1.0
