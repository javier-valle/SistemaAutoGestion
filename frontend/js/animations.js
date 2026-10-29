(() => {
  'use strict';
  if (window.__agAnimationsInit) return;
  window.__agAnimationsInit = true;
  function initPageTransitions() {
    document.addEventListener('click', (e) => {
      const link = e.target.closest('a[href]');
      if (!link) return;
      const href = link.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('http') || href.startsWith('mailto:')) return;
      if (link.hasAttribute('download')) return;
      if (href.startsWith('blob:') || href.startsWith('data:') || href.startsWith('javascript:')) return;
      if (link.target === '_blank') return;
      e.preventDefault();
      const page = document.querySelector('.ag-page');
      if (page) {
        page.classList.add('ag-page-exit');
        page.addEventListener('animationend', () => {
          window.location.href = href;
        }, { once: true });
      } else {
        window.location.href = href;
      }
    });
    const page = document.querySelector('.ag-page');
    if (page) {
      page.classList.add('ag-page-enter');
      page.addEventListener('animationend', () => {
        page.classList.remove('ag-page-enter');
      }, { once: true });
    }
  }
  function initButtonRipple() {
    document.addEventListener('pointerdown', (e) => {
      const btn = e.target.closest('.ag-btn');
      if (!btn) return;
      const rect = btn.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width * 100).toFixed(1);
      const y = ((e.clientY - rect.top) / rect.height * 100).toFixed(1);
      btn.style.setProperty('--ripple-x', x + '%');
      btn.style.setProperty('--ripple-y', y + '%');
    });
  }
  window.agFlashRow = function(row) {
    if (!row) return;
    row.classList.remove('flash');
    void row.offsetWidth;
    row.classList.add('flash');
    row.addEventListener('animationend', () => {
      row.classList.remove('flash');
    }, { once: true });
  };
  window.agEmptyState = function(container, icon, title, desc) {
    if (!container) return;
    container.innerHTML = `
      <div class="ag-empty">
        <div class="ag-empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${icon || '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>'}</svg>
        </div>
        <p class="ag-empty-title">${title || 'Sin datos'}</p>
        <p class="ag-empty-desc">${desc || 'No hay elementos para mostrar en este momento.'}</p>
      </div>`;
  };
  window.agSkeleton = function(container, type) {
    if (!container) return;
    if (type === 'cards') {
      container.innerHTML = `
        <div class="ag-grid ag-grid-4">
          <div class="ag-skeleton ag-skeleton-card"></div>
          <div class="ag-skeleton ag-skeleton-card"></div>
          <div class="ag-skeleton ag-skeleton-card"></div>
          <div class="ag-skeleton ag-skeleton-card"></div>
        </div>`;
    } else if (type === 'table') {
      container.innerHTML = `
        <div style="padding:1rem;">
          <div class="ag-skeleton ag-skeleton-text w-75"></div>
          <div class="ag-skeleton ag-skeleton-text w-50"></div>
          <div class="ag-skeleton ag-skeleton-text w-75"></div>
          <div class="ag-skeleton ag-skeleton-text w-25"></div>
          <div class="ag-skeleton ag-skeleton-text w-50"></div>
        </div>`;
    } else {
      container.innerHTML = `
        <div style="padding:1rem;">
          <div class="ag-skeleton ag-skeleton-text w-75"></div>
          <div class="ag-skeleton ag-skeleton-text w-50"></div>
          <div class="ag-skeleton ag-skeleton-text w-25"></div>
        </div>`;
    }
  };
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initPageTransitions();
      initButtonRipple();
    });
  } else {
    initPageTransitions();
    initButtonRipple();
  }
})();
