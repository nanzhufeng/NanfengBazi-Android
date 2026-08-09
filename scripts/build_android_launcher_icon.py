#!/usr/bin/env python3
"""Build the Android launcher icon from the approved PNG master.

The supplied master contains a dark icon surface on a white export canvas.
Android supplies the launcher mask, so the export canvas and baked outer card
edge are not packaged. The cream/orange artwork keeps its master coordinates;
only the dark material is extended behind it for adaptive-icon masks.
"""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter


CANVAS_SIZE = 1254
# Android launchers render the 108dp adaptive foreground through an effective
# ~4/3 normalization before applying the final mask. Pre-scaling by 3/4 keeps
# the approved master coordinates visible on the actual launcher surface.
ADAPTIVE_FOREGROUND_COMPENSATION = 0.75
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fit_material_background(source: np.ndarray, artwork_core: np.ndarray) -> np.ndarray:
    """Fit the master dark material without carrying its white export canvas."""
    height, width, _ = source.shape
    yy, xx = np.mgrid[0:height, 0:width]
    x = (xx - (width - 1) / 2) / width
    y = (yy - (height - 1) / 2) / height
    luminance = (
        source[..., 0] * 0.2126
        + source[..., 1] * 0.7152
        + source[..., 2] * 0.0722
    )

    # Sample only genuine navy material well inside the baked card. Artwork,
    # white canvas, outer shadow, and rounded-card edge are deliberately absent.
    sample = (
        (xx >= 165)
        & (xx <= 1089)
        & (yy >= 125)
        & (yy <= 1035)
        & (luminance < 105)
        & ~artwork_core
        & ((xx % 5) == 0)
        & ((yy % 5) == 0)
    )
    features = np.stack(
        [
            np.ones_like(x),
            x,
            y,
            x * x,
            y * y,
            x * y,
            x * x * y,
            x * y * y,
        ],
        axis=-1,
    )
    design = features[sample]
    if design.shape[0] < 1_000:
        raise RuntimeError("not enough dark-material samples in the master")

    channels = []
    for channel in range(3):
        coefficients, *_ = np.linalg.lstsq(
            design, source[..., channel][sample], rcond=None
        )
        channels.append(features @ coefficients)
    background = np.stack(channels, axis=-1)
    return np.clip(background, 0, 255).astype(np.uint8)


def build_layers(master: Image.Image) -> tuple[Image.Image, Image.Image, Image.Image]:
    source = np.asarray(master.convert("RGB"), dtype=np.float64)
    height, width, _ = source.shape
    if (width, height) != (CANVAS_SIZE, CANVAS_SIZE):
        raise ValueError(
            f"master must be {CANVAS_SIZE}x{CANVAS_SIZE}, got {width}x{height}"
        )

    yy, xx = np.mgrid[0:height, 0:width]
    red, green, blue = source[..., 0], source[..., 1], source[..., 2]
    luminance = red * 0.2126 + green * 0.7152 + blue * 0.0722
    inside_art_area = (
        (xx >= 175) & (xx <= 1085) & (yy >= 125) & (yy <= 1035)
    )
    cream = (luminance >= 122) & ((red - blue) >= 4)
    orange = (red >= 115) & (red >= blue * 1.65) & (green >= blue * 1.12)
    artwork_core = inside_art_area & (cream | orange)

    core_image = Image.fromarray((artwork_core.astype(np.uint8) * 255), mode="L")
    neighborhood = core_image.filter(ImageFilter.MaxFilter(45))
    neighborhood_array = np.asarray(neighborhood, dtype=np.float64) / 255.0

    background_array = fit_material_background(source, artwork_core)
    difference = np.linalg.norm(source - background_array.astype(np.float64), axis=-1)

    # Keep exact master pixels around the visible artwork and its baked shadows.
    # The distance ramp makes the transition into the fitted navy material quiet.
    alpha = np.clip((difference - 2.5) / 18.0, 0.0, 1.0)
    alpha *= neighborhood_array
    alpha[artwork_core] = 1.0
    alpha_image = Image.fromarray(np.uint8(np.round(alpha * 255)), mode="L")
    alpha_image = alpha_image.filter(ImageFilter.GaussianBlur(0.55))

    alpha_array = np.asarray(alpha_image, dtype=np.uint8)
    foreground_array = np.dstack(
        [source.astype(np.uint8), alpha_array]
    )
    # Transparent RGB is visually irrelevant but retaining the white export
    # canvas there bloats the packaged PNG substantially.
    foreground_array[alpha_array == 0, :3] = 0
    extracted_foreground = Image.fromarray(foreground_array, mode="RGBA")
    compensated_size = round(CANVAS_SIZE * ADAPTIVE_FOREGROUND_COMPENSATION)
    compensated = extracted_foreground.resize(
        (compensated_size, compensated_size), Image.Resampling.LANCZOS
    )
    foreground = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    offset = (CANVAS_SIZE - compensated_size) // 2
    foreground.alpha_composite(compensated, (offset, offset))
    background = Image.fromarray(background_array, mode="RGB")
    composite = Image.alpha_composite(background.convert("RGBA"), foreground)

    alpha_bounds = foreground.getchannel("A").getbbox()
    if alpha_bounds is None:
        raise RuntimeError("foreground is empty")
    visible_width = alpha_bounds[2] - alpha_bounds[0]
    visible_height = alpha_bounds[3] - alpha_bounds[1]
    if visible_width > CANVAS_SIZE * 0.53 or visible_height > CANVAS_SIZE * 0.53:
        raise RuntimeError(
            "compensated foreground exceeds the launcher-safe visual bound: "
            f"{alpha_bounds}"
        )
    return background, foreground, composite


def save_outputs(project_root: Path, master_path: Path) -> None:
    master = Image.open(master_path)
    background, foreground, composite = build_layers(master)

    drawable = project_root / "app/src/main/res/drawable-nodpi"
    drawable.mkdir(parents=True, exist_ok=True)
    background.save(drawable / "app_icon_background.png", optimize=True)
    foreground.save(drawable / "app_icon_source.png", optimize=True)

    for density, size in LEGACY_SIZES.items():
        directory = project_root / f"app/src/main/res/mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        icon = composite.convert("RGB").resize((size, size), Image.Resampling.LANCZOS)
        icon.save(directory / "ic_launcher.png", optimize=True)
        icon.save(directory / "ic_launcher_round.png", optimize=True)

    print(f"master_sha256={sha256(master_path)}")
    print(f"background_sha256={sha256(drawable / 'app_icon_background.png')}")
    print(f"foreground_sha256={sha256(drawable / 'app_icon_source.png')}")
    print(
        "adaptive_foreground_compensation="
        f"{ADAPTIVE_FOREGROUND_COMPENSATION:.2f}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--master",
        type=Path,
        default=Path("design/assets/app-icon-master.png"),
    )
    args = parser.parse_args()
    root = args.project_root.resolve()
    master = args.master if args.master.is_absolute() else root / args.master
    save_outputs(root, master)


if __name__ == "__main__":
    main()
