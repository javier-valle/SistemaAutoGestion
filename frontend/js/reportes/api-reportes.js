
function rangoQS() {
    const d = document.getElementById('fDesde').value;
    const h = document.getElementById('fHasta').value;
    return `desde=${d}&hasta=${h}`;
}
function esc(s) {
    return String(s ?? '').replace(/[&<>"'`=]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;','`':'&#96;','=':'&#61;'}[c]));
}
function formatCurrency(amount) {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(amount);
}
function formatInt(n) {
    return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 0 }).format(n);
}
