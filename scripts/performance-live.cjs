async page => {
  page.setDefaultNavigationTimeout(90000);
  const context = await page.context().browser().newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
  const qa = await context.newPage();
  qa.setDefaultNavigationTimeout(90000);
  const longTasks=[]; const errors=[];
  qa.on('pageerror', error => errors.push(error.message));
  try {
    const started = Date.now();
    const response = await qa.goto('https://nuobo.store/',{waitUntil:'domcontentloaded',timeout:90000});
    const domContentLoaded = Date.now() - started;
    await qa.evaluate(()=>document.fonts.ready);
    await qa.waitForTimeout(2200);
    const report = await qa.evaluate(() => {
      const resources = performance.getEntriesByType('resource');
      const byType = type => resources.filter(entry => entry.initiatorType === type).reduce((sum, entry) => sum + (entry.transferSize || 0), 0);
      return {
        navigation: performance.getEntriesByType('navigation')[0]?.toJSON(),
        jsBytes: byType('script'), cssBytes: byType('link'), imageBytes: byType('img'),
        scripts: resources.filter(entry => entry.initiatorType === 'script').map(entry => ({name:entry.name.split('/').pop(),bytes:entry.transferSize,duration:Math.round(entry.duration)})),
        globeLoaded: document.querySelector('#global-reach')?.dataset.globeReady === 'true',
        heroLoaded: document.querySelector('.holiday-hero')?.dataset.sceneReady === 'true',
        pageHeight: document.documentElement.scrollHeight
      };
    });
    const observer = await qa.evaluateHandle(() => { const values=[]; const observer=new PerformanceObserver(list=>values.push(...list.getEntries().map(entry=>entry.duration))); observer.observe({type:'longtask',buffered:true}); window.__perfLongTasks={values,observer}; return true; });
    await qa.waitForTimeout(1200);
    const taskReport = await qa.evaluate(() => { const values=window.__perfLongTasks.values; window.__perfLongTasks.observer.disconnect(); return {count:values.length,max:values.length?Math.max(...values):0}; });
    report.domContentLoaded=domContentLoaded; report.longTasks=taskReport; report.errors=errors; report.status=response?.status();
    return report;
  } finally { await context.close(); }
}
