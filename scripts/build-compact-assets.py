from pathlib import Path
from urllib.request import urlretrieve
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
REVIEW = ROOT / 'output/asset-review'
DEST = ROOT / 'images/editorial'
DEST.mkdir(parents=True, exist_ok=True)

for source, target, limit in [
    ('holiday-1512389142860.jpg', 'holiday-foliage.webp', 1800),
    ('holiday-1512909006721.jpg', 'holiday-gifting.webp', 1400),
    ('holiday-1482517967863.jpg', 'holiday-gold-detail.webp', 1400),
    ('QM0924-3M.png', 'QM0924-3M-cutout.webp', 900),
    ('QM01185-2M.png', 'QM01185-2M-cutout.webp', 900),
    ('QM1524-1.5M.png', 'QM1524-1.5M-cutout.webp', 900),
]:
    image = Image.open(REVIEW / source)
    if source.endswith('.png'):
        image = image.convert('RGBA')
        mask = Image.new('L', image.size)
        mask.putdata([255 if min(pixel[:3]) > 238 and max(pixel[:3]) - min(pixel[:3]) < 18 else 0 for pixel in image.getdata()])
        for x in range(image.width):
            for y in (0, image.height - 1):
                if mask.getpixel((x, y)) == 255:
                    ImageDraw.floodfill(mask, (x, y), 128)
        for y in range(image.height):
            for x in (0, image.width - 1):
                if mask.getpixel((x, y)) == 255:
                    ImageDraw.floodfill(mask, (x, y), 128)
        alpha = Image.new('L', image.size)
        alpha.putdata([0 if pixel == 128 else 255 for pixel in mask.getdata()])
        image.putalpha(alpha)
    image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
    image.save(DEST / target, 'WEBP', quality=92 if source.endswith('.png') else 78)
    print(target, image.size, (DEST / target).stat().st_size)

image = Image.open(ROOT / 'images/company/video-display-poster.png').convert('RGB')
image.thumbnail((600, 600), Image.Resampling.LANCZOS)
image.save(DEST / 'workshop-film.webp', 'WEBP', quality=85)

for name, url in {
    'manrope-regular.ttf': 'https://fonts.gstatic.com/s/manrope/v20/xn7_YHE41ni1AdIRqAuZuw1Bx9mbZk79FO_F.ttf',
    'manrope-semibold.ttf': 'https://fonts.gstatic.com/s/manrope/v20/xn7_YHE41ni1AdIRqAuZuw1Bx9mbZk4jE-_F.ttf',
    'playfair-regular.ttf': 'https://fonts.gstatic.com/s/playfairdisplay/v40/nuFvD-vYSZviVYUb_rj3ij__anPXJzDwcbmjWBN2PKdFvUDQ.ttf',
}.items():
    urlretrieve(url, ROOT / 'fonts' / name)
    print(name, (ROOT / 'fonts' / name).stat().st_size)
