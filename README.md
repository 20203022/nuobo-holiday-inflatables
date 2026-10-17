# Nuobo Holiday Inflatables

Nuobo is a B2B/B2C independent storefront for holiday inflatable decorations. The site combines a product catalog, single-item cart, multi-product wholesale inquiry, account area, administrator dashboard, editorial company content, and a Three.js global-market experience.

The current storefront is built around Spring Boot, Thymeleaf, Spring Security, JPA, Vue/Element Plus runtime assets, and a locally bundled Three.js scene. Product images remain the source of truth; WebGL scenes use product photography as textured planes and fall back to the original images when WebGL is unavailable.

## Features

- Christmas and Halloween catalog with 12 products per page.
- Product search with model/name/size/holiday suggestions.
- Cart quantity persistence, live totals, stock clamping, and checkout entry.
- Multi-product wholesale inquiry with per-product quantities and CSV export in the admin area.
- User accounts, password reset, account orders, admin navigation, and POST-only logout.
- Admin dashboard with order, inquiry, product completeness, and privacy-friendly storefront event metrics.
- Product, company, production, quality-control, and export-market content.
- Large animated globe, wave effect, product-film dialog, reduced-motion support, and WebGL fallback.
- Payment entry retained as a disabled placeholder. Online payment currently does not create orders or call a payment provider.

## Repository layout

| Path | Purpose |
| --- | --- |
| `*.html` | Thymeleaf storefront templates |
| `admin/` | Administrator templates |
| `*.java` | Controller, service, model, and configuration source snapshot |
| `images/`, `videos/`, `fonts/` | Storefront assets |
| `vendor/` | Local Vue/Element Plus and Three.js browser bundles |
| `build/` | Frontend runtime build metadata and build script; dependencies are ignored |
| `scripts/` | Reproducible browser, catalog, asset, and regression checks |
| `ASSET-SOURCES.md` | Image, font, map, and library attribution record |
| `output/` | Local audit artifacts; intentionally excluded from Git |

The workspace is a source snapshot of the deployed application rather than a complete Maven project. The production Spring Boot JAR and server-only configuration stay outside this repository. Add the original Maven/Gradle project metadata before using this repository as a standalone backend build.

## Frontend asset build

Node.js 18+ is recommended. Build the local Vue/Element Plus runtime from the `build` directory:

```powershell
cd build
npm ci
npm run build
```

The generated runtime is copied into the storefront static asset location by the build script. `build/node_modules` and generated output are ignored.

## Browser checks

The browser checks use the Playwright CLI and a real desktop browser. Install Node.js/npm and run from the repository root:

```powershell
npx --yes @playwright/cli -s=nuobo open --browser msedge --config scripts/browser-live.json
npx --yes @playwright/cli -s=nuobo run-code --filename scripts/verify-compact.cjs
npx --yes @playwright/cli -s=nuobo run-code --filename scripts/final-live-smoke.cjs
npx --yes @playwright/cli -s=nuobo close
```

The checks cover desktop layout, product tabs, catalog pagination, cart totals, inquiry quantities, globe/wave animation, reduced motion, search suggestions, invalid routes, and payment-entry behavior. They do not submit real inquiries, create real users, place orders, send emails, or perform payment/refund transactions.

## Production configuration

Keep production settings outside Git. At minimum configure:

- Database connection and JPA settings.
- SMTP credentials and administrator email.
- Application base URL and upload directory.
- Alipay credentials only when online payment is intentionally enabled.
- A settlement currency and USD conversion policy before enabling checkout payment.

Do not put passwords, private keys, SMTP credentials, payment keys, database dumps, uploaded customer data, or server deployment archives in this repository.

The admin bootstrap reads `NUOBO_ADMIN_EMAIL` and `NUOBO_ADMIN_PASSWORD` from the runtime environment. The placeholder values in this source snapshot are intentionally unusable.

## Payment status

The checkout page currently keeps the payment entry visible as a disabled “coming soon” placeholder. The backend `/checkout/pay` endpoint returns before creating an order or calling Alipay. This is intentional until the settlement currency, payment channel, sandbox order flow, idempotent callback handling, and refund verification are completed.

## Asset attribution

See [`ASSET-SOURCES.md`](ASSET-SOURCES.md) for editorial image sources, licenses, Natural Earth data, Three.js, D3, Element Plus, Vue, fonts, and the distinction between original product photographs and editorial imagery.

## License

No public software license has been selected for the Nuobo application source. All rights to Nuobo business content, product photography, trademarks, and company materials remain with their respective owners. Third-party assets retain the licenses listed in `ASSET-SOURCES.md`.
