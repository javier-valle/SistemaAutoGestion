


const PORTAL_API = (() => {
    const { protocol, hostname, port } = window.location;
    const sinProxy = protocol === 'file:' ||
        ((hostname === 'localhost' || hostname === '127.0.0.1') && (port === '5501' || port === '5502'));
    return sinProxy ? 'http://localhost:8080/api' : '/api';
})();

const PORTAL_TOKEN_KEY = 'ag_portal_token';
const PORTAL_NOMBRE_KEY = 'ag_portal_nombre';

function portalEsc(s) {
    return String(s ?? '').replace(/[&<>"'`=]/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;', '=': '&#61;'
    }[c]));
}
function portalToken() { return sessionStorage.getItem(PORTAL_TOKEN_KEY); }
function portalNombre() { return sessionStorage.getItem(PORTAL_NOMBRE_KEY) || ''; }
function portalSetSesion(token, nombre) {
    sessionStorage.setItem(PORTAL_TOKEN_KEY, token);
    sessionStorage.setItem(PORTAL_NOMBRE_KEY, nombre || '');
}
function portalSalir() {
    sessionStorage.removeItem(PORTAL_TOKEN_KEY);
    sessionStorage.removeItem(PORTAL_NOMBRE_KEY);
    window.location.href = '/';
}

function portalGuard() {
    if (!portalToken()) { window.location.href = '/'; return false; }
    return true;
}

/** Descarga autenticada: el token va en la cabecera, nunca en la URL. */
async function portalDescargar(endpoint, nombre) {
    const token = portalToken();
    const res = await fetch(PORTAL_API + endpoint, {
        headers: token ? { 'Authorization': 'Bearer ' + token } : {}
    });
    if (res.status === 401 && token) { portalSalir(); throw new Error('Sesión vencida'); }
    if (!res.ok) {
        const err = new Error('No se pudo descargar el archivo (' + res.status + ')');
        err.status = res.status;
        throw err;
    }
    const blob = await res.blob();
    if (!blob || !blob.size) throw new Error('El archivo llegó vacío');
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.rel = 'noopener';
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { try { a.remove(); URL.revokeObjectURL(url); } catch (_) { } }, 1500);
}

async function portalFetch(endpoint, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = portalToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(PORTAL_API + endpoint, { ...options, headers });
    if (res.status === 401 && token) { portalSalir(); throw new Error('Sesión vencida'); }
    if (res.status === 429) { const e429 = new Error('Demasiados intentos. Espera un minuto.'); e429.status = 429; throw e429; }
    if (!res.ok) {

        let detalle = null;
        try { const j = await res.json(); detalle = j && (j.mensaje || j.message) || null; } catch (_) {  }
        const err = new Error('No se pudo completar la solicitud (' + res.status + ')');
        err.status = res.status;
        err.detalle = detalle;
        throw err;
    }
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : null;
}
