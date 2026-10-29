

(function () {
  'use strict';

  var LINKS = [
    ['/portal/estado.html', 'Inicio', true],
    ['/portal/historial.html', 'Historial', true],
    ['/portal/cotizacion.html', 'Cotización', true],
    ['/portal/comprobantes.html', 'Comprobantes', true],
    ['/portal/cita.html', 'Citas', true],
    ['/portal/encuesta.html', 'Encuesta', true],
    ['/portal/ayuda.html', 'Ayuda', false],
    ['/portal/soporte.html', 'Soporte', false]
  ];

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function iniciales(nombre) {
    var partes = String(nombre || '').trim().split(/\s+/).filter(Boolean);
    if (!partes.length) return 'AG';
    return (partes[0][0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
  }

  function montar() {
    if (document.querySelector('.pu-bar')) return;

    var logueado = typeof portalToken === 'function' && !!portalToken();
    var nombre = typeof portalNombre === 'function' ? portalNombre() : '';
    var aqui = location.pathname.replace(/\/+$/, '') || '/';

    var wrap = document.querySelector('.ag-wrap');
    if (wrap && !wrap.id) wrap.id = 'contenido';

    var skip = document.createElement('a');
    skip.className = 'pu-skip';
    skip.href = '#contenido';
    skip.textContent = 'Saltar al contenido';

    var bar = document.createElement('header');
    bar.className = 'pu-bar';

    var nav = LINKS.filter(function (l) { return logueado || !l[2]; }).map(function (l) {
      var actual = aqui === l[0].replace(/\.html$/, '') || aqui === l[0];
      return '<a href="' + l[0] + '"' + (actual ? ' aria-current="page"' : '') + '>' + esc(l[1]) + '</a>';
    }).join('');

    bar.innerHTML =
      '<a class="pu-brand" href="' + (logueado ? '/portal/estado.html' : '/') + '" aria-label="AutoGestión, inicio">' +
        '<i aria-hidden="true">AG</i><span>AutoGestión</span>' +
      '</a>' +
      '<nav class="pu-nav" aria-label="Navegación del portal">' + nav + '</nav>' +
      (logueado
        ? '<details class="pu-user">' +
            '<summary aria-label="Menú de cuenta">' +
              '<span class="pu-avatar" aria-hidden="true">' + esc(iniciales(nombre)) + '</span>' +
              '<span class="pu-user-name">' + esc(nombre || 'Mi cuenta') + '</span>' +
            '</summary>' +
            '<div class="pu-menu">' +
              '<div class="pu-menu-head"><b>' + esc(nombre || 'Mi cuenta') + '</b><span>Sesión de cliente</span></div>' +
              '<a href="/portal/estado.html#perfil">Mis datos</a>' +
              '<a href="/portal/ayuda.html">Guía de uso</a>' +
              '<a href="/portal/soporte.html">Soporte</a>' +
              '<button type="button" class="pu-menu-danger" id="puSalirBtn">Cerrar sesión</button>' +
            '</div>' +
          '</details>'
        : '<a class="pu-out" href="/">Ingresar</a>');

    document.body.insertBefore(skip, document.body.firstChild);
    document.body.insertBefore(bar, skip.nextSibling);

    var details = bar.querySelector('.pu-user');
    if (details) {
      document.addEventListener('click', function (e) {
        if (!details.contains(e.target)) details.removeAttribute('open');
      });
      document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') details.removeAttribute('open');
      });
    }

    var salir = bar.querySelector('#puSalirBtn');
    if (salir) salir.addEventListener('click', function () {
      if (typeof portalSalir !== 'function') return;
      abrirDialogo();
    });

    var activo = bar.querySelector('.pu-nav [aria-current]');
    if (activo && activo.scrollIntoView) activo.scrollIntoView({ inline: 'center', block: 'nearest' });
  }

  function abrirDialogo() {
    var dlg = document.getElementById('puSalirDlg');
    if (!dlg) {
      dlg = document.createElement('dialog');
      dlg.id = 'puSalirDlg';
      dlg.className = 'pu-dialog';
      dlg.innerHTML =
        '<form method="dialog" class="dlg-body">' +
          '<h3>¿Cerrar sesión?</h3>' +
          '<p>Tu sesión se borra de este navegador. Para volver a entrar necesitarás tu documento y placa, o un código nuevo.</p>' +
        '</form>' +
        '<div class="dlg-foot">' +
          '<button type="button" class="pu-btn pu-btn-secondary" id="puCancelarSalir">Quedarme</button>' +
          '<button type="button" class="pu-btn" id="puConfirmarSalir" style="background:var(--pu-err);color:#2a0a10 !important;">Cerrar sesión</button>' +
        '</div>';
      document.body.appendChild(dlg);
      dlg.querySelector('#puCancelarSalir').addEventListener('click', function () { dlg.close(); });
      dlg.querySelector('#puConfirmarSalir').addEventListener('click', function () {
        dlg.close();
        portalSalir();
      });
      dlg.addEventListener('click', function (e) { if (e.target === dlg) dlg.close(); });
    }
    if (typeof dlg.showModal === 'function') dlg.showModal();
    else if (typeof portalSalir === 'function') portalSalir();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', montar);
  else montar();
})();
