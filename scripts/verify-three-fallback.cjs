async page => {
  const context=await page.context().browser().newContext({viewport:{width:1440,height:900}});
  const qa=await context.newPage();
  const errors=[];
  qa.on('pageerror',error=>errors.push(error.message));
  try {
    const html=await page.evaluate(()=>{
      const doc=document.documentElement.cloneNode(true);
      const hero=doc.querySelector('.holiday-hero');
      delete hero.dataset.sceneReady;delete hero.dataset.sceneAnimating;
      doc.querySelector('#nuobo-el-root').innerHTML='';
      return '<!DOCTYPE html>'+doc.outerHTML;
    });
    await qa.addInitScript(()=>{
      const getContext=HTMLCanvasElement.prototype.getContext;
      HTMLCanvasElement.prototype.getContext=function(type,...args){return /webgl/.test(type)?null:getContext.call(this,type,...args);};
    });
    await qa.route('**/js/**',async route=>{
      const pathname=new URL(route.request().url()).pathname;
      const response=await page.request.get('http://127.0.0.1:4179'+pathname);
      await route.fulfill({response});
    });
    for(const resource of ['**/css/**','**/fonts/**','**/images/editorial/**'])await qa.route(resource,async route=>{
      const response=await page.request.get('http://127.0.0.1:4179'+new URL(route.request().url()).pathname);
      await route.fulfill({response});
    });
    await qa.route('https://nuobo.store/?fallback-check',route=>route.fulfill({body:html,contentType:'text/html'}));
    await qa.goto('https://nuobo.store/?fallback-check');
    await qa.waitForTimeout(2200);
    await qa.getByRole('button',{name:'Halloween',exact:true}).click();
    await qa.locator('.hero-product-tree').evaluate(image=>image.decode());
    const fallback=await qa.evaluate(()=>{
      const hero=document.querySelector('.holiday-hero');
      const image=hero.querySelector('.hero-product-tree');
      return !hero.dataset.sceneReady&&getComputedStyle(image).opacity==='1'&&image.naturalWidth>0&&hero.querySelector('[data-scene-products]').getAttribute('href')==='/category/halloween'&&hero.querySelector('#hero-scene-toggle').hidden;
    });
    await qa.screenshot({path:'output/three-webgl-fallback.png'});
    if(!fallback||errors.length)throw new Error(JSON.stringify({fallback,errors,state:await qa.locator('.holiday-hero').getAttribute('data-scene-ready'),context:await qa.evaluate(()=>HTMLCanvasElement.prototype.getContext.toString()),toggle:await qa.locator('#hero-scene-toggle').evaluate(el=>el.outerHTML)}));
    return {fallback,errors};
  } finally {await context.close();}
}
