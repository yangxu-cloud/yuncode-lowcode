# Security Rules

## Always
- Never log passwords, tokens, or PII
- Never commit .env files or credentials to git
- Keep dependencies up to date — run `npm audit` / `pnpm audit` regularly
- Use HTTPS for all external requests


## Authentication
- Validate session tokens on every protected route
- Use short-lived JWTs with refresh token rotation
- Implement rate limiting on auth endpoints

## Dependencies
- Prefer well-maintained packages with a small dependency tree
- Check for known vulnerabilities before adding a new package
