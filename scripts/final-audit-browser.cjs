async page => {
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultNavigationTimeout(120000);
  qa.setDefaultTimeout(60000);
  const report = {};
  const errors = [];
  qa.on('pageerror', error => errors.push(error.message));
  const preview = page.url().includes('audit-preview');
  try {
    if (preview) await qa.route('**/js/main.js*', async route => {
      const result = await qa.request.get('http://127.0.0.1:4179/main.js');
      await route.fulfill({body:await result.text(),contentType:'text/javascript'});
    });
    await qa.goto('https://nuobo.store/products',{waitUntil:'domcontentloaded'});
    const card = qa.locator('.product-card').filter({has:qa.locator('form[action="/cart/add"]')}).first();
    await card.getByRole('button',{name:'Add to cart',exact:true}).click();
    await qa.waitForURL('**/cart',{waitUntil:'domcontentloaded'});
    await qa.locator('.cart-qty-input').waitFor();
    report.testProductStock = await qa.locator('.cart-qty-input').getAttribute('max');
    if (Number(report.testProductStock) < 4) throw new Error('Choose a test product with at least four units in stock');
    const firstQty = qa.locator('.cart-qty-input');
    await firstQty.fill('2');
    await firstQty.dispatchEvent('input');
    await qa.getByRole('link',{name:'Proceed to checkout',exact:true}).click();
    await qa.waitForURL(url => url.pathname === '/login',{waitUntil:'domcontentloaded'});
    await qa.goto('https://nuobo.store/cart',{waitUntil:'domcontentloaded'});
    report.checkoutFlushesQuantity = await qa.locator('.cart-qty-input').inputValue() === '2';
    await qa.locator('.cart-qty-input').fill('3');
    await qa.locator('[role="status"]').filter({hasText:'Quantities saved'}).waitFor();
    await qa.reload({waitUntil:'domcontentloaded'});
    report.autoSavePersists = await qa.locator('.cart-qty-input').inputValue() === '3';
    await qa.route('**/cart/update', route => route.fulfill({status:503,body:'Unavailable'}));
    await qa.locator('.cart-qty-plus').click();
    await qa.getByRole('link',{name:'Proceed to checkout',exact:true}).click();
    await qa.locator('[role="status"]').filter({hasText:'Could not save'}).waitFor();
    report.failedSaveBlocksCheckout = new URL(qa.url()).pathname === '/cart';
    await qa.unroute('**/cart/update');
    await qa.locator('.cart-qty-input').fill('4');
    await qa.locator('[role="status"]').filter({hasText:'Quantities saved'}).waitFor();
    await qa.reload({waitUntil:'domcontentloaded'});
    report.failedSaveRecovers = await qa.locator('.cart-qty-input').inputValue() === '4';
    await qa.getByRole('button',{name:'Remove item',exact:true}).click();
    await qa.locator('.cart-empty-state').waitFor();

    await qa.goto('https://nuobo.store/products',{waitUntil:'domcontentloaded'});
    await qa.locator('.bulk-inquiry-add').nth(0).click();
    await qa.locator('.bulk-inquiry-add').nth(1).click();
    await qa.goto('https://nuobo.store/inquiry',{waitUntil:'domcontentloaded'});
    await qa.locator('.inquiry-product-row').first().waitFor();
    report.inquiryTwoProducts = await qa.locator('.inquiry-product-row').count() === 2;
    const input = qa.locator('.inquiry-quantity-input').first();
    await input.fill('24');
    await qa.locator('.inquiry-product-row').first().getByRole('button',{name:/Increase quantity/}).click();
    report.inquiryTypedPlus = await qa.locator('.inquiry-quantity-input').first().inputValue() === '25';
    report.inquiryPayload = await qa.locator('input[name="productQuantities"]').first().inputValue() === '25';
    const sibling = await context.newPage();
    await sibling.goto('https://nuobo.store/inquiry',{waitUntil:'domcontentloaded'});
    await sibling.locator('.inquiry-product-row').first().waitFor();
    await sibling.getByRole('button',{name:'Clear all',exact:true}).click();
    await qa.locator('#inquiryEmptyState').waitFor({state:'visible'});
    report.inquiryCrossTab = true;
    await sibling.close();

    const home = await qa.request.get('https://nuobo.store/');
    const head = (await home.text()).match(/<head[\s\S]*?<\/head>/)[0];
    const checkout = await qa.request.get('http://127.0.0.1:4179/checkout.html');
    const markup = (await checkout.text()).replace(/<head[\s\S]*?<\/head>/,head);
    await qa.route('https://nuobo.store/checkout', route => route.fulfill({body:markup,contentType:'text/html'}));
    let paymentRequest;
    await qa.route('**/checkout/pay', route => {
      paymentRequest = {headers:route.request().headers(),body:route.request().postData()};
      return route.fulfill({body:'error: isolated test only',contentType:'text/plain'});
    });
    qa.on('dialog', dialog => dialog.accept());
    await qa.goto('https://nuobo.store/checkout',{waitUntil:'domcontentloaded'});
    const csrf = await qa.locator('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = await qa.locator('meta[name="_csrf_header"]').getAttribute('content');
    for(const id of ['shipName','shipPhone','shipAddress','shipCity','shipState','shipZip']) await qa.locator('#'+id).fill('Isolated test');
    await qa.locator('.checkout-pay-btn').click();
    await qa.waitForFunction(()=>!document.querySelector('.checkout-pay-btn').disabled);
    report.paymentCsrfHeader = paymentRequest?.headers[csrfHeader.toLowerCase()] === csrf;
    report.paymentErrorRecovery = await qa.locator('.checkout-pay-btn').isEnabled();
    await qa.unroute('**/checkout/pay');
    await qa.unroute('https://nuobo.store/checkout');
    report.errors = errors;
    for(const [name,value] of Object.entries(report)) if(value === false) throw new Error(name+' failed | '+JSON.stringify(report));
    if(errors.length) throw new Error(errors.join('; '));
    return report;
  } catch (error) {
    throw new Error(error.message+' | URL: '+qa.url()+' | Cart status: '+await qa.locator('p[role="status"]').textContent().catch(()=>''));
  } finally { await context.close(); }
}
