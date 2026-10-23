// frontend/js/reportes/charts.js?v=1
function svgBars(el, labels, values, color) {
    const W = 560, H = 180, pad = 28;
    const max = Math.max(1, ...values);
    const n = values.length;
    const bw = n ? Math.min(46, (W - pad * 2) / n * 0.6) : 0;
    let s = `<svg class="ag-chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Gráfico de barras con ${n} categorías">`;
    for (let g = 0; g <= 3; g++) {
        const y = pad + (H - pad * 2) * g / 3;
        s += `<line x1="${pad}" y1="${y}" x2="${W - 8}" y2="${y}" stroke="var(--ag-border)" stroke-width="1"/>`;
    }
    values.forEach((v, i) => {
        const h = (H - pad * 2) * v / max;
        const x = pad + ((W - pad * 2) / n) * i + ((W - pad * 2) / n - bw) / 2;
        const y = H - pad - h;
        s += `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${bw.toFixed(1)}" height="${h.toFixed(1)}" rx="4" fill="${color}"><title>${esc(String(labels[i] || '').slice(0, 30))}: ${v}</title></rect>`;
        if (n <= 14) s += `<text x="${(x + bw / 2).toFixed(1)}" y="${(H - 10).toFixed(1)}" text-anchor="middle" font-size="7">${esc(String(labels[i] || '').slice(0, 14))}</text>`;
    });
    s += '</svg>';
    document.getElementById(el).innerHTML = s;
}
