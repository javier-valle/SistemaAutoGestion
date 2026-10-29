
function exportarPDF() {
    const tabLabel = (document.querySelector('.ag-tab.active') || {}).textContent || 'reporte';
    const desde = document.getElementById('fDesde').value || '—';
    const hasta = document.getElementById('fHasta').value || '—';
    const w = window.open('', '_blank', 'width=1000,height=800');
    if (!w) { alert('Permite ventanas emergentes'); return; }
    w.document.write(`<html lang="es"><head><meta charset="UTF-8"><title>Reporte ${esc(tabLabel)}</title></head>
    <body style="font-family:Arial;color:#111;margin:24px;"><h1>AutoGestión · Reporte de ${esc(tabLabel)}</h1>
    <p>Rango ${esc(desde)} al ${esc(hasta)} · Zona América/Lima · Generado ${new Date().toLocaleString('es-PE')}</p>
    <script>window.onload=function(){window.print();}<\/script></body></html>`);
    w.document.close();
}
function exportarClientesPorDia() {
    apiDescargar(`/reportes/clientes-por-dia/export?${rangoQS()}&formato=csv`, 'clientes-por-dia.csv');
}
