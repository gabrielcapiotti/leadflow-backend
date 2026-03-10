# GitHub Actions CI/CD Pipeline Guide

## Overview

This guide documents the complete GitHub Actions CI/CD pipeline for the LeadFlow Backend application. The pipeline automates building, testing, securing, and deploying the application across multiple environments.

## Pipeline Architecture

```
┌─────────────┐
│   Push to   │
│  Repository │
└──────┬──────┘
       │
       ├─────────────────────────────────────────────┐
       │                                             │
       ▼                                             ▼
   ┌──────────┐                                ┌──────────────┐
   │  Build   │                                │    Tests     │
   └──────────┘                                └──────────────┘
       │                                             │
       ├─────────────────────────────────────────────┤
       │                                             │
       ▼                                             ▼
   ┌──────────┐                                ┌──────────────┐
   │ Security │─────────────────────────────────│  Docker      │
   │  Scans   │                                │  Build & Push│
   └──────────┘                                └──────────────┘
       │                                             │
       └─────────────────────────────────────────────┘
                       │
                       ▼
          ┌────────────────────────┐
          │   Deploy to Staging    │
          │  (if master/main)      │
          └────────────────────────┘
                       │
       ┌───────────────┴───────────────┐
       │                               │
  Manual Approval                  Auto Rollback
       │                               │
       ▼                               ▼
  ┌─────────────┐            ┌──────────────────┐
  │  Production │            │  Previous Version│
  │  Deployment │────Fail────│   (if failure)   │
  └─────────────┘            └──────────────────┘
```

## Workflows

### 1. Build Workflow

**File:** `.github/workflows/build.yml`

**Trigger:**
- Push to: `master`, `main`, `develop`
- Pull requests to: `master`, `main`, `develop`

**Steps:**

1. **Checkout Code**
   - Fetches full git history (depth: 0) for SonarQube analysis

2. **Setup JDK 21**
   - Installs Java 21 with Temurin distribution
   - Maven dependencies cached

3. **Compile Code**
   ```bash
   mvn clean compile -DskipTests
   ```
   - Skips tests to fail fast on compilation errors
   - Runs in parallel with max heap: 1GB

4. **Code Style Check**
   - Runs Checkstyle validation
   - Continues on error (warnings don't block build)

5. **SonarQube Analysis** (master branch only)
   ```bash
   mvn sonar:sonar \
     -Dsonar.projectKey=leadflow-backend \
     -Dsonar.sources=src/main \
     -Dsonar.host.url=$SONARQUBE_HOST_URL \
     -Dsonar.login=$SONARQUBE_TOKEN
   ```
   - Quality gate checks
   - Code coverage, complexity analysis
   - Technical debt tracking

6. **Artifact Caching**
   - Caches Maven repository for faster builds
   - Key: `$OS-maven-$hashOf(pom.xml)`

7. **Upload Logs** (on failure)
   - Uploads target/ directory for debugging
   - 7-day retention

**Environment Variables:**
- `MAVEN_OPTS=-Xmx1024m` - Java max heap

**Secrets Required:**
- `SONARQUBE_HOST_URL` - SonarQube server URL
- `SONARQUBE_TOKEN` - SonarQube authentication token

---

### 2. Test Workflow

**File:** `.github/workflows/test.yml`

**Trigger:**
- Push to: `master`, `main`, `develop`
- Pull requests to: `master`, `main`, `develop`

**Services:**
- PostgreSQL 15 (test database)
- Redis 7 (cache database)

**Steps:**

1. **Checkout Code**

2. **Setup JDK 21**

3. **Run Unit Tests**
   ```bash
   mvn test -DskipIntegrationTests
   ```
   - Isolated unit tests
   - No external dependencies
   - Faster feedback

4. **Run Integration Tests**
   ```bash
   mvn test -DincludeIntegrationTests
   ```
   - Tests against real PostgreSQL
   - Tests against real Redis
   - Database migration validation

5. **Generate Test Report**
   - Creates Surefire report
   - Collects all test result text files

6. **Upload Test Results**
   - 30-day retention
   - Accessible via Actions tab

7. **Upload Coverage Reports**
   - Codecov integration
   - JaCoCo XML format
   - Historical coverage tracking

8. **Comment PR with Test Results**
   - Adds test summary to pull request
   - First 2000 characters of test output
   - Only for pull requests

9. **Notify on Failure**
   - Sets CI status to failed
   - Visible in PR/commit status

**Environment Variables:**
```
MAVEN_OPTS=-Xmx1024m
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/leadflow_test
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

---

### 3. Deploy Workflow

**File:** `.github/workflows/deploy.yml`

**Trigger:**
- Push to: `master`, `main`
- Manual trigger (workflow_dispatch) with environment selection

**Supported Environments:**
- `staging` - Staging deployment
- `production` - Production deployment

**Steps:**

1. **Checkout Code**

2. **Setup JDK 21**

3. **Build Application**
   ```bash
   mvn clean package -DskipTests
   ```

4. **Setup Docker Buildx**
   - Multi-platform build support

5. **Docker Registry Login**
   ```
   Registry: $DOCKER_REGISTRY
   Username: $DOCKER_USERNAME
   Password: $DOCKER_PASSWORD
   ```

6. **Build and Push Docker Image**
   ```bash
   Tags:
   - $REGISTRY/leadflow-backend:$COMMIT_SHA
   - $REGISTRY/leadflow-backend:latest
   ```
   - Uses GitHub Actions cache
   - Reduces build time on subsequent runs

7. **Setup Kubernetes**
   - Downloads kubectl

8. **Configure kubeconfig**
   ```bash
   echo $KUBE_CONFIG | base64 -d > ~/.kube/config
   ```

9. **Update Deployment Manifests**
   ```bash
   kubectl set image deployment/leadflow-backend \
     -n $ENVIRONMENT \
     leadflow=$REGISTRY/leadflow-backend:$COMMIT_SHA
   ```

10. **Wait for Rollout**
    ```bash
    kubectl rollout status deployment/leadflow-backend \
      -n $ENVIRONMENT \
      --timeout=5m
    ```
    - Waits for all pods to be ready
    - 5-minute timeout

11. **Run Smoke Tests**
    ```bash
    Health check URL: https://api-$ENV.leadflow.io/api/actuator/health
    Retries: 30 attempts, 10-second interval = 5 minutes
    ```
    - Verifies health endpoint
    - Confirms deployment success

12. **Slack Notification** (success)
    - Formatted message with commit SHA
    - Deployment environment
    - Author information

13. **Slack Notification** (failure)
    - Error indication
    - Link to workflow logs

14. **Create Deployment Record**
    - JSON artifact with deployment metadata

15. **Upload Deployment Summary**
    - 30-day retention
    - Useful for deployment tracking

16. **Automatic Rollback** (production only)
    - Reverts to previous Kubernetes deployment
    - Only triggered on production failure
    - Notifies Slack

**Secrets Required:**
- `DOCKER_REGISTRY` - Docker registry URL
- `DOCKER_USERNAME` - Docker registry username
- `DOCKER_PASSWORD` - Docker registry password
- `KUBE_CONFIG` - Base64-encoded kubeconfig file
- `SLACK_WEBHOOK_URL` - Slack webhook for notifications

**Environment Configuration:**
- Each environment (`staging`, `production`) needs GitHub environment configured
- Set approval requirements for production deployments

---

### 4. Security Workflow

**File:** `.github/workflows/security.yml`

**Trigger:**
- Push to: `master`, `main`, `develop`
- Pull requests to: `master`, `main`, `develop`
- Scheduled: Daily at 2 AM UTC

**Security Scans:**

1. **Dependency Check**
   - Maven dependency vulnerability scanning
   - NVD database
   - Experimental features enabled
   - Retired dependency detection

2. **OWASP ZAP**
   - Web application security scanner
   - Active scanning against staging
   - Only on master branch (after merge)
   - Requires staging URL in secrets

3. **Trivy**
   - Container and filesystem scanning
   - SARIF format for GitHub security tab
   - Automatically uploaded to security tab

4. **GitLeaks**
   - Detects hardcoded secrets
   - Prevents credential leakage
   - Scans entire repository

5. **Maven License Check**
   - License compliance validation
   - Excludes test dependencies
   - Detects GPL/incompatible licenses

6. **SBOM Generation**
   - Software Bill of Materials (SPDX format)
   - Dependency inventory
   - Supply chain security

**Artifacts Generated:**
- `dependency-check-report/` - Full dependency report
- `zap-security-report/` - Web vulnerability report
- `sbom-spdx/bom.xml` - Software bill of materials
- Trivy results uploaded to Security tab

**Slack Notifications:**
- Critical issues on scheduled runs
- Sends to `SLACK_SECURITY_WEBHOOK_URL`
- Only on failures

---

## GitHub Secrets Setup

### Required Secrets

Set these secrets in GitHub Repository Settings → Secrets and variables → Actions:

```yaml
# Docker Registry
DOCKER_REGISTRY: docker.io
DOCKER_USERNAME: <your-docker-username>
DOCKER_PASSWORD: <your-docker-token>

# Kubernetes
KUBE_CONFIG: <base64-encoded-kubeconfig>

# SonarQube
SONARQUBE_HOST_URL: https://sonarqube.example.com
SONARQUBE_TOKEN: <sonarqube-token>

# Slack
SLACK_WEBHOOK_URL: https://hooks.slack.com/services/...
SLACK_SECURITY_WEBHOOK_URL: https://hooks.slack.com/services/...

# Staging
STAGING_URL: https://api-staging.leadflow.io
```

### Generating Secrets

**Base64-encoded kubeconfig:**
```bash
base64 -i ~/.kube/config > kubeconfig.b64
cat kubeconfig.b64  # Copy to GitHub secret
```

**Docker Token:**
- Go to Docker Hub → Account Settings → Security
- Create access token
- Use token as password

**Slack Webhook:**
- Go to Slack workspace → Apps → Incoming Webhooks
- Create new webhook for #deployments channel
- Copy webhook URL

---

## GitHub Environments

Create two environments for production safety:

### Staging Environment
```yaml
# Settings → Environments → staging
Name: staging
Deployment branches: main, master
```

### Production Environment
```yaml
# Settings → Environments → production
Name: production
Deployment branches: main, master
Required reviewers: @security-team, @devops-team
```

---

## Usage Guide

### Automatic Deployments

```bash
# Push to master/main - automatically deploys to staging
git push origin main
# → Build → Test → Security → Deploy to Staging

# Manual production deployment
# In GitHub Actions tab:
# 1. Select "Deploy" workflow
# 2. Click "Run workflow"
# 3. Select "production" environment
# 4. Requires approval from security/devops team
```

### Manual CI Trigger

```bash
# Re-run specific workflow
GitHub → Actions → Select workflow → Re-run jobs
```

### Checking Build Status

```bash
# In GitHub - Check commit status
- Green checkmark = All workflows passed
- Red X = Workflow failed
- Yellow circle = In progress

# Click details for full logs
```

---

## Monitoring & Debugging

### View Workflow Logs

1. **Push Notifications**
   - GitHub: Commit status on PR/commit
   - Slack: Automated notifications
   - Email: Optional GitHub notifications

2. **Accessing Logs**
   ```
   GitHub → Actions → Select workflow → Select run → View logs
   ```

3. **Common Issues**

   **Build Failure:**
   - Check "Build" step logs
   - Look for compilation errors
   - Verify pom.xml dependencies

   **Test Failure:**
   - Check "Test" step logs
   - Review test reports artifact
   - Verify database connectivity

   **Security Scan Issues:**
   - Check vulnerability details
   - Review Dependency Check report
   - Assess false positives

   **Deployment Failure:**
   - Check kubectl commands output
   - Verify Kubernetes connectivity
   - Confirm image exists in registry
   - Check health check endpoint

### Performance Metrics

**Typical Execution Times:**
- Build: 3-5 minutes
- Tests: 5-8 minutes
- Security scans: 3-5 minutes
- Docker build: 2-3 minutes
- Deployment: 2-5 minutes (including rollout wait)

**Total pipeline:** ~20-30 minutes

---

## Rollback Procedures

### Automatic Rollback (Production)

Triggered automatically on:
1. Deployment fails
2. Smoke tests fail
3. Health check fails

```bash
kubectl rollout undo deployment/leadflow-backend -n production
```

### Manual Rollback

**Via GitHub Actions:**
```bash
# In Actions tab → Deploy workflow
# Trigger with previous COMMIT_SHA
```

**Via kubectl directly:**
```bash
# View rollout history
kubectl rollout history deployment/leadflow-backend -n production

# Rollback to previous version
kubectl rollout undo deployment/leadflow-backend -n production

# Rollback to specific revision
kubectl rollout undo deployment/leadflow-backend -n production --to-revision=5
```

---

## Security & Best Practices

### Secrets Management
- ✅ Never commit secrets
- ✅ Use GitHub Secrets for sensitive data
- ✅ Rotate tokens/passwords regularly
- ✅ Use environment-specific secrets
- ✅ GitLeaks scans for accidental exposure

### RBAC & Approvals
- ✅ Require reviews for master branch
- ✅ Production deployments require approval
- ✅ Security team must review security scans
- ✅ Staging for pre-production validation

### Artifact Retention
- Build logs: 7 days
- Test results: 30 days
- Deployment summaries: 30 days
- Reports: 30 days

### Vulnerability Management
- Daily security scans (scheduler)
- Immediate notifications on critical issues
- SBOM for supply chain tracking
- License compliance checks

---

## Metrics & Observability

### Build Metrics

**Success Rate:**
```
GitHub Actions → Workflows → Build
View: Success rate over 30 days
Target: > 95%
```

**Failure Trends:**
- Identify common failure patterns
- Track regressions
- Monitor build duration trends

### Deployment Metrics

**Deployment Frequency:**
- Measure: Deployments per day/week
- Target: Multiple times per day
- Trend: Should increase with pipeline maturity

**Lead Time for Changes:**
- Measure: Time from commit to production
- Target: < 1 hour
- SLA: < 2 hours

**Mean Time to Recovery (MTTR):**
- Measure: Time to rollback on failure
- Target: < 5 minutes
- Automated rollback helps achieve this

**Change Failure Rate:**
- Measure: % of deployments requiring hotfixes
- Target: < 15%
- Reduce via better testing

---

## Troubleshooting

### Build Fails with "Java out of memory"

**Solution:**
```yaml
# Increase heap in workflow
env:
  MAVEN_OPTS: -Xmx2048m
```

### Docker Push Fails "Authentication required"

**Solution:**
```bash
# Verify secrets are set
# Settings → Secrets → DOCKER_PASSWORD
# Try re-creating Docker token
```

### Kubernetes Deployment Timeout

**Solution:**
```bash
# Check pod status
kubectl get pods -n staging
kubectl describe pod <pod-name> -n staging

# Check resource limits
kubectl top nodes
kubectl top pods -n staging

# Increase timeout in workflow (see deploy.yml line 109)
```

### Health Check Fails "Connection refused"

**Solution:**
```bash
# Verify application startup
kubectl logs -n staging -l app=leadflow-backend

# Check service exposure
kubectl get svc -n staging

# Test endpoint manually
curl https://api-staging.leadflow.io/api/actuator/health
```

### Security Scan "False Positive" CVEs

**Solution:**
```bash
# Review in Dependency Check UI
# Check if CVE applies to our configuration
# Create suppression rule if necessary
# Add to pom.xml:
<suppressions>
  <suppression>
    <cve>CVE-XXXX-XXXXX</cve>
  </suppression>
</suppressions>
```

---

## Integration Points

### Pre-commit Hook

Add local validation before push:
```bash
# .git/hooks/pre-commit
#!/bin/bash
mvn clean compile checkstyle:check
```

### IDE Integration

**VS Code:**
- Install "GitHub Actions" extension
- View workflow status in sidebar

**IntelliJ IDEA:**
- Built-in GitHub Actions support
- View workflow history

### External Tools

**Datadog/New Relic:**
- Deployments can create events
- Track correlation with error rates

**PagerDuty:**
- Trigger incidents on deployment failures
- Escalate to on-call engineer

**Confluence/Jira:**
- Auto-update deployment status
- Link commits to tickets

---

## Future Enhancements

1. **Canary Deployments**
   - Route small % of traffic to new version
   - Monitor metrics before full rollout

2. **Blue-Green Deployments**
   - Maintain two production environments
   - Switch traffic during deployment

3. **Cost Optimization**
   - Cache strategies
   - Parallel job execution
   - Self-hosted runners

4. **Advanced Scanning**
   - DAST (Dynamic Application Security Testing)
   - IAST (Interactive Application Security Testing)
   - Fuzzing

5. **Observability**
   - Metrics dashboards
   - Log aggregation
   - Trace correlation

---

## Support & Reference

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)

---

**Last Updated:** 2024
**Maintained By:** DevOps Team
**Document Version:** 1.0
