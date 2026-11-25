"""Package the approved logo at Android and store sizes (requires Pillow)."""

import argparse
from hashlib import sha256
from io import BytesIO
from math import hypot
from pathlib import Path
from xml.etree import ElementTree
from xml.sax.saxutils import quoteattr

from PIL import Image, ImageDraw, PngImagePlugin


ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "branding"
RES = ROOT / "app/src/main/res"
MASTER_HASH = "4647175356f74f879b2c91466de2879f0e11e7a1788460a742d735d8374daf84"
NAVY = (1, 5, 22, 255)
LANCZOS = Image.Resampling.LANCZOS
DENSITIES = {"ldpi": 36, "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def without_source_frame(master):
    # The supplied frame is baked into RGB. Feather only the perimeter of the
    # rounded plate, staying inside both its white matte and its blue stroke.
    # All interior pixels are kept without tinting or reconstruction.
    size = master.width
    radius = 310
    half = size / 2
    edge = [abs(i + 0.5 - half) - (half - radius) for i in range(size)]
    mask = Image.new("L", master.size)
    mask.putdata([
        round(255 * min(1, max(0, (-distance - 18) / 16)))
        for y in edge
        for x in edge
        for distance in (hypot(max(x, 0), max(y, 0)) + min(max(x, y), 0) - radius,)
    ])
    result = master.convert("RGBA")
    result.putalpha(mask)
    return result


def centered(art, size, width, background):
    result = Image.new("RGBA", (size, size), background)
    inset = (size - width) // 2
    result.alpha_composite(art.resize((width, width), LANCZOS), (inset, inset))
    return result


def icons():
    master_path = BRAND / "logo-master.png"
    if sha256(master_path.read_bytes()).hexdigest() != MASTER_HASH:
        raise ValueError("The approved master PNG was changed")
    with Image.open(master_path) as master:
        art = without_source_frame(master)

    profile = PngImagePlugin.PngInfo()
    profile.add(b"sRGB", b"\x00")
    play = centered(art, 512, 472, NAVY)
    foreground = centered(art, 432, 236, (0, 0, 0, 0))
    background = Image.new("RGBA", (432, 432), NAVY)
    yield BRAND / "play-store-icon.png", play, {"pnginfo": profile}
    yield BRAND / "readme-logo.png", play.resize((256, 256), LANCZOS), {}
    yield BRAND / "android/adaptive-foreground.png", foreground, {}
    yield BRAND / "android/adaptive-background.png", background, {}
    yield RES / "drawable-xxxhdpi/brand_adaptive_foreground.png", foreground, {}
    yield RES / "drawable-xxxhdpi/brand_onboarding.png", art.resize((320, 320), LANCZOS), {}

    # Pre-adaptive launchers need a real legacy round asset. On API 26+
    # neither adaptive resource has a pre-applied outer mask.
    for density, size in DENSITIES.items():
        for round_icon in (False, True):
            canvas = size * 4
            width = round(canvas * (0.81 if round_icon else 0.92))
            icon = centered(art, canvas, width, NAVY)
            if round_icon:
                circle = Image.new("L", icon.size)
                ImageDraw.Draw(circle).ellipse((0, 0, canvas - 1, canvas - 1), fill=255)
                icon.putalpha(circle)
            suffix = "_round" if round_icon else ""
            yield RES / f"mipmap-{density}/ic_launcher{suffix}.webp", icon.resize((size, size), LANCZOS), {}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify dimensions, pixel content, and profile")
    args = parser.parse_args()
    for path, image, options in icons():
        buffer = BytesIO()
        if path.suffix == ".webp":
            image.save(buffer, "WEBP", lossless=True)
        else:
            image.save(buffer, "PNG", optimize=True, **options)
        if path.name == "play-store-icon.png" and buffer.tell() > 1024 * 1024:
            raise ValueError("The Play icon exceeds 1024 KB")
        if args.check:
            with Image.open(path) as actual, Image.open(buffer) as expected:
                if path.name == "play-store-icon.png" and (
                    actual.mode != "RGBA" or path.stat().st_size > 1024 * 1024
                ):
                    raise ValueError("The Play icon must be 32-bit PNG and at most 1024 KB")
                if actual.info.get("srgb") != expected.info.get("srgb"):
                    raise ValueError(f"Unexpected sRGB metadata: {path}")
                if actual.size != expected.size or actual.convert("RGBA").tobytes() != expected.convert("RGBA").tobytes():
                    raise ValueError(f"Unexpected pixels or dimensions: {path}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(buffer.getvalue())
    monochrome(args.check)
    print("Verified branding assets." if args.check else "Created branding assets.")


def monochrome(check):
    svg = ElementTree.parse(BRAND / "android/monochrome.svg").getroot()
    shape = svg.find("{http://www.w3.org/2000/svg}path")
    path_data = quoteattr(shape.attrib["d"])
    vector = f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData={path_data} />
</vector>
"""
    output = RES / "drawable/ic_launcher_monochrome.xml"
    if check and output.read_text(encoding="utf-8") != vector:
        raise ValueError("The Android monochrome asset is out of sync")
    if not check:
        output.write_text(vector, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
