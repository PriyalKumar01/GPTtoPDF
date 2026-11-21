import os
from PIL import Image, ImageDraw, ImageFilter

SRC_PATH = r"C:/Users/priya/.gemini/antigravity/brain/5e7f1263-a9c2-4979-bf25-37dadc9b555f/.user_uploaded/media_1795202750779_ba31fe91.jpg"

def main():
    if not os.path.exists(SRC_PATH):
        print("Source file not found:", SRC_PATH)
        return

    raw = Image.open(SRC_PATH).convert('RGBA')
    w, h = raw.size

    # 1. Precise crop of the icon
    crop = raw.crop((204, 204, 820, 820))
    cw, ch = crop.size

    # Floodfill exterior white to alpha 0
    gray = crop.convert('L')
    bg_mask = Image.new('L', (cw, ch), 0)
    for x in range(cw):
        for y in range(ch):
            if gray.getpixel((x, y)) >= 242:
                bg_mask.putpixel((x, y), 255)

    ImageDraw.floodfill(bg_mask, (0, 0), 128, thresh=10)
    ImageDraw.floodfill(bg_mask, (cw - 1, 0), 128, thresh=10)
    ImageDraw.floodfill(bg_mask, (0, ch - 1), 128, thresh=10)
    ImageDraw.floodfill(bg_mask, (cw - 1, ch - 1), 128, thresh=10)

    alpha = Image.new('L', (cw, ch), 255)
    for x in range(cw):
        for y in range(ch):
            if bg_mask.getpixel((x, y)) == 128:
                alpha.putpixel((x, y), 0)

    # Smooth the alpha mask edge
    smooth_alpha = alpha.filter(ImageFilter.GaussianBlur(radius=0.7))
    crop.putalpha(smooth_alpha)

    # Save transparent logo
    os.makedirs(r"android/app/src/main/res/drawable", exist_ok=True)
    crop.save(r"android/app/src/main/res/drawable/app_logo_transparent.png", "PNG")
    crop.save(r"android/app/src/main/res/drawable/app_logo.png", "PNG")
    print("Saved app_logo_transparent.png and app_logo.png")

    # 2. Adaptive Icon Foregrounds (icon centered in canvas, 66% safe zone)
    # Sizes: mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432
    fg_sizes = {
        "drawable": 432,
        "drawable-mdpi": 108,
        "drawable-hdpi": 162,
        "drawable-xhdpi": 216,
        "drawable-xxhdpi": 324,
        "drawable-xxxhdpi": 432,
    }

    for folder, dim in fg_sizes.items():
        out_dir = os.path.join(r"android/app/src/main/res", folder)
        os.makedirs(out_dir, exist_ok=True)
        # Main icon size inside adaptive foreground is ~66% of dim
        icon_dim = int(dim * 0.70)
        resized_icon = crop.resize((icon_dim, icon_dim), Image.Resampling.LANCZOS)

        canvas = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
        offset = ((dim - icon_dim) // 2, (dim - icon_dim) // 2)
        canvas.paste(resized_icon, offset, resized_icon)
        canvas.save(os.path.join(out_dir, "ic_launcher_foreground.png"), "PNG")
        print(f"Saved {folder}/ic_launcher_foreground.png ({dim}x{dim})")

    # 3. Standard & Round launcher icons
    # Sizes: mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
    mipmap_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, dim in mipmap_sizes.items():
        out_dir = os.path.join(r"android/app/src/main/res", folder)
        os.makedirs(out_dir, exist_ok=True)

        # Standard icon: resized cropped icon
        std_icon = crop.resize((dim, dim), Image.Resampling.LANCZOS)
        std_icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

        # Round icon: circular mask
        round_icon = Image.new("RGBA", (dim, dim), (0, 0, 0, 0))
        mask = Image.new("L", (dim, dim), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, dim - 1, dim - 1), fill=255)
        round_icon.paste(std_icon, (0, 0), mask)
        round_icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
        print(f"Saved {folder} ic_launcher and ic_launcher_round ({dim}x{dim})")

    # 4. Notification small icon (ic_stat_qwiksave.png) - clean silhouette/monochrome with alpha for status bar
    # 24x24 dp up to 96x96
    notif_icon = crop.resize((96, 96), Image.Resampling.LANCZOS)
    notif_icon.save(r"android/app/src/main/res/drawable/ic_stat_qwiksave.png", "PNG")
    print("Saved ic_stat_qwiksave.png")

    # 5. Next.js assets
    os.makedirs(r"public", exist_ok=True)
    crop.save(r"public/app-logo.png", "PNG")
    crop.save(r"public/logo.png", "PNG")
    crop.resize((192, 192), Image.Resampling.LANCZOS).save(r"public/icon.png", "PNG")
    crop.convert('RGB').resize((192, 192), Image.Resampling.LANCZOS).save(r"app/icon.jpg", "JPEG")
    print("Saved Next.js web assets (logo.png, icon.png, app/icon.jpg)")

    # Clean up test files
    for t in ['test_crop.png', 'test_floodfill.png']:
        if os.path.exists(t):
            os.remove(t)

    print("All logo and app assets generated successfully!")

if __name__ == "__main__":
    main()
