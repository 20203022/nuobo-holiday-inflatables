async page => {
  const context = await page.context().browser().newContext({reducedMotion:'reduce'});
  const qa = await context.newPage();
  const logs = [];
  qa.on('console', msg => {if(msg.type()==='error') logs.push(msg.text());});
  qa.on('requestfailed', request => logs.push(request.method()+' '+new URL(request.url()).pathname+' '+request.failure()?.errorText));
  try {
    await qa.route('**/js/main.js*', async route => {
      const source = await qa.request.get('http://127.0.0.1:4179/main.js');
      await route.fulfill({body:await source.text(),contentType:'text/javascript'});
    });
    await qa.goto('https://nuobo.store/products',{waitUntil:'domcontentloaded'});
    await qa.locator('.product-card form[action="/cart/add"]').first().getByRole('button',{name:'Add to cart'}).click();
    await qa.waitForURL('**/cart',{waitUntil:'domcontentloaded'});
    await qa.locator('.cart-qty-input').waitFor();
    const result = await qa.evaluate(async () => {
      const form = document.querySelector('.cart-qty-form');
      const body = new URLSearchParams(new FormData(form));
      let response;
      try { response = await fetch(form.action,{method:'POST',body}); }
      catch(error) {return {fetchError:error.message,action:form.action,origin:location.origin,hasCsrf:body.has('_csrf')};}
      const html = new DOMParser().parseFromString(await response.text(),'text/html');
      return {status:response.status,url:response.url,hasCsrf:body.has('_csrf'),sentProduct:body.get('productId'),forms:[...html.querySelectorAll('.cart-qty-form')].map(f=>({id:f.querySelector('[name="productId"]')?.value,qty:f.querySelector('.cart-qty-input')?.value})),title:html.title};
    });
    return {result,logs};
  } finally {await context.close();}
}
