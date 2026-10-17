async page => {
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultTimeout(45000);
  const report = {};
  const errors = [];
  const analyticsResponses = [];
  qa.on('pageerror', error => errors.push(error.message));
  qa.on('response', response => { if (response.url().includes('/analytics/event')) analyticsResponses.push(response.status()); });
  try {
    await qa.goto('https://nuobo.store/products',{waitUntil:'domcontentloaded'});
    report.scriptVersion = await qa.locator('script[src*="/js/main.js"]').getAttribute('src');
    report.searchCss = await qa.locator('link[href*="search-upgrade.css"]').count() === 1;
    await qa.locator('input[name="q"]').fill('QM');
    await qa.locator('.search-suggestions a').first().waitFor();
    report.searchSuggestions = await qa.locator('.search-suggestions a').count() > 0;
    const suggestionUrl = await qa.locator('.search-suggestions a').first().getAttribute('href');
    report.suggestionHasProductUrl = /^\/product\/\d+$/.test(suggestionUrl);
    await qa.goto('https://nuobo.store/product/355',{waitUntil:'domcontentloaded'});
    await qa.locator('.detail-readiness').waitFor();
    report.productReadiness = await qa.locator('.detail-readiness').isVisible();
    report.analyticsScriptPageView = await qa.waitForFunction(() => window.location.pathname === '/product/355').then(() => true);
    await qa.waitForTimeout(500);
    report.analyticsAccepted = analyticsResponses.some(status => status >= 200 && status < 400);
    const suggestions = await qa.request.get('https://nuobo.store/api/search-suggestions?q=QM');
    report.searchApi = suggestions.status() === 200 && (await suggestions.json()).length > 0;
    const unknownCategory = await qa.request.get('https://nuobo.store/category/no-such-category', {maxRedirects:0});
    report.invalidCategory404 = unknownCategory.status() === 404;
    const admin = await qa.request.get('https://nuobo.store/admin/dashboard', {maxRedirects:0});
    report.adminProtected = admin.status() === 302;
    report.errors = errors;
    for (const [key,value] of Object.entries(report)) if (value === false) throw new Error(key + ' failed: ' + JSON.stringify(report));
    if (errors.length) throw new Error(errors.join('; '));
    return report;
  } finally { await context.close(); }
}
