# Robin AI Code Review Setup Guide

This document explains the architecture, credentials, and installation requirements for the automated **Robin AI Code Review** workflow in CodeMateX ([.github/workflows/robin.yml](../.github/workflows/robin.yml)).

---

## 1. Overview

CodeMateX uses [Robin](https://github.com/antongulin/robin) (`v2.10.2`) to provide automated, agentic AI code reviews on incoming pull requests and on-demand slash commands.

* **Trigger Events**:
  * Pull requests (`opened`, `reopened`, `ready_for_review`).
  * Issue comments on pull requests starting with `/review`, `/robin`, `/summary`, or `/help` by repository owners, members, or collaborators.
* **Review Persona & Rules**: Defined in [.github/code-reviewer.md](../.github/code-reviewer.md).
* **Execution Mode**: Agent Mode (`agent-mode: auto`) - Robin inspects changed diffs, reads related definitions, call sites, and repository files via tool calls before submitting reviews.
* **Review Stance**: Non-blocking advisor mode (`request-changes: false`) - Robin comments with actionable feedback without blocking PR merges.

---

## 2. Review Identity: Dedicated GitHub App (`reviewmatex[bot]`)

Reviews are authored and posted by a dedicated GitHub App identity (**`reviewmatex[bot]`**) rather than the generic `github-actions[bot]` or a personal user account.

### Why a GitHub App?
1. **Scoped & Secure**: Uses short-lived (1-hour) installation access tokens minted at runtime and revoked immediately when the job finishes.
2. **Dedicated Persona**: PR reviews and inline suggestions display the custom bot name and avatar with a GitHub verified `[bot]` badge.
3. **No Account Seats**: Does not consume a paid organization seat or require maintaining a separate personal account email/password.
4. **Clean Permissions**: Eliminates self-review conflicts on GitHub where user accounts cannot approve or review their own PRs.

---

## 3. GitHub App Registration

The GitHub App ([reviewmatex](https://github.com/settings/apps/reviewmatex)) must be configured with the following settings:

### Basic Information
* **GitHub App name**: `reviewmatex` (or your preferred bot name)
* **Homepage URL**: `https://github.com/hossain-khan/android-code-with-ai`
* **Webhook**: **Active** unchecked (no webhook endpoint is needed; authentication is initiated from GitHub Actions)

### Required Repository Permissions
The App must be granted the following repository-level permissions:

| Permission | Access | Purpose |
|---|---|---|
| **Contents** | Read-only | Read repository code, diffs, and context files during agent mode |
| **Pull requests** | Read & write | Create PR review comments, status comments, and inline suggestions |
| **Actions** | Read-only | Detect superseded review runs when new commits or comments arrive |

---

## 4. Repository Installation Scope

The GitHub App must be explicitly installed on the target repository:

1. Navigate to the App settings page: `https://github.com/settings/apps/<app-slug>`.
2. Select **Install App** from the left navigation sidebar.
3. Click **Install** next to your account or organization.
4. Under **Repository access**, select **Only select repositories** and pick `android-code-with-ai`.
5. Click **Save** / **Install**.

> [!IMPORTANT]
> If the App is not installed on `android-code-with-ai`, or if any of the three permissions above are missing, `actions/create-github-app-token` will fail to generate an installation token and the review job will fail.

---

## 5. Required Repository Secrets & Variables

The workflow relies on repository-level configuration located under **Settings** -> **Secrets and variables** -> **Actions**:

### Variables (Repository Variables tab)

| Variable Name | Description | Example |
|---|---|---|
| `ROBIN_APP_CLIENT_ID` | The GitHub App Client ID found on the App's **General** settings page | `Iv23...` |

### Secrets (Repository Secrets tab)

| Secret Name | Description | Source |
|---|---|---|
| `ROBIN_APP_PRIVATE_KEY` | Full RSA Private Key (`.pem` file contents) generated in the App's settings | Generated under **Private keys** in App settings |
| `OPENROUTER_API_KEY` | Provider API key for LLM inference | OpenRouter or OpenAI-compatible provider dashboard |
| `LLM_BASE_URL` | OpenAI-compatible endpoint base URL | e.g. `https://openrouter.ai/api/v1` |
| `LLM_MODEL` | Target LLM model name | e.g. `google/gemini-2.5-pro` or equivalent |

---

## 6. Workflow Architecture

The workflow [.github/workflows/robin.yml](../.github/workflows/robin.yml) executes the review in two steps within a single runner job:

```mermaid
sequenceDiagram
    autonumber
    actor PR as Pull Request / Author
    participant GHA as GitHub Actions Runner
    participant AppToken as actions/create-github-app-token
    participant Robin as antongulin/robin
    participant GH as GitHub API

    PR->>GHA: PR opened / ready_for_review / /review comment
    GHA->>AppToken: Request installation token using Client ID & Private Key
    AppToken->>GH: Authenticate via signed JWT
    GH-->>AppToken: Return ephemeral installation token (valid 1 hour)
    AppToken-->>GHA: Output steps.app-token.outputs.token
    GHA->>Robin: Execute Robin action with app token & LLM credentials
    Robin->>GH: Fetch diff & repository context
    Robin->>Robin: LLM evaluation & multi-turn agent inspection
    Robin->>GH: Post PR review as reviewmatex[bot]
    GHA->>AppToken: Post-job cleanup: revoke ephemeral token
```

---

## 7. Troubleshooting & Diagnostics

### Error: `HttpError: Resource not accessible by integration`
* **Cause**: The GitHub App is missing one of the three required permissions (`Contents: read`, `Pull requests: write`, `Actions: read`).
* **Fix**: Visit the App's **Permissions & events** settings, grant the missing permission, save changes, and accept the updated permissions prompt under **Install App**.

### Error: `Integration not found` or `Installation not found`
* **Cause**: The GitHub App is not installed on `android-code-with-ai` or `ROBIN_APP_CLIENT_ID` is incorrect.
* **Fix**: Ensure the App is installed under **Install App** for this repository, and verify that `vars.ROBIN_APP_CLIENT_ID` matches the Client ID displayed on the App's General settings page.

### Error: `error:0909006C:PEM routines:get_name:no start line`
* **Cause**: The `ROBIN_APP_PRIVATE_KEY` secret does not include the full PEM wrapper.
* **Fix**: Paste the complete `.pem` file contents into the secret, including `-----BEGIN RSA PRIVATE KEY-----` and `-----END RSA PRIVATE KEY-----`.
