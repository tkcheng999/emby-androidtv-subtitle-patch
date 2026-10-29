# Build, install and roll back

## Inputs you must supply

| Input | Tested version / requirement |
|---|---|
| Original APK | Emby for Android TV 2.5.03g, exact SHA-256 below |
| JDK | 17 (production helper targets Java 8 bytecode) |
| Android platform | API 34 `android.jar` |
| Android build tools | 34.0.0: `d8`, `zipalign`, `apksigner` |
| Apktool | 3.0.3 executable/wrapper |
| Host tools | Python 3, POSIX shell, `zip`, `unzip`, `shasum` |
| Device tools | ADB, only for an authorized device |
| Signing | Your own local keystore; keep the same key for updates |

Original APK digest:

```text
0246821852b9a7b6e1448f751601c0040c1f3a9de80c4ea778fb2ba67a16d147
```

The version string alone is insufficient. A differently packaged APK can have different method/register shapes. Obtain your own copy through an appropriate distribution channel; this repository does not provide one or download one automatically. Do not bypass the digest check just to make another version build.

## 1. Run the host suite

```sh
sh tests/run_host_tests.sh
```

This needs no Android SDK, television, network account or original APK. The script compiles the Java helper with **host-only stubs** into `build/host-tests` and runs the Python injection tests. Production builds never use those stubs.

## 2. Prepare private signing material

Example for Bash; enter a new local password at the prompt. It is unrelated to your Emby account. This example deliberately contains no fixed password.

```bash
mkdir -p "$HOME/.local/share/emby-subtitle-patch"
chmod 700 "$HOME/.local/share/emby-subtitle-patch"
export KEYSTORE="$HOME/.local/share/emby-subtitle-patch/signing.p12"
export KEY_ALIAS=subtitle-patch
read -r -s -p 'Local APK signing password: ' EMBY_SIGNING_PASSWORD
printf '\n'
export EMBY_SIGNING_PASSWORD
export KS_PASS_ENV=EMBY_SIGNING_PASSWORD

# Run once, only if you do not already have the signing key you need.
keytool -genkeypair -storetype PKCS12 -keystore "$KEYSTORE" \
  -alias "$KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 3650 \
  -dname 'CN=Local Subtitle Patch' -storepass:env EMBY_SIGNING_PASSWORD
chmod 600 "$KEYSTORE"
```

Do not regenerate the key for every build. Android updates require the existing application's signing identity. Another person's key cannot update the project owner's installed package. Uninstalling to work around a mismatch would remove app data; it is not part of the normal update procedure.

## 3. Configure tools and build

Place the original APK at repository-root `original.apk` (Git ignores it). Configure executable paths for your machine, for example:

```bash
export JAVA_HOME='/path/to/jdk-17'
export PATH="$JAVA_HOME/bin:$PATH"
export APKTOOL='/path/to/apktool'
export ANDROID_JAR='/path/to/android-sdk/platforms/android-34/android.jar'
export BUILD_TOOLS='/path/to/android-sdk/build-tools/34.0.0'

# KEYSTORE, KEY_ALIAS, KS_PASS_ENV and the named password variable must already be set.
sh build.sh
unset EMBY_SIGNING_PASSWORD
```

`APKTOOL` must be a single executable path, not a string such as `java -jar ...`. If needed, make a local executable wrapper that runs `java -jar /path/to/apktool.jar "$@"`.

Output: `build/emby-subtitle-test.apk`.

The pipeline:

1. Checks the original digest.
2. Removes prior generated decoded/classes/dex/unsigned/aligned output.
3. Decodes the original APK.
4. Runs `patch.py` against the clean decoded tree.
5. Copies the approved banner into `res/drawable/app_icon.png`.
6. Compiles only `src/local/emby/subtitles/*.java` against `android.jar`.
7. Uses D8 to produce helper DEX with minimum API 24.
8. Rebuilds native resources/smali, appends the helper as the next `classesN.dex`.
9. Runs zipalign, signs using the password environment variable, and verifies the signature.

The tested original APK reports minSdk 24. Only Android 11 on the original TV was physically tested. “Min API 24” is not evidence of broad device compatibility.

### Optional diagnostic build

```bash
EMBY_TRACE_CUES=1 sh build.sh
```

This injects the `SubtitleView` cue hook. The installed acceptance build had this enabled. Without it, the hook is not injected; the helper class may still be packaged. The full acceptance build and a build without diagnostics are not byte-identical.

### Reproducible vs byte-identical

The workflow is repeatable from a pinned input and uses idempotent injection. It does **not** promise bit-for-bit signed APK reproduction across keys, tool versions, ZIP metadata or timestamps. Compare extracted DEX and resource entries when checking code-equivalence; historical signed hashes in `ARTIFACTS.json` identify the original deployment only.

## 4. Install without clearing app data

Set `TV_SERIAL` to your own authorized device or ADB serial. No real network address is provided here.

```bash
export TV_SERIAL='YOUR_ADB_SERIAL'
adb -s "$TV_SERIAL" install -r build/emby-subtitle-test.apk
```

The independent package is `local.emby.subtitletest`. Its displayed name remains **Emby 字幕测试版**; the banner reads **Emby / free subtitle**. The square application icon is unchanged; the Android TV banner is the approved new artwork. A custom launcher can override app banners, so verify the actual launcher tile.

Normal `install -r` with the same signing key retains application data. Do not use `pm clear` or uninstall the working package. Open the app yourself and verify the library, a new title, automatic subtitles, manual switching and Off.

The official Emby package can coexist. Shared deep-link filters may present an Android chooser; use explicit components for authorized automation when necessary. Do not uninstall other applications as a generic troubleshooting step.

## 5. Rollback

Before replacing a working build, retain its APK outside Git. Reinstall that earlier APK with the same key using `adb install -r`. If Android refuses a downgrade because version metadata differs, investigate the specific build; do not automatically wipe app data. The initial icon-only update kept all DEX, manifest and non-banner payload entries unchanged, and its prior APK was retained locally.

## 6. Common failures

| Failure | Action |
|---|---|
| Original digest mismatch | Confirm exact artifact; stop rather than relax validation |
| Unexpected smali shape | Inspect the target version and hook contract; do not insert by approximate matches |
| Missing Java/SDK executable | Verify the environment paths above |
| Signature mismatch on install | Use the key that signed the existing independent app |
| Subtitle menu still empty before playback | Check server metadata; metadata-empty sources are supported during playback, not preplay discovery |
| Video cannot open a private/direct URL | Diagnose routing and server delivery separately; this patch adds no proxy |
| New banner not visible | Check launcher override/cache; do not clear Emby data |
