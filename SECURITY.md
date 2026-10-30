# Security

- Please report vulnerabilities privately through **Security → Report a vulnerability** on GitHub, not in public issues.
- App files are untrusted input; `tools/validate.py` rejects anything outside the documented format before any build.
- CI never receives signing credentials: Apple Watch builds are unsigned, Wear OS builds use a throwaway debug key, and fork pull requests run read-only.
