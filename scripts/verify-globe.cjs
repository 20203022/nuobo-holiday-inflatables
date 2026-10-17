async (page) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const url = page.url();
    await page.reload();
    await page.locator('#global-reach').scrollIntoViewIfNeeded();
    await page.waitForFunction(() => document.querySelector('#global-reach').dataset.globeReady === 'true');
    const pixels = await page.evaluate(() => {
        const globe = document.querySelector('#nuobo-globe-canvas');
        const context = globe.getContext('webgl2') || globe.getContext('webgl');
        const data = new Uint8Array(globe.width * globe.height * 4);
        context.readPixels(0, 0, globe.width, globe.height, context.RGBA, context.UNSIGNED_BYTE, data);
        let visiblePixels = 0;
        for (let index = 3; index < data.length; index += 4) if (data[index] > 10) visiblePixels++;
        const wave = document.querySelector('#nuobo-wave-canvas');
        const waveData = wave.getContext('2d').getImageData(0, 0, wave.width, wave.height).data;
        let wavePixels = 0;
        for (let index = 3; index < waveData.length; index += 4) if (waveData[index] > 5) wavePixels++;
        return {visiblePixels, wavePixels, width:globe.width, height:globe.height};
    });
    if (pixels.visiblePixels < 1000 || pixels.wavePixels < 500) throw new Error('Blank canvas detected');
    const initial = await page.evaluate(() => [...document.querySelectorAll('.world-stage canvas')].map(canvas => canvas.toDataURL()));
    await page.waitForFunction(initial => [...document.querySelectorAll('.world-stage canvas')].every((canvas, index) => canvas.toDataURL() !== initial[index]), initial, {polling:250});
    await page.getByRole('button', {name:'Pause motion', exact:true}).click();
    const paused = await page.evaluate(() => [...document.querySelectorAll('.world-stage canvas')].map(canvas => canvas.toDataURL()));
    await page.waitForTimeout(400);
    const stopped = await page.evaluate(paused => [...document.querySelectorAll('.world-stage canvas')].every((canvas, index) => canvas.toDataURL() === paused[index]), paused);
    if (!stopped) throw new Error('Pause did not stop both canvases');
    await page.getByRole('button', {name:'North America', exact:true}).click();
    await page.waitForFunction(() => [...document.querySelectorAll('.globe-place-label')].some(label => label.textContent === 'North America' && !label.hidden));
    await page.screenshot({path:'output/globe-north-america.png'});
    const beforeDrag = await page.locator('#nuobo-globe-canvas').screenshot();
    const bounds = await page.locator('#nuobo-globe-canvas').boundingBox();
    await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    await page.mouse.down();
    await page.mouse.move(bounds.x + bounds.width / 2 + 120, bounds.y + bounds.height / 2 + 25, {steps:12});
    await page.mouse.up();
    const afterDrag = await page.locator('#nuobo-globe-canvas').screenshot();
    const changed = beforeDrag.length !== afterDrag.length || beforeDrag.some((value,index) => value !== afterDrag[index]);
    if (!changed) throw new Error('Drag did not rotate globe');
    const layouts = [];
    for (const viewport of [{width:1280,height:800},{width:1920,height:1080},{width:390,height:844}]) {
        await page.setViewportSize(viewport);
        await page.locator('#global-reach').scrollIntoViewIfNeeded();
        layouts.push({...viewport, ...await page.evaluate(() => ({overflow:document.documentElement.scrollWidth > innerWidth,canvasWidth:document.querySelector('#nuobo-globe-canvas').width}))});
        await page.screenshot({path:'output/globe-' + viewport.width + '.png'});
    }
    await page.setViewportSize({width:1440,height:900});
    await page.emulateMedia({reducedMotion:'reduce'});
    await page.goto(url);
    await page.locator('#global-reach').scrollIntoViewIfNeeded();
    await page.waitForFunction(() => document.querySelector('#global-reach').dataset.globeReady === 'true');
    await page.getByRole('button', {name:'Resume motion', exact:true}).waitFor();
    const still = await page.evaluate(() => document.querySelector('#nuobo-globe-canvas').toDataURL());
    await page.waitForTimeout(350);
    const reducedMotion = await page.evaluate(still => document.querySelector('#nuobo-globe-canvas').toDataURL() === still, still);
    await page.emulateMedia({reducedMotion:'no-preference'});
    return {pixels, animated:true, pauseWorks:stopped, regionSwitch:true, dragWorks:changed, reducedMotion, layouts, errors};
}
