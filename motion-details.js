export function initMotionDetails() {
    const motion = matchMedia('(prefers-reduced-motion: reduce)');
    const tracked = [];
    document.querySelectorAll('.product-card .card-img-link, .home-holiday-photo').forEach(surface => {
        const isProduct = surface.classList.contains('card-img-link');
        const image = surface.querySelector('img');
        if (!image) return;
        let bounds;
        let frame = 0;
        let x = 0;
        let y = 0;
        const reset = () => {
            cancelAnimationFrame(frame); frame = 0;
            for (const name of ['--image-tilt-x','--image-tilt-y','--photo-pan-x','--photo-pan-y']) image.style.removeProperty(name);
        };
        tracked.push(reset);
        surface.addEventListener('pointerenter', () => { bounds = surface.getBoundingClientRect(); });
        surface.addEventListener('pointermove', event => {
            if (motion.matches || event.pointerType !== 'mouse' || !bounds) return;
            x = Math.max(-1, Math.min(1, (event.clientX - bounds.left) / bounds.width * 2 - 1));
            y = Math.max(-1, Math.min(1, (event.clientY - bounds.top) / bounds.height * 2 - 1));
            if (frame) return;
            frame = requestAnimationFrame(() => {
                frame = 0;
                image.style.setProperty(isProduct ? '--image-tilt-x' : '--photo-pan-y', (isProduct ? -y * 3 : -y * 5).toFixed(2) + (isProduct ? 'deg' : 'px'));
                image.style.setProperty(isProduct ? '--image-tilt-y' : '--photo-pan-x', (isProduct ? x * 3 : -x * 6).toFixed(2) + (isProduct ? 'deg' : 'px'));
            });
        }, {passive:true});
        surface.addEventListener('pointerleave', reset);
        surface.addEventListener('blur', reset);
    });
    motion.addEventListener('change', event => { if (event.matches) tracked.forEach(reset => reset()); });
}
