async (page) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  const results = [];
  const routes = ['/product/355', '/category/halloween', '/about', '/contact', '/login', '/register', '/faq', '/privacy', '/return-policy', '/checkout', '/cart'];
  for (const route of routes) {
    const response = await page.goto('https://nuobo.store' + route);
    await page.evaluate(() => document.fonts.ready);
    await page.locator('h1').first().waitFor();
    const result = await page.evaluate(() => ({
      title: document.title,
      heading: document.querySelector('h1')?.textContent,
      overflow: document.documentElement.scrollWidth > innerWidth,
      brokenImages: [...document.images].filter(image => image.getBoundingClientRect().top < innerHeight && image.complete && !image.naturalWidth).map(image => image.src),
      background: getComputedStyle(document.querySelector('.product-catalog-hero,.about-hero,.contact-hero,.auth-visual') || document.body).backgroundImage,
      cartQuantity: document.querySelector('.cart-qty-input')?.value,
      cartTotal: document.querySelector('.cart-summary-total strong')?.textContent
    }));
    results.push({route, status: response.status(), finalUrl: page.url(), ...result});
    if (['/product/355', '/about', '/contact', '/login', '/cart'].includes(route)) {
      await page.screenshot({path: 'output/editorial-' + route.replaceAll('/', '-') + '-live.png'});
    }
  }
  await page.setViewportSize({width:1280,height:800});
  for (const route of ['/', '/products', '/inquiry', '/login']) {
    await page.goto('https://nuobo.store' + route);
    results.push({route, width:1280, ...await page.evaluate(() => ({overflow:document.documentElement.scrollWidth > innerWidth}))});
  }
  await page.emulateMedia({reducedMotion:'reduce'});
  await page.goto('https://nuobo.store/');
  await page.getByRole('button', {name:'Resume slideshow'}).waitFor();
  results.push({reducedMotion:true, slideshowPaused:true});
  await page.emulateMedia({reducedMotion:'no-preference'});
  await page.setViewportSize({width:1440,height:900});
  return {results, errors};
}
