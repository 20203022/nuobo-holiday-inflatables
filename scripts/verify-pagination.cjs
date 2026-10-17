async page => {
  const context=await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa=await context.newPage();
  qa.setDefaultNavigationTimeout(120000);
  // Use Playwright's HTTP transport for the deployed responses on this test host.
  await qa.route('https://nuobo.store/**',async route=>{
    if(route.request().resourceType()==='image'){await route.abort();return;}
    try {
      const response=await route.fetch({timeout:60000});
      await route.fulfill({response});
    }catch(error){await route.continue();}
  });
  const report=[];
  try {
    for(const route of ['/products','/category/christmas','/category/halloween','/search?q=inflatable']){
      await qa.goto('https://nuobo.store'+route,{waitUntil:'domcontentloaded'});
      const details=async()=>{
        await qa.locator('.product-grid').waitFor({timeout:60000});
        return qa.evaluate(()=>{
        const cards=[...document.querySelectorAll('.product-grid .product-card')];
        return {ids:cards.map(card=>card.querySelector('.card-img-link').getAttribute('href')),columns:getComputedStyle(document.querySelector('.product-grid')).gridTemplateColumns.split(' ').length,rows:new Set(cards.map(card=>Math.round(card.getBoundingClientRect().top))).size};
        });
      };
      const first=await details();
      if(first.ids.length!==12||first.columns!==4||first.rows!==3)throw new Error(route+' '+JSON.stringify(first));
      const next=await qa.getByRole('link',{name:'Next',exact:true}).getAttribute('href');
      if(route.includes('?q=')&&!next.includes('q=inflatable'))throw new Error('Search query lost');
      await qa.getByRole('link',{name:'Next',exact:true}).click({noWaitAfter:true});
      await qa.waitForURL(url=>url.searchParams.get('page')==='1',{waitUntil:'domcontentloaded'});
      const second=await details();
      if(second.ids.length!==12||second.ids.some(id=>first.ids.includes(id)))throw new Error('Page overlap or incorrect size: '+route);
      await qa.getByRole('link',{name:'Previous',exact:true}).click({noWaitAfter:true});
      await qa.waitForURL(url=>url.searchParams.get('page')==='0',{waitUntil:'domcontentloaded'});
      if(JSON.stringify((await details()).ids)!==JSON.stringify(first.ids))throw new Error('Previous link changed products');
      const total=Number(await qa.locator('.simple-catalog-count').innerText().then(value=>value.match(/\d+/)[0]));
      const lastPage=Math.ceil(total/12)-1;
      const lastUrl=new URL('https://nuobo.store'+route);lastUrl.searchParams.set('page',lastPage);
      await qa.goto(lastUrl.href,{waitUntil:'domcontentloaded'});
      const last=await details();
      if(last.ids.length!==(total%12||12)||await qa.getByRole('link',{name:'Next',exact:true}).count())throw new Error('Last page failed: '+route);
      report.push({route,first:first.ids.length,second:second.ids.length,columns:first.columns,rows:first.rows,total,last:last.ids.length,unique:true});
    }
    return report;
  } catch(error){throw new Error(qa.url()+': '+error.message);} finally {await context.close();}
}
