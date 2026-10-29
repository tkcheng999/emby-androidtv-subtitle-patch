"""Host tests for patch.py (not run by the reviewer). Usage: python3 -I tests/test_patch.py"""
import importlib.util
import shutil
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location('emby_patch', ROOT / 'patch.py')
patch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patch)

PRISTINE_SMALI = '''.class public Ltv/emby/embyatv/playback/PlaybackController;
.method private addCCStreamIfNeeded()Z
    .locals 6

    .line 1674
    const/4 v0, 0x0
    return v0
.end method
.method public getSubtitleStreamIndex()I
    .locals 1

    .line 217
    const/4 v0, -0x1
    return v0
.end method
.method public switchSubtitleStream(I)V
    .locals 20

    move-object/from16 v0, p0
    return-void
.end method
'''

PRISTINE_MANIFEST = '''<?xml version="1.0" encoding="utf-8" standalone="no"?><manifest xmlns:android="http://schemas.android.com/apk/res/android" package="tv.emby.embyatv">
    <permission android:name="tv.emby.embyatv.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" android:protectionLevel="signature"/>
    <uses-permission android:name="tv.emby.embyatv.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
    <application android:icon="@drawable/logoicon512" android:label="@string/app_name" android:name="tv.emby.embyatv.TvApp">
        <activity android:label="@string/app_name" android:name="tv.emby.embyatv.startup.StartupActivity"/>
        <provider android:authorities="tv.emby.embyatv.recommendations" android:name="tv.emby.embyatv.integration.RecommendationContentProvider"/>
        <provider android:authorities="tv.emby.embyatv.androidx-startup" android:name="androidx.startup.InitializationProvider"/>
    </application>
</manifest>
'''

CONSTANT_SMALI = '''.class public Ltv/emby/embyatv/integration/RecommendationContentProvider;
.field public static final CONTENT_URI:Ljava/lang/String; = "content://tv.emby.embyatv.recommendations/"
'''


class PatchTest(unittest.TestCase):
    def setUp(self):
        self.dir = Path(tempfile.mkdtemp())

    def tearDown(self):
        shutil.rmtree(self.dir)

    def tree(self, smali, manifest):
        playback = self.dir / 'smali_classes2/tv/emby/embyatv/playback'
        playback.mkdir(parents=True)
        (playback / 'PlaybackController.smali').write_text(smali, encoding='utf-8')
        (playback / 'VideoManager.smali').write_text((ROOT / 'tests/fixtures/VideoManager.smali').read_text())
        (self.dir / 'AndroidManifest.xml').write_text(manifest, encoding='utf-8')
        decoder = self.dir / 'smali/com/google/android/exoplayer2/text/SubtitleDecoderFactory$1.smali'
        decoder.parent.mkdir(parents=True)
        decoder.write_text((ROOT / 'tests/fixtures/SubtitleDecoderFactory.smali').read_text())
        spinner = self.dir / 'smali_classes2/tv/emby/embyatv/ui/MediaSelectorSpinner.smali'
        spinner.parent.mkdir(parents=True)
        spinner.write_text((ROOT / 'tests/fixtures/MediaSelectorSpinner.smali').read_text())
        integration = self.dir / 'smali/tv/emby/embyatv/integration'
        integration.mkdir(parents=True)
        (integration / 'RecommendationContentProvider.smali').write_text(CONSTANT_SMALI, encoding='utf-8')

    def snapshot(self):
        return {str(p.relative_to(self.dir)): p.read_text(encoding='utf-8') for p in sorted(self.dir.rglob('*')) if p.is_file()}

    def test_pristine_then_repeat_is_stable(self):
        self.tree(PRISTINE_SMALI, PRISTINE_MANIFEST)
        patch.main(self.dir)
        first = self.snapshot()
        patch.main(self.dir)
        self.assertEqual(first, self.snapshot())
        smali = first['smali_classes2/tv/emby/embyatv/playback/PlaybackController.smali']
        for name in ('prepare', 'current', 'select'):
            self.assertEqual(smali.count('Llocal/emby/subtitles/Bridge;->' + name + '('), 1)
        manifest = first['AndroidManifest.xml']
        self.assertIn('package="local.emby.subtitletest"', manifest)
        self.assertNotIn('"tv.emby.embyatv.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"', manifest)
        self.assertIn('android:authorities="local.emby.subtitletest.recommendations"', manifest)
        self.assertIn('android:authorities="local.emby.subtitletest.androidx-startup"', manifest)
        self.assertIn('<application android:icon="@drawable/logoicon512" android:label="Emby 字幕测试版"', manifest)
        self.assertIn('<activity android:label="@string/app_name"', manifest, 'only the application label changes')
        self.assertIn('android:name="tv.emby.embyatv.TvApp"', manifest, 'implementation class names are kept')
        self.assertIn('"content://local.emby.subtitletest.recommendations/"',
                      first['smali/tv/emby/embyatv/integration/RecommendationContentProvider.smali'])

    def test_already_injected_controller_and_manifest_are_unchanged(self):
        # Synthetic fixture: repeatability of the injection contract, not upstream APK compatibility.
        smali = PRISTINE_SMALI
        for header, hook in patch.HOOKS:
            smali = smali.replace(header, header + hook)
        manifest = patch.patch_manifest(PRISTINE_MANIFEST)
        self.assertEqual(patch.patch_controller(smali), smali)
        self.assertEqual(patch.patch_manifest(manifest), manifest)

    def test_pgs_decoder_advertised_without_changing_decoder_implementation(self):
        original = (ROOT / 'tests/fixtures/SubtitleDecoderFactory.smali').read_text()
        result = patch.patch_decoder_factory(original)
        self.assertIn('patch_pgs_original', result)
        self.assertEqual(result, patch.patch_decoder_factory(result))
        original_decoder = original.split('.method public supportsFormat')[0]
        self.assertEqual(original_decoder, result.split('.method public supportsFormat')[0])
        self.assertIn('Lcom/google/android/exoplayer2/text/pgs/PgsDecoder;-><init>()V', result)

    def test_only_explicit_preplay_popup_click_is_hooked(self):
        original = (ROOT / 'tests/fixtures/MediaSelectorSpinner.smali').read_text()
        result = patch.patch_preplay_spinner(original)
        self.assertEqual(result, patch.patch_preplay_spinner(result))
        self.assertEqual(result.count('Bridge;->preplayClick'), 1)
        method = result.split('.method synthetic lambda$performClick$2')[1].split('.end method')[0]
        self.assertIn('Bridge;->preplayClick', method)
        self.assertIn('->setSelection(I)V', method)

    def test_external_mapping_hook_is_idempotent(self):
        original = (ROOT / 'tests/fixtures/VideoManager.smali').read_text()
        result = patch.patch_external_mapping(original)
        self.assertEqual(result, patch.patch_external_mapping(result))
        self.assertEqual(result.count('Bridge;->selectExternal'), 1)

    def test_unexpected_shape_fails(self):
        self.tree(PRISTINE_SMALI.replace('    .locals 20', '    .locals 21'), PRISTINE_MANIFEST)
        with self.assertRaises(SystemExit):
            patch.main(self.dir)


if __name__ == '__main__':
    unittest.main()
