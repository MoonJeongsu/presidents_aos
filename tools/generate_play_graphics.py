from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFont

OUT_DIR = Path(r"C:\projects\usa\구글플레이")
ICON_SRC = Path(
    r"c:\projects\usa\PresidentialSpeeches\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"
)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    icon = Image.open(ICON_SRC).convert("RGBA")

    icon512 = icon.resize((512, 512), Image.Resampling.LANCZOS)
    icon512_path = OUT_DIR / "icon_512x512.png"
    icon512.save(icon512_path, optimize=True)

    width, height = 1200, 500
    bg = Image.new("RGB", (width, height), "#0B1533")
    draw = ImageDraw.Draw(bg)

    for y in range(height):
        t = y / height
        r = int(11 + (20 - 11) * t)
        g = int(21 + (35 - 21) * t)
        b = int(51 + (75 - 51) * t)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    random.seed(42)
    for _ in range(40):
        x = random.randint(520, 1180)
        y = random.randint(20, 480)
        size = random.randint(1, 3)
        draw.ellipse((x, y, x + size, y + size), fill=(255, 255, 255))

    icon_feature = icon.resize((380, 380), Image.Resampling.LANCZOS)
    bg.paste(icon_feature, (70, 60), icon_feature)

    try:
        font_ko = ImageFont.truetype("malgun.ttf", 58)
        font_en = ImageFont.truetype("malgun.ttf", 40)
        font_sub = ImageFont.truetype("malgun.ttf", 28)
    except OSError:
        font_ko = ImageFont.load_default()
        font_en = ImageFont.load_default()
        font_sub = ImageFont.load_default()

    text_x = 500
    draw.text((text_x, 130), "미국 대통령 연설문", fill="#FFFFFF", font=font_ko)
    draw.text((text_x, 210), "US Presidential Speeches", fill="#D6DEEA", font=font_en)
    draw.text((text_x, 280), "Washington to Obama", fill="#B8C4D9", font=font_sub)
    draw.text((text_x, 330), "번역 · Neural 음성 · 577편 연설", fill="#9FB0CC", font=font_sub)
    draw.rectangle((500, 390, 1120, 394), fill="#C9A227")

    feature_path = OUT_DIR / "feature_1200x500.png"
    bg.save(feature_path, "PNG", optimize=True)

    print(f"saved {icon512_path} {icon512.size}")
    print(f"saved {feature_path} {bg.size}")


if __name__ == "__main__":
    main()
