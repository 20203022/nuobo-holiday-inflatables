async page => {
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultTimeout(45000);
  qa.setDefaultNavigationTimeout(60000);
  const report = {};
  const errors = [];
  qa.on('pageerror', error => errors.push(error.message));
  try {
    await qa.goto('https://nuobo.store/products',{waitUntil:'domcontentloaded'});
    report.latestScript = await qa.locator('script[src*="/js/main.js"]').getAttribute('src');
    if (!report.latestScript.includes('final-audit-v2')) throw new Error('Old frontend version');
    await qa.evaluate(async () => {
      const images = [...document.querySelectorAll('.product-card img')].slice(0,4);
      await Promise.all(images.map(img => img.decode()));
    });
    report.loadedProductImages = await qa.locator('.product-card img').evaluateAll(images => images.slice(0,4).map(img => ({width:img.naturalWidth,height:img.naturalHeight})));
    await qa.screenshot({path:'output/final-live-products.png'});
    await qa.locator('.bulk-inquiry-add').nth(0).click();
    await qa.goto('https://nuobo.store/inquiry',{waitUntil:'domcontentloaded'});
    await qa.locator('.inquiry-quantity-input').fill('24');
    await qa.getByRole('button',{name:/Increase quantity/}).click();
    report.inquiryClickRetained = await qa.locator('.inquiry-quantity-input').inputValue() === '25';
    await qa.reload({waitUntil:'domcontentloaded'});
    report.inquiryReload = await qa.locator('.inquiry-quantity-input').inputValue() === '25';
    await qa.getByRole('button',{name:'Clear all',exact:true}).click();
    await qa.goto('https://nuobo.store/register',{waitUntil:'domcontentloaded'});
    report.registrationPassword = await qa.locator('#regPassword').getAttribute('minlength') === '8';
    report.registrationCsrf = await qa.locator('form input[name="_csrf"]').count() > 0;
    await qa.screenshot({path:'output/final-live-register.png'});
    await qa.goto('https://nuobo.store/login',{waitUntil:'domcontentloaded'});
    report.loginCsrf = await qa.locator('form input[name="_csrf"]').count() > 0;
    await qa.locator('input[name="email"]').fill('nonexistent-final-audit@example.test');
    await qa.locator('input[name="password"]').fill('InvalidTestPassword42');
    await qa.locator('form button[type="submit"]').click();
    await qa.waitForURL(url => url.pathname === '/login' && url.search.includes('error'),{waitUntil:'domcontentloaded'});
    report.invalidLoginFeedback = await qa.locator('.alert-error').isVisible();
    const logout = await qa.request.get('https://nuobo.store/logout',{maxRedirects:0});
    report.logoutGetDoesNotMutate = logout.status() === 404;
    report.errors = errors;
    for(const [key,value] of Object.entries(report)) if(value === false) throw new Error(key+' failed');
    if(errors.length) throw new Error(errors.join('; '));
    return report;
  } finally {await context.close();}
}
