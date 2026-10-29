# Publication checks — 2026-10-30

The owner explicitly requested a public repository. Publication was prepared from a file whitelist, not the private development workspace history.

- Complete Chinese report and English build/architecture/testing notes written.
- Production Java helper and `patch.py` verified identical to the accepted implementation.
- Host suite: 71 Java assertions and 6 Python tests passed after replacing private reference classes with synthetic structural fixtures.
- Clean production build completed using the pinned original APK, API 34, build-tools 34.0.0, JDK 17, Apktool 3.0.3 and cue tracing enabled.
- Extracted DEX matched the installed accepted build; banner pixels matched. Signed APK byte identity is not promised.
- Independent publication review found one README command-spacing error; it was corrected. No other material issue was reported within that review's scope.
- Gitleaks 8.30.1 scan of the intended tracked-file snapshot found zero leaks. A separate targeted check found no private deployment endpoints, usernames, local user paths or private-key material in candidate text files.
- Initial whole-directory scanning also saw ignored decoded upstream build output and reported generic token-pattern matches there. Those files are not part of the repository. The tracked snapshot was scanned separately rather than allowlisting those patterns.
- The generated banner was inspected for metadata concerns; it contains generation-provenance/public-certificate metadata, not a private signing key or account credential.
- Git uses a GitHub noreply author address. No previous private Git history is imported.
- No original APK, built APK, signing material, raw device logs/screenshots or review-session dumps are published.

The local build used a private signing key outside this repository. Only artifact digests, not that key or its password, appear in the documentation. A successful scanner run is evidence of the performed check, not a guarantee that arbitrary future submissions contain no sensitive information.
