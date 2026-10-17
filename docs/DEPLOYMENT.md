# Deployment Notes

This document describes the release expectations for the Nuobo storefront. It intentionally omits server addresses, credentials, and private deployment commands.

## Release checklist

1. Build the local Vue/Element Plus runtime from `build/`.
2. Compile the Spring Boot application from the private backend build project.
3. Run the browser checks against a staging environment.
4. Confirm product images, catalog pagination, cart totals, inquiry quantities, account navigation, and admin access.
5. Confirm the payment entry remains disabled until currency and payment-provider approval are complete.
6. Back up the previous application artifact and database before a production restart.
7. Verify `/`, `/products`, both category routes, `/contact`, `/inquiry`, `/cart`, `/login`, and `/admin/dashboard` after deployment.

## Runtime requirements

- Java 17.
- Spring Boot 3.x with the project’s private dependency configuration.
- MySQL compatible with the deployed Hibernate dialect.
- SMTP service for inquiry, account, and order notifications.
- A reverse proxy/CDN configured for HTTPS, compression, and long-lived caching of versioned static assets.

## Caching

Versioned CSS, JavaScript, image, font, and video assets may be cached for 30 days. HTML remains dynamic because it contains CSRF tokens, account state, cart state, and inquiry state. Do not apply a blanket CDN cache rule to authenticated or form pages.

## Payment and refunds

Payment is intentionally disabled in the current release. Before enabling it:

- Define whether product prices are USD and how they convert to the provider settlement currency.
- Verify sandbox page payment and signed server notifications.
- Verify duplicate notifications do not double-decrement stock or coupons.
- Verify stock contention and expired pending orders.
- Verify a sandbox refund, duplicate refund attempt, refund failure, and refund status query.
- Enable the payment entry only after these checks pass.
