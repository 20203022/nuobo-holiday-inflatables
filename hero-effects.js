import * as THREE from './vendor/three.module.js';
import { RoomEnvironment } from './vendor/RoomEnvironment.js';

export async function initHeroScene(hero) {
    const canvas = hero.querySelector('#nuobo-hero-scene');
    const toggle = hero.querySelector('#hero-scene-toggle');
    const motion = matchMedia('(prefers-reduced-motion: reduce)');
    const renderer = new THREE.WebGLRenderer({canvas, alpha:true, antialias:true, powerPreference:'low-power'});
    renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 1.25));
    renderer.setClearColor(0x000000, 0);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;
    const scene = new THREE.Scene();
    const environment = new RoomEnvironment();
    const pmrem = new THREE.PMREMGenerator(renderer);
    const environmentMap = pmrem.fromScene(environment,0.04);
    scene.environment = environmentMap.texture;
    environment.dispose(); pmrem.dispose();
    const camera = new THREE.OrthographicCamera(-10, 10, 5, -5, 0.1, 100);
    camera.position.z = 20;
    scene.add(new THREE.AmbientLight(0xffffff, 1.8));
    const key = new THREE.DirectionalLight(0xfff2d7, 3);
    key.position.set(-3, 5, 8); scene.add(key);
    const fill = new THREE.DirectionalLight(0xffffff, 1.6);
    fill.position.set(4, -1, 5); scene.add(fill);
    const stage = new THREE.Group(); scene.add(stage);
    const loader = new THREE.TextureLoader();
    const textures = new Map();
    const paths = ['/images/editorial/QM0924-3M-cutout.webp', '/images/editorial/QM01185-2M-cutout.webp', '/images/editorial/QM1524-1.5M-cutout.webp'];
    let destroyed = false;
    let visible = false;
    let paused = motion.matches;
    let frame = 0;
    let last = 0;
    let time = 0;
    let bounds;
    let unit = 1;
    let stageWidth = 10;
    let theme = hero.dataset.sceneTheme || 'christmas';
    const pointer = new THREE.Vector2();
    const targetPointer = new THREE.Vector2();
    try {
        // All three small textures load together, so rapid theme changes never race.
        await Promise.all(paths.map(async path => {
            const texture = await loader.loadAsync(path);
            if (destroyed) { texture.dispose(); return; }
            texture.colorSpace = THREE.SRGBColorSpace;
            texture.anisotropy = Math.min(4, renderer.capabilities.getMaxAnisotropy());
            textures.set(path, texture);
        }));
    } catch (error) {
        destroyed=true; textures.forEach(texture => texture.dispose()); environmentMap.dispose(); renderer.dispose(); throw error;
    }
    theme = hero.dataset.sceneTheme || 'christmas';
    const productGeometry = new THREE.PlaneGeometry(1, 1);
    const products = Array.from({length:2}, () => {
        const mesh = new THREE.Mesh(productGeometry, new THREE.MeshBasicMaterial({transparent:true, alphaTest:0.04, toneMapped:false, depthWrite:true}));
        stage.add(mesh); return mesh;
    });
    const gold = new THREE.MeshStandardMaterial({color:0xd9bc7f, metalness:0.72, roughness:0.28, side:THREE.DoubleSide});
    const starShape = new THREE.Shape();
    for (let i=0;i<10;i++) {
        const angle = i * Math.PI / 5 + Math.PI / 2;
        const radius = i % 2 ? 0.44 : 1;
        const x = Math.cos(angle) * radius, y = Math.sin(angle) * radius;
        if (i===0) starShape.moveTo(x,y); else starShape.lineTo(x,y);
    }
    starShape.closePath();
    const starGeometry = new THREE.ExtrudeGeometry(starShape, {depth:0.16, bevelEnabled:true, bevelThickness:0.055, bevelSize:0.045, bevelSegments:2, steps:1});
    starGeometry.center();
    const stars = [new THREE.Mesh(starGeometry,gold),new THREE.Mesh(starGeometry,gold)];
    stars.forEach(star => stage.add(star));
    const moonShape = new THREE.Shape();
    moonShape.moveTo(0.35,0.92);
    moonShape.bezierCurveTo(-1.3,0.83,-1.25,-0.85,0.35,-0.92);
    moonShape.bezierCurveTo(-0.38,-0.5,-0.4,0.55,0.35,0.92);
    const moonGeometry = new THREE.ExtrudeGeometry(moonShape,{depth:0.12,bevelEnabled:true,bevelSize:0.035,bevelThickness:0.035,bevelSegments:2,steps:1});
    moonGeometry.center();
    const moon = new THREE.Mesh(moonGeometry,gold); stage.add(moon);
    let ribbonGeometry;
    const ribbon = new THREE.Mesh(new THREE.BufferGeometry(),gold); stage.add(ribbon);

    function rebuildRibbon() {
        const curve = new THREE.CatmullRomCurve3([
            new THREE.Vector3(-stageWidth*.46,-2.85,-0.8),
            new THREE.Vector3(-stageWidth*.12,-3.5,-0.8),
            new THREE.Vector3(stageWidth*.34,-2.8,-1.1),
            new THREE.Vector3(stageWidth*.48,-.2,-1.5),
            new THREE.Vector3(stageWidth*.36,2.3,-1.3),
            new THREE.Vector3(stageWidth*.13,3.35,-.9)
        ]);
        const vertices = [], indices = [];
        for(let i=0;i<=96;i++) {
            const t=i/96;
            const point=curve.getPoint(t);
            const tangent=curve.getTangent(t);
            const twist=t*Math.PI*2.5;
            const side = new THREE.Vector3(-tangent.y,tangent.x,0).normalize();
            side.multiplyScalar(Math.cos(twist)).addScaledVector(new THREE.Vector3(0,0,1),Math.sin(twist)).multiplyScalar(.11);
            vertices.push(point.x-side.x,point.y-side.y,point.z-side.z,point.x+side.x,point.y+side.y,point.z+side.z);
            if(i<96){const k=i*2;indices.push(k,k+1,k+2,k+1,k+3,k+2);}
        }
        ribbonGeometry?.dispose();
        ribbonGeometry = new THREE.BufferGeometry();
        ribbonGeometry.setAttribute('position',new THREE.Float32BufferAttribute(vertices,3));
        ribbonGeometry.setIndex(indices); ribbonGeometry.computeVertexNormals();
        ribbon.geometry = ribbonGeometry;
    }
    function setProduct(mesh, image, path, index) {
        const texture=textures.get(path);
        mesh.material.map=texture; mesh.material.needsUpdate=true;
        const rect=image.getBoundingClientRect();
        const ratio=texture.image.width/texture.image.height;
        const height=Math.min(rect.height,rect.width/ratio);
        const width=height*ratio;
        mesh.scale.set(width*unit,height*unit,1);
        mesh.position.set((rect.left+rect.width/2-bounds.left-bounds.width/2)*unit-stage.position.x,(bounds.top+bounds.height/2-rect.top-rect.height/2)*unit-stage.position.y,index*.4+.3);
        mesh.userData.baseY=mesh.position.y;
    }
    function resize() {
        if(destroyed)return;
        bounds=hero.getBoundingClientRect();
        unit=10/Math.max(1,bounds.height);
        const aspect=bounds.width/Math.max(1,bounds.height);
        renderer.setSize(bounds.width,bounds.height,false);
        camera.left=-aspect*5;camera.right=aspect*5;camera.updateProjectionMatrix();
        const host=hero.querySelector('[data-scene-products]').getBoundingClientRect();
        stage.position.set((host.left+host.width/2-bounds.left-bounds.width/2)*unit,(bounds.top+bounds.height/2-host.top-host.height/2)*unit,0);
        stageWidth=host.width*unit;
        const primary=hero.querySelector('.hero-product-tree');
        const second=hero.querySelector('.hero-product-stack');
        setProduct(products[0],primary,theme==='christmas'?paths[0]:paths[2],0);
        products[1].visible=theme==='christmas';
        if(products[1].visible)setProduct(products[1],second,paths[1],1);
        stars[0].position.set(-stageWidth*.45,1.6,1);
        stars[0].scale.setScalar(.38);
        stars[1].position.set(stageWidth*.18,3.45,-.4);
        stars[1].scale.setScalar(.22);
        stars.forEach(star=>{star.visible=theme==='christmas';});
        moon.visible=theme==='halloween';moon.position.set(stageWidth*.28,2.65,-.6);moon.scale.setScalar(.72);
        rebuildRibbon();render();
    }
    function render() {
        if(destroyed)return;
        const active=!paused&&!motion.matches;
        if(active)pointer.lerp(targetPointer,0.075);
        stage.rotation.y=pointer.x*.035;
        stage.rotation.x=-pointer.y*.02;
        products.forEach((mesh,i)=>{mesh.position.y=mesh.userData.baseY+(motion.matches?0:Math.sin(time*.8+i*1.8)*.055);});
        stars.forEach((star,i)=>{star.rotation.set(.15,motion.matches ? .35 : Math.sin(time*.45+i)*.6,motion.matches ? .12 : time*.08*(i?-1:1));});
        moon.rotation.y=motion.matches ? .12 : Math.sin(time*.35)*.24;
        ribbon.rotation.y=motion.matches?0:Math.sin(time*.32)*.055;
        renderer.render(scene,camera);
    }
    function active() {return !destroyed&&visible&&!paused&&!document.hidden&&!motion.matches;}
    function tick(now) {
        frame=0;
        if(!active()){last=0;return;}
        if(!last||now-last>=40){time+=last?Math.min((now-last)/1000,.1):0;last=now;render();}
        frame=requestAnimationFrame(tick);
    }
    function schedule() {
        hero.dataset.sceneAnimating=String(active());
        if(active()&&!frame)frame=requestAnimationFrame(tick);
        else if(!active()){cancelAnimationFrame(frame);frame=0;last=0;}
    }
    function sync() {toggle.textContent=paused?'Resume scene':'Pause scene';toggle.setAttribute('aria-pressed',String(paused));render();schedule();}
    const onPointer=event=>{
        if(paused||motion.matches||event.pointerType!=='mouse')return;
        targetPointer.set(THREE.MathUtils.clamp((event.clientX-bounds.left)/bounds.width*2-1,-1,1),THREE.MathUtils.clamp((event.clientY-bounds.top)/bounds.height*2-1,-1,1));
    };
    const onEnter=()=>{bounds=hero.getBoundingClientRect();};
    const onLeave=()=>targetPointer.set(0,0);
    const onToggle=()=>{paused=!paused;sync();};
    const onMotion=event=>{if(event.matches){paused=true;targetPointer.set(0,0);pointer.set(0,0);sync();}};
    const onTheme=event=>{theme=event.detail;resize();};
    const observer=new IntersectionObserver(entries=>{visible=entries[0].isIntersecting;if(visible)bounds=hero.getBoundingClientRect();schedule();});
    const resizeObserver=new ResizeObserver(resize);
    function dispose() {
        if(destroyed)return;destroyed=true;cancelAnimationFrame(frame);observer.disconnect();resizeObserver.disconnect();
        hero.dataset.sceneAnimating='false';
        hero.removeEventListener('pointerenter',onEnter);hero.removeEventListener('pointermove',onPointer);hero.removeEventListener('pointerleave',onLeave);hero.removeEventListener('nuobo:theme',onTheme);
        toggle.removeEventListener('click',onToggle);motion.removeEventListener('change',onMotion);document.removeEventListener('visibilitychange',schedule);
        productGeometry.dispose();products.forEach(mesh=>mesh.material.dispose());starGeometry.dispose();moonGeometry.dispose();ribbon.geometry.dispose();gold.dispose();textures.forEach(texture=>texture.dispose());environmentMap.dispose();renderer.dispose();
    }
    canvas.addEventListener('webglcontextlost',event=>{event.preventDefault();delete hero.dataset.sceneReady;toggle.hidden=true;dispose();},{once:true});
    window.addEventListener('pagehide',event=>{if(!event.persisted)dispose();},{once:true});
    hero.addEventListener('pointerenter',onEnter,{passive:true});hero.addEventListener('pointermove',onPointer,{passive:true});hero.addEventListener('pointerleave',onLeave);hero.addEventListener('nuobo:theme',onTheme);
    toggle.addEventListener('click',onToggle);motion.addEventListener('change',onMotion);document.addEventListener('visibilitychange',schedule);
    resize();hero.dataset.sceneReady='true';toggle.hidden=false;sync();
    observer.observe(hero);resizeObserver.observe(hero);
}
