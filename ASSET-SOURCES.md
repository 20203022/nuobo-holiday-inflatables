# Editorial imagery

Downloaded on 2026-10-13. These photographs illustrate holiday settings; they
are not presented as Nuobo products, facilities, or customer installations.
The original product photos remain the source for all product cards and
product detail galleries.

| Asset | Source | License |
| --- | --- | --- |
| christmas-interior.webp | https://www.pexels.com/photo/1708601/ | https://www.pexels.com/license/ |
| holiday-mantel.webp | https://www.pexels.com/photo/5727887/ | https://www.pexels.com/license/ |
| gift-flatlay.webp | https://www.pexels.com/photo/1303098/ | https://www.pexels.com/license/ |
| christmas-detail.webp | https://www.pexels.com/photo/1656564/ | https://www.pexels.com/license/ |
| halloween-display.webp | https://images.unsplash.com/photo-1509557965875-b88c97052f0e | https://unsplash.com/license |

Product stages are deterministic compositions of existing Nuobo product
photographs QM0924-3M, QM01185-2M, and QM1524-1.5M, on a white background.
Products are not stretched or cropped. The script records the composition
and converts downloaded photos to WebP for the website.

## Globe visualization

The globe is an original Three.js scene. Land points and the static fallback
map are derived from Natural Earth's public-domain 1:110m land dataset:
https://github.com/nvkelso/natural-earth-vector/blob/master/geojson/ne_110m_land.geojson
https://www.naturalearthdata.com/about/terms-of-use/

Three.js 0.160.1 and OrbitControls are vendored locally under the MIT license:
https://github.com/mrdoob/three.js/blob/r160/LICENSE
D3 Geo 3.1.1 and D3 Array 3.2.4 are used only by the offline asset build script
under the ISC license. The animation does not use third-party website code.
Routes illustrate export markets, rather than warehouse locations or a live
shipment tracking system.

## Compact homepage assets (2026-10-13)

| Asset | Source | License |
| --- | --- | --- |
| holiday-foliage.webp | https://images.unsplash.com/photo-1512389142860-9c449e58a543 | https://unsplash.com/license |
| holiday-gifting.webp | https://images.unsplash.com/photo-1512909006721-3d6018887383 | https://unsplash.com/license |
| holiday-gold-detail.webp | https://images.unsplash.com/photo-1482517967863-00e15c9b44be | https://unsplash.com/license |

Transparent product cutouts derive from Nuobo's original product images;
only the connected outside white background is removed. The film thumbnail
derives from the existing Nuobo product film poster.

Local UI runtime: Vue 3.5.22 and Element Plus 2.11.5 (MIT). Local typefaces:
Manrope and Playfair Display (SIL Open Font License), sourced from Google Fonts.
Dependency license texts are shipped alongside the bundled assets.

## Three.js holiday scene (2026-10-13)

The hero uses Nuobo's original transparent product photos as textured planes,
with original extruded stars, a crescent, and a twisted ribbon mesh. Product
photographs are not volumetric product models. The lighting environment uses
Three.js RoomEnvironment (MIT):
https://cdn.jsdelivr.net/npm/three@0.160.1/examples/jsm/environments/RoomEnvironment.js
The hero and globe share one locally bundled Three.js engine. Rendering stops
offscreen and in hidden tabs, respects reduced motion, and limits DPR to 1.25
and frame rate to 25 fps. Original product images remain the WebGL fallback.
