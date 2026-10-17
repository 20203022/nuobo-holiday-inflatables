async page => {
  await page.setViewportSize({width:1440,height:900});
  await page.emulateMedia({reducedMotion:'reduce'});
  await page.goto('https://nuobo.store/?compact-final=20261013');
  await page.evaluate(()=>document.fonts.ready);
  for(const selector of ['.home-catalog','.home-holidays','#global-reach']) {
    await page.locator(selector).scrollIntoViewIfNeeded();
  }
  await page.waitForFunction(()=>document.querySelector('#global-reach').dataset.globeReady==='true');
  await page.evaluate(async()=>{await Promise.all([...document.querySelectorAll('main img')].filter(img=>img.getBoundingClientRect().width>0).map(img=>img.decode().catch(()=>{})));scrollTo({top:0,behavior:'instant'});});
  await page.screenshot({path:'output/compact-home-final.png',fullPage:true});
  const imageProblems=await page.evaluate(()=>[...document.querySelectorAll('main img')].filter(img=>img.getBoundingClientRect().width>0&&!img.naturalWidth).map(img=>img.src));
  return {homepageHeight:await page.evaluate(()=>document.documentElement.scrollHeight),imageProblems,screenshot:'output/compact-home-final.png'};
}
