/* =========================================================
   AutoGestion - Visor 3D de vehículos
   ---------------------------------------------------------
   · Extruye la MISMA silueta que el render 2D (vehPerfil),
     así el 2D y el 3D comparten proporciones exactas.
   · three.js se importa bajo demanda desde /js/vendor
     (bundle local, sin CDN). Si falla o no hay WebGL,
     se cae al render SVG con un aviso.
   · Controles: arrastrar (orbit), rueda/pellizco (zoom),
     reset, auto-rotación opcional, pausa cuando no se ve.
   · El contexto WebGL y el entorno de estudio se reutilizan
     entre aperturas (sin reciclar contextos GPU, que provoca
     cuelgues de frames); al cerrar se liberan geometrías,
     materiales y texturas del vehículo y se retira el grupo.
   ========================================================= */
(function () {
  'use strict';
  if (window.__agVeh3D) return;
  window.__agVeh3D = true;

  /* ---------- ¿Hay WebGL? ---------- */
  function hayWebGL() {
    try {
      const c = document.createElement('canvas');
      return !!(window.WebGLRenderingContext && (c.getContext('webgl2') || c.getContext('webgl', { failIfMajorPerformanceCaveat: false })));
    } catch (e) { return false; }
  }
  if (!hayWebGL()) return;                 // sin WebGL: el botón 3D no aparece
  document.documentElement.classList.add('ag-veh3d-on');

  /* ---------- URL base del script (para import relativo) ---------- */
  const SCRIPTO = (document.currentScript && document.currentScript.src)
    || (document.querySelector('script[src*="vehiculo3d"]') || {}).src
    || location.href;
  const BASE = SCRIPTO.replace(/[^/]*$/, '');

  let threeP = null;
  function cargarThree() {
    if (!threeP) threeP = import(/* webpackIgnore */ BASE + 'vendor/three.module.min.js');
    return threeP;
  }

  /* ---------- Delegación de clics en botones .ag-veh-3d ---------- */
  document.addEventListener('click', function (e) {
    const btn = e.target && e.target.closest ? e.target.closest('.ag-veh-3d') : null;
    if (!btn) return;
    e.preventDefault();
    abrir({
      marca: btn.dataset.marca || '',
      modelo: btn.dataset.modelo || '',
      placa: btn.dataset.placa || '',
      anio: btn.dataset.anio || '',
      genero: btn.dataset.genero || ''
    });
  });
  window.vehAbrir3D = abrir;

  /* =======================================================
     MODAL
     ======================================================= */
  let ACTIVO = null;
  let GL = null;   // { canvas, renderer, scene, shared } compartidos entre aperturas

  function icono(nombre) {
    const s = 'viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"';
    const m = {
      rot: `<svg ${s}><path d="M21 12a9 9 0 1 1-2.64-6.36"/><polyline points="21 3 21 9 15 9"/></svg>`,
      reset: `<svg ${s}><path d="M3 12a9 9 0 1 0 2.64-6.36"/><polyline points="3 3 3 9 9 9"/><path d="M12 8v4l3 2"/></svg>`,
      close: `<svg ${s}><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
      cube: `<svg ${s}><path d="M12 2.5 21 7v10l-9 4.5L3 17V7z"/><path d="M3 7l9 4.5L21 7"/><path d="M12 11.5V21.5"/></svg>`
    };
    return m[nombre] || '';
  }

  function tituloDe(v) {
    const partes = [v.marca, v.modelo].filter(Boolean).join(' ');
    return partes || 'Vehículo';
  }

  async function abrir(v) {
    if (ACTIVO) cerrar();                       // una sola instancia WebGL
    const ov = document.createElement('div');
    ov.className = 'ag-3d-overlay';
    ov.innerHTML = `
      <div class="ag-3d-panel" role="dialog" aria-modal="true" aria-label="Visor 3D del vehículo">
        <div class="ag-3d-bar">
          <span class="ag-3d-ico">${icono('cube')}</span>
          <span class="ag-3d-tit"><b></b><small></small></span>
          <span class="ag-3d-acts">
            <button type="button" class="ag-3d-btn" data-act="rot" title="Rotación automática" aria-pressed="true">${icono('rot')}</button>
            <button type="button" class="ag-3d-btn" data-act="reset" title="Restablecer vista">${icono('reset')}</button>
            <button type="button" class="ag-3d-btn ag-3d-close" data-act="close" title="Cerrar">${icono('close')}</button>
          </span>
        </div>
        <div class="ag-3d-stage">
          <div class="ag-3d-load"><span class="ag-3d-spin"></span><em>Cargando visor 3D…</em></div>
          <canvas class="ag-3d-cv" tabindex="0" aria-label="Vista 3D: arrastra para girar"></canvas>
          <div class="ag-3d-msg" hidden></div>
        </div>
        <div class="ag-3d-hint">Arrastra para girar · rueda o pellizco para acercar</div>
      </div>`;
    ov.querySelector('.ag-3d-tit b').textContent = tituloDe(v);
    ov.querySelector('.ag-3d-tit small').textContent =
      [v.placa, v.anio].filter(Boolean).join(' · ') || 'Vista interactiva';

    document.body.appendChild(ov);
    document.body.classList.add('ag-3d-lock');

    const ctx = { ov, v, cerrado: false, raf: 0, disposables: [], observers: [], listeners: [] };
    ACTIVO = ctx;

    ctx.on = (el, tipo, fn, op) => { el.addEventListener(tipo, fn, op); ctx.listeners.push([el, tipo, fn, op]); };
    ov.addEventListener('click', e => {
      const b = e.target.closest('[data-act]'); if (!b) return;
      if (b.dataset.act === 'close') cerrar();
      if (b.dataset.act === 'reset' && ctx.reset) ctx.reset();
      if (b.dataset.act === 'rot' && ctx.rot) ctx.rot();
    });
    ctx.on(document, 'keydown', e => { if (e.key === 'Escape') cerrar(); });

    const stage = ov.querySelector('.ag-3d-stage');
    const canvas = ov.querySelector('.ag-3d-cv');
    const msg = ov.querySelector('.ag-3d-msg');

    try {
      const THREE = await cargarThree();
      if (ctx.cerrado) return;
      montar(THREE, ctx, canvas, stage);
    } catch (err) {
      if (ctx.cerrado) return;
      console.warn('[vehiculo3d] sin visor 3D:', err);
      msg.innerHTML = `<div class="ag-3d-fallback">
          <span class="ag-3d-fb-label">${icono('cube')} Vista 3D no disponible</span>
          <div class="ag-veh-render">${window.vehCarSVG ? vehCarSVG(v.marca, v.modelo, v.placa) : ''}</div>
          <small>Mostrando la vista lateral. El modelo 3D se cargará cuando el navegador lo permita.</small>
        </div>`;
      msg.hidden = false;
      stage.querySelectorAll('canvas.ag-3d-cv').forEach(c => c.remove());
      const ld = ov.querySelector('.ag-3d-load'); if (ld) ld.remove();
    }
    return ctx;
  }

  function cerrar() {
    const ctx = ACTIVO; if (!ctx) return;
    ctx.cerrado = true;
    if (ctx.raf) cancelAnimationFrame(ctx.raf);
    ctx.listeners.forEach(([el, t, f, o]) => el.removeEventListener(t, f, o));
    ctx.observers.forEach(o => { try { o.disconnect(); } catch (e) { /**/ } });
    ctx.disposables.forEach(d => { try { d && d.dispose && d.dispose(); } catch (e) { /**/ } });
    if (ctx.auto && ctx.auto.parent) ctx.auto.parent.remove(ctx.auto);
    ctx.ov.remove();
    document.body.classList.remove('ag-3d-lock');
    ACTIVO = null;
  }
  window.vehCerrar3D = cerrar;

  /* =======================================================
     ESCENA
     ======================================================= */
  function montar(THREE, ctx, canvas, stage) {
    const perf = {
      movil: matchMedia('(max-width: 700px), (pointer: coarse)').matches,
      dprCap: 2
    };
    if (perf.movil) perf.dprCap = 1.5;
    if ((navigator.deviceMemory || 8) <= 4) perf.dprCap = 1.25;

    /* --- contexto y escena compartidos entre aperturas --- */
    if (GL && GL.canvas && GL.canvas !== canvas) { canvas.replaceWith(GL.canvas); canvas = GL.canvas; }
    else if (!GL) {
      GL = { canvas: canvas, shared: [] };
      canvas.addEventListener('webglcontextlost', function (e) { e.preventDefault(); GL = null; });
    }

    const crearRenderer = () => {
      const r = new THREE.WebGLRenderer({
        canvas, antialias: true, powerPreference: 'high-performance', stencil: false
      });
      r.outputColorSpace = THREE.SRGBColorSpace;
      r.toneMapping = THREE.ACESFilmicToneMapping;
      r.toneMappingExposure = 0.96;
      r.shadowMap.enabled = true;
      r.shadowMap.type = THREE.PCFSoftShadowMap;
      GL.renderer = r;
      return r;
    };
    const renderer = GL.renderer || crearRenderer();
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, perf.dprCap));
    ctx.renderer = renderer;

    const camera = new THREE.PerspectiveCamera(36, 1, 5, 3000);

    let scene = GL.scene;
    if (!scene) {
      const scn = new THREE.Scene();
      scn.background = new THREE.Color('#101a2b');
      scn.fog = new THREE.Fog('#101a2b', 620, 1400);

      /* --- entorno (estudio) para reflejos PBR --- */
      const env = entorno(THREE, renderer);
      scn.environment = env;
      GL.shared.push(env);

      /* --- luces --- */
      scn.add(new THREE.HemisphereLight(0xcfe0ff, 0x223046, 0.5));
      const key = new THREE.DirectionalLight(0xffffff, 1.55);
      key.position.set(160, 240, 190);
      key.castShadow = true;
      key.shadow.mapSize.set(perf.movil ? 1024 : 1536, perf.movil ? 1024 : 1536);
      key.shadow.camera.left = -170; key.shadow.camera.right = 170;
      key.shadow.camera.top = 140; key.shadow.camera.bottom = -60;
      key.shadow.camera.near = 40; key.shadow.camera.far = 800;
      key.shadow.bias = -0.0006;
      key.shadow.normalBias = 1.4;
      scn.add(key);
      const fill = new THREE.DirectionalLight(0x9fc2ff, 0.42);
      fill.position.set(-220, 120, -160);
      scn.add(fill);

      /* --- suelo con sombra --- */
      const sueloTex = degradadoCircular(THREE, '#1a2740', '#0d1526');
      const piso = new THREE.Mesh(
        new THREE.CircleGeometry(460, 48),
        new THREE.MeshStandardMaterial({ map: sueloTex, roughness: 0.95, metalness: 0 })
      );
      piso.rotation.x = -Math.PI / 2;
      piso.receiveShadow = true;
      scn.add(piso);
      GL.shared.push(sueloTex, piso.geometry, piso.material);

      /* sombra de contacto suave (garantiza el apoyo visual) */
      const contactoSombra = degradadoSombra(THREE);
      const sombraMesh = new THREE.Mesh(
        new THREE.CircleGeometry(160, 40),
        new THREE.MeshBasicMaterial({ map: contactoSombra, transparent: true, depthWrite: false, opacity: 0.85 })
      );
      sombraMesh.rotation.x = -Math.PI / 2;
      sombraMesh.position.y = 0.4;
      sombraMesh.scale.set(1, 0.42, 1);
      scn.add(sombraMesh);
      GL.shared.push(contactoSombra, sombraMesh.geometry, sombraMesh.material);

      GL.scene = scene = scn;
    }

    /* --- vehículo --- */
    const perfil = window.vehPerfil ? window.vehPerfil(ctx.v) : null;
    if (!perfil) throw new Error('sin perfil');
    const auto = construirVehiculo(THREE, perfil, perf, ctx);
    scene.add(auto);
    ctx.auto = auto;

    /* --- encuadre --- */
    const bb = new THREE.Box3().setFromObject(auto);
    const size = bb.getSize(new THREE.Vector3());
    const centro = bb.getCenter(new THREE.Vector3());
    const radio = Math.max(size.x, size.z * 1.6, size.y) * 0.62;

    /* vista frontal 3/4 del lado del conductor: se ven placa y persona */
    const vistaIni = { theta: Math.PI - 0.95, phi: 1.36, r: radio * 1.8 };
    const objetivo = new THREE.Vector3(0, size.y * 0.42, 0);

    const estado = {
      theta: vistaIni.theta, phi: vistaIni.phi, r: vistaIni.r,
      vT: 0, vP: 0, sucio: true, auto: !perf.movil, ultimo: 0
    };
    ctx.reset = () => { estado.theta = vistaIni.theta; estado.phi = vistaIni.phi; estado.r = vistaIni.r; estado.vT = estado.vP = 0; estado.sucio = true; if (ctx.kick) ctx.kick(); };
    ctx.rot = () => {
      estado.auto = !estado.auto;
      const b = ctx.ov.querySelector('[data-act="rot"]');
      if (b) { b.classList.toggle('on', estado.auto); b.setAttribute('aria-pressed', String(estado.auto)); }
      estado.sucio = true; estado.ultimo = 0; if (ctx.kick) ctx.kick();
    };
    ctx.ov.querySelector('[data-act="rot"]').classList.toggle('on', estado.auto);

    const clampR = () => { estado.r = Math.max(radio * 1.35, Math.min(radio * 4.6, estado.r)); };

    /* ---------- controles (orbit + zoom + táctil) ---------- */
    const punteros = new Map();
    let pinch = 0;
    const marcar = () => { estado.sucio = true; estado.ultimo = performance.now(); if (ctx.kick) ctx.kick(); };
    ctx.on(canvas, 'pointerdown', e => {
      canvas.setPointerCapture(e.pointerId);
      punteros.set(e.pointerId, { x: e.clientX, y: e.clientY });
      estado.vT = estado.vP = 0; marcar();
    });
    ctx.on(canvas, 'pointermove', e => {
      if (!punteros.has(e.pointerId)) return;
      const p = punteros.get(e.pointerId);
      const dx = e.clientX - p.x, dy = e.clientY - p.y;
      punteros.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (punteros.size === 1) {
        estado.vT = -dx * 0.0055;
        estado.vP = -dy * 0.0042;
        estado.theta += estado.vT;
        estado.phi = Math.max(0.24, Math.min(1.46, estado.phi + estado.vP));
      } else if (punteros.size === 2) {
        const [a, b] = [...punteros.values()];
        const d = Math.hypot(a.x - b.x, a.y - b.y);
        if (pinch) { estado.r *= pinch / d; clampR(); }
        pinch = d;
      }
      marcar();
    });
    const soltar = e => { punteros.delete(e.pointerId); if (punteros.size < 2) pinch = 0; };
    ctx.on(canvas, 'pointerup', soltar);
    ctx.on(canvas, 'pointercancel', soltar);
    ctx.on(canvas, 'pointerleave', soltar);
    ctx.on(canvas, 'wheel', e => {
      e.preventDefault();
      estado.r *= Math.exp(e.deltaY * 0.0012);
      clampR(); marcar();
    }, { passive: false });
    ctx.on(canvas, 'keydown', e => {
      const k = e.key;
      if (k === 'ArrowLeft') estado.theta += 0.12;
      else if (k === 'ArrowRight') estado.theta -= 0.12;
      else if (k === 'ArrowUp') estado.phi = Math.max(0.24, estado.phi - 0.08);
      else if (k === 'ArrowDown') estado.phi = Math.min(1.46, estado.phi + 0.08);
      else if (k === '+' || k === '=') { estado.r *= 0.9; clampR(); }
      else if (k === '-') { estado.r *= 1.1; clampR(); }
      else return;
      e.preventDefault(); marcar();
    });

    /* ---------- resize ---------- */
    function medir() {
      const w = stage.clientWidth || 320, h = stage.clientHeight || 220;
      renderer.setSize(w, h, false);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      estado.sucio = true;
      if (ctx.kick) ctx.kick();
    }
    if (window.ResizeObserver) {
      const ro = new ResizeObserver(medir); ro.observe(stage); ctx.observers.push(ro);
    } else ctx.on(window, 'resize', medir);
    medir();

    /* ---------- pausa cuando no se ve ---------- */
    let visible = true, pagVisible = !document.hidden;
    if (window.IntersectionObserver) {
      const io = new IntersectionObserver(es => { visible = es[0].isIntersecting; estado.sucio = true; if (ctx.kick) ctx.kick(); }, { threshold: 0.02 });
      io.observe(stage); ctx.observers.push(io);
    }
    ctx.on(document, 'visibilitychange', () => { pagVisible = !document.hidden; estado.sucio = true; if (ctx.kick) ctx.kick(); });

    /* ---------- bucle con render bajo demanda ---------- */
    const marco = () => {
      ctx.raf = 0;
      if (ctx.cerrado) return;
      const ahora = performance.now();
      const inactivo = ahora - estado.ultimo > 2600;

      if (Math.abs(estado.vT) > 2e-5 || Math.abs(estado.vP) > 2e-5) {
        estado.theta += estado.vT; estado.phi = Math.max(0.24, Math.min(1.46, estado.phi + estado.vP));
        estado.vT *= 0.90; estado.vP *= 0.90; estado.sucio = true;
      }
      if (estado.auto && inactivo && visible && pagVisible) { estado.theta += 0.0031; estado.sucio = true; }

      if (estado.sucio && visible && pagVisible) {
        estado.sucio = false;
        const sp = Math.sin(estado.phi), cp = Math.cos(estado.phi);
        camera.position.set(
          objetivo.x + estado.r * sp * Math.sin(estado.theta),
          objetivo.y + estado.r * cp,
          objetivo.z + estado.r * sp * Math.cos(estado.theta)
        );
        camera.lookAt(objetivo);
        renderer.render(scene, camera);
        const ld = ctx.ov.querySelector('.ag-3d-load');
        if (ld) ld.classList.add('done');
      }
      const gira = estado.auto && inactivo && visible && pagVisible;
      const inercia = Math.abs(estado.vT) > 2e-5 || Math.abs(estado.vP) > 2e-5;
      if ((gira || inercia) && !ctx.cerrado) ctx.raf = requestAnimationFrame(marco);
    };
    const kick = () => { if (!ctx.raf && !ctx.cerrado) ctx.raf = requestAnimationFrame(marco); };
    estado.sucio = true;
    kick();
    ctx.kick = kick;
    ctx.estado = estado;
  }

  /* ---------- entorno de estudio (equirect -> PMREM) ---------- */
  function entorno(THREE, renderer) {
    const c = document.createElement('canvas');
    c.width = 512; c.height = 256;
    const g = c.getContext('2d');
    const grad = g.createLinearGradient(0, 0, 0, 256);
    grad.addColorStop(0, '#d7e7ff');
    grad.addColorStop(0.42, '#8fb1de');
    grad.addColorStop(0.52, '#41567a');
    grad.addColorStop(1, '#0e1626');
    g.fillStyle = grad; g.fillRect(0, 0, 512, 256);
    const bola = (x, y, rx, ry, a) => {
      g.save(); g.globalAlpha = a; g.fillStyle = '#ffffff';
      g.beginPath(); g.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2); g.fill(); g.restore();
    };
    bola(140, 56, 74, 24, 0.95);      // softbox principal
    bola(370, 48, 92, 28, 0.85);      // softbox lateral
    bola(255, 96, 46, 16, 0.55);      // relleno
    g.save(); g.globalAlpha = 0.5; g.fillStyle = '#ffdcae';
    g.beginPath(); g.ellipse(60, 132, 66, 26, 0, 0, Math.PI * 2); g.fill(); g.restore();

    const tex = new THREE.CanvasTexture(c);
    tex.mapping = THREE.EquirectangularReflectionMapping;
    tex.colorSpace = THREE.SRGBColorSpace;
    const pmrem = new THREE.PMREMGenerator(renderer);
    const env = pmrem.fromEquirectangular(tex).texture;
    pmrem.dispose(); tex.dispose();
    return env;
  }

  function degradadoCircular(THREE, dentro, fuera) {
    const c = document.createElement('canvas');
    c.width = c.height = 256;
    const g = c.getContext('2d');
    const rg = g.createRadialGradient(128, 118, 10, 128, 128, 126);
    rg.addColorStop(0, dentro); rg.addColorStop(1, fuera);
    g.fillStyle = rg; g.fillRect(0, 0, 256, 256);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return t;
  }

  /** Mancha oscura de contacto bajo el vehículo. */
  function degradadoSombra(THREE) {
    const c = document.createElement('canvas');
    c.width = c.height = 256;
    const g = c.getContext('2d');
    const rg = g.createRadialGradient(128, 128, 8, 128, 128, 124);
    rg.addColorStop(0, 'rgba(2,6,14,.78)');
    rg.addColorStop(0.45, 'rgba(2,6,14,.42)');
    rg.addColorStop(1, 'rgba(2,6,14,0)');
    g.fillStyle = rg; g.fillRect(0, 0, 256, 256);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return t;
  }

  /* =======================================================
     CONSTRUCCIÓN DEL VEHÍCULO
     ======================================================= */
  function construirVehiculo(THREE, perfil, perf, ctx) {
    const suelo = perfil.suelo;
    const cont = perfil.contorno;
    let minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
    cont.forEach(p => { minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]); minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]); });
    const cx = (minX + maxX) / 2;
    const altura = suelo - minY;                    // altura total
    const mitadLargo = (maxX - minX) / 2;
    const ancho = perfil.ancho, HW = ancho / 2;
    const cinturaY = perfil.cinturaY != null ? (suelo - perfil.cinturaY) : altura * 0.62;

    /* puntos del contorno en coordenadas 3D (Y arriba, suelo = 0) */
    const P = cont.map(p => [p[0] - cx, suelo - p[1]]);

    /* estrechamiento lateral: menos ancho bajo el cinturón y en morro/cola */
    function taper(x, y) {
      let fy = 1;
      const yb = altura * 0.2;
      if (y < yb) fy = 0.94 + 0.06 * (y / yb);
      else if (y > cinturaY) {
        const t = Math.min(1, (y - cinturaY) / Math.max(1, altura - cinturaY));
        fy = 1 - 0.17 * t;
      }
      let fx = 1;
      const ax = Math.abs(x) / mitadLargo;
      if (ax > 0.6) { const t = (ax - 0.6) / 0.4; fx = 1 - 0.09 * t * t; }
      return fy * fx;
    }
    function aplicarTaper(geo) {
      const pos = geo.attributes.position;
      for (let i = 0; i < pos.count; i++) {
        pos.setZ(i, pos.getZ(i) * taper(pos.getX(i), pos.getY(i)));
      }
      pos.needsUpdate = true;
      geo.computeVertexNormals();
      return geo;
    }

    const grupo = new THREE.Group();

    /* ---------- carrocería (extrusión del perfil 2D) ---------- */
    const shape = new THREE.Shape();
    let area = 0;
    P.forEach((p, i) => { if (i === 0) shape.moveTo(p[0], p[1]); else shape.lineTo(p[0], p[1]); });
    for (let i = 0; i < P.length; i++) {
      const a = P[i], b = P[(i + 1) % P.length];
      area += (a[0] * b[1] - b[0] * a[1]);
    }
    const orden = P.slice();
    if (area < 0) orden.reverse();
    const shape2 = new THREE.Shape();
    orden.forEach((p, i) => { i === 0 ? shape2.moveTo(p[0], p[1]) : shape2.lineTo(p[0], p[1]); });

    const geoCuerpo = new THREE.ExtrudeGeometry(shape2, {
      depth: ancho, bevelEnabled: true, bevelThickness: 1.4, bevelSize: 0.3,
      bevelOffset: 0, bevelSegments: 2, curveSegments: 1
    });
    geoCuerpo.translate(0, 0, -HW);
    aplicarTaper(geoCuerpo);

    const paint = new THREE.MeshPhysicalMaterial({
      color: new THREE.Color(perfil.color),
      metalness: 0.5, roughness: 0.27,
      clearcoat: 1, clearcoatRoughness: 0.05,
      envMapIntensity: 1.1, side: THREE.DoubleSide
    });
    const cuerpo = new THREE.Mesh(geoCuerpo, paint);
    cuerpo.castShadow = true; cuerpo.receiveShadow = true;
    grupo.add(cuerpo);
    ctx.disposables.push(geoCuerpo, paint);

    /* ---------- cristales laterales ---------- */
    const vidrioMat = new THREE.MeshPhysicalMaterial({
      color: 0x0d1626, metalness: 0, roughness: 0.06,
      transparent: true, opacity: 0.86, envMapIntensity: 1.5,
      clearcoat: 1, clearcoatRoughness: 0.03, side: THREE.DoubleSide
    });
    ctx.disposables.push(vidrioMat);
    perfil.vidrios.forEach(quad => {
      [-1, 1].forEach(lado => {
        const sh = new THREE.Shape();
        quad.forEach((p, i) => {
          const X = p[0] - cx, Y = suelo - p[1];
          i === 0 ? sh.moveTo(X, Y) : sh.lineTo(X, Y);
        });
        const g = new THREE.ExtrudeGeometry(sh, { depth: 0.9, bevelEnabled: false, curveSegments: 1 });
        g.translate(0, 0, (HW + 0.5) * lado + (lado < 0 ? -0.9 : 0));
        aplicarTaper(g);
        const m = new THREE.Mesh(g, vidrioMat);
        grupo.add(m);
        ctx.disposables.push(g);
      });
    });

    /* ---------- líneas de puertas y cintura sobre los laterales ---------- */
    const gapMat = new THREE.MeshStandardMaterial({
      color: 0x0b111c, roughness: 0.75, metalness: 0.15, side: THREE.DoubleSide
    });
    const trimMat = new THREE.MeshStandardMaterial({
      color: 0xd7e2ee, roughness: 0.24, metalness: 0.9, envMapIntensity: 1.3, side: THREE.DoubleSide
    });
    ctx.disposables.push(gapMat, trimMat);
    const lateral = (ptsSvg, w, mat) => {
      if (!ptsSvg || ptsSvg.length < 2) return;
      [-1, 1].forEach(lado => {
        tiraLateral(THREE, ptsSvg.map(p => [p[0] - cx, suelo - p[1]]), HW, taper, w, lado, 0.3)
          .forEach(g => { grupo.add(new THREE.Mesh(g, mat)); ctx.disposables.push(g); });
      });
    };
    (perfil.puertas || []).forEach(p => lateral(p, 1.15, gapMat));
    lateral(perfil.cintura, 1.2, trimMat);

    /* ---------- parabrisas y luneta (cintas a través del ancho) ---------- */
    const ref = [0, altura * 0.45];    const cristalExt = new THREE.MeshPhysicalMaterial({
      color: 0x16273d, metalness: 0, roughness: 0.05, transparent: true,
      opacity: 0.9, envMapIntensity: 1.6, clearcoat: 1, side: THREE.DoubleSide
    });
    ctx.disposables.push(cristalExt);
    cinta(THREE, perfil.parabrisas, cx, suelo, HW, 0.95, 0.4, taper, ref).forEach(g => {
      grupo.add(new THREE.Mesh(g, cristalExt)); ctx.disposables.push(g);
    });
    cinta(THREE, perfil.trasera, cx, suelo, HW, 0.93, 0.4, taper, ref).forEach(g => {
      grupo.add(new THREE.Mesh(g, cristalExt)); ctx.disposables.push(g);
    });

    /* ---------- faros, traseros y parrilla (bandas del morro/cola) ---------- */
    const matFaro = new THREE.MeshPhysicalMaterial({
      color: 0xfff6dd, emissive: 0xffe7a8, emissiveIntensity: 0.35,
      metalness: 0.1, roughness: 0.12, clearcoat: 1, side: THREE.DoubleSide
    });
    const matTrasero = new THREE.MeshPhysicalMaterial({
      color: 0xc9302a, emissive: 0x7e150f, emissiveIntensity: 0.45,
      metalness: 0.1, roughness: 0.18, clearcoat: 1, side: THREE.DoubleSide
    });
    const matParrilla = new THREE.MeshStandardMaterial({
      color: 0x12161f, metalness: 0.55, roughness: 0.5, side: THREE.DoubleSide
    });
    ctx.disposables.push(matFaro, matTrasero, matParrilla);

    const rangoY = (polys, suelo) => {
      let a = 1e9, b = -1e9;
      polys.forEach(q => q.forEach(p => { const Y = suelo - p[1]; a = Math.min(a, Y); b = Math.max(b, Y); }));
      return [a, b];
    };
    const [fLo, fHi] = rangoY([perfil.faroF], suelo);
    const [tLo, tHi] = rangoY([perfil.faroT], suelo);
    const [gLo, gHi] = rangoY([perfil.parrilla], suelo);

    bandaCara(THREE, P, p => p[0] > mitadLargo - 13 && p[1] >= fLo - 1 && p[1] <= fHi + 1, HW, 0.86, 0.8, taper, ref, -1, -0.42)
      .forEach(g => { grupo.add(new THREE.Mesh(g, matFaro)); ctx.disposables.push(g); });
    bandaCara(THREE, P, p => p[0] > mitadLargo - 13 && p[1] >= fLo - 1 && p[1] <= fHi + 1, HW, 0.86, 0.8, taper, ref, 0.42, 1)
      .forEach(g => { grupo.add(new THREE.Mesh(g, matFaro)); ctx.disposables.push(g); });
    bandaCara(THREE, P, p => p[0] > mitadLargo - 13 && p[1] >= gLo - 1 && p[1] <= gHi + 1, HW, 0.85, 0.9, taper, ref)
      .forEach(g => { grupo.add(new THREE.Mesh(g, matParrilla)); ctx.disposables.push(g); });
    bandaCara(THREE, P, p => p[0] < -mitadLargo + 12 && p[1] >= tLo - 1 && p[1] <= tHi + 1, HW, 0.9, 1.0, taper, ref)
      .forEach(g => { grupo.add(new THREE.Mesh(g, matTrasero)); ctx.disposables.push(g); });

    /* ---------- manijas y espejos ---------- */
    const cromo = new THREE.MeshStandardMaterial({ color: 0xc7d2df, metalness: 0.9, roughness: 0.28 });
    ctx.disposables.push(cromo);
    (perfil.manijas || []).forEach(m => {
      const X = m[0] - cx, Y = suelo - m[1];
      const g = new THREE.BoxGeometry(7.5, 1.6, 1.6);
      [-1, 1].forEach(l => {
        const mesh = new THREE.Mesh(g, cromo);
        mesh.position.set(X, Y, (HW + 0.6) * taper(X, Y) * l);
        grupo.add(mesh);
      });
      ctx.disposables.push(g);
    });
    if (perfil.espejo) {
      const X = perfil.espejo[0] - cx, Y = suelo - perfil.espejo[1];
      const g = new THREE.BoxGeometry(4.5, 3.4, 3.2);
      const gStalk = new THREE.BoxGeometry(2.4, 1, 4.6);
      [-1, 1].forEach(l => {
        const z = (HW + 1.8) * taper(X, Y) * l;
        const m = new THREE.Mesh(g, cromo); m.position.set(X + 1.5, Y + 1.6, z); grupo.add(m);
        const s = new THREE.Mesh(gStalk, cromo); s.position.set(X, Y + 0.6, (HW - 0.4) * l); grupo.add(s);
      });
      ctx.disposables.push(g, gStalk);
    }

    /* ---------- ruedas ---------- */
    const ruedaGeo = crearRueda(THREE, perfil.ruedas[0].r, ancho * 0.135);
    ctx.disposables.push.apply(ctx.disposables, ruedaGeo.geos);
    const matsR = materialesRueda(THREE, ctx);
    perfil.ruedas.forEach(r => {
      const X = r.x - cx, Y = suelo - r.y;
      [-1, 1].forEach(l => {
        const w = new THREE.Group();
        ruedaGeo.build(w, matsR);
        w.position.set(X, Y, (HW * 0.95) * taper(X, Y) * l);
        w.rotation.x = l > 0 ? Math.PI / 2 : -Math.PI / 2;
        grupo.add(w);
      });
    });

    /* ---------- placas delantera y trasera (tipo Perú) ---------- */
    agregarPlacas(THREE, grupo, ctx, perfil, { P, mitadLargo, altura });

    /* ---------- mini persona junto a la puerta del conductor ---------- */
    const manoX = (perfil.manijas && perfil.manijas.length ? perfil.manijas[0][0] : cx + mitadLargo * 0.35) - cx;
    grupo.add(construirPersona(THREE, ctx, (ctx.v && ctx.v.genero) || '', {
      alto: altura * 1.15,
      x: manoX,
      z: -(HW + altura * 0.086)
    }));

    return grupo;
  }

  /* =======================================================
     PLACAS (texto real del vehículo, delante y atrás)
     ======================================================= */
  /** Texto de placa sobre fondo claro, guiño al estilo de la app. */
  function texturaPlaca(THREE, texto) {
    const c = document.createElement('canvas');
    c.width = 256; c.height = 96;
    const g = c.getContext('2d');
    g.fillStyle = '#f4f7fb';
    g.fillRect(0, 0, 256, 96);
    g.strokeStyle = '#10151d';
    g.lineWidth = 7;
    g.strokeRect(5, 5, 246, 86);
    g.fillStyle = '#1d4ed8';           // banda superior, como .ag-plate
    g.fillRect(12, 12, 232, 12);
    g.fillStyle = '#0f172a';
    g.font = 'bold 50px ui-monospace, "Courier New", monospace';
    g.textAlign = 'center';
    g.textBaseline = 'middle';
    g.fillText(texto, 128, 62, 216);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    return t;
  }

  /** Placa en el parachoques frontal (+x) y en la trasera (−x). */
  function agregarPlacas(THREE, grupo, ctx, perfil, d) {
    const txt = ((perfil.placa || '').trim() || '___-___').toUpperCase();
    const tex = texturaPlaca(THREE, txt);
    const cara = new THREE.MeshStandardMaterial({ map: tex, roughness: 0.38, metalness: 0.1 });
    const base = new THREE.MeshStandardMaterial({ color: 0xe9edf3, roughness: 0.5, metalness: 0.35 });
    ctx.disposables.push(tex, cara, base);

    const w = Math.max(14, d.mitadLargo * 0.19);   // ~4.5 % del largo total
    const h = w * 0.38;
    const y = Math.max(h, d.altura * 0.17);        // altura del parachoques
    const banda = d.P.filter(p => p[1] >= y - h * 0.7 && p[1] <= y + h * 0.7);
    const xF = banda.length ? Math.max.apply(null, banda.map(p => p[0])) : d.mitadLargo;
    const xR = banda.length ? Math.min.apply(null, banda.map(p => p[0])) : -d.mitadLargo;

    const geo = new THREE.BoxGeometry(1.4, h, w);
    // caras del cubo: [+x, −x, +y, −y, +z, −z]
    const delantera = new THREE.Mesh(geo, [cara, base, base, base, base, base]);
    delantera.position.set(xF - 0.1, y, 0);
    const trasera = new THREE.Mesh(geo, [base, cara, base, base, base, base]);
    trasera.position.set(xR + 0.1, y, 0);
    grupo.add(delantera, trasera);
    ctx.disposables.push(geo);
  }

  /* =======================================================
     MINI PERSONA JUNTO A LA PUERTA (escala + género)
     ======================================================= */
  /**
   * Figura humana de escala junto a la puerta del conductor.
   * El diseño —hombros, pelo y ropa— cambia con el género del cliente:
   * MASCULINO (hombros anchos, pelo corto, polo azul),
   * FEMENINO (hombros estrechos, pelo largo, top malva),
   * OTROS/sin especificar (complexión media, pelo medio, verde).
   */
  function construirPersona(THREE, ctx, genero, o) {
    const g = (genero || '').toUpperCase();
    const esF = g === 'FEMENINO';
    const esM = g === 'MASCULINO';
    const H = o.alto;

    const piel = new THREE.MeshStandardMaterial({ color: 0xd8a183, roughness: 0.75 });
    const camisa = new THREE.MeshStandardMaterial({
      color: esM ? 0x3f6ea8 : esF ? 0xb05a86 : 0x4f8a6b, roughness: 0.82
    });
    const pantalon = new THREE.MeshStandardMaterial({
      color: esM ? 0x333a46 : esF ? 0x3c4250 : 0x454b57, roughness: 0.86
    });
    const pelo = new THREE.MeshStandardMaterial({
      color: esM ? 0x2a2018 : esF ? 0x3a2a20 : 0x4a3628, roughness: 0.92
    });
    const zapato = new THREE.MeshStandardMaterial({ color: 0x1a1e26, roughness: 0.6 });
    ctx.disposables.push(piel, camisa, pantalon, pelo, zapato);

    const grupo = new THREE.Group();
    const caja = (w, h, dp, mat, x, y, z) => {
      const geo = new THREE.BoxGeometry(w, h, dp);
      const m = new THREE.Mesh(geo, mat);
      m.position.set(x, y, z);
      m.castShadow = true;
      grupo.add(m);
      ctx.disposables.push(geo);
      return m;
    };

    const hombros = H * (esM ? 0.25 : esF ? 0.205 : 0.228);
    const piernaH = H * 0.47, torsoH = H * 0.31, cabezaR = H * 0.067;
    const hombrosY = piernaH + torsoH;
    const cabezaY = hombrosY + cabezaR + H * 0.035;

    /* piernas + zapatos */
    const px = hombros * 0.26;
    caja(hombros * 0.34, piernaH, hombros * 0.42, pantalon, -px, piernaH / 2, 0);
    caja(hombros * 0.34, piernaH, hombros * 0.42, pantalon, px, piernaH / 2, 0);
    caja(hombros * 0.36, H * 0.03, hombros * 0.58, zapato, -px, H * 0.016, hombros * 0.08);
    caja(hombros * 0.36, H * 0.03, hombros * 0.58, zapato, px, H * 0.016, hombros * 0.08);
    /* torso */
    caja(hombros, torsoH, hombros * 0.46, camisa, 0, piernaH + torsoH / 2, 0);
    /* brazos colgados + manos */
    const bx = hombros / 2 + hombros * 0.075;
    caja(hombros * 0.15, torsoH * 1.05, hombros * 0.2, camisa, -bx, piernaH + torsoH * 0.55, 0);
    caja(hombros * 0.15, torsoH * 1.05, hombros * 0.2, camisa, bx, piernaH + torsoH * 0.55, 0);
    caja(hombros * 0.14, H * 0.05, hombros * 0.18, piel, -bx, piernaH + torsoH * 0.03, 0);
    caja(hombros * 0.14, H * 0.05, hombros * 0.18, piel, bx, piernaH + torsoH * 0.03, 0);
    /* cuello + cabeza */
    caja(hombros * 0.18, H * 0.045, hombros * 0.2, piel, 0, hombrosY + H * 0.022, 0);
    const cabezaGeo = new THREE.SphereGeometry(cabezaR, 14, 12);
    const cabeza = new THREE.Mesh(cabezaGeo, piel);
    cabeza.position.set(0, cabezaY, 0);
    cabeza.castShadow = true;
    grupo.add(cabeza);
    ctx.disposables.push(cabezaGeo);

    /* pelo: corona común + caída según género */
    const coronaGeo = new THREE.SphereGeometry(cabezaR * 1.06, 14, 10, 0, Math.PI * 2, 0, Math.PI * 0.55);
    const corona = new THREE.Mesh(coronaGeo, pelo);
    corona.position.copy(cabeza.position);
    grupo.add(corona);
    ctx.disposables.push(coronaGeo);
    if (esF) {
      caja(cabezaR * 1.7, H * 0.16, H * 0.05, pelo, 0, cabezaY - H * 0.075, -cabezaR * 0.72);
    } else if (!esM) {
      caja(cabezaR * 1.5, H * 0.07, H * 0.045, pelo, 0, cabezaY - H * 0.03, -cabezaR * 0.7);
    }

    grupo.position.set(o.x, 0, o.z);
    grupo.rotation.y = Math.PI - 0.45;   // mira hacia fuera, ligeramente al frente
    return grupo;
  }

  /* ---------- materiales de llanta (compartidos) ---------- */
  function materialesRueda(THREE, ctx) {
    if (ctx._mRueda) return ctx._mRueda;
    const m = {
      goma: new THREE.MeshStandardMaterial({ color: 0x14181f, roughness: 0.92, metalness: 0 }),
      aro: new THREE.MeshStandardMaterial({ color: 0xcfdae6, roughness: 0.26, metalness: 0.92, envMapIntensity: 1.2 }),
      disco: new THREE.MeshStandardMaterial({ color: 0x6d7887, roughness: 0.55, metalness: 0.7 }),
      sombra: new THREE.MeshStandardMaterial({ color: 0x0a0d13, roughness: 1, metalness: 0, side: THREE.DoubleSide }),
      pinza: new THREE.MeshStandardMaterial({ color: 0x9b3b33, roughness: 0.5, metalness: 0.3 })
    };
    ctx.disposables.push(m.goma, m.aro, m.disco, m.sombra, m.pinza);
    ctx._mRueda = m;
    return m;
  }

  /** Geometrías de rueda compartidas entre las 4 ruedas. */
  function crearRueda(THREE, r, ancho) {
    const hw = ancho / 2, rimR = r * 0.63;
    const perfilGoma = [
      new THREE.Vector2(rimR * 0.99, -hw),
      new THREE.Vector2(r * 0.94, -hw),
      new THREE.Vector2(r, -hw * 0.66),
      new THREE.Vector2(r, hw * 0.66),
      new THREE.Vector2(r * 0.94, hw),
      new THREE.Vector2(rimR * 0.99, hw)
    ];
    const goma = new THREE.LatheGeometry(perfilGoma, 26);
    const aro = new THREE.CylinderGeometry(rimR, rimR, ancho * 0.66, 26, 1, true);
    const fondo = new THREE.CircleGeometry(rimR * 0.98, 24);
    fondo.rotateX(-Math.PI / 2);
    const disco = new THREE.CylinderGeometry(rimR * 0.7, rimR * 0.7, ancho * 0.16, 20);
    const pinza = new THREE.CylinderGeometry(rimR * 0.76, rimR * 0.76, ancho * 0.3, 12, 1, true, 0.2, 1.15);
    const buje = new THREE.CylinderGeometry(r * 0.13, r * 0.13, ancho * 0.4, 14);
    const radio = new THREE.BoxGeometry(rimR * 0.9, ancho * 0.3, Math.max(2, r * 0.17));
    const geos = [goma, aro, fondo, disco, pinza, buje, radio];
    return {
      geos,
      build(group, mats) {
        const t = new THREE.Mesh(goma, mats.goma); t.castShadow = true; group.add(t);
        group.add(new THREE.Mesh(aro, mats.aro));
        const f = new THREE.Mesh(fondo, mats.sombra); f.position.y = -ancho * 0.3; group.add(f);
        group.add(new THREE.Mesh(disco, mats.disco));
        group.add(new THREE.Mesh(pinza, mats.pinza));
        for (let i = 0; i < 5; i++) {
          const a = (i / 5) * Math.PI * 2;
          const s = new THREE.Mesh(radio, mats.aro);
          s.position.set(Math.cos(a) * rimR * 0.5, ancho * 0.2, Math.sin(a) * rimR * 0.5);
          s.rotation.y = -a;
          group.add(s);
        }
        const b = new THREE.Mesh(buje, mats.aro); b.position.y = ancho * 0.26; group.add(b);
      }
    };
  }

  /* =======================================================
     CINTAS (parabrisas, luneta, faros, parrilla)
     Banda que sigue un tramo del contorno cruzando el ancho.
     ======================================================= */
  /** ptsSvg: polilínea ya muestreada (coords SVG) que se convertirá en banda. */
  function cinta(THREE, ptsSvg, cx, suelo, HW, inset, fuera, taper, ref) {
    if (!ptsSvg || ptsSvg.length < 2) return [];
    return banda(THREE, ptsSvg.map(p => [p[0] - cx, suelo - p[1]]), HW, inset, fuera, taper, ref);
  }

  /** Convierte una polilínea del contorno en una banda cruzando el vehículo.
   *  za/zb: fracciones del semiancho (−1..1) para acotar el tramo lateral. */
  function banda(THREE, pts3, HW, inset, fuera, taper, ref, za, zb) {
    if (!pts3 || pts3.length < 2) return [];
    if (za == null) { za = -1; zb = 1; }
    const rx = ref[0], ry = ref[1];
    const izq = [], der = [];
    for (let i = 0; i < pts3.length; i++) {
      const a = pts3[Math.max(0, i - 1)], b = pts3[Math.min(pts3.length - 1, i + 1)];
      let tx = b[0] - a[0], ty = b[1] - a[1];
      const len = Math.hypot(tx, ty) || 1; tx /= len; ty /= len;
      let nx = ty, ny = -tx;
      /* la normal debe apuntar hacia fuera del vehículo */
      if ((pts3[i][0] - rx) * nx + (pts3[i][1] - ry) * ny < 0) { nx = -nx; ny = -ny; }
      const x = pts3[i][0] + nx * fuera, y = pts3[i][1] + ny * fuera;
      const hw = HW * taper(x, y) * inset;
      izq.push([x, y, hw * za]);
      der.push([x, y, hw * zb]);
    }
    const pos = [];
    for (let i = 0; i < pts3.length - 1; i++) {
      const a = izq[i], b = der[i], c = der[i + 1], d = izq[i + 1];
      pos.push.apply(pos, a.concat(b, c, a, c, d));
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.computeVertexNormals();
    return [g];
  }

  /** Filtra un tramo del contorno y lo convierte en banda. */
  function bandaCara(THREE, P, filtro, HW, inset, fuera, taper, ref, za, zb) {
    const sel = P.filter(filtro);
    if (sel.length < 2) return [];
    return banda(THREE, sel, HW, inset, fuera, taper, ref, za, zb);
  }

  /** Tira fina pegada al lateral (líneas de puerta, cintura, etc.). */
  function tiraLateral(THREE, pts3, HW, taper, w, lado, fuera) {
    if (!pts3 || pts3.length < 2) return [];
    const zp = p => (HW * taper(p[0], p[1]) + fuera) * lado;
    const pos = [];
    const empujar = (a, b) => {
      let tx = b[0] - a[0], ty = b[1] - a[1];
      const len = Math.hypot(tx, ty) || 1;
      const n = Math.max(2, Math.ceil(len / 2.5));
      for (let i = 0; i < n; i++) {
        const p0 = [a[0] + tx * (i / n), a[1] + ty * (i / n)];
        const p1 = [a[0] + tx * ((i + 1) / n), a[1] + ty * ((i + 1) / n)];
        const s0 = [-ty / len * w / 2, tx / len * w / 2];
        const A1 = [p0[0] + s0[0], p0[1] + s0[1]], A2 = [p0[0] - s0[0], p0[1] - s0[1]];
        const B1 = [p1[0] + s0[0], p1[1] + s0[1]], B2 = [p1[0] - s0[0], p1[1] - s0[1]];
        const q = [A1, A2, B2, A1, B2, B1];
        q.forEach(v => pos.push(v[0], v[1], zp(v)));
      }
    };
    for (let i = 0; i < pts3.length - 1; i++) empujar(pts3[i], pts3[i + 1]);
    if (!pos.length) return [];
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.computeVertexNormals();
    return [g];
  }
})();
