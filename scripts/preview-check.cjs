async (page) => {
  const source = await (await page.request.get('http://127.0.0.1:4179/template')).text();
  const baseHtml = await (await page.request.get('https://nuobo.store/')).text();
  const html = await page.evaluate(({source,baseHtml}) => {
    const base = new DOMParser().parseFromString(baseHtml, 'text/html');
    const doc = new DOMParser().parseFromString(source, 'text/html');
    const head = base.head.cloneNode(true);
    head.querySelectorAll('script').forEach(node => node.remove());
    head.querySelectorAll('link').forEach(node => { if (node.href.includes('unpkg.com') || node.href.includes('fonts.googleapis') || node.href.includes('fonts.gstatic')) node.remove(); });
    const main = doc.createElement('script');
    main.defer = true;
    main.src = 'https://nuobo.store/js/main.js?preview';
    for (const src of ['https://nuobo.store/js/vendor/ui-runtime.js?preview']) {
      const script = doc.createElement('script'); script.defer = true; script.src = src; head.append(script);
    }
    head.append(main);
    for (const path of ['global-effects.css', 'home-compact.css', 'three-experience.css', 'home-expanded.css', 'vendor/ui-runtime.css']) {
      const style = doc.createElement('link'); style.rel = 'stylesheet'; style.href = 'https://nuobo.store/css/' + path + '?preview'; head.append(style);
    }
    doc.head.replaceWith(head);
    const header = base.querySelector('.site-header').cloneNode(true);
    const inquiry=header.querySelector('.nav-inquiry');
    inquiry.childNodes[0].textContent='Wholesale ';
    header.querySelector('.desktop-nav').append(inquiry);
    header.querySelector('#nuobo-el-root').innerHTML = '';
    doc.querySelector('header').replaceWith(header);
    doc.querySelector('footer').replaceWith(base.querySelector('.site-footer').cloneNode(true));
    const grids = [...base.querySelectorAll('.product-grid')];
    doc.querySelectorAll('.product-grid').forEach((grid, index) => { grid.innerHTML = [...grids[index].children].slice(0, 4).map(card => card.outerHTML).join(''); grid.querySelectorAll('article').forEach(card => card.classList.remove('fade-in', 'scroll-reveal', 'is-visible')); });
    doc.querySelectorAll('[th\\:text]').forEach(node => node.removeAttribute('th:text'));
    return '<!DOCTYPE html>' + doc.documentElement.outerHTML;
  }, {source,baseHtml});
  for (const resource of ['**/css/style.css*', '**/css/global-effects.css*', '**/css/home-compact.css*', '**/css/three-experience.css*', '**/css/home-expanded.css*', '**/css/vendor/*', '**/fonts/*', '**/js/main.js*', '**/js/motion-details.js*', '**/js/globe-effects.js*', '**/js/wave-effects.js*', '**/js/vendor/*', '**/images/globe/*', '**/images/editorial/*', '**/images/company/*']) {
    await page.route(resource, async route => {
      const pathname = new URL(route.request().url()).pathname;
      const response = await page.request.get('http://127.0.0.1:4179' + pathname);
      await route.fulfill({ response });
    });
  }
  await page.route('https://nuobo.store/?editorial-preview', route => route.fulfill({ body: html, contentType: 'text/html' }));
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('https://nuobo.store/?editorial-preview',{waitUntil:'domcontentloaded'});
  await page.screenshot({ path: 'output/editorial-home-preview.png' });
  return { preview: 'output/editorial-home-preview.png', title: await page.title() };
}
