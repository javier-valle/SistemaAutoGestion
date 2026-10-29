




(function () {
  'use strict';
  if (window.__agUiKit) return;
  window.__agUiKit = true;

  const IS_PAGES = /\/pages\//.test(window.location.pathname);
  const P = IS_PAGES ? '' : 'pages/';


  const TOAST_ICONS = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
    danger: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
    warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="13"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
  };
  function toast(message, type = 'success', ms = 3800) {
    let stack = document.querySelector('.ag-toast-stack');
    if (!stack) {
      stack = document.createElement('div');
      stack.className = 'ag-toast-stack';
      document.body.appendChild(stack);
    }
    const t = document.createElement('div');
    t.className = `ag-toast t-${type}`;
    const icon = document.createElement('span');
    icon.innerHTML = TOAST_ICONS[type] || TOAST_ICONS.info;
    const txt = document.createElement('span');
    txt.textContent = message;
    t.append(icon, txt);
    stack.appendChild(t);
    setTimeout(() => {
      t.classList.add('ag-toast-out');
      setTimeout(() => t.remove(), 300);
    }, ms);
  }
  window.toast = toast;


  const origShowSuccess = window.showSuccess;
  const origShowError = window.showError;
  window.showSuccess = function (id, msg) {
    if (typeof origShowSuccess === 'function') { try { origShowSuccess(id, msg); } catch (_) {  } }
    toast(msg, 'success');
  };
  window.showError = function (id, msg) {
    if (typeof origShowError === 'function') { try { origShowError(id, msg); } catch (_) {  } }
    toast(msg, 'danger');
  };


  const TOUR_STEPS = {
    dashboard: [
      { sel: '.ag-grid.ag-grid-4', title: 'Indicadores del taller', text: 'Aqui ves el pulso del negocio en tiempo real: OT completadas, tiempo promedio, ingresos del mes y alertas de stock.' },
      { sel: '.ag-flow-steps', title: 'Flujo de trabajo', text: 'Todo vehiculo sigue este camino: Recepcion, Diagnostico, Cotizacion, OT, Pago y Entrega. Cada modulo del sistema corresponde a un paso.' },
      { sel: '.ag-grid-4', title: 'Acceso rapido', text: 'Atajos a los modulos mas usados. Tambien puedes usar Ctrl+K en cualquier momento para saltar a cualquier pagina.' }
    ],
    recepcion: [
      { sel: '#recWizard', title: 'Wizard en 4 pasos', text: 'Cliente, vehículo, problema y confirmación. No avanzas si el paso actual está incompleto, y puedes retroceder sin perder nada.' },
      { sel: '#cliSearch', title: 'Busca primero', text: 'Escribe documento, nombre o teléfono: si el cliente existe, elige su tarjeta y sus vehículos aparecen solos.' },
      { sel: '#recepcionesContainer', title: 'Lista en vivo', text: 'Cada tarjeta muestra placa, estado y problema reportado. Cuando avances el vehiculo de etapa, su estado cambia aqui.' }
    ],
    cotizacion: [
      { sel: '#diagnosticoForm', title: '1. Diagnostico', text: 'Selecciona una recepcion PENDIENTE y describe los hallazgos. Al guardar obtendras un ID de diagnostico.' },
      { sel: '#cotizacionForm', title: '2. Armar la cotizacion', text: 'Ingresa el ID del diagnostico y agrega servicios y productos. El total se calcula automaticamente abajo.' },
      { sel: '#cotizacionesContainer', title: '3. Aprobacion', text: 'Las cotizaciones quedan listadas. Cuando el cliente apruebe, pulsa Aprobar: con eso la OT podra crearse.' }
    ],
    orden_trabajo: [
      { sel: '#otForm', title: 'Crear una OT', text: 'Solo se listan cotizaciones APROBADAS. Elige una, asigna un mecanico y crea la orden de trabajo.' },
      { sel: '#ordenesContainer', title: 'Seguimiento', text: 'Cada tarjeta muestra el avance con barra de progreso. Usa el boton de estado para avanzar la OT paso a paso.' },
      { sel: '#filtroEstado', title: 'Filtros', text: 'Filtra por estado para enfocarte en lo pendiente. Si eres mecanico, veras solo tus OT asignadas.' }
    ],
    inventario: [
      { sel: '#searchProducto', title: 'Buscar productos', text: 'Filtra el catalogo al instante por nombre, tipo o precio sin recargar la pagina.' },
      { sel: '#movimientoForm', title: 'Movimientos', text: 'Registra entradas (compras) o ajustes de inventario. El stock se actualiza al momento.' },
      { sel: '#alertasContainer', title: 'Alertas de stock', text: 'Los productos bajo su minimo aparecen aqui y en el dashboard. No dejes sin stock un repuesto critico.' }
    ],
    pago_entrega: [
      { sel: '#ordenesContainer', title: 'OT finalizadas', text: 'Aqui aparecen las ordenes listas para cobrar. Veras monto, estado de pago y de entrega.' },
      { sel: '#ordenesContainer ~ .ag-layout-stack, .ag-stat-row', title: 'Resumen del dia', text: 'Contador de pagos y entregas pendientes, y el total por cobrar en tiempo real.' }
    ],
    mecanico: [
      { sel: '.ag-grid.ag-grid-4', title: 'Tu carga de trabajo', text: 'Resumen de tus OT: asignadas, en proceso, finalizadas y pendientes.' },
      { sel: '#misOrdenes', title: 'Mis ordenes', text: 'Avanza cada OT con el boton de estado: Iniciar, Pasar a Prueba, Finalizar. Registra repuestos usados desde Ordenes.' }
    ],
    recepcionista: [
      { sel: '.ag-grid.ag-grid-3', title: 'Tu panel', text: 'Recepciones activas y cotizaciones pendientes/aprobadas de un vistazo.' },
      { sel: '.ag-action-link', title: 'Accesos rapidos', text: 'Entra directo a registrar recepciones o cotizar. Usa Ctrl+K para navegar mas rapido.' }
    ],
    tutorial: [
      { sel: '.ag-tut-grid', title: 'Modulos del sistema', text: 'Cada tarjeta explica un modulo con sus pasos clave. Pulsa para ir directamente a esa pagina.' },
      { sel: '#quizBox', title: 'Ponte a prueba', text: 'Responde el mini-quiz para afianzar el flujo del taller.' }
    ],
    equipo: [
      { sel: '#eqQ', title: 'Busca a cualquiera', text: 'Por nombre, email o documento. Los filtros separan por rol.' },
      { sel: '#eqBody', title: 'Carga visible', text: 'Cada mecánico muestra sus OT activas para repartir parejo.' }
    ],
    clientes: [
      { sel: '#cliFQ', title: 'Busca primero', text: 'Documento, nombre, teléfono o placa. Crear duplicados ensucia la base.' },
      { sel: '#cliBody', title: 'Ficha completa', text: 'El botón Ver abre vehículos, visitas y comprobantes de cada cliente.' }
    ],
    reportes: [
      { sel: '#fDesde', title: 'El rango manda', text: 'Hoy, 7 días, mes o mes anterior. Todo se calcula en ese rango.' },
      { sel: '#repTabs', title: 'Una pestaña por pregunta', text: 'Clientes, ingresos, inventario, gastos, resultado y mecánicos. Cada una explica cómo leerla.' }
    ]
  };

  let tourState = null;

  function ensureTourDom() {
    if (document.getElementById('agTourOverlay')) return;
    const ov = document.createElement('div');
    ov.className = 'ag-tour-overlay hidden';
    ov.id = 'agTourOverlay';
    ov.innerHTML = `
      <div class="ag-tour-highlight" id="agTourHighlight"></div>
      <div class="ag-tour-popover" id="agTourPopover" role="dialog" aria-modal="true">
        <button class="tour-close" id="agTourClose" aria-label="Cerrar tutorial">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <span class="tour-step-badge" id="agTourBadge">Tutorial</span>
        <h4 id="agTourTitle"></h4>
        <p id="agTourText"></p>
        <div class="tour-controls">
          <span class="tour-progress" id="agTourProgress"></span>
          <button class="ag-btn ag-btn-outline" id="agTourSkip">Saltar</button>
          <button class="ag-btn ag-btn-primary" id="agTourNext">Siguiente</button>
        </div>
      </div>`;
    document.body.appendChild(ov);
    ov.addEventListener('click', (e) => { if (e.target === ov) stopTour(); });
    document.getElementById('agTourClose').addEventListener('click', stopTour);
    document.getElementById('agTourSkip').addEventListener('click', stopTour);
    document.getElementById('agTourNext').addEventListener('click', () => tourAdvance(1));

    let tourRaf = 0;
    const reponer = () => {
      if (!tourState) return;
      cancelAnimationFrame(tourRaf);
      tourRaf = requestAnimationFrame(() => { if (tourState) placeStep(); });
    };
    window.addEventListener('resize', reponer);
    document.addEventListener('scroll', reponer, true);
    document.addEventListener('keydown', (e) => {
      if (!tourState) return;
      if (e.key === 'Escape') stopTour();
      if (e.key === 'ArrowRight' || e.key === 'Enter') { e.preventDefault(); tourAdvance(1); }
      if (e.key === 'ArrowLeft') tourAdvance(-1);
    });
  }

  function placeStep() {
    if (!tourState) return;
    const { steps, idx } = tourState;
    const step = steps[idx];
    const ov = document.getElementById('agTourOverlay');
    const hl = document.getElementById('agTourHighlight');
    const pop = document.getElementById('agTourPopover');
    const el = step.sel ? document.querySelector(step.sel) : null;

    ov.classList.toggle('no-target', !el);
    document.getElementById('agTourBadge').textContent = 'Paso ' + (idx + 1) + ' de ' + steps.length;
    document.getElementById('agTourTitle').textContent = step.title;
    document.getElementById('agTourText').textContent = step.text;
    document.getElementById('agTourProgress').textContent = (idx + 1) + ' / ' + steps.length;
    document.getElementById('agTourNext').textContent = idx === steps.length - 1 ? 'Listo' : 'Siguiente';
    if (el) {
      const r = el.getBoundingClientRect();
      hl.style.display = 'block';
      hl.style.top = (r.top - 6) + 'px';
      hl.style.left = (r.left - 6) + 'px';
      hl.style.width = (r.width + 12) + 'px';
      hl.style.height = (r.height + 12) + 'px';
      const below = r.bottom + 14 + 240 < window.innerHeight;
      const top = below ? r.bottom + 14 : Math.max(14, r.top - 254);
      let left = Math.min(Math.max(14, r.left), window.innerWidth - 334);
      pop.style.top = top + 'px';
      pop.style.left = left + 'px';
      try { el.scrollIntoView({ behavior: 'smooth', block: 'center' }); } catch (_) {  }
    } else {
      hl.style.display = 'none';
      pop.style.top = '18vh';
      pop.style.left = '50%';
      pop.style.transform = 'translateX(-50%)';
    }
  }

  function tourAdvance(dir) {
    if (!tourState) return;
    const next = tourState.idx + dir;
    if (next < 0) return;
    if (next >= tourState.steps.length) { stopTour(); return; }
    tourState.idx = next;
    placeStep();
  }

  function stopTour(markDone = true) {
    if (tourState && markDone) {
      try { localStorage.setItem('ag-tour-' + tourState.page, 'done'); } catch (_) {  }
    }
    tourState = null;
    const ov = document.getElementById('agTourOverlay');
    if (ov) ov.classList.add('hidden');
  }

  function startTour(page) {
    const steps = TOUR_STEPS[page];
    if (!steps || !steps.length) return;
    ensureTourDom();
    document.getElementById('agTourOverlay').classList.remove('hidden');
    tourState = { page, steps, idx: 0 };
    placeStep();
  }
  window.agStartTour = startTour;


  const GUIDE_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5" fill="currentColor" stroke="none" opacity=".22"/><circle cx="12" cy="12" r="8.5"/><path d="M9.6 9.4a2.5 2.5 0 0 1 4.9.8c0 1.7-2.5 2.1-2.5 3.4"/><path d="M12 16.9v.01"/></svg>';
  const AG_GUIDES = {
    dashboard: { title: 'Tu tablero de un vistazo', steps: ['Revisa los indicadores: OT, ingresos y stock', 'Sigue el flujo del taller paso a paso', 'Usa los accesos rapidos o pulsa Ctrl+K'], cta: { label: 'Registrar recepcion', page: 'recepcion' } },
    recepcion: { title: 'Aqui entra cada vehiculo', steps: ['1. Registra cliente + vehiculo + problema', '2. Filtra por estado para ubicar cada caso', '3. Lo pendiente pasa a diagnostico en Cotizacion'], cta: { label: 'Ir a cotizacion', page: 'cotizacion' } },
    cotizacion: { title: 'Diagnostica y cotiza en 3 pasos', steps: ['1. Crea el diagnostico de una recepcion', '2. Agrega servicios y productos (total automatico)', '3. Aprueba para que nazca la OT'], cta: { label: 'Ver ordenes de trabajo', page: 'orden_trabajo' } },
    orden_trabajo: { title: 'Del taller a la entrega', steps: ['1. Crea la OT desde una cotizacion APROBADA', '2. Avanza el estado: PENDIENTE > EN_PROCESO > EN_PRUEBA > FINALIZADA', '3. Registra los repuestos usados (descuenta stock)'], cta: { label: 'Ir a pago y entrega', page: 'pago_entrega' } },
    inventario: { title: 'Tu almacen bajo control', steps: ['1. Busca productos al instante', '2. Registra entradas y ajustes de stock', '3. Vigila las alertas de stock minimo'], cta: null },
    pago_entrega: { title: 'Cobrar y entregar, sin perderse', steps: ['1. Registra el pago: elige BOLETA o FACTURA', '2. Revisa tu constancia e imprimela', '3. Registra la entrega del vehiculo'], cta: null },
    mecanico: { title: 'Tu trabajo de hoy', steps: ['1. Revisa tus OT asignadas', '2. Avanza cada una: Iniciar > Prueba > Finalizar', '3. Los repuestos se registran en Ordenes'], cta: { label: 'Abrir mis ordenes', page: 'orden_trabajo' } },
    recepcionista: { title: 'La puerta del taller', steps: ['1. Registra cada ingreso en Recepcion', '2. Cotiza y haz seguimiento a pendientes', '3. Lo aprobado sigue a Ordenes de Trabajo'], cta: { label: 'Nueva recepcion', page: 'recepcion' } },
    equipo: { title: 'Tu equipo, con nombre y apellido', steps: ['1. Crea mecánicos con DNI y especialidad', '2. Mira su carga: OT activas por persona', '3. Desactiva sin borrar; nada se pierde'], cta: null },
    clientes: { title: 'Tus clientes en un lugar', steps: ['1. Busca por documento, nombre o teléfono', '2. Abre la ficha: vehículos, visitas y comprobantes', '3. Lanza una recepción con el cliente ya cargado'], cta: { label: 'Nueva recepción', page: 'recepcion' } },
    reportes: { title: 'Los números no muerden', steps: ['1. Elige el rango: hoy, 7 días o el mes', '2. Lee el recuadro: te dice qué significa cada número', '3. Exporta a CSV para Excel'], cta: null }
  };

  function pageCanSee(page) {
    try {
      const u = (typeof getUser === 'function') ? getUser() : null;
      const map = window.AG_ROLE_PAGES;
      if (!u || !map || !map[page]) return true;
      return map[page].includes(u.rol);
    } catch (_) { return true; }
  }

  function mountGuide() {
    const page = (window.location.pathname.match(/([a-z_]+)\.html/i) || [null, ''])[1] || '';
    const g = AG_GUIDES[page];
    if (!g) return;
    try { if (localStorage.getItem('ag-guide-off-' + page)) return; } catch (_) {  }
    const host = document.querySelector('.ag-page');
    if (!host || host.querySelector('.ag-guide')) return;
    const el = document.createElement('div');
    el.className = 'ag-guide';
    const steps = g.steps.map((s, i) => `<li><b>${i + 1}</b><span>${s}</span></li>`).join('');
    const cta = (g.cta && pageCanSee(g.cta.page))
      ? `<a class="ag-cta ag-guide-cta" href="${P + g.cta.page}.html">${g.cta.label}<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg></a>` : '';
    el.innerHTML = `
      <div class="ag-guide-icon">${GUIDE_ICON}</div>
      <div class="ag-guide-body">
        <p class="ag-guide-title">${g.title}</p>
        <ul class="ag-guide-steps">${steps}</ul>
      </div>
      ${cta}
      <button class="ag-guide-close" aria-label="Ocultar guia" title="Ocultar guia">&times;</button>`;
    el.querySelector('.ag-guide-close').addEventListener('click', () => {
      try { localStorage.setItem('ag-guide-off-' + page, '1'); } catch (_) {  }
      el.remove();
    });
    const header = host.querySelector('.ag-page-header, .ag-head');
    if (header && header.nextSibling) header.after(el);
    else host.prepend(el);
  }
  window.agMountGuide = mountGuide;


  const GLOSARIO = {
    ot: 'OT = Orden de Trabajo: qué se le hace al auto, quién y en qué estado va.',
    ruc: 'RUC: 11 dígitos de empresa o negocio. Sin RUC válido no hay factura.',
    dni: 'DNI peruano: 8 dígitos. Obligatorio en boletas mayores a S/ 700.',
    boleta: 'Boleta: para consumidor final (DNI). No da crédito fiscal.',
    factura: 'Factura: para empresas (RUC + razón social + dirección). Da crédito fiscal.',
    serie: 'Serie + correlativo (B001-00000001): número único que jamás se repite.',
    igv: 'IGV: impuesto del 18 % ya incluido en los precios del taller.',
    kardex: 'Kardex: saldo inicial + entradas − salidas = saldo final de cada producto.',
    merma: 'Merma: pérdida de almacén (vencido, roto). Se registra con motivo.',
    stock: 'Stock mínimo: debajo de él salta la alerta y toca comprar.',
    utilidad: 'Utilidad estimada = ingresos sin IGV − consumos − gastos. Estimación académica.',
    cotizacion: 'Cotización: presupuesto que el cliente aprueba. Aprobada → nace la OT.'
  };
  function enhanceGlosarioEl(el) {
    const key = (el.dataset.g || '').toLowerCase();
    const def = GLOSARIO[key];
    if (!def || el.dataset.gDone) return;
    el.dataset.gDone = '1';
    el.style.borderBottom = '1px dotted var(--ag-oil)';
    el.style.cursor = 'help';
    el.title = def;
    el.addEventListener('click', () => toast(def, 'info', 6000));
  }
  function initGlosario() {
    document.querySelectorAll('[data-g]').forEach(enhanceGlosarioEl);

    try {
      new MutationObserver(muts => {
        muts.forEach(m => m.addedNodes.forEach(n => {
          if (!n || n.nodeType !== 1) return;
          if (n.matches && n.matches('[data-g]')) enhanceGlosarioEl(n);
          if (n.querySelectorAll) n.querySelectorAll('[data-g]').forEach(enhanceGlosarioEl);
        }));
      }).observe(document.body, { childList: true, subtree: true });
    } catch (_) {  }
  }
  window.agInitGlosario = initGlosario;


  function initHelp() {
    const page = (window.location.pathname.match(/([a-z_]+)\.html/i) || [null, ''])[1] || '';
    if (document.querySelector('.ag-fab')) return;
    const fab = document.createElement('button');
    fab.className = 'ag-fab';
    fab.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="8.5" fill="currentColor" stroke="none" opacity=".22"/><circle cx="12" cy="12" r="8.5"/><path d="M9.6 9.4a2.5 2.5 0 0 1 4.9.8c0 1.7-2.5 2.1-2.5 3.4"/><path d="M12 16.9v.01"/></svg><span>Como usar</span>';
    fab.title = 'Ver tour guiado de esta pagina';
    fab.addEventListener('click', () => startTour(page));
    document.body.appendChild(fab);


    try {
      if (TOUR_STEPS[page] && !localStorage.getItem('ag-tour-' + page)) {
        setTimeout(() => startTour(page), 900);
      }
    } catch (_) {  }
  }


  const NAV_ENTRIES = [
    { page: 'dashboard', label: 'Dashboard', desc: 'Resumen e indicadores', keywords: 'inicio resumen kpi indicadores' },
    { page: 'recepcion', label: 'Recepcion', desc: 'Registrar ingreso de vehiculos', keywords: 'ingreso vehiculo cliente nuevo' },
    { page: 'cotizacion', label: 'Cotizacion', desc: 'Diagnostico y cotizaciones', keywords: 'presupuesto diagnostico precio servicios' },
    { page: 'orden_trabajo', label: 'Ordenes de trabajo', desc: 'Gestion y seguimiento de OT', keywords: 'ot mecanico estados avance' },
    { page: 'inventario', label: 'Inventario', desc: 'Repuestos e insumos', keywords: 'stock productos almacen movimiento' },
    { page: 'pago_entrega', label: 'Pago y Entrega', desc: 'Cobros y entrega de vehiculos', keywords: 'cobro caja entrega finalizar' },
    { page: 'mecanico', label: 'Panel Mecanico', desc: 'OT asignadas', keywords: 'taller tareas' },
    { page: 'recepcionista', label: 'Panel Recepcionista', desc: 'Recepciones y cotizaciones', keywords: 'front desk' },
    { page: 'tutorial', label: 'Tutorial', desc: 'Aprende a usar el sistema', keywords: 'ayuda guia como usar manual faq quiz' },
    { page: 'equipo', label: 'Equipo', desc: 'Mecánicos y personal del taller', keywords: 'mecanicos personal usuarios roles especialidad' },
    { page: 'clientes', label: 'Clientes', desc: 'Personas y empresas, vehículos e historial', keywords: 'clientes buscar dni ruc razon social telefono placa' },
    { page: 'reportes', label: 'Reportes', desc: 'Clientes, ingresos, inventario, gastos y resultado', keywords: 'reportes graficos kpi csv utilidad kardex gastos' },
    { page: 'comentarios', label: 'Comentarios', desc: 'Encuestas y comentarios de los clientes', keywords: 'encuesta satisfaccion comentarios estrellas opinion clientes' }
  ];

  let paletteOpen = false;

  function openPalette() {
    if (paletteOpen) return;
    paletteOpen = true;
    const ov = document.createElement('div');
    ov.className = 'ag-palette-overlay';
    ov.id = 'agPalette';
    ov.innerHTML = `
      <div class="ag-palette" role="dialog" aria-label="Buscador de paginas">
        <input class="ag-palette-input" id="agPaletteInput" type="text" placeholder="Buscar pagina... (ej: inventario, pago, tutorial)" autocomplete="off">
        <div class="ag-palette-list" id="agPaletteList"></div>
        <div class="ag-palette-footer"><span><kbd>↑↓</kbd> navegar</span><span><kbd>Enter</kbd> abrir</span><span><kbd>Esc</kbd> cerrar</span></div>
      </div>`;
    document.body.appendChild(ov);
    const input = document.getElementById('agPaletteInput');
    const list = document.getElementById('agPaletteList');
    let selected = 0;
    let results = [];


    function allowedPages() {
      try {
        const u = (typeof getUser === 'function') ? getUser() : null;
        const map = window.AG_ROLE_PAGES;
        if (!u || !map) return null;
        return Object.keys(map).filter(p => map[p].includes(u.rol));
      } catch (_) { return null; }
    }

    function render(q) {
      const query = (q || '').trim().toLowerCase();
      const allowed = allowedPages();
      results = NAV_ENTRIES.filter(e =>
        (!allowed || allowed.includes(e.page)) &&
        (!query || e.label.toLowerCase().includes(query) || e.desc.toLowerCase().includes(query) || e.keywords.includes(query))
      );
      selected = 0;
      if (!results.length) {
        list.textContent = '';
        const empty = document.createElement('div');
        empty.className = 'ag-palette-empty';
        empty.textContent = `Sin resultados para "${q}"`;
        list.appendChild(empty);
        return;
      }
      list.innerHTML = results.map((e, i) => `
        <div class="ag-palette-item ${i === 0 ? 'selected' : ''}" data-page="${e.page}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
          ${e.label}<span class="pal-hint">${e.desc}</span>
        </div>`).join('');
      list.querySelectorAll('.ag-palette-item').forEach((item, i) => {
        item.addEventListener('click', () => go(results[i].page));
      });
    }
    function go(page) { closePalette(); window.location.href = P + page + '.html'; }
    function move(dir) {
      if (!results.length) return;
      selected = (selected + dir + results.length) % results.length;
      list.querySelectorAll('.ag-palette-item').forEach((el, i) => el.classList.toggle('selected', i === selected));
      list.querySelectorAll('.ag-palette-item')[selected]?.scrollIntoView({ block: 'nearest' });
    }
    input.addEventListener('input', () => render(input.value));
    input.addEventListener('keydown', (e) => {
      if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
      if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
      if (e.key === 'Enter' && results[selected]) { e.preventDefault(); go(results[selected].page); }
    });
    ov.addEventListener('click', (e) => { if (e.target === ov) closePalette(); });
    render('');
    setTimeout(() => input.focus(), 30);
  }

  function closePalette() {
    paletteOpen = false;
    document.getElementById('agPalette')?.remove();
  }

  function initPalette() {
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        paletteOpen ? closePalette() : openPalette();
      }
      if (e.key === 'Escape' && paletteOpen) closePalette();
    });

    const tryInject = setInterval(() => {
      const right = document.querySelector('.ag-navbar-right');
      if (!right) return;
      clearInterval(tryInject);
      if (right.querySelector('.ag-navbar-search')) return;
      const btn = document.createElement('button');
      btn.className = 'ag-navbar-search';
      btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Buscar... <kbd>Ctrl K</kbd>';
      btn.addEventListener('click', openPalette);
      right.insertBefore(btn, right.firstChild);
    }, 200);
    setTimeout(() => clearInterval(tryInject), 8000);
  }


  function initNavbarHelp() {
    const tryInject = setInterval(() => {
      const right = document.querySelector('.ag-navbar-right');
      if (!right) return;
      clearInterval(tryInject);
      if (right.querySelector('.ag-navbar-help')) return;
      const btn = document.createElement('a');
      btn.className = 'ag-navbar-help';
      btn.href = P + 'tutorial.html';
      btn.title = 'Tutorial del sistema';
      btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg><span>Tutorial</span>';
      right.insertBefore(btn, right.firstChild);
    }, 200);
    setTimeout(() => clearInterval(tryInject), 8000);
  }


  function initHola() {
    let msg = null;
    try { msg = sessionStorage.getItem('ag-hola-msg'); sessionStorage.removeItem('ag-hola-msg'); } catch (_) {  }
    if (msg) setTimeout(() => toast(msg, 'info', 6000), 800);
  }


  function init() {
    initPalette();
    initNavbarHelp();
    initHola();
    try { mountGuide(); } catch (_) {  }
    try { initGlosario(); } catch (_) {  }
    const page = (window.location.pathname.match(/([a-z_]+)\.html/i) || [null, ''])[1] || '';

    if (page && page !== 'tutorial' && TOUR_STEPS[page]) initHelp();
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
