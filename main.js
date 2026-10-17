const initElementPlus = () => {
    const root = document.getElementById('nuobo-el-root');
    if (!window.Vue || !window.ElementPlus) {
        document.documentElement.classList.add('element-fallback');
        return;
    }
    if (root) {
        const app = window.Vue.createApp({
            render: () => window.Vue.h(window.ElementPlus.ElBacktop, { right: 30, bottom: 30 }, () => window.Vue.h('span', { class: 'el-backtop-mark', 'aria-hidden': 'true' }, '\u2191'))
        });
        app.mount(root);
    }
    window.NuoboUI = {
        message(options) {
            return window.ElementPlus.ElMessage({ duration: 2200, showClose: true, ...options });
        },
        notification(options) {
            return window.ElementPlus.ElNotification({ duration: 3000, ...options });
        }
    };
};

const trackNuoboEvent = (event, details = {}) => {
    const token = document.querySelector('meta[name="_csrf"]')?.content || '';
    const data = new URLSearchParams({
        event,
        path: window.location.pathname,
        productId: details.productId ? String(details.productId) : '',
        value: details.value || '',
        _csrf: token
    });
    try {
        if (navigator.sendBeacon && token) {
            navigator.sendBeacon('/analytics/event', data);
        } else {
            fetch('/analytics/event', { method: 'POST', body: data, keepalive: true }).catch(() => {});
        }
    } catch { /* Analytics must never interrupt the buyer journey. */ }
};

document.addEventListener('DOMContentLoaded', () => {
    const loadUiRuntime = () => {
        if (window.Vue && window.ElementPlus) { initElementPlus(); return; }
        const script = document.createElement('script');
        script.src = '/js/vendor/ui-runtime.js?v=20261013-compact-v1';
        script.async = true;
        script.onload = initElementPlus;
        document.head.appendChild(script);
    };
    if ('requestIdleCallback' in window) requestIdleCallback(loadUiRuntime, {timeout: 1800});
    else setTimeout(loadUiRuntime, 800);
    const schedulePageView = () => trackNuoboEvent('page_view');
    if ('requestIdleCallback' in window) requestIdleCallback(schedulePageView, {timeout: 2500});
    else setTimeout(schedulePageView, 1200);
    document.querySelectorAll('form[action="/cart/add"]').forEach(form => form.addEventListener('submit', () => {
        trackNuoboEvent('cart_add', { productId: form.querySelector('[name="productId"]')?.value });
    }));
    document.querySelectorAll('form[action="/contact"]').forEach(form => form.addEventListener('submit', () => trackNuoboEvent('inquiry_submit')));
    document.querySelectorAll('.simple-catalog-search').forEach(form => form.addEventListener('submit', () => {
        trackNuoboEvent('search_submit', { value: form.querySelector('[name="q"]')?.value });
    }));
    const detailId = document.querySelector('[data-product-id]')?.dataset.productId;
    if (detailId && document.querySelector('.product-detail-grid')) trackNuoboEvent('product_view', { productId: detailId });
    const detailMeta = document.querySelector('.product-detail-grid .detail-meta');
    if (detailMeta) {
        const readiness = document.createElement('div');
        readiness.className = 'detail-readiness';
        readiness.innerHTML = '<div class="detail-readiness-header"><span>Buyer information checklist</span><strong>Ready to confirm</strong></div><p>For carton size, voltage, plug, private-label packaging or delivery timing, add your requirements to the bulk inquiry.</p>';
        detailMeta.before(readiness);
    }
    const storyCopy = document.querySelector('.home-story-copy');
    if (storyCopy && !storyCopy.querySelector('.home-proof-strip')) {
        const proof = document.createElement('div');
        proof.className = 'home-proof-strip';
        proof.innerHTML = '<figure><img src="/images/company/factory-testing.png" width="640" height="480" alt="Nuobo team testing an inflatable product" loading="lazy"><figcaption>Inflation testing</figcaption></figure><figure><img src="/images/company/factory-sewing.png" width="640" height="480" alt="Nuobo industrial sewing production" loading="lazy"><figcaption>Industrial sewing</figcaption></figure><p><strong>Made in Yiwu, checked before export.</strong><span>Real production and quality-control imagery shows how each order moves from design to packing.</span></p>';
        storyCopy.querySelector('.home-craft-lines')?.after(proof);
    }
    import('/js/motion-details.js?v=20261013-three-v1').then(module => module.initMotionDetails()).catch(error => console.warn('Detail motion unavailable', error));
    const heroStage = document.querySelector('.holiday-hero');
    if (heroStage) {
        const productLink = heroStage.querySelector('[data-scene-products]');
        const primaryImage = productLink.querySelector('.hero-product-tree');
        const secondImage = productLink.querySelector('.hero-product-stack');
        heroStage.dataset.sceneTheme = 'christmas';
        heroStage.querySelectorAll('[data-scene-theme]').forEach(button => button.addEventListener('click', () => {
            const theme = button.dataset.sceneTheme;
            if (heroStage.dataset.sceneTheme === theme) return;
            heroStage.dataset.sceneTheme = theme;
            heroStage.querySelectorAll('[data-scene-theme]').forEach(other => other.setAttribute('aria-pressed', String(other === button)));
            productLink.href = '/category/' + theme;
            productLink.setAttribute('aria-label', `Explore Nuobo ${theme === 'christmas' ? 'Christmas' : 'Halloween'} inflatables`);
            primaryImage.src = theme === 'christmas' ? '/images/editorial/QM0924-3M-cutout.webp' : '/images/editorial/QM1524-1.5M-cutout.webp';
            primaryImage.alt = theme === 'christmas' ? 'Nuobo Christmas dragon and decorated tree inflatable' : 'Nuobo illuminated Halloween spider inflatable';
            secondImage.hidden = theme === 'halloween';
            heroStage.dispatchEvent(new CustomEvent('nuobo:theme', {detail: theme}));
        }));
        const loadHeroScene = async () => {
            const connection = navigator.connection;
            if (connection?.saveData || /(^|-)2g$/.test(connection?.effectiveType || '')) return;
            try {
                const module = await import('/js/vendor/hero-runtime.js?v=20261013-three-v1');
                await module.initHeroScene(heroStage);
            } catch (error) {
                delete heroStage.dataset.sceneReady;
                heroStage.querySelector('#hero-scene-toggle').hidden = true;
                console.warn('The holiday scene is using product photography.', error);
            }
        };
        if ('requestIdleCallback' in window) requestIdleCallback(loadHeroScene, {timeout:5000});
        else setTimeout(loadHeroScene, 3500);
    }
    const globalStage = document.getElementById('global-reach');
    if (globalStage) {
        import('/js/wave-effects.js?v=20261013-compact-v1').then(module => module.initWave(globalStage)).catch(error => console.warn('Wave visualization unavailable', error));
        const loadGlobalEffects = async () => {
            try {
                const effects = await import('/js/vendor/globe-runtime.js?v=20261013-world-v2');
                await effects.initGlobalEffects(globalStage);
            } catch (error) {
                globalStage.querySelector('#nuobo-globe-canvas').hidden = true;
                globalStage.querySelector('.world-globe-fallback').hidden = false;
                globalStage.querySelector('#global-globe-toggle').hidden = true;
                globalStage.querySelector('.world-market-tabs').hidden = true;
                console.warn('Global market visualization is using the static map.', error);
            }
        };
        if ('IntersectionObserver' in window) {
            const globeLoader = new IntersectionObserver(entries => {
                if (!entries.some(entry => entry.isIntersecting)) return;
                globeLoader.disconnect();
                const start = () => {
                    const connection = navigator.connection;
                    if (connection?.saveData || /(^|-)2g$/.test(connection?.effectiveType || '')) return;
                    loadGlobalEffects();
                };
                if ('requestIdleCallback' in window) requestIdleCallback(start, {timeout:1800});
                else setTimeout(start, 700);
            }, { rootMargin: '150px 0px' });
            globeLoader.observe(globalStage.querySelector('#nuobo-globe-canvas'));
        } else {
            loadGlobalEffects();
        }
    }
    const productTabs = [...document.querySelectorAll('[data-product-tab]')];
    const selectProductTab = tab => {
        productTabs.forEach(other => {
            const selected = tab === other;
            other.setAttribute('aria-selected', String(selected));
            other.tabIndex = selected ? 0 : -1;
            const panel = document.getElementById(other.dataset.productTab);
            if (panel) panel.hidden = !selected;
            if (selected && panel && !matchMedia('(prefers-reduced-motion: reduce)').matches) panel.animate([{opacity:0.35,transform:'translateY(8px)'},{opacity:1,transform:'translateY(0)'}], {duration:280,easing:'cubic-bezier(.22,1,.36,1)'});
        });
    };
    productTabs.forEach((tab, index) => {
        tab.addEventListener('click', () => selectProductTab(tab));
        tab.addEventListener('keydown', event => {
            const targets = { ArrowRight: (index + 1) % productTabs.length, ArrowLeft: (index + productTabs.length - 1) % productTabs.length, Home: 0, End: productTabs.length - 1 };
            if (!(event.key in targets)) return;
            event.preventDefault();
            const next = productTabs[targets[event.key]];
            selectProductTab(next);
            next.focus();
        });
    });
    const filmDialog = document.getElementById('home-film-dialog');
    if (filmDialog) {
        const video = filmDialog.querySelector('video');
        document.querySelector('[data-film-open]').addEventListener('click', () => {
            filmDialog.showModal();
            video.play().catch(() => {});
        });
        filmDialog.querySelector('[data-film-close]').addEventListener('click', () => filmDialog.close());
        filmDialog.addEventListener('click', event => { if (event.target === filmDialog && (event.clientX < filmDialog.getBoundingClientRect().left || event.clientX > filmDialog.getBoundingClientRect().right || event.clientY < filmDialog.getBoundingClientRect().top || event.clientY > filmDialog.getBoundingClientRect().bottom)) filmDialog.close(); });
        filmDialog.addEventListener('close', () => { video.pause(); video.currentTime = 0; });
    }
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    document.documentElement.classList.add('motion-ready');
    document.body.classList.add('premium-ready');

    const revealItems = [...document.querySelectorAll('.fade-in')];
    revealItems.forEach((item, index) => {
        item.classList.add('scroll-reveal');
        item.style.setProperty('--reveal-delay', `${Math.min(index % 6, 5) * 65}ms`);
    });

    if (reducedMotion || !('IntersectionObserver' in window)) {
        revealItems.forEach(item => item.classList.add('is-visible'));
    } else {
        const revealObserver = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                entry.target.classList.add('is-visible');
                revealObserver.unobserve(entry.target);
            });
        }, { threshold: 0.12, rootMargin: '0px 0px -7% 0px' });
        revealItems.forEach(item => revealObserver.observe(item));
    }

    const progress = document.createElement('div');
    progress.className = 'scroll-progress';
    progress.setAttribute('aria-hidden', 'true');
    document.body.appendChild(progress);

    let scrollFrame = 0;
    const updateMotion = () => {
        scrollFrame = 0;
        const documentHeight = document.documentElement.scrollHeight - window.innerHeight;
        const progressRatio = documentHeight > 0 ? window.scrollY / documentHeight : 0;
        progress.style.transform = `scaleX(${Math.min(1, Math.max(0, progressRatio))})`;
    };
    const requestMotionUpdate = () => {
        if (!scrollFrame) scrollFrame = window.requestAnimationFrame(updateMotion);
    };
    window.addEventListener('scroll', requestMotionUpdate, { passive: true });
    window.addEventListener('resize', requestMotionUpdate, { passive: true });
    requestMotionUpdate();

    const header = document.querySelector('.site-header');
    const updateHeaderState = () => header?.classList.toggle('is-scrolled', window.scrollY > 24);
    updateHeaderState();
    window.addEventListener('scroll', updateHeaderState, { passive: true });

    const animateCounter = element => {
        if (element.dataset.counted === 'true') return;
        const target = Number.parseInt(element.textContent.trim(), 10);
        if (!Number.isFinite(target)) return;
        element.dataset.counted = 'true';
        const duration = 850;
        const start = performance.now();
        const tick = now => {
            const ratio = Math.min(1, (now - start) / duration);
            const eased = 1 - Math.pow(1 - ratio, 3);
            element.textContent = String(Math.round(target * eased));
            if (ratio < 1) window.requestAnimationFrame(tick);
        };
        window.requestAnimationFrame(tick);
    };
    const counters = [...document.querySelectorAll('.countdown .number')];
    if (reducedMotion || !('IntersectionObserver' in window)) {
        counters.forEach(element => { element.dataset.counted = 'true'; });
    } else {
        const counterObserver = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) return;
                animateCounter(entry.target);
                counterObserver.unobserve(entry.target);
            });
        }, { threshold: 0.8 });
        counters.forEach(counter => counterObserver.observe(counter));
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    document.querySelectorAll('form[method="post"], form[method="POST"]').forEach(form => {
        if (!csrfToken || form.querySelector('input[name="_csrf"]')) return;
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = csrfToken;
        form.appendChild(input);
    });

    const hamburger = document.getElementById('hamburger');
    const mobileMenu = document.getElementById('mobileMenu');
    if (hamburger && mobileMenu) {
        const closeMenu = () => {
            mobileMenu.classList.remove('is-open');
            hamburger.classList.remove('open');
            hamburger.setAttribute('aria-expanded', 'false');
        };
        closeMenu();
        hamburger.addEventListener('click', () => {
            const open = mobileMenu.classList.toggle('is-open');
            hamburger.classList.toggle('open', open);
            hamburger.setAttribute('aria-expanded', String(open));
        });
        mobileMenu.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
        window.addEventListener('resize', () => { if (window.innerWidth > 820) closeMenu(); });
    }

    document.querySelectorAll('[data-home-video-play]').forEach(button => {
        const frame = button.closest('figure');
        const video = frame?.querySelector('video');
        if (!video) return;
        button.addEventListener('click', async () => {
            button.hidden = true;
            frame.classList.add('is-playing');
            video.controls = true;
            try {
                await video.play();
            } catch {
                video.controls = false;
                frame.classList.remove('is-playing');
                button.hidden = false;
            }
        });
        video.addEventListener('ended', () => {
            video.controls = false;
            frame.classList.remove('is-playing');
            button.hidden = false;
        });
    });

    const formatCartAmount = amount => `$${amount.toFixed(2)}`;
    const cartSaveStatus = document.createElement('p');
    cartSaveStatus.className = 'cart-page-subtitle';
    cartSaveStatus.setAttribute('role', 'status');
    document.querySelector('.cart-qty-form')?.closest('.cart-layout')?.before(cartSaveStatus);
    const pendingCartForms = new Set();
    let cartSaveTimer;
    let cartSaveChain = Promise.resolve();
    let cartSaveFailed = false;
    const flushCartChanges = () => {
        clearTimeout(cartSaveTimer);
        const changes = [...pendingCartForms].map(form => {
            const input = form.querySelector('.cart-qty-input');
            input.value = String(normalizeCartQuantity(input));
            return { form, body: new URLSearchParams(new FormData(form)) };
        });
        pendingCartForms.clear();
        if (!changes.length) return cartSaveChain;
        cartSaveStatus.textContent = 'Saving quantities...';
        cartSaveChain = cartSaveChain.then(async () => {
            cartSaveFailed = false;
            for (const { form, body } of changes) {
                try {
                    const response = await fetch(form.action, {
                        method: 'POST', body, headers: { 'X-Requested-With': 'XMLHttpRequest' },
                        signal: AbortSignal.timeout(20000)
                    });
                    if (!response.ok || response.redirected) throw new Error('Cart update failed');
                    const saved = await response.json();
                    const input = form.querySelector('.cart-qty-input');
                    if (saved.removed) throw new Error('Product availability changed. Refresh your cart.');
                    if (input.value === body.get('quantity')) {
                        input.max = String(saved.stock);
                        input.value = String(saved.quantity);
                        const price = form.closest('.cart-line-item').querySelector('.cart-item-price');
                        price.dataset.unitPrice = String(saved.unitPrice);
                        price.textContent = formatCartAmount(saved.unitPrice);
                        const control = input.closest('.cart-qty');
                        control.querySelector('.cart-qty-minus').disabled = saved.quantity <= 1;
                        control.querySelector('.cart-qty-plus').disabled = saved.quantity >= saved.stock;
                        updateCartTotals();
                    }
                } catch {
                    cartSaveFailed = true;
                    pendingCartForms.add(form);
                }
            }
            cartSaveStatus.textContent = cartSaveFailed ? 'Could not save quantities. Please try Update again.' : 'Quantities saved';
        });
        return cartSaveChain;
    };
    const scheduleCartSave = input => {
        const form = input.closest('.cart-qty-form');
        if (!form || !form.closest('.cart-line-item')) return;
        pendingCartForms.add(form);
        clearTimeout(cartSaveTimer);
        cartSaveTimer = setTimeout(flushCartChanges, 400);
    };
    document.querySelector('.cart-summary-card a[href="/checkout"]')?.addEventListener('click', async event => {
        event.preventDefault();
        const link = event.currentTarget;
        await flushCartChanges();
        if (!cartSaveFailed) window.location.assign(link.href);
    });
    const normalizeCartQuantity = input => {
        const min = Number.parseInt(input.min, 10) || 1;
        const declaredMax = Number.parseInt(input.max, 10);
        const max = Number.isInteger(declaredMax) && declaredMax >= min ? declaredMax : Number.MAX_SAFE_INTEGER;
        const requested = Number.parseInt(input.value, 10);
        return Math.min(max, Math.max(min, Number.isInteger(requested) ? requested : min));
    };
    const updateCartTotals = () => {
        let subtotal = 0;
        document.querySelectorAll('.cart-line-item').forEach(item => {
            const quantityInput = item.querySelector('.cart-qty-input');
            const unitPrice = Number.parseFloat(item.querySelector('.cart-item-price')?.dataset.unitPrice || '');
            if (!quantityInput || !Number.isFinite(unitPrice)) return;
            const lineTotal = normalizeCartQuantity(quantityInput) * unitPrice;
            subtotal += lineTotal;
            const lineTotalElement = item.querySelector('.cart-subtotal-value');
            if (lineTotalElement) lineTotalElement.textContent = formatCartAmount(lineTotal);
        });
        if (!document.querySelector('.cart-line-item')) return;
        document.querySelectorAll('.cart-head-subtotal, .cart-summary-subtotal, .cart-summary-total strong').forEach(element => {
            element.textContent = formatCartAmount(subtotal);
        });
    };
    document.querySelectorAll('.cart-qty-input').forEach(input => {
        const control = input.closest('.cart-qty, .detail-qty-controls');
        if (!control) return;
        const decrease = control.querySelector('.cart-qty-minus');
        const increase = control.querySelector('.cart-qty-plus');
        const syncQuantity = () => {
            const quantity = normalizeCartQuantity(input);
            input.value = String(quantity);
            const min = Number.parseInt(input.min, 10) || 1;
            const max = Number.parseInt(input.max, 10);
            if (decrease) decrease.disabled = quantity <= min;
            if (increase) increase.disabled = Number.isInteger(max) && quantity >= max;
            updateCartTotals();
        };
        decrease?.addEventListener('click', () => {
            input.value = String(normalizeCartQuantity(input) - 1);
            syncQuantity();
            scheduleCartSave(input);
        });
        increase?.addEventListener('click', () => {
            input.value = String(normalizeCartQuantity(input) + 1);
            syncQuantity();
            scheduleCartSave(input);
        });
        input.addEventListener('input', () => { updateCartTotals(); scheduleCartSave(input); });
        input.addEventListener('change', () => { syncQuantity(); scheduleCartSave(input); });
        syncQuantity();
    });

    const storageKey = 'nuoboBulkInquiryProducts';
    const normalizeQuantity = value => {
        const quantity = Number.parseInt(String(value), 10);
        return Number.isInteger(quantity) && quantity > 0 ? Math.min(quantity, 100000) : 1;
    };
    const readSelectedProducts = () => {
        try {
            const saved = JSON.parse(window.localStorage.getItem(storageKey) || '[]');
            return Array.isArray(saved)
                ? saved
                    .filter(product => product && /^\d+$/.test(String(product.id)))
                    .map(product => ({ ...product, quantity: normalizeQuantity(product.quantity) }))
                : [];
        } catch {
            return [];
        }
    };
    const writeSelectedProducts = products => window.localStorage.setItem(storageKey, JSON.stringify(products));
    const selectedProducts = () => readSelectedProducts();
    const isSelected = id => selectedProducts().some(product => String(product.id) === String(id));
    const setButtonState = button => {
        const selected = isSelected(button.dataset.productId);
        button.classList.toggle('is-selected', selected);
        button.setAttribute('aria-pressed', String(selected));
        button.textContent = selected ? 'Added to Inquiry' : (button.classList.contains('detail-bulk-inquiry') ? 'Add to Bulk Inquiry' : 'Add to Inquiry');
    };
    const updateButtons = () => document.querySelectorAll('.bulk-inquiry-add').forEach(setButtonState);
    const updateInquiryNav = () => {
        const badge = document.getElementById('inquiryNavCount');
        if (!badge) return;
        const count = selectedProducts().length;
        badge.hidden = count === 0;
        badge.textContent = String(count);
    };
    const renderInquiryPage = () => {
        const emptyState = document.getElementById('inquiryEmptyState');
        const content = document.getElementById('inquiryContent');
        const list = document.getElementById('inquiryProductList');
        const form = document.getElementById('inquiryForm');
        if (!emptyState || !content || !list || !form) return;

        const products = selectedProducts();
        emptyState.hidden = products.length > 0;
        content.hidden = products.length === 0;
        const count = document.getElementById('inquiryProductCount');
        if (count) count.textContent = String(products.length);
        list.replaceChildren();
        form.querySelectorAll('[data-inquiry-product-input]').forEach(input => input.remove());

        products.forEach(product => {
            const row = document.createElement('article');
            row.className = 'inquiry-product-row';
            const imageFrame = document.createElement('div');
            imageFrame.className = 'inquiry-product-image';
            if (product.image) {
                const image = document.createElement('img');
                image.src = product.image;
                image.alt = product.name || product.model || 'Selected product';
                image.loading = 'lazy';
                imageFrame.appendChild(image);
            } else {
                imageFrame.classList.add('is-empty');
                imageFrame.textContent = 'Product';
            }
            const details = document.createElement('div');
            details.className = 'inquiry-product-details';
            const model = document.createElement('span');
            model.textContent = product.model || 'Model';
            const name = document.createElement('strong');
            name.textContent = product.name || 'Selected product';
            details.append(model, name);
            const quantity = normalizeQuantity(product.quantity);
            const quantityControl = document.createElement('div');
            quantityControl.className = 'inquiry-product-quantity';
            const quantityLabel = document.createElement('span');
            quantityLabel.textContent = 'Qty';
            const quantityControls = document.createElement('div');
            quantityControls.className = 'inquiry-quantity-controls';
            const decrease = document.createElement('button');
            decrease.type = 'button';
            decrease.className = 'inquiry-quantity-button';
            decrease.setAttribute('aria-label', `Decrease quantity for ${product.model || product.name || 'product'}`);
            decrease.textContent = '-';
            decrease.disabled = quantity <= 1;
            const quantityInput = document.createElement('input');
            quantityInput.type = 'number';
            quantityInput.className = 'inquiry-quantity-input';
            quantityInput.min = '1';
            quantityInput.max = '100000';
            quantityInput.value = String(quantity);
            quantityInput.setAttribute('aria-label', `Quantity for ${product.model || product.name || 'product'}`);
            const increase = document.createElement('button');
            increase.type = 'button';
            increase.className = 'inquiry-quantity-button';
            increase.setAttribute('aria-label', `Increase quantity for ${product.model || product.name || 'product'}`);
            increase.textContent = '+';
            increase.disabled = quantity >= 100000;
            const saveQuantity = (value, shouldNormalize = true) => {
                const updatedQuantity = normalizeQuantity(value);
                writeSelectedProducts(selectedProducts().map(selected => String(selected.id) === String(product.id)
                    ? { ...selected, quantity: updatedQuantity }
                    : selected));
                const submittedQuantity = form.querySelector(`[data-inquiry-product-quantity="${product.id}"]`);
                if (submittedQuantity) submittedQuantity.value = String(updatedQuantity);
                if (shouldNormalize) quantityInput.value = String(updatedQuantity);
                decrease.disabled = updatedQuantity <= 1;
                increase.disabled = updatedQuantity >= 100000;
            };
            decrease.addEventListener('click', () => saveQuantity(normalizeQuantity(quantityInput.value) - 1));
            increase.addEventListener('click', () => saveQuantity(normalizeQuantity(quantityInput.value) + 1));
            quantityInput.addEventListener('input', () => saveQuantity(quantityInput.value, false));
            quantityInput.addEventListener('change', () => saveQuantity(quantityInput.value));
            quantityControls.append(decrease, quantityInput, increase);
            quantityControl.append(quantityLabel, quantityControls);
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'inquiry-product-remove';
            remove.setAttribute('aria-label', `Remove ${product.model || product.name || 'product'}`);
            remove.textContent = 'Remove';
            remove.addEventListener('click', () => {
                writeSelectedProducts(selectedProducts().filter(selected => String(selected.id) !== String(product.id)));
                refreshBulkInquiry();
                window.NuoboUI?.message({ message: 'Product removed from inquiry', type: 'info' });
            });
            row.append(imageFrame, details, quantityControl, remove);
            list.appendChild(row);

            const idInput = document.createElement('input');
            idInput.type = 'hidden';
            idInput.name = 'productIds';
            idInput.value = String(product.id);
            idInput.dataset.inquiryProductInput = 'true';
            form.appendChild(idInput);

            const productQuantityInput = document.createElement('input');
            productQuantityInput.type = 'hidden';
            productQuantityInput.name = 'productQuantities';
            productQuantityInput.value = String(quantity);
            productQuantityInput.dataset.inquiryProductInput = 'true';
            productQuantityInput.dataset.inquiryProductQuantity = String(product.id);
            form.appendChild(productQuantityInput);
        });
    };
    const refreshBulkInquiry = () => {
        updateButtons();
        updateInquiryNav();
        renderInquiryPage();
    };

    document.querySelectorAll('.bulk-inquiry-add').forEach(button => {
        button.addEventListener('click', () => {
            const product = {
                id: button.dataset.productId,
                model: button.dataset.productModel,
                name: button.dataset.productName,
                image: button.dataset.productImage || '',
                quantity: 1
            };
            const products = selectedProducts();
            const wasSelected = isSelected(product.id);
            writeSelectedProducts(wasSelected
                ? products.filter(selected => String(selected.id) !== String(product.id))
                : [...products, product]);
            refreshBulkInquiry();
            window.NuoboUI?.message({ message: wasSelected ? 'Removed from inquiry' : 'Added to inquiry', type: wasSelected ? 'info' : 'success' });
        });
    });
    document.getElementById('inquiryPageClear')?.addEventListener('click', () => {
        window.localStorage.removeItem(storageKey);
        refreshBulkInquiry();
        window.NuoboUI?.message({ message: 'Inquiry list cleared', type: 'info' });
    });
    if (document.querySelector('.inquiry-success')) {
        window.localStorage.removeItem(storageKey);
    }
    refreshBulkInquiry();
    document.querySelectorAll('.simple-catalog-search input[name="q"]').forEach(input => {
        const wrapper = input.closest('.simple-catalog-search');
        const box = document.createElement('div');
        box.className = 'search-suggestions';
        box.hidden = true;
        input.parentElement?.classList.add('search-autocomplete');
        (input.parentElement || wrapper).appendChild(box);
        let timer;
        let controller;
        const close = () => { box.hidden = true; box.replaceChildren(); };
        input.addEventListener('input', () => {
            clearTimeout(timer);
            controller?.abort();
            const query = input.value.trim();
            if (query.length < 2) { close(); return; }
            timer = setTimeout(async () => {
                controller = new AbortController();
                try {
                    const response = await fetch(`/api/search-suggestions?q=${encodeURIComponent(query)}`, { signal: controller.signal });
                    if (!response.ok) return close();
                    const results = await response.json();
                    box.replaceChildren(...results.map(result => {
                        const link = document.createElement('a');
                        link.href = result.url;
                        const title = document.createElement('strong');
                        title.textContent = result.name || result.model;
                        const meta = document.createElement('small');
                        meta.textContent = `${result.model} · ${result.category}`;
                        link.append(title, meta);
                        return link;
                    }));
                    box.hidden = results.length === 0;
                } catch (error) { if (error.name !== 'AbortError') close(); }
            }, 180);
        });
        input.addEventListener('blur', () => setTimeout(close, 160));
    });
    window.addEventListener('storage', event => {
        if (event.key === storageKey || event.key === null) refreshBulkInquiry();
    });
});

function openImagePreview(src) {
    const modal = document.getElementById('imagePreviewModal');
    const target = document.getElementById('imagePreviewTarget');
    if (!modal || !target) return;
    target.src = src;
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeImagePreview(event) {
    if (event && event.target && event.target.id !== 'imagePreviewModal' && !event.target.classList.contains('image-preview-close')) return;
    const modal = document.getElementById('imagePreviewModal');
    if (!modal) return;
    modal.classList.remove('open');
    document.body.style.overflow = '';
}
