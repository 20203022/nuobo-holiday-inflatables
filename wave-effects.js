export function initWave(section) {
    const canvas = section.querySelector('#nuobo-wave-canvas');
    const context = canvas.getContext('2d');
    const toggle = section.querySelector('#global-wave-toggle');
    if (!context) { toggle.hidden = true; return; }
    const motion = matchMedia('(prefers-reduced-motion: reduce)');
    let paused = motion.matches;
    let visible = false;
    let frame = 0;
    let time = 0;
    let last = 0;
    let width = 1;
    let height = 1;
    function draw() {
        context.clearRect(0, 0, width, height);
        for (let row = 0; row < 12; row++) {
            const depth = row / 11;
            context.strokeStyle = `rgba(86, 113, 78, ${0.13 + depth * 0.15})`;
            context.lineWidth = 0.8;
            context.beginPath();
            for (let i = 0; i <= 90; i++) {
                const x = i / 90 * width;
                const y = height * (0.34 + depth * 0.33) + Math.sin(i * 0.065 + time * 0.85 + depth * 3.1) * (17 + depth * 9) + Math.cos(i * 0.032 - time * 0.47) * 8;
                if (i === 0) context.moveTo(x, y);
                else context.lineTo(x, y);
            }
            context.stroke();
        }
        section.dataset.waveReady = 'true';
    }
    function active() { return visible && !paused && !document.hidden; }
    function tick(now) {
        frame = 0;
        if (!active()) { last = 0; return; }
        if (!last || now - last >= 32) {
            time += last ? Math.min((now - last) / 1000, 0.1) : 0;
            last = now;
            draw();
        }
        frame = requestAnimationFrame(tick);
    }
    function schedule() {
        if (active() && !frame) frame = requestAnimationFrame(tick);
        if (!active()) { cancelAnimationFrame(frame); frame = 0; last = 0; }
    }
    function sync() {
        toggle.textContent = paused ? 'Resume wave' : 'Pause wave';
        toggle.setAttribute('aria-pressed', String(paused));
        draw();
        schedule();
    }
    toggle.addEventListener('click', () => { paused = !paused; sync(); });
    motion.addEventListener('change', event => { if (event.matches) { paused = true; sync(); } });
    new IntersectionObserver(entries => { visible = entries[0].isIntersecting; schedule(); }).observe(canvas);
    new ResizeObserver(() => {
        const bounds = canvas.getBoundingClientRect();
        width = Math.max(1, bounds.width); height = Math.max(1, bounds.height);
        canvas.width = Math.round(width); canvas.height = Math.round(height);
        draw();
    }).observe(canvas);
    document.addEventListener('visibilitychange', schedule);
    sync();
}
