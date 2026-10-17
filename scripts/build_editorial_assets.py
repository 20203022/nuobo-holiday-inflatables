from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'images' / 'editorial'
SOURCE = ROOT / 'output' / 'asset-review'


def studio(name, products):
    stage = Image.new('RGB', (2000, 1000), 'white')
    for filename, box in products:
        image = Image.open(SOURCE / filename).convert('RGB')
        image.thumbnail((box[2], box[3]), Image.Resampling.LANCZOS)
        stage.paste(image, (box[0] + (box[2] - image.width) // 2,
                            box[1] + box[3] - image.height))
    stage.save(OUT / name, 'WEBP', quality=94, method=6)


studio('studio-christmas.webp', [('QM0924-3M.png', (1080, 130, 510, 650)),
                               ('QM01185-2M.png', (1580, 180, 280, 600))])
studio('studio-halloween.webp', [('QM1524-1.5M.png', (1080, 220, 760, 650))])

for filename in ('christmas-interior', 'holiday-mantel', 'gift-flatlay',
                 'christmas-detail', 'halloween-display'):
    image = Image.open(OUT / f'{filename}.jpg').convert('RGB')
    image.save(OUT / f'{filename}.webp', 'WEBP', quality=87, method=6)

print('Built 2 product stages and 5 optimized editorial photographs.')
