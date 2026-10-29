# Privacy and publication boundary

No account is required to run the host tests. Do not submit Emby credentials, access tokens, signed playback URLs, cookies, private keys, raw app-data directories, device addresses, library item IDs or full chat transcripts in issues or pull requests.

This repository was assembled from an explicit file whitelist. It excludes upstream APKs, generated build output, full decompiled classes, historical private plugin packages, raw device logs and screenshots. Even a launcher screenshot can expose a Wi-Fi SSID. Private artifacts should remain outside the repository.

`.gitignore` helps prevent accidental staging but is not a security boundary. Before publishing a change:

```sh
git status --short
git ls-files
gitleaks git . --redact

# For an uncommitted snapshot: scan a clean directory containing only intended files.
# `gitleaks dir .` also sees local ignored build/decompilation output.
```

Review findings locally. Do not paste an unredacted scanner report into GitHub. Also search for actual private account/server strings known to your own deployment; generic scanners may miss usernames, private endpoints and unusual token formats.

Source diagnostics intentionally avoid logging subtitle cue text, but track labels and exception messages can contain sensitive metadata. Sanitize runtime output before sharing it. Use generic placeholders such as `YOUR_ADB_SERIAL` and `https://media.example.invalid` in examples.

If a secret is accidentally published, revoke or rotate it first. Deleting the current file does not remove it from Git history or copies. Coordinate history cleanup separately; do not assume a force-push makes an exposed credential safe again.

No licensing/payment checks or authentication code are changed by this patch. Do not add such changes as part of a subtitle fix.
