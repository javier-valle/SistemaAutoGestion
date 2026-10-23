// frontend/js/reportes/tablas.js?v=1
function renderTabla(config) {
    const { columnas, filas, formato = 'text', id } = config || {};
    const safeId = id || 'tbl_' + Math.random().toString(36).slice(2, 8);
    const heads = (columnas || []).map(c => `<th>${esc(c)}</th>`).join('');
    const body = (filas || []).map(row => `<tr>${row.map(cell => {
        let val = cell != null ? cell : '—';
        if (formato === 'num') return `<td class="col-num">${formatInt(val)}</td>`;
        if (formato === 'currency') return `<td class="col-num">${formatCurrency(val)}</td>`;
        return `<td>${esc(String(val))}</td>`;
    }).join('')}</tr>`).join('');
    return `<div class="ag-table-wrap"><table class="ag-table" id="${safeId}"><thead><tr>${heads}</tr></thead><tbody>${body || '<tr><td colspan="9" class="ag-text-center ag-text-muted">Sin datos</td></tr>'}</tbody></table></div>`;
}
function renderKpi(icon, valor, etiqueta) {
    const iconMap = {
        'ic-blue':'#0ea5e9','ic-amber':'#f59e0b','ic-green':'#10b981','ic-red':'#ef4444'
    };
    return `<div class="ag-indicator"><div class="ag-indicator-icon ${icon}" style="color:${iconMap[icon]||iconMap['ic-blue']}">${valor}</div><div class="ag-indicator-label">${esc(etiqueta)}</div></div>`;
}
