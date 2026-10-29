const API_BASE = (() => {
    // Solo localStorage y mismo-origen: ?apiBase= en la URL permitía a un
    // enlace malicioso redirigir la API y robar el token (phishing).
    const saved = localStorage.getItem('apiBase');
    if (saved) {
        try {
            const u = new URL(saved);
            if (u.protocol === 'http:' || u.protocol === 'https:') return saved.replace(/\/$/, '');
        } catch (_) { /* valor inválido: se ignora */ }
    }
    // Docker: Nginx sirve la web y reenvia /api al backend (mismo origen, sin CORS).
    // Live Server (5501/5502) o file:// no tienen proxy: hablan directo con el backend.
    const { protocol, hostname, port } = window.location;
    const sinProxy = protocol === 'file:' ||
        ((hostname === 'localhost' || hostname === '127.0.0.1') && (port === '5501' || port === '5502'));
    return sinProxy ? 'http://localhost:8080/api' : '/api';
})();
/* Escape HTML: todo dato del backend/usuario que se interpole en innerHTML
   debe pasar por aquí. Cubre & < > " ' ` =. */
function esc(s) {
    return String(s ?? '').replace(/[&<>"'`=]/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;', '=': '&#61;'
    }[c]));
}
function getToken() { return localStorage.getItem('token'); }
function setToken(token) { localStorage.setItem('token', token); }
function clearToken() { localStorage.removeItem('token'); localStorage.removeItem('user'); }
function getUser() { const u = localStorage.getItem('user'); return u ? JSON.parse(u) : null; }
function setUser(user) { localStorage.setItem('user', JSON.stringify(user)); }
function isLoggedIn() { return !!getToken(); }
/* Rutas base: el login del personal vive en frontend/admin/index.html y las paginas en
   frontend/admin/pages/. Desde pages/ hay que subir un nivel (../index.html); desde /admin/ no.
   La raiz (/) es el portal del cliente. */
function agIsInPages() { return window.location.pathname.replace(/\\/g, '/').includes('/pages/'); }
function agLoginPath() { return agIsInPages() ? '../index.html' : 'index.html'; }
function logout() { clearToken(); window.location.href = agLoginPath(); }
function checkAuth() { if (!isLoggedIn()) { window.location.href = agLoginPath(); return false; } return true; }

/* =========================================================
   LIMPIEZA DE OBJETOS: quita vacíos ("" / null / undefined)
   para no romper @Email/@Size opcionales. El backend ya exige
   lo obligatorio con mensajes claros. */
function agLimpia(obj) {
    const o = {};
    for (const [k, v] of Object.entries(obj || {})) {
        if (v === '' || v === undefined || v === null) continue;
        o[k] = v;
    }
    return o;
}

/* =========================================================
   PERMISOS POR ROL
   Define que paginas puede ver cada rol. El navbar, los
   guards de pagina, el buscador Ctrl+K y el tutorial usan
   este mismo mapa como unica fuente de verdad.
   ========================================================= */
const ROLE_PAGES = {
    dashboard:     ['ADMIN', 'ALMACENERO'],
    recepcion:     ['ADMIN', 'RECEPCIONISTA'],
    clientes:      ['ADMIN', 'RECEPCIONISTA'],
    cotizacion:    ['ADMIN', 'RECEPCIONISTA'],
    orden_trabajo: ['ADMIN', 'RECEPCIONISTA', 'MECANICO'],
    inventario:    ['ADMIN', 'ALMACENERO', 'MECANICO'],
    pago_entrega:  ['ADMIN', 'RECEPCIONISTA'],
    mecanico:      ['MECANICO'],
    recepcionista: ['RECEPCIONISTA'],
    equipo:        ['ADMIN'],
    reportes:      ['ADMIN', 'RECEPCIONISTA', 'ALMACENERO'],
    tutorial:      ['ADMIN', 'MECANICO', 'ALMACENERO', 'RECEPCIONISTA'],
};
window.AG_ROLE_PAGES = ROLE_PAGES;

/* Guard de pagina: exige sesion y que el rol tenga acceso.
   Sin sesion -> login; con sesion pero rol sin permiso -> pagina de acceso denegado.
   Uso: if (!guardPage('inventario')) return; */
function guardPage(page) {
    if (!checkAuth()) return false;
    const user = getUser();
    const allowed = ROLE_PAGES[page] || [];
    if (user && allowed.includes(user.rol)) return true;
    window.location.href = 'acceso_denegado.html';
    return false;
}

/* Pagina de inicio segun el rol (post-login y redirecciones).
   Desde el login (raiz) incluye 'pages/'; desde dentro de pages/ es relativo. */
function homePageForRol(rol, fromPages) {
    const inPages = typeof fromPages === 'boolean' ? fromPages : agIsInPages();
    const page = rol === 'MECANICO' ? 'mecanico.html'
        : rol === 'RECEPCIONISTA' ? 'recepcionista.html'
        : rol === 'ALMACENERO' ? 'inventario.html'
        : 'dashboard.html';
    return inPages ? page : 'pages/' + page;
}

async function apiFetch(endpoint, options = {}) {
    const token = getToken();
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        await agFalla(response);
        const ct = response.headers.get('content-type');
        if (ct && ct.includes('application/json')) return await response.json();
        return null;
    } catch (error) { console.error('API Error:', error); throw error; }
}

/* Descarga de archivos (PDF) con la misma sesion y los mismos errores que
   apiFetch. El archivo lo genera el backend: aqui solo se trae el blob. */
async function apiFetchBlob(endpoint) {
    const token = getToken();
    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { headers });
        await agFalla(response);
        return await response.blob();
    } catch (error) { console.error('API Error:', error); throw error; }
}

/* Descarga un archivo del backend (PDF, CSV...) con la sesión del usuario y
   lo guarda con el nombre indicado. Un <a href="/api/..."> no sirve: no lleva
   el token y /api tampoco existe en el puerto del frontend. */
async function apiDescargar(endpoint, nombre) {
    const blob = await apiFetchBlob(endpoint);
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = nombre;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 800);
    return blob;
}

/* Unico lugar donde un HTTP de error se convierte en el mensaje que ve el
   usuario (BusinessException, validacion Bean, 401 y 403). */
async function agFalla(response) {
    if (response.status === 401) {
        // Token invalido o expirado: cerrar sesion y volver al login.
        clearToken();
        throw new Error('Sesion expirada. Inicia sesion nuevamente');
    }
    if (response.status === 403) {
        // Autenticado pero sin permisos por rol: NO cerrar sesion, solo informar.
        throw new Error('No tienes permisos para realizar esta accion con tu rol');
    }
    if (!response.ok) {
        let msg = `Error ${response.status}`;
        let campo = null;
        try {
            const body = await response.json();
            /* BusinessException: { mensaje, campo, sugerencia } en lenguaje claro */
            if (body?.mensaje) msg = body.sugerencia ? `${body.mensaje} ${body.sugerencia}` : body.mensaje;
            else if (body?.error) msg = body.error;
            else if (body?.message) msg = body.message;
            /* Validación Bean: mapa { campo: mensaje } sin clave fija */
            else if (body && typeof body === 'object') {
                const parts = Object.entries(body)
                    .filter(([, v]) => typeof v === 'string' && v)
                    .map(([k, v]) => `${k}: ${v}`);
                if (parts.length) { msg = parts.join(' · '); campo = Object.keys(body)[0]; }
            }
            campo = campo || body?.campo || null;
        } catch (_) { try { const t = await response.text(); msg = t || msg; } catch (_) { /* noop */ } }
        const err = new Error(msg);
        if (campo) err.campo = campo;
        throw err;
    }
}

function showAlert(containerId, message, type = 'danger') {
    const container = document.getElementById(containerId);
    if (!container) return;
    const svgMap = {
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
        danger: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        warning: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3.8L2.6 19.5h18.8z" fill="currentColor" stroke="none" opacity=".25"/><path d="M12 3.8L2.6 19.5h18.8L12 3.8z"/><path d="M12 9.8v4.2M12 17v.01"/></svg>',
        info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
    };
    container.innerHTML = `
        <div class="ag-alert ag-alert-${type}">
            ${svgMap[type] || svgMap.info}
            <span>${esc(message)}</span>
            <button class="ag-alert-close" onclick="this.parentElement.remove()">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
        </div>`;
    setTimeout(() => { const a = container.querySelector('.ag-alert'); if (a) a.remove(); }, 5000);
}
function showSuccess(id, msg) { showAlert(id, msg, 'success'); }
function showError(id, msg) { showAlert(id, msg, 'danger'); }

/* =========================================================
   STEPPER DE CANTIDAD ( - / input / + )
   Reemplaza los spinners nativos de input[type=number].
   Uso: container.innerHTML = agStepper('miInput', { value: 1, min: 1, size: 'sm' });
   ========================================================= */
function agStepper(inputId, { value = 1, min = 1, max = 9999, step = 1, size = '' } = {}) {
    const sizeClass = size === 'sm' ? ' ag-stepper--sm' : size === 'lg' ? ' ag-stepper--lg' : '';
    const iconMinus = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>';
    const iconPlus = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>';
    const btn = (dir, label, icon) => `<button type="button" class="ag-stepper-btn" aria-label="${label}" onclick="agStepperStep('${inputId}', ${dir})">${icon}</button>`;
    return `<span class="ag-stepper${sizeClass}">
        ${btn(-1, 'Disminuir', iconMinus)}
        <input type="number" class="ag-stepper-value" id="${inputId}" value="${value}" min="${min}" max="${max}" step="${step}" inputmode="numeric">
        ${btn(1, 'Aumentar', iconPlus)}
    </span>`;
}
window.agStepper = agStepper;
window.agStepperStep = function (inputId, dir) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const step = parseFloat(input.step) || 1;
    const min = input.min !== '' ? parseFloat(input.min) : -Infinity;
    const max = input.max !== '' ? parseFloat(input.max) : Infinity;
    const current = parseFloat(input.value);
    const base = isNaN(current) ? (min === -Infinity ? 0 : min) : current;
    input.value = Math.min(max, Math.max(min, base + dir * step));
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
};

/* =========================================================
   NAVBAR SEGUN ROL
   ========================================================= */
/* Set duotono AutoGestion: trazo 1.8, remates redondos y capa suave
   (fill con opacidad) para dar profundidad sin perder legibilidad a 15px. */
const AG_SW = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">';
const AG_SOFT = ' fill="currentColor" stroke="none" opacity=".28"';
const NAV_ICONS = {
    dashboard: `${AG_SW}<rect x="3.5" y="3.5" width="7" height="7" rx="2"${AG_SOFT}/><rect x="13.5" y="3.5" width="7" height="7" rx="2"/><rect x="3.5" y="13.5" width="7" height="7" rx="2"/><rect x="13.5" y="13.5" width="7" height="7" rx="2"/></svg>`,
    recepcion: `${AG_SW}<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="2.4"${AG_SOFT}/><circle cx="12" cy="12" r="2.4"/><path d="M12 9.6V4"/><path d="M9.9 13.2L4.9 16.1"/><path d="M14.1 13.2L19.1 16.1"/></svg>`,
    cotizacion: `${AG_SW}<path d="M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8z"${AG_SOFT}/><path d="M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8z"/><path d="M13.5 3.5V8H18"/><path d="M9 12.5h6M9 16h4"/></svg>`,
    orden_trabajo: `${AG_SW}<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/><circle cx="16.6" cy="7.4" r="1.4"${AG_SOFT}/></svg>`,
    inventario: `${AG_SW}<path d="M12 2.8l8.5 4.6L12 12 3.5 7.4z"${AG_SOFT}/><path d="M12 2.8l8.5 4.6L12 12 3.5 7.4 12 2.8z"/><path d="M3.5 7.4v9.2L12 21.2l8.5-4.6V7.4"/><path d="M12 12v9.2"/></svg>`,
    pago_entrega: `${AG_SW}<rect x="2.8" y="7" width="18.4" height="11" rx="2.5"${AG_SOFT}/><rect x="2.8" y="7" width="18.4" height="11" rx="2.5"/><circle cx="12" cy="12.5" r="2.6"/><path d="M6.3 10.3h.01M17.7 14.7h.01"/></svg>`,
    equipo: `${AG_SW}<circle cx="9" cy="8.5" r="3.2"${AG_SOFT}/><circle cx="9" cy="8.5" r="3.2"/><path d="M3.3 19.5c.7-3.1 2.9-4.8 5.7-4.8s5 1.7 5.7 4.8"/><path d="M15.2 5.9a3.2 3.2 0 0 1 0 5.4"/><path d="M17.3 14.9c2 .7 3.2 2.2 3.6 4.3"/></svg>`,
    reportes: `${AG_SW}<path d="M4 20h16"/><rect x="6.6" y="13" width="3.2" height="7" rx="1.2"/><rect x="11" y="9" width="3.2" height="11" rx="1.2"${AG_SOFT}/><rect x="11" y="9" width="3.2" height="11" rx="1.2"/><rect x="15.4" y="5" width="3.2" height="15" rx="1.2"/></svg>`,
    clientes: `${AG_SW}<rect x="3" y="5.5" width="18" height="13.5" rx="2.5" fill="currentColor" stroke="none" opacity=".22"/><rect x="3" y="5.5" width="18" height="13.5" rx="2.5"/><circle cx="8.3" cy="11" r="2"/><path d="M5.9 15.9c.5-1.6 1.4-2.4 2.4-2.4s1.9.8 2.4 2.4"/><path d="M14 9.5h4.5M14 13h4.5"/></svg>`,
    mecanico: `${AG_SW}<path d="M5 15.5C5 11 8 8 12 8s7 3 7 7.5"${AG_SOFT}/><path d="M5 15.5C5 11 8 8 12 8s7 3 7 7.5"/><path d="M12 8V5.8"/><path d="M10.3 5.8h3.4"/><path d="M3 15.5h18"/><path d="M6.5 18.5h11"/></svg>`,
    recepcionista: `${AG_SW}<path d="M4.5 14.5v-2.5a7.5 7.5 0 0 1 15 0v2.5"/><rect x="3.2" y="13.2" width="3.6" height="6" rx="1.8"${AG_SOFT}/><rect x="3.2" y="13.2" width="3.6" height="6" rx="1.8"/><rect x="17.2" y="13.2" width="3.6" height="6" rx="1.8"/><path d="M20.8 18.5c0 1.9-1.6 3-3.6 3H14"/></svg>`,
};
const NAV_LABELS = {
    dashboard: 'Dashboard', recepcion: 'Recepcion', cotizacion: 'Cotizacion',
    orden_trabajo: 'Ordenes', inventario: 'Inventario', pago_entrega: 'Pago / Entrega',
    mecanico: 'Panel Mecanico', recepcionista: 'Panel Recepcionista',     equipo: 'Equipo', clientes: 'Clientes', reportes: 'Reportes',
};
const ROLE_NAV = {
    ADMIN: ['dashboard', 'recepcion', 'clientes', 'cotizacion', 'orden_trabajo', 'inventario', 'pago_entrega', 'reportes', 'equipo'],
    MECANICO: ['mecanico', 'orden_trabajo', 'inventario'],
    ALMACENERO: ['dashboard', 'inventario', 'reportes'],
    RECEPCIONISTA: ['recepcionista', 'recepcion', 'clientes', 'cotizacion', 'orden_trabajo', 'pago_entrega', 'reportes'],
};

/* Grupos del menú lateral, en orden del flujo del taller */
const NAV_GROUPS = [
    { titulo: 'Operación', paginas: ['dashboard', 'recepcion', 'clientes', 'recepcionista'] },
    { titulo: 'Taller', paginas: ['cotizacion', 'orden_trabajo', 'mecanico'] },
    { titulo: 'Caja', paginas: ['pago_entrega'] },
    { titulo: 'Almacén', paginas: ['inventario'] },
    { titulo: 'Gestión', paginas: ['reportes', 'equipo'] },
    { titulo: 'Ayuda', paginas: ['tutorial'] },
];
const NAV_TUTORIAL_ICON = `${AG_SW}<path d="M12 6.8C10 5 7.4 4.7 4.8 5.2v13.2c2.6-.5 5.2-.2 7.2 1.6 2-1.8 4.6-2.1 7.2-1.6V5.2c-2.6-.5-5.2-.2-7.2 1.6z" fill="currentColor" stroke="none" opacity=".22"/><path d="M12 6.8C10 5 7.4 4.7 4.8 5.2v13.2c2.6-.5 5.2-.2 7.2 1.6 2-1.8 4.6-2.1 7.2-1.6V5.2c-2.6-.5-5.2-.2-7.2 1.6z"/><path d="M12 6.8V20"/></svg>`;
function agIsMobile() { return window.innerWidth < 1024; }
function agSidebarSaved() {
    try { return localStorage.getItem('ag-sidebar') || 'open'; }
    catch (_) { return 'open'; }
}
function agApplySidebar() {
    const mobile = agIsMobile();
    const open = !mobile && agSidebarSaved() === 'open';
    document.body.classList.toggle('sb-open', open);
    document.body.classList.toggle('sb-closed', !open && !mobile);
    if (!mobile) document.body.classList.remove('sb-m-open');
}
function agToggleSidebar() {
    if (agIsMobile()) {
        document.body.classList.toggle('sb-m-open');
        return;
    }
    try { localStorage.setItem('ag-sidebar', agSidebarSaved() === 'open' ? 'closed' : 'open'); }
    catch (_) { /* noop */ }
    agApplySidebar();
}
function agCloseSidebarMobile() { document.body.classList.remove('sb-m-open'); }
window.agToggleSidebar = agToggleSidebar;
window.agCloseSidebarMobile = agCloseSidebarMobile;

function renderNavbar(activePage) {
    const user = getUser();
    if (!user) return '';
    const roleLabel = user.rol === 'ADMIN' ? 'Administrador' : user.rol === 'MECANICO' ? 'Mecanico' : user.rol === 'RECEPCIONISTA' ? 'Recepcionista' : 'Almacenero';
    const dotClass = user.rol === 'ADMIN' ? 'dot-admin' : user.rol === 'MECANICO' ? 'dot-mecanico' : user.rol === 'RECEPCIONISTA' ? 'dot-recepcionista' : 'dot-almacenero';
    const allowed = ROLE_NAV[user.rol] || [];
    const linkFor = (page) => {
        if (page === 'tutorial') return `<a href="tutorial.html" class="${activePage === 'tutorial' ? 'active' : ''}" title="Tutorial del sistema">${NAV_TUTORIAL_ICON}<span>Tutorial</span></a>`;
        let label = NAV_LABELS[page] || page;
        if (page === 'mecanico' && user.rol === 'MECANICO') label = 'Panel';
        if (page === 'recepcionista' && user.rol === 'RECEPCIONISTA') label = 'Panel';
        return `<a href="${page}.html" class="${activePage === page ? 'active' : ''}" title="${label}">${NAV_ICONS[page] || ''}<span>${label}</span></a>`;
    };
    const groups = NAV_GROUPS.map(g => {
        const items = g.paginas.filter(p => p === 'tutorial' || allowed.includes(p));
        if (!items.length) return '';
        return `<div class="ag-sb-group"><p class="ag-sb-title">${g.titulo}</p>${items.map(linkFor).join('')}</div>`;
    }).join('');
    setTimeout(agApplySidebar, 0);
    return `
    <nav class="ag-navbar ag-topbar">
        <button class="ag-hamburger" onclick="agToggleSidebar()" aria-label="Abrir menú" title="Menú">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <a class="ag-navbar-brand" href="${homePageForRol(user.rol)}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
            AutoGestion
        </a>
        <div class="ag-navbar-right">
            <div class="ag-user-pill" title="${roleLabel}">
                <span class="ag-user-pill-dot ${dotClass}"></span>
                <span class="ag-user-pill-role">${roleLabel}</span>
            </div>
            <button class="ag-btn-ghost" onclick="logout()">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                Salir
            </button>
        </div>
    </nav>
    <div class="ag-sb-overlay" id="agSbOverlay" onclick="agCloseSidebarMobile()"></div>
    <aside class="ag-sidebar" id="agSidebar" aria-label="Menú principal">
        <a class="ag-sb-brand" href="${homePageForRol(user.rol)}">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
            <span>AutoGestion</span>
        </a>
        <nav class="ag-sb-nav">${groups}</nav>
        <div class="ag-sb-foot">
            <span class="ag-user-pill-dot ${dotClass}"></span>
            <span>${roleLabel}</span>
        </div>
    </aside>`;
}
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') agCloseSidebarMobile(); });
window.addEventListener('resize', () => { if (!agIsMobile()) document.body.classList.remove('sb-m-open'); agApplySidebar(); });
function renderThemeToggle() {
    const stored = localStorage.getItem('ag-theme') || 'system';
    const icons = {
        system: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>',
        light: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>',
        dark: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>'
    };
    return `<button class="ag-theme-toggle" id="themeToggle" title="Cambiar tema" aria-label="Cambiar tema">${icons[stored]}</button>`;
}
function formatCurrency(amount) {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(amount);
}
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE') + ' ' + d.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}
function estadoBadge(estado) {
    const map = {
        'PENDIENTE': 'secondary', 'EN_DIAGNOSTICO': 'info', 'COTIZADA': 'warning',
        'EN_TRABAJO': 'primary', 'EN_PROCESO': 'warning', 'EN_PRUEBA': 'info',
        'FINALIZADA': 'success', 'CANCELADA': 'danger', 'APROBADA': 'success',
        'RECHAZADA': 'danger', 'ENTREGADA': 'success',
    };
    return `<span class="ag-badge ag-badge-${map[estado] || 'secondary'}">${estado.replace(/_/g, ' ')}</span>`;
}
function tipoBadge(tipo) {
    return `<span class="ag-badge ag-badge-${tipo === 'REPUESTO' ? 'repuesto' : 'insumo'}">${tipo}</span>`;
}
function stockClass(actual, minimo) {
    if (actual === 0) return 'row-danger';
    if (actual < minimo) return 'row-danger';
    return '';
}
function initTheme() {
    const stored = localStorage.getItem('ag-theme') || 'system';
    applyTheme(stored);
    const btn = document.getElementById('themeToggle');
    if (!btn) return;
    const order = ['system', 'light', 'dark'];
    const iconMap = {
        system: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>',
        light: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>',
        dark: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>'
    };
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    mq.addEventListener('change', () => {
        if (localStorage.getItem('ag-theme') === 'system') applyTheme('system');
    });
    btn.addEventListener('click', () => {
        const current = localStorage.getItem('ag-theme') || 'system';
        const next = order[(order.indexOf(current) + 1) % order.length];
        localStorage.setItem('ag-theme', next);
        applyTheme(next);
        btn.innerHTML = iconMap[next];
        btn.title = `Tema: ${next}`;
    });
}
function applyTheme(mode) {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const resolved = mode === 'system' ? (mq.matches ? 'dark' : 'light') : mode;
    document.documentElement.setAttribute('data-bs-theme', resolved);
}
