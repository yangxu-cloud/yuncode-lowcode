# Git Workflow Rules

## Commits
- Write imperative commit messages: "Add feature" not "Added feature"
- Keep the subject line under 72 characters
- Reference issue numbers when relevant: "Fix login bug (#42)"
- Never commit secrets, credentials, or .env files

## Branches
- Feature branches: `feat/short-description`
- Bug fixes: `fix/short-description`
- Never push directly to main or master

## Pull Requests
- Keep PRs focused — one logical change per PR
- Include a description of what changed and why
- All tests must pass before merging
