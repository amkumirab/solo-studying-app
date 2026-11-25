# Solo Studying branding

The approved image is the color master. Do not resize or re-encode it in place.
The distribution assets use the same artwork at different scales. They remove
the outer white matte and blue plate outline without changing the content of the
gateway, book, or arrows.

| Asset | Size | Use |
| --- | --- | --- |
| [Master PNG](logo-master.png) | 1254 × 1254 px | Exact original, including its original framing and corners |
| [Play Store icon](play-store-icon.png) | 512 × 512 px | Full-square 32-bit sRGB PNG; the Play Store supplies its own mask and shadow |
| [Adaptive foreground](android/adaptive-foreground.png) | 432 × 432 px | 108 dp layer with square canvas, transparent padding, and a centered mark |
| [Adaptive background](android/adaptive-background.png) | 432 × 432 px | Solid navy matching the Android color resource |
| [Monochrome](android/monochrome.svg) | 108 × 108 viewport | Authored vector paths, also packaged as an Android VectorDrawable for themed icons |
| [README mark](readme-logo.png) | 256 × 256 px | Documentation and project pages |

The adaptive foreground has a transparent perimeter. It deliberately retains
the navy shadows and lighting within the artwork rather than attempting a
fully background-free cutout. The onboarding image is a 320 × 320 px crop-free
version of that same artwork with transparent edges.

## Android

API 26+ launchers apply their own shapes to the color layers. Compatible
themed launchers can use the separate monochrome layer. Legacy launcher and
round fallbacks are supplied at ldpi, mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi
(36, 48, 72, 96, 144 and 192 px). Android 12+ uses the new launcher icon
on the native splash with a navy background. Older Android versions retain
the existing startup behavior. No extra startup activity is used.

Regenerate the assets with Python and Pillow (WebP support required):

```sh
python scripts/generate_branding.py
python scripts/generate_branding.py --check
```

The script checks the master SHA-256 and never writes to the master.

## Integration and verification

- `AndroidManifest.xml` keeps the application and round icon IDs and points only
  the main launcher Activity at the branded starting theme. The alarm Activity
  and Compose app theme are unchanged.
- `OnboardingScreen.kt` and `TutorialScreen.kt` each use the color mark in their
  existing 64 dp and 80 dp slots. There was no separate About or Settings logo
  to replace. The project README uses the documentation-sized mark.
- The Java/Kotlin resource tests check both adaptive IDs, the separate themed
  layer and the native splash theme. The Compose tests verify both logo slots.
- Debug and E2E APKs built successfully; all 617 local tests passed. Lint
  reported zero errors and seven pre-existing warnings.
- On the API 36 test emulator, the launcher and native splash were visually
  verified in light and dark system modes, and both onboarding variants were
  captured. Circle, rounded-square and squircle previews at 36, 48 and 110 px
  verified the composition on light and dark backgrounds.
- Actual wallpaper-tinted launcher rendering, manufacturer launchers, physical
  devices and the pre-26 fallback were not tested on devices. The monochrome
  layer was verified as a packaged drawable rather than as an OEM launcher
  screenshot. The Play icon was not uploaded.

References: [Android adaptive icon specs](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive),
[Android splash screen](https://developer.android.com/develop/ui/views/launch/splash-screen),
and [Play Store icon specs](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).
