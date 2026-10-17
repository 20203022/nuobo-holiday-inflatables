async page => {
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultNavigationTimeout(120000);
  const errors = [];
  qa.on('pageerror', error => errors.push(error.message));
  const report = {pages:[]};
  try {
    for (const route of ['/', '/products', '/category/christmas', '/category/halloween', '/product/355', '/about', '/contact', '/login', '/register', '/faq', '/privacy', '/return-policy', '/cart', '/inquiry']) {
      const response = await qa.goto('https://nuobo.store'+route,{waitUntil:'domcontentloaded'});
      await qa.evaluate(()=>document.fonts.ready);
      await qa.locator('h1').first().waitFor();
      const result = await qa.evaluate(()=>({
        title:document.title,
        overflow:document.documentElement.scrollWidth>innerWidth,
        brokenImages:[...document.images].filter(img=>img.getBoundingClientRect().width>0&&img.getBoundingClientRect().height>0&&img.getBoundingClientRect().top<innerHeight&&img.complete&&!img.naturalWidth).map(img=>img.src),
        externalUI:[...document.scripts].some(script=>script.src.includes('unpkg.com')),
        collidingNav:[...document.querySelectorAll('.nav-wrap>*')].filter(el=>el.getBoundingClientRect().width>0).some((el,i,list)=>i&&el.getBoundingClientRect().left<list[i-1].getBoundingClientRect().right-1),
      }));
      report.pages.push({route,status:response.status(),...result});
      if(['/', '/products', '/category/halloween', '/about', '/contact', '/login'].includes(route)) await qa.screenshot({path:'output/compact-live-'+(route==='/'?'home':route.replaceAll('/','-'))+'.png'});
      if(response.status()!==200||result.overflow||result.brokenImages.length||result.collidingNav) throw new Error('Layout check failed: '+route+' '+JSON.stringify(result));
    }
    await qa.goto('https://nuobo.store/');
    const card = qa.locator('#panel-arrivals .product-card').filter({has:qa.locator('form[action="/cart/add"]')}).first();
    const id = await card.locator('input[name="productId"]').inputValue();
    await card.getByRole('button',{name:'Add to cart',exact:true}).click();
    await qa.waitForURL('**/cart');
    await qa.locator('.cart-qty-input').waitFor();
    const amount = async () => Number((await qa.locator('.cart-summary-total strong').innerText()).replace(/[^0-9.]/g,''));
    const unit = Number(await qa.locator('.cart-item-price').getAttribute('data-unit-price'));
    await qa.getByRole('button',{name:'Increase quantity',exact:true}).click();
    report.cartRealtime = Math.abs(await amount()-unit*2)<0.01;
    await qa.getByRole('button',{name:'Update',exact:true}).click();
    await qa.waitForURL('**/cart');
    await qa.reload();
    report.cartPersisted = await qa.locator('.cart-qty-input').inputValue()==='2' && Math.abs(await amount()-unit*2)<0.01;
    await qa.locator('.cart-qty-input').fill('3');
    report.cartTypedQuantity = Math.abs(await amount()-unit*3)<0.01;
    await qa.screenshot({path:'output/compact-live-cart-filled.png'});
    await qa.getByRole('button',{name:'Remove item',exact:true}).click();
    await qa.locator('.cart-empty-state').waitFor();
    report.cartRemoved = true;
    await qa.goto('https://nuobo.store/');
    await qa.locator('#panel-arrivals .bulk-inquiry-add').nth(0).click();
    await qa.locator('#panel-arrivals .bulk-inquiry-add').nth(1).click();
    report.inquiryAdded = await qa.locator('#inquiryNavCount').innerText()==='2';
    report.elementMessage = await qa.locator('.el-message').count()>0;
    await qa.goto('https://nuobo.store/inquiry');
    await qa.locator('.inquiry-product-row').first().waitFor();
    report.inquiryMultiple = await qa.locator('.inquiry-product-row').count()===2;
    const qty = qa.locator('.inquiry-quantity-input').first();
    await qty.fill('12'); await qty.blur();
    await qa.reload();
    await qa.locator('.inquiry-quantity-input').first().waitFor();
    report.inquiryQuantity = await qa.locator('.inquiry-quantity-input').first().inputValue()==='12' && await qa.locator('input[name="productQuantities"]').first().inputValue()==='12';
    await qa.screenshot({path:'output/compact-live-inquiry-filled.png'});
    await qa.getByRole('button',{name:'Clear all',exact:true}).click();
    report.inquiryClear = await qa.locator('#inquiryEmptyState').isVisible();
    report.productId = id;
    report.errors = errors;
    for(const [key,value] of Object.entries(report)) if(typeof value==='boolean'&&!value) throw new Error(key+' failed');
    if(errors.length) throw new Error(errors.join('; '));
    return report;
  } finally { await context.close(); }
}
