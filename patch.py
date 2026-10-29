"""Patch an apktool-decoded Emby Android TV 2.5.03g tree for the subtitle test build.

Usage: python3 patch.py [DECODED_DIR]   (default: ./decoded)

Idempotent: pristine input and output of this script (or of the earlier version, whose
injected smali is identical) both end in the same state. Anything else fails loudly.
"""
import re
import os
import sys
from pathlib import Path

OLD = 'tv.emby.embyatv'
NEW = 'local.emby.subtitletest'
LABEL = 'Emby 字幕测试版'

# (pristine method header, injected hook). Bridge signatures must stay in sync with src/local/emby/subtitles/Bridge.java.
HOOKS = [
    ('.method private addCCStreamIfNeeded()Z\n    .locals 6',
     '\n\n    invoke-static {p0}, Llocal/emby/subtitles/Bridge;->prepare(Ljava/lang/Object;)V'),
    ('.method public getSubtitleStreamIndex()I\n    .locals 1',
     '\n\n    invoke-static {p0}, Llocal/emby/subtitles/Bridge;->current(Ljava/lang/Object;)I\n'
     '    move-result v0\n    if-ltz v0, :patch_original_index\n    return v0\n    :patch_original_index'),
    ('.method public switchSubtitleStream(I)V\n    .locals 20',
     '\n\n    invoke-static/range {p0 .. p1}, Llocal/emby/subtitles/Bridge;->select(Ljava/lang/Object;I)Z\n'
     '    move-result v0\n    if-eqz v0, :patch_original_select\n    return-void\n    :patch_original_select'),
]

# Providers renamed in the manifest; code constants that address them must follow.
AUTHORITIES = ('recommendations', 'search')


def patch_controller(text):
    for header, hook in HOOKS:
        done = text.count(header + hook)
        if done == 1:
            continue
        if done > 1 or text.count(header) != 1:
            raise SystemExit('unexpected PlaybackController.smali shape at: ' + header.splitlines()[0])
        text = text.replace(header, header + hook)
    return text


def patch_manifest(text):
    text = text.replace('package="%s"' % OLD, 'package="%s"' % NEW)
    # Custom permissions are global on the device: two packages may not both define the same name.
    text = re.sub(r'(<(?:uses-)?permission\b[^>]*\bandroid:name=")' + re.escape(OLD) + r'\.',
                  lambda m: m.group(1) + NEW + '.', text)
    text = text.replace(OLD + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION', NEW + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION')
    text = re.sub(r'android:authorities="([^"]+)"',
                  lambda m: 'android:authorities="' + m.group(1).replace(OLD, NEW) + '"', text)
    text, labels = re.subn(r'(<application\b[^>]*?\sandroid:label=")[^"]*(")',
                           lambda m: m.group(1) + LABEL + m.group(2), text, count=1)
    if 'package="%s"' % NEW not in text or labels != 1:
        raise SystemExit('unexpected AndroidManifest.xml shape')
    return text


def patch_authority_constants(text):
    for name in AUTHORITIES:
        text = text.replace('"content://%s.%s' % (OLD, name), '"content://%s.%s' % (NEW, name))
        text = text.replace('"%s.%s"' % (OLD, name), '"%s.%s"' % (NEW, name))
    return text


def patch_decoder_factory(text):
    # This APK already constructs PgsDecoder in createDecoder, but omits its MIME
    # from supportsFormat. Advertise that existing decoder as a native fallback.
    header = '.method public supportsFormat(Lcom/google/android/exoplayer2/Format;)Z\n    .locals 1'
    hook = '''.method public supportsFormat(Lcom/google/android/exoplayer2/Format;)Z
    .locals 2

    iget-object v0, p1, Lcom/google/android/exoplayer2/Format;->sampleMimeType:Ljava/lang/String;
    const-string v1, "application/pgs"
    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :patch_pgs_original
    return v0
    :patch_pgs_original'''
    if text.count(hook) == 1:
        return text
    if text.count(header) != 1 or 'Lcom/google/android/exoplayer2/text/pgs/PgsDecoder;-><init>()V' not in text:
        raise SystemExit('unexpected SubtitleDecoderFactory shape or missing PGS decoder')
    return text.replace(header, hook)


def patch_preplay_spinner(text):
    header = '.method synthetic lambda$performClick$2$tv-emby-embyatv-ui-MediaSelectorSpinner(Landroid/widget/PopupWindow;ILandroid/view/View;)V\n    .locals 0'
    hook = '\n\n    invoke-static {p0, p2}, Llocal/emby/subtitles/Bridge;->preplayClick(Ljava/lang/Object;I)V'
    if text.count(header + hook) == 1:
        return text
    if text.count(header) != 1:
        raise SystemExit('unexpected MediaSelectorSpinner row click shape')
    return text.replace(header, header + hook)


def patch_external_mapping(text):
    header = '.method private enabledSubtitleTrack(Ljava/util/List;IZ)V\n    .locals 7'
    hook = '\n\n    invoke-static {p0, p1, p2, p3}, Llocal/emby/subtitles/Bridge;->selectExternal(Ljava/lang/Object;Ljava/util/List;IZ)Z\n    move-result v0\n    if-eqz v0, :patch_external_original\n    return-void\n    :patch_external_original'
    if text.count(header + hook) == 1:
        return text
    if text.count(header) != 1:
        raise SystemExit('unexpected VideoManager subtitle mapping shape')
    return text.replace(header, header + hook)


def rewrite(path, fn):
    old = path.read_text(encoding='utf-8')
    new = fn(old)
    if new != old:
        path.write_text(new, encoding='utf-8')
        return True
    return False


def main(decoded):
    controllers = sorted(decoded.glob('smali*/tv/emby/embyatv/playback/PlaybackController.smali'))
    if len(controllers) != 1:
        raise SystemExit('expected one PlaybackController.smali, found %d' % len(controllers))
    changed = [str(controllers[0])] if rewrite(controllers[0], patch_controller) else []
    manifest = decoded / 'AndroidManifest.xml'
    if rewrite(manifest, patch_manifest):
        changed.append(str(manifest))
    factories = sorted(decoded.glob('smali*/com/google/android/exoplayer2/text/SubtitleDecoderFactory$1.smali'))
    if len(factories) != 1:
        raise SystemExit('expected one SubtitleDecoderFactory implementation')
    if rewrite(factories[0], patch_decoder_factory):
        changed.append(str(factories[0]))
    spinners = sorted(decoded.glob('smali*/tv/emby/embyatv/ui/MediaSelectorSpinner.smali'))
    if len(spinners) != 1:
        raise SystemExit('expected one MediaSelectorSpinner')
    if rewrite(spinners[0], patch_preplay_spinner):
        changed.append(str(spinners[0]))
    managers = sorted(decoded.glob('smali*/tv/emby/embyatv/playback/VideoManager.smali'))
    if len(managers) != 1:
        raise SystemExit('expected one VideoManager')
    if rewrite(managers[0], patch_external_mapping):
        changed.append(str(managers[0]))
    if os.environ.get('EMBY_TRACE_CUES') == '1':
        views = list(decoded.glob('smali*/com/google/android/exoplayer2/ui/SubtitleView.smali'))
        if len(views) != 1:
            raise SystemExit('expected one SubtitleView')
        text = views[0].read_text()
        needle = '    iput-object p1, p0, Lcom/google/android/exoplayer2/ui/SubtitleView;->cues:Ljava/util/List;'
        hook = '    invoke-static {p0, p1}, Llocal/emby/subtitles/Diagnostics;->cues(Ljava/lang/Object;Ljava/util/List;)V\n\n'
        if needle not in text:
            raise SystemExit('unexpected SubtitleView')
        if hook not in text:
            views[0].write_text(text.replace(needle, hook + needle))
    for smali in sorted(decoded.glob('smali*/**/*.smali')):
        if rewrite(smali, patch_authority_constants):
            changed.append(str(smali))
    print('patched %d file(s)' % len(changed))
    for name in changed:
        print('  ' + name)


if __name__ == '__main__':
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent / 'decoded')
