import * as THREE from './vendor/three.module.js';
import { OrbitControls } from './vendor/OrbitControls.js';

export async function initGlobalEffects(section) {
    const canvas = section.querySelector('#nuobo-globe-canvas');
    const toggle = section.querySelector('#global-globe-toggle');
    const motion = matchMedia('(prefers-reduced-motion: reduce)');
    let paused = motion.matches;
    let visible = false;
    let frame = 0;
    let lastTime = 0;
    let elapsed = 0;
    let width = 1;
    let height = 1;
    let targetRotation = null;
    let renderer;
    try {
        renderer = new THREE.WebGLRenderer({canvas, alpha: true, antialias: true, powerPreference: 'low-power', preserveDrawingBuffer: false});
    } catch {
        canvas.hidden = true;
        toggle.hidden = true;
        section.querySelector('.world-market-tabs').hidden = true;
        return;
    }
    renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 1.25));
    renderer.setClearColor(0x000000, 0);
    const scene = new THREE.Scene();
    scene.add(new THREE.AmbientLight(0xffffff, 2.3));
    const daylight = new THREE.DirectionalLight(0xffffff, 2.2);
    daylight.position.set(-3, 4, 7);
    scene.add(daylight);
    const camera = new THREE.PerspectiveCamera(38, 1, 0.1, 50);
    camera.position.set(0, 0.65, 7.4);
    const controls = new OrbitControls(camera, canvas);
    controls.enablePan = false;
    controls.enableZoom = false;
    controls.enableDamping = false;
    controls.minPolarAngle = Math.PI * 0.23;
    controls.maxPolarAngle = Math.PI * 0.73;
    controls.rotateSpeed = 0.45;
    canvas.style.touchAction = 'pan-y';
    const globe = new THREE.Group();
    globe.rotation.y = -1.25;
    scene.add(globe);
    const radius = 2.04;
    const polar = (lat, lon, scale = radius) => {
        const phi = THREE.MathUtils.degToRad(lat);
        const theta = THREE.MathUtils.degToRad(lon);
        return new THREE.Vector3(Math.cos(phi) * Math.sin(theta), Math.sin(phi), Math.cos(phi) * Math.cos(theta)).multiplyScalar(scale);
    };
    globe.add(new THREE.Mesh(new THREE.SphereGeometry(radius - 0.025, 32, 24), new THREE.MeshStandardMaterial({color: 0xe3e7de, roughness:0.9, metalness:0.03})));
    // One grid geometry keeps the globe's draw-call count small.
    const gridPoints = [];
    function addGridLine(points) {
        for (let i = 1; i < points.length; i++) gridPoints.push(points[i - 1], points[i]);
    }
    for (let lat = -60; lat <= 60; lat += 30) addGridLine(Array.from({length: 73}, (_, i) => polar(lat, i * 5)));
    for (let lon = 0; lon < 360; lon += 30) addGridLine(Array.from({length: 37}, (_, i) => polar(i * 5 - 90, lon)));
    globe.add(new THREE.LineSegments(new THREE.BufferGeometry().setFromPoints(gridPoints), new THREE.LineBasicMaterial({color: 0x91a184, transparent: true, opacity: 0.25})));
    const response = await fetch('/images/globe/land-points.json');
    if (!response.ok) { renderer.dispose(); throw new Error('Globe geography could not be loaded'); }
    const landPoints = (await response.json()).filter((_, i) => i % 2 === 0);
    const positions = new Float32Array(landPoints.length * 3);
    landPoints.forEach(([lat, lon], i) => polar(lat, lon, radius + 0.012).toArray(positions, i * 3));
    const landGeometry = new THREE.BufferGeometry();
    landGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const dotMaterial = new THREE.ShaderMaterial({
        transparent: true, depthWrite: false,
        uniforms: {color: {value: new THREE.Color(0x596f50)}, pointScale: {value: renderer.getPixelRatio()}},
        vertexShader: 'uniform float pointScale; void main(){vec4 p=modelViewMatrix*vec4(position,1.0); gl_Position=projectionMatrix*p; gl_PointSize=clamp(15.0*pointScale/-p.z,1.5,5.0);}',
        fragmentShader: 'uniform vec3 color; void main(){float d=length(gl_PointCoord-vec2(.5)); if(d>.5)discard; gl_FragColor=vec4(color,1.0-smoothstep(.3,.5,d));}'
    });
    globe.add(new THREE.Points(landGeometry, dotMaterial));
    const origin = polar(29.306, 120.075, radius + 0.055);
    const labels = [];
    const routes = [];
    const labelHost = section.querySelector('#nuobo-globe-labels');
    const dotGeometry = new THREE.SphereGeometry(0.038, 8, 8);
    function addLocation(name, position, isOrigin) {
        const marker = new THREE.Mesh(dotGeometry, new THREE.MeshBasicMaterial({color: isOrigin ? 0xa33f46 : 0x607855}));
        marker.position.copy(position);
        globe.add(marker);
        const label = document.createElement('span');
        label.className = 'globe-place-label' + (isOrigin ? ' is-origin' : '');
        label.textContent = name;
        labelHost.append(label);
        labels.push({label, position});
    }
    addLocation('Yiwu', origin, true);
    const destinations = [{name: 'Europe', lat: 50.1, lon: 10.4}, {name: 'North America', lat: 39.7, lon: -98.6}, {name: 'Southeast Asia', lat: 10.5, lon: 105.5}, {name: 'Middle East', lat: 25.2, lon: 55.3}];
    destinations.forEach((location, index) => {
        const target = polar(location.lat, location.lon, radius + 0.055);
        addLocation(location.name, target, false);
        const start = origin.clone().normalize();
        const end = target.clone().normalize();
        const angle = Math.acos(THREE.MathUtils.clamp(start.dot(end), -1, 1));
        const points = Array.from({length: 65}, (_, i) => {
            const t = i / 64;
            return start.clone().multiplyScalar(Math.sin((1 - t) * angle) / Math.sin(angle)).addScaledVector(end, Math.sin(t * angle) / Math.sin(angle)).normalize().multiplyScalar(radius + 0.06 + Math.sin(t * Math.PI) * (0.35 + angle * 0.22));
        });
        const curve = new THREE.CatmullRomCurve3(points);
        globe.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), new THREE.LineBasicMaterial({color: 0xac884f, transparent: true, opacity: 0.7})));
        const traveler = new THREE.Mesh(new THREE.SphereGeometry(0.027, 8, 6), new THREE.MeshBasicMaterial({color: 0xa33f46}));
        globe.add(traveler);
        routes.push({curve, traveler, offset: index * 0.24});
    });
    const projected = new THREE.Vector3();
    const worldPosition = new THREE.Vector3();
    const cameraDirection = new THREE.Vector3();
    const normal = new THREE.Vector3();
    let labelTime = -1;
    function render(delta = 0) {
        if (targetRotation !== null) {
            globe.rotation.y += (targetRotation - globe.rotation.y) * Math.min(1, delta * 6);
            if (Math.abs(targetRotation - globe.rotation.y) < 0.004) targetRotation = null;
        } else if (!paused) globe.rotation.y += delta * 0.045;
        globe.updateMatrixWorld();
        if (delta === 0 || elapsed - labelTime >= 0.1 || targetRotation !== null) {
            labelTime = elapsed;
            cameraDirection.copy(camera.position).normalize();
            labels.forEach(({label, position}) => {
                worldPosition.copy(position).applyMatrix4(globe.matrixWorld);
                label.hidden = normal.copy(worldPosition).normalize().dot(cameraDirection) <= 0.2;
                projected.copy(worldPosition).project(camera);
                label.style.transform = `translate3d(${(projected.x + 1) * width / 2 + 10}px, ${(-projected.y + 1) * height / 2}px, 0) translateY(-50%)`;
            });
        }
        routes.forEach(({curve, traveler, offset}) => traveler.position.copy(curve.getPoint((elapsed * 0.09 + offset) % 1)));
        renderer.render(scene, camera);
        section.dataset.globeReady = 'true';
    }
    function canAnimate() { return visible && !document.hidden && (!paused || targetRotation !== null); }
    function tick(now) {
        frame = 0;
        if (!canAnimate()) { lastTime = 0; return; }
        if (!lastTime || now - lastTime >= 40) {
            const delta = lastTime ? Math.min((now - lastTime) / 1000, 0.1) : 0;
            lastTime = now;
            elapsed += delta;
            render(delta);
        }
        if (canAnimate()) frame = requestAnimationFrame(tick);
        else lastTime = 0;
    }
    function schedule() {
        section.dataset.globeAnimating=String(canAnimate());
        if (canAnimate() && !frame) frame = requestAnimationFrame(tick);
        else if (!canAnimate()) { cancelAnimationFrame(frame); frame = 0; lastTime = 0; }
    }
    function syncPause() {
        toggle.textContent = paused ? 'Resume globe' : 'Pause globe';
        toggle.setAttribute('aria-pressed', String(paused));
        render(); schedule();
    }
    toggle.addEventListener('click', () => { paused = !paused; syncPause(); });
    motion.addEventListener('change', event => { if (event.matches) { paused = true; targetRotation = null; syncPause(); } });
    controls.addEventListener('start', () => { paused = true; targetRotation = null; syncPause(); });
    controls.addEventListener('change', () => render());
    section.querySelectorAll('[data-globe-region]').forEach(button => button.addEventListener('click', () => {
        section.querySelectorAll('[data-globe-region]').forEach(other => other.setAttribute('aria-pressed', String(other === button)));
        controls.reset();
        const rotations = {asia: -1.25, europe: -0.12, america: 1.72};
        paused = true;
        let rotation = rotations[button.dataset.globeRegion];
        while (rotation - globe.rotation.y > Math.PI) rotation -= Math.PI * 2;
        while (rotation - globe.rotation.y < -Math.PI) rotation += Math.PI * 2;
        if (motion.matches) globe.rotation.y = rotation;
        else targetRotation = rotation;
        syncPause();
    }));
    new IntersectionObserver(entries => { visible = entries[0].isIntersecting; schedule(); }, {threshold: 0.05}).observe(canvas);
    document.addEventListener('visibilitychange', schedule);
    new ResizeObserver(() => {
        const bounds = canvas.parentElement.getBoundingClientRect();
        width = Math.max(1, bounds.width); height = Math.max(1, bounds.height);
        renderer.setSize(width, height, false);
        camera.aspect = width / height; camera.updateProjectionMatrix();
        render();
    }).observe(canvas.parentElement);
    syncPause();
}
