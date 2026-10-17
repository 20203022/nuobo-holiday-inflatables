async page => {
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultTimeout(45000);
  const report = {};
  try {
    for (const route of ['/products','/category/christmas','/category/halloween','/search?q=inflatable']) {
      await qa.goto('https://nuobo.store' + route,{waitUntil:'domcontentloaded'});
      const first = await qa.locator('.card-img-link').evaluateAll(links => links.map(link => link.getAttribute('href')));
      const total = Number((await qa.locator('.simple-catalog-count').innerText()).split(' ')[0]);
      const next = await qa.getByRole('link',{name:'Next',exact:true}).getAttribute('href');
      await qa.goto('https://nuobo.store' + next,{waitUntil:'domcontentloaded'});
      const second = await qa.locator('.card-img-link').evaluateAll(links => links.map(link => link.getAttribute('href')));
      const lastRoute = new URL(next,'https://nuobo.store');
      lastRoute.searchParams.set('page',String(Math.floor((total - 1) / 12)));
      await qa.goto(lastRoute.toString(),{waitUntil:'domcontentloaded'});
      const last = await qa.locator('.card-img-link').count();
      report[route] = {first:first.length,second:second.length,last,total,unique:new Set([...first,...second]).size === first.length + second.length};
    }
    for (const [key,value] of Object.entries(report)) if (value.first !== 12 || value.second !== 12 || !value.unique || value.last !== (value.total % 12 || 12)) throw new Error(key+' failed '+JSON.stringify(value));
    return report;
  } finally {await context.close();}
}
