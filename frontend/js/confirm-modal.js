(function() {
    'use strict';
    let modalOverlay = null;
    let modalContent = null;
    function createModal() {
        if (modalOverlay) return;
        modalOverlay = document.createElement('div');
        modalOverlay.className = 'ag-modal-overlay';
        modalOverlay.innerHTML = `
            <div class="ag-modal">
                <div class="ag-card-header">
                    <h3 id="confirmTitle">Confirmar</h3>
                    <button class="ag-btn ag-btn-outline ag-btn-sm" id="confirmCloseBtn">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="18" y1="6" x2="6" y2="18"/>
                            <line x1="6" y1="6" x2="18" y2="18"/>
                        </svg>
                    </button>
                </div>
                <div class="ag-card-body" style="padding: 1.25rem;">
                    <p id="confirmMessage" class="ag-text-muted" style="margin-bottom: 1.5rem;"></p>
                    <div class="ag-d-flex ag-gap-3 ag-justify-end">
                        <button class="ag-btn ag-btn-outline ag-btn-sm" id="confirmCancelBtn">Cancelar</button>
                        <button class="ag-btn ag-btn-primary ag-btn-sm" id="confirmActionBtn">Confirmar</button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalOverlay);
        modalContent = modalOverlay.querySelector('.ag-modal');
        modalOverlay.addEventListener('click', (e) => {
            if (e.target === modalOverlay) {
                closeModal(false);
            }
        });
        document.getElementById('confirmCloseBtn').addEventListener('click', () => closeModal(false));
        document.getElementById('confirmCancelBtn').addEventListener('click', () => closeModal(false));
        document.getElementById('confirmActionBtn').addEventListener('click', () => closeModal(true));
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modalOverlay.classList.contains('show')) {
                closeModal(false);
            }
        });
    }
    let resolvePromise = null;
    function closeModal(confirmed) {
        if (!modalOverlay) return;
        modalOverlay.classList.remove('show');
        if (resolvePromise) {
            resolvePromise(confirmed);
            resolvePromise = null;
        }
        setTimeout(() => {
            modalOverlay.classList.remove('show');
        }, 200);
    }
    window.confirmarAccion = function(mensaje, opciones = {}) {
        return new Promise((resolve) => {
            if (!modalOverlay) {
                createModal();
            }
            resolvePromise = resolve;
            const titulo = opciones.titulo || 'Confirmar acción';
            const textoConfirmar = opciones.textoConfirmar || opciones.okLabel || 'Confirmar';
            const textoCancelar = opciones.cancelLabel || 'Cancelar';
            const tipo = opciones.tipo || 'primary'; 
            document.getElementById('confirmTitle').textContent = titulo;
            document.getElementById('confirmMessage').textContent = mensaje;
            document.getElementById('confirmActionBtn').textContent = textoConfirmar;
            document.getElementById('confirmCancelBtn').textContent = textoCancelar;
            const actionBtn = document.getElementById('confirmActionBtn');
            actionBtn.className = 'ag-btn ag-btn-sm';
            if (tipo === 'danger') {
                actionBtn.classList.add('ag-btn-danger');
            } else {
                actionBtn.classList.add('ag-btn-primary');
            }
            modalOverlay.classList.add('show');
        });
    };
})();