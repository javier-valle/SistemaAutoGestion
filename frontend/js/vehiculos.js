














(function () {
  'use strict';
  if (window.__agVehiculos) return;
  window.__agVehiculos = true;

  const SW = 'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"';




  function hexRGB(h) {
    h = (h || '#888888').replace('#', '');
    if (h.length === 3) h = h.split('').map(c => c + c).join('');
    const n = parseInt(h.slice(0, 6), 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
  }
  function rgbHex(r, g, b) {
    const c = v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0');
    return '#' + c(r) + c(g) + c(b);
  }

  function mezcla(hex, t) {
    const [r, g, b] = hexRGB(hex);
    const dst = t >= 0 ? 255 : 0, k = Math.abs(t);
    return rgbHex(r + (dst - r) * k, g + (dst - g) * k, b + (dst - b) * k);
  }
  const claro = (h, t) => mezcla(h, t);
  const oscuro = (h, t) => mezcla(h, -t);




  const EMBLEMAS = {
    toyota: `<ellipse cx="12" cy="12" rx="8.5" ry="6.2"/><ellipse cx="12" cy="12" rx="2.7" ry="6.2"/><path d="M6.3 10.4h11.4"/>`,
    hyundai: `<ellipse cx="12" cy="12" rx="8.5" ry="6.2"/><path d="M9.6 7.3L8.1 16.7M14.4 7.3L15.9 16.7M8.7 11.8h6.6"/>`,
    kia: `<ellipse cx="12" cy="12" rx="8.5" ry="6.2"/><text x="12" y="14.6" text-anchor="middle" font-size="7" font-weight="800" font-family="inherit" fill="currentColor" stroke="none">KIA</text>`,
    nissan: `<circle cx="12" cy="12" r="8"/><rect x="4.6" y="10.1" width="14.8" height="3.8" rx="1"/>`,
    honda: `<path d="M8.2 5.5v13M15.8 5.5v13M8.2 12h7.6M6 5.5h12M6 18.5h12"/>`,
    chevrolet: `<path d="M10 4.5h4v5h5.5v5H14v5h-4v-5H4.5v-5H10z"/>`,
    volkswagen: `<circle cx="12" cy="12" r="8"/><path d="M7.6 8.6L12 15l4.4-6.4"/><path d="M8.7 16.4h6.6"/>`,
    suzuki: `<path d="M16.6 8.2c-1.2-1.7-3.3-2.3-5.1-1.6-2.1.8-2.1 3 .4 3.6l2 .4c2.7.6 2.7 3.1-.3 3.7-1.7.4-3.7-.2-4.8-1.6"/>`,
    mazda: `<circle cx="12" cy="12" r="8"/><path d="M5.4 14.6c2.2 0 3.7-1.2 4.8-3 .9 1.4 1.3 2.1 1.8 2.6.5-.5.9-1.2 1.8-2.6 1.1 1.8 2.6 3 4.8 3"/>`,
    ford: `<ellipse cx="12" cy="12" rx="9" ry="6"/><text x="12" y="14.2" text-anchor="middle" font-size="5.6" font-style="italic" font-weight="700" font-family="inherit" fill="currentColor" stroke="none">Ford</text>`,
    mitsubishi: `<path d="M12 3.5L14.6 8 12 12.5 9.4 8Z"/><path d="M7.4 11L10 15.5 7.4 20 4.8 15.5Z"/><path d="M16.6 11L19.2 15.5 16.6 20 14 15.5Z"/>`,
    generico: `<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="2.4"/><path d="M12 9.6V4"/><path d="M9.9 13.2L4.9 16.1"/><path d="M14.1 13.2L19.1 16.1"/>`
  };



  const MARCAS = [
    { id: 'toyota', nombre: 'Toyota', color: '#cf3a2c', carro: 'pickup' },
    { id: 'hyundai', nombre: 'Hyundai', color: '#2b5f8a', carro: 'sedan' },
    { id: 'kia', nombre: 'Kia', color: '#a93246', carro: 'hatch' },
    { id: 'nissan', nombre: 'Nissan', color: '#75809a', carro: 'sedan' },
    { id: 'honda', nombre: 'Honda', color: '#1f5fa8', carro: 'sedan' },
    { id: 'chevrolet', nombre: 'Chevrolet', color: '#b3872c', carro: 'suv' },
    { id: 'volkswagen', nombre: 'Volkswagen', color: '#2f5da3', carro: 'hatch' },
    { id: 'suzuki', nombre: 'Suzuki', color: '#0e7c86', carro: 'hatch' },
    { id: 'mazda', nombre: 'Mazda', color: '#8c1f28', carro: 'sedan' },
    { id: 'ford', nombre: 'Ford', color: '#274b73', carro: 'pickup' },
    { id: 'mitsubishi', nombre: 'Mitsubishi', color: '#a31f34', carro: 'suv' }
  ];
  const ALIAS = { vw: 'volkswagen', chevy: 'chevrolet', mercedes: null, bmw: null };

  function normalizarMarca(nombre) {
    const n = (nombre || '').trim().toLowerCase();
    if (!n) return null;
    if (ALIAS[n] !== undefined) return ALIAS[n];
    const m = MARCAS.find(x => x.nombre.toLowerCase() === n || x.id === n);
    return m ? m.id : null;
  }
  function marcaDe(nombre) {
    const id = normalizarMarca(nombre);
    return MARCAS.find(x => x.id === id) || null;
  }


  function colorPorPlaca(placa) {
    const s = (placa || 'ABC-000').toUpperCase();
    let h = 0;
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 360;
    return `hsl(${h}, 52%, 44%)`;
  }
  function colorVehiculo(marca, placa) {
    const m = marcaDe(marca);
    return m ? m.color : colorPorPlaca(placa);
  }

  function emblemaSVG(marca, cls) {
    const m = marcaDe(marca);
    const inner = (m && EMBLEMAS[m.id]) || EMBLEMAS.generico;
    return `<svg class="${cls || 'veh-emblem'}" viewBox="0 0 24 24" fill="none" stroke="currentColor" ${SW} aria-hidden="true">${inner}</svg>`;
  }
  window.vehEmblema = emblemaSVG;





  const TIPOS = ['pickup', 'suv', 'van', 'hatch', 'sedan'];
  const TABLA_MODELOS = [
    { t: 'pickup', k: ['hilux', 'ranger', 'navara', 'np300', 'np3000', 'frontier', 'l200', 'l 200', 'dmax', 'd max', 'd-max', 'amarok', 's10', 'luv', 'k2700', 'dynasty', 'bighorn', 'tacoma', 'maverick', 'hilux sw4', 'revo', 'tulo', 'minicargo'] },
    { t: 'suv', k: ['tucson', 'sportage', 'tracker', 'hrv', 'hr-v', 'cx5', 'cx 5', 'cx3', 'cx 3', 'cx9', 'cx 9', 'asx', 'outlander', 'ecosport', 'vitara', 'seltos', 'kicks', 'duster', 'trax', 'equinox', 'corolla cross', 'corolla cross', 'rav4', 'rav 4', 'prado', 'landcruiser', 'land cruiser', 'everest', 'murasena', 'murasena', 'santa fe', 'santafe', 'tucson', 'wrangler', 'compass', 'journey', 'captiva', 'duet', 'terios', 'rush', 'beetle', 'trailblazer', 'montero', 'pajero', 'yeti', 'karoq', 'kompass', 'nexon', 'c3 aircross', '2008', 'q5', 'x1', 'glc', 'leveret'] },
    { t: 'van', k: ['hiace', 'hi ace', 'townace', 'town ace', 'urvan', 'sprinter', 'trajet', 'staria', 'h1', 'h 1', 'caravan', 'dell', 'transit', 'master', 'vito', 'montana', 'dmax van', 'minibus', 'coaster', 'urban'] },
    { t: 'hatch', k: ['yaris', 'spark', 'picanto', 'i10', 'i 10', 'i20', 'i 20', 'swift', 'gol', 'golfox', 'polo', 'mazda2', 'mazda 2', 'fiesta', 'go ', ' morning', 'insight', 'fit', 'jazz', 'up!', 'up ', 'corsa', 'c3', '208', 'i30', 'i 30', 'aveo hatch', 'onix', 'hb20', 'beleno', 'seed', 'ceed', 'rio', 'march', 'micra', 'c4', 'clio', 'figo', 'fusion hatch', 'agile', 'monza', 'virtus', 'golf', 'fox', 'pointer'] },
    { t: 'sedan', k: ['corolla', 'accent', 'sentra', 'civic', 'city', 'versa', 'joy', 'mazda3', 'mazda 3', 'focus', 'lancer', 'virtus', 'optra', 'ep3', 'logan', 'sail', 'newer', 'tiida', 'almera', 'sunny', 'plantinum', 'platinum', 'camry', 'esquire', 'premio', 'allion', 'premio', 'cerato', 'k3', 'elantra', 'avante', 'wrangler sedan', 'a4', 'serie 3', 'civic ferio', 'cobalt', 'malibu', 'model s', 'wagon r'] }
  ];
  function sinAcento(s) {
    return (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();
  }

  function estiloDe(marca, modelo) {
    const m = sinAcento(modelo);
    if (m) {
      for (const fila of TABLA_MODELOS) {
        for (const k of fila.k) if (m.includes(sinAcento(k))) return fila.t;
      }
    }
    const mk = marcaDe(marca);
    return (mk && mk.carro) || 'sedan';
  }






  function muestrear(segs, paso) {
    paso = paso || 2.2;
    const pts = [];
    let cx = 0, cy = 0;
    for (const s of segs) {
      const t = s[0];
      if (t === 'M') { cx = s[1]; cy = s[2]; pts.push([cx, cy]); }
      else if (t === 'L') {
        const n = Math.max(1, Math.ceil(Math.hypot(s[1] - cx, s[2] - cy) / paso));
        for (let i = 1; i <= n; i++) pts.push([cx + (s[1] - cx) * i / n, cy + (s[2] - cy) * i / n]);
        cx = s[1]; cy = s[2];
      } else if (t === 'C') {
        const x1 = s[1], y1 = s[2], x2 = s[3], y2 = s[4], x3 = s[5], y3 = s[6];
        const n = Math.max(6, Math.ceil((Math.hypot(x1 - cx, y1 - cy) + Math.hypot(x2 - x1, y2 - y1) + Math.hypot(x3 - x2, y3 - y2)) / paso));
        for (let i = 1; i <= n; i++) {
          const u = i / n, v = 1 - u;
          pts.push([
            v * v * v * cx + 3 * v * v * u * x1 + 3 * v * u * u * x2 + u * u * u * x3,
            v * v * v * cy + 3 * v * v * u * y1 + 3 * v * u * u * y2 + u * u * u * y3
          ]);
        }
        cx = x3; cy = y3;
      } else if (t === 'AR') {
        const ax = s[1], ay = s[2], r = s[3], yB = s[4];
        const al = Math.asin(Math.max(-1, Math.min(1, (yB - ay) / r)));
        const a0 = al, a1 = -(Math.PI + al);
        const n = Math.max(10, Math.ceil(Math.abs(a1 - a0) * r / paso));
        for (let i = 1; i <= n; i++) {
          const a = a0 + (a1 - a0) * i / n;
          pts.push([ax + r * Math.cos(a), ay + r * Math.sin(a)]);
        }
        cx = ax + r * Math.cos(a1); cy = ay + r * Math.sin(a1);
      }
    }
    return pts;
  }
  function dPath(pts) {
    return 'M' + pts.map(p => p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join('L') + 'Z';
  }
  function dLine(pts) {
    return 'M' + pts.map(p => p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join('L');
  }




  let UID = 0;
  const TINTA = 'rgba(15,23,42,.55)';
  const LINEA = 'rgba(10,18,32,.42)';

  function defs(g, color, brazos) {
    const c = color;
    return `<defs>
      <linearGradient id="${g}p" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="${claro(c, .55)}"/>
        <stop offset=".16" stop-color="${claro(c, .22)}"/>
        <stop offset=".44" stop-color="${c}"/>
        <stop offset=".60" stop-color="${claro(c, .18)}"/>
        <stop offset=".74" stop-color="${oscuro(c, .18)}"/>
        <stop offset="1" stop-color="${oscuro(c, .48)}"/>
      </linearGradient>
      <linearGradient id="${g}v" x1="0" y1="0" x2=".35" y2="1">
        <stop offset="0" stop-color="#e9f2fc"/>
        <stop offset=".38" stop-color="#b3c8de"/>
        <stop offset=".68" stop-color="#6d87a3"/>
        <stop offset="1" stop-color="#3d5270"/>
      </linearGradient>
      <linearGradient id="${g}m" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#f4f8fc"/>
        <stop offset=".38" stop-color="#c9d4e0"/>
        <stop offset=".62" stop-color="#8b9aab"/>
        <stop offset="1" stop-color="#dfe7ef"/>
      </linearGradient>
      <radialGradient id="${g}t" cx=".38" cy=".34" r=".78">
        <stop offset="0" stop-color="#39414f"/>
        <stop offset=".62" stop-color="#1a212c"/>
        <stop offset="1" stop-color="#080b11"/>
      </radialGradient>
      <radialGradient id="${g}s" cx=".5" cy=".5" r=".5">
        <stop offset="0" stop-color="rgba(8,12,20,.42)"/>
        <stop offset=".5" stop-color="rgba(8,12,20,.18)"/>
        <stop offset="1" stop-color="rgba(8,12,20,0)"/>
      </radialGradient>
      <radialGradient id="${g}d" cx=".5" cy=".5" r=".5">
        <stop offset="0" stop-color="#7d8896"/>
        <stop offset="1" stop-color="#4d5765"/>
      </radialGradient>
      ${brazos > 6 ? `<linearGradient id="${g}b" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="${claro(c, .35)}"/><stop offset="1" stop-color="${oscuro(c, .35)}"/>
      </linearGradient>` : ''}
    </defs>`;
  }


  function rueda(cx, cy, r, brazos, g) {
    const rim = r * 0.63, disco = r * 0.47, buje = r * 0.15;
    let radios = '';
    const paso = 360 / brazos;
    for (let i = 0; i < brazos; i++) {
      const a = i * paso;
      const inner = r * 0.17, outer = r * 0.6;
      const w = 7.5, wm = 3.4;
      const rad = d => d * Math.PI / 180;
      const p = (ang, rr) => [(cx + rr * Math.cos(rad(ang))).toFixed(1), (cy + rr * Math.sin(rad(ang))).toFixed(1)];
      const a1 = p(a - w, outer), a2 = p(a + w, outer), m1 = p(a - wm, (inner + outer) / 2), m2 = p(a + wm, (inner + outer) / 2), i1 = p(a - 9, inner), i2 = p(a + 9, inner);
      radios += `<path d="M${a1} L${m1} L${i1} L${i2} L${m2} L${a2} Z"/>`;
    }
    let bujes = '';
    for (let i = 0; i < 5; i++) {
      const a = (i * 72 - 90) * Math.PI / 180;
      bujes += `<circle cx="${(cx + (buje + 2.6) * Math.cos(a)).toFixed(1)}" cy="${(cy + (buje + 2.6) * Math.sin(a)).toFixed(1)}" r="1.5" fill="#2b3542"/>`;
    }
    return `<g>
      <circle cx="${cx}" cy="${cy}" r="${r}" fill="url(#${g}t)" stroke="#05070b" stroke-width="1.4"/>
      <circle cx="${cx}" cy="${cy}" r="${r - 1.6}" fill="none" stroke="rgba(255,255,255,.10)" stroke-width="1.2" stroke-dasharray="4 5"/>
      <path d="M${cx - r * .72} ${cy - r * .55} A ${r * .9} ${r * .9} 0 0 1 ${cx + r * .3} ${cy - r * .84}" fill="none" stroke="rgba(255,255,255,.16)" stroke-width="2" stroke-linecap="round"/>
      <circle cx="${cx}" cy="${cy}" r="${rim}" fill="#141a24" stroke="#0a0e15" stroke-width="1"/>
      <circle cx="${cx}" cy="${cy}" r="${disco}" fill="url(#${g}d)"/>
      <circle cx="${cx}" cy="${cy}" r="${disco * .74}" fill="none" stroke="rgba(20,26,36,.5)" stroke-width="1.2" stroke-dasharray="2.4 4.6"/>
      <path d="M${(cx - disco * .96).toFixed(1)} ${(cy - disco * .2).toFixed(1)} A ${disco} ${disco} 0 0 1 ${(cx - disco * .3).toFixed(1)} ${(cy - disco * .95).toFixed(1)}" fill="none" stroke="#9c4540" stroke-width="5" stroke-linecap="round" opacity=".85"/>
      <g fill="url(#${g}m)" stroke="rgba(10,18,32,.55)" stroke-width="1">${radios}</g>
      <circle cx="${cx}" cy="${cy}" r="${rim}" fill="none" stroke="url(#${g}m)" stroke-width="${(r * .1).toFixed(1)}"/>
      <circle cx="${cx}" cy="${cy}" r="${buje + 1.4}" fill="url(#${g}m)" stroke="rgba(10,18,32,.5)" stroke-width="1"/>
      ${bujes}
    </g>`;
  }


  function cristal(pts, g) {
    const xs = pts.map(p => p[0]), ys = pts.map(p => p[1]);
    const x0 = Math.min(...xs), x1 = Math.max(...xs), y0 = Math.min(...ys), y1 = Math.max(...ys);
    const w = x1 - x0, h = y1 - y0;
    const brillo = [
      [x0 + w * .10, y1], [x0 + w * .34, y1], [x0 + w * .52, y0], [x0 + w * .30, y0]
    ];
    return `<path d="${dPath(pts)}" fill="url(#${g}v)" stroke="rgba(8,14,24,.65)" stroke-width="1.3" stroke-linejoin="round"/>
      <path d="${dPath(brillo)}" fill="#ffffff" opacity=".22"/>
      <path d="${dLine(pts)}Z" fill="none" stroke="rgba(255,255,255,.5)" stroke-width="1"/>`;
  }


  function faro(pts, g, trasero) {
    const xs = pts.map(p => p[0]), ys = pts.map(p => p[1]);
    const cx = (Math.min(...xs) + Math.max(...xs)) / 2, cy = (Math.min(...ys) + Math.max(...ys)) / 2;
    const r = Math.min(Math.max(...xs) - Math.min(...xs), Math.max(...ys) - Math.min(...ys)) * .22;
    return `<path d="${dPath(pts)}" fill="${trasero ? '#c8352f' : `url(#${g}m)`}" stroke="rgba(8,14,24,.6)" stroke-width="1.2" stroke-linejoin="round"/>
      <circle cx="${cx.toFixed(1)}" cy="${cy.toFixed(1)}" r="${r.toFixed(1)}" fill="${trasero ? '#f4a09a' : '#fde68a'}" opacity=".9"/>
      <path d="${dLine(pts)}Z" fill="none" stroke="rgba(255,255,255,.75)" stroke-width="1.2" opacity=".8"/>
      <path d="${dPath(pts)}" fill="none" stroke="${trasero ? '#7f1d1d' : '#7c8ba0'}" stroke-width="1"/>`;
  }


  function manija(x, y, largo) {
    return `<rect x="${x}" y="${y}" width="${largo}" height="3" rx="1.5" fill="url(#GIDm)" stroke="rgba(10,18,32,.5)" stroke-width=".9"/>`;
  }


  function espejo(x, y, grande) {
    const s = grande ? 1.25 : 1;
    return `<path d="M${x} ${y} l${(7 * s).toFixed(1)} ${(-3 * s).toFixed(1)}" stroke="${oscuro('#223149', .2)}" stroke-width="${(2.6 * s).toFixed(1)}" stroke-linecap="round"/>
      <path d="M${x + 5 * s} ${y - 6 * s} l${(9 * s).toFixed(1)} ${(-3 * s).toFixed(1)} q${(4 * s).toFixed(1)} ${(-1 * s).toFixed(1)} ${(4 * s).toFixed(1)} ${(3 * s).toFixed(1)} l0 ${(4 * s).toFixed(1)} q0 ${(3 * s).toFixed(1)} ${(-4 * s).toFixed(1)} ${(2.6 * s).toFixed(1)} l${(-9 * s).toFixed(1)} ${(-1 * s).toFixed(1)} z"
        fill="#1f2937" stroke="rgba(8,14,24,.7)" stroke-width="1.1" stroke-linejoin="round"/>
      <path d="M${x + 7 * s} ${y - 5.6 * s} l${(7 * s).toFixed(1)} ${(-2.4 * s).toFixed(1)}" stroke="#fbbf24" stroke-width="1.4" stroke-linecap="round" opacity=".9"/>`;
  }


  function sombraArco(cx, cy, r) {
    return `<path d="M${(cx - r).toFixed(1)} ${(cy + 6).toFixed(1)} A ${r} ${r} 0 0 1 ${(cx + r).toFixed(1)} ${(cy + 6).toFixed(1)}"
      fill="none" stroke="rgba(6,10,18,.55)" stroke-width="3.4" stroke-linecap="round"/>`;
  }





  const SPECS = {

    sedan: {
      suelo: 89, ruedaR: 15, ejeT: 63, ejeF: 185, yB: 82.6, archR: 17.5, ancho: 83,
      contorno: [
        ['M', 18, 82.6],
        ['C', 14.5, 78, 14, 70, 15.5, 62],
        ['C', 16.5, 56, 17.5, 51.5, 21, 48.8],
        ['C', 32, 47.6, 46, 46.8, 60, 46.6],
        ['C', 65, 42, 73, 27.5, 82, 24.4],
        ['C', 100, 23.4, 122, 22.8, 149, 23.4],
        ['C', 156, 24, 161, 36, 166, 52],
        ['C', 190, 52.2, 212, 53.4, 226, 55.4],
        ['C', 230.5, 56.6, 233, 61, 232, 68],
        ['C', 231.5, 75, 231, 79.5, 228.5, 82.6],
        ['L', 200.25, 82.6],
        ['AR', 185, 74, 17.5, 82.6],
        ['L', 78.25, 82.6],
        ['AR', 63, 74, 17.5, 82.6],
        ['L', 18, 82.6]
      ],
      parabrisas: [['M', 149, 23.4], ['C', 156, 24, 161, 36, 166, 52]],
      trasera: [['M', 60, 46.6], ['C', 65, 42, 73, 27.5, 82, 24.4]],
      vidrios: [
        [[158, 47.6], [120, 46.8], [120, 27.7], [146, 27.4]],
        [[115, 46.5], [67, 45.6], [83, 28.4], [115, 28.1]]
      ],
      puertas: [[[160, 48.4], [157.5, 82.6]], [[120, 47], [118, 82.6]], [[82, 45.8], [81, 82.6]]],
      manijas: [[127, 53.8], [88, 53.4]],
      espejo: [157, 46.6, false],
      faroF: [[214, 57], [224, 58.4], [231.4, 63], [214.5, 62.2]],
      faroT: [[19.5, 54], [23, 50.6], [33, 49], [33, 53.4], [21, 57.4]],
      parrilla: [[217, 65], [231.4, 68], [231.4, 73.4], [217.5, 71]],
      cintura: [[159, 47.8], [120, 47], [115, 46.7], [67, 45.8]],
      combustible: [46, 57, 4.4],
      extras(g, c) {
        return linea([[214, 59.5], [150, 57.4], [70, 56.6], [26, 58.8]], 'rgba(255,255,255,.35)', 1.4)
          + linea([[215, 61.6], [150, 59.6], [70, 58.8], [27, 61]], 'rgba(10,18,32,.18)', 1.6)
          + linea([[168, 55], [200, 56], [222, 58]], 'rgba(10,18,32,.3)', 1.1)
          + linea([[24, 52.4], [58, 49.6]], 'rgba(10,18,32,.28)', 1.1)
          + `<circle cx="46" cy="57" r="4.4" fill="none" stroke="${LINEA}" stroke-width="1.2"/>`
          + linea([[24, 80.6], [40, 80.6]], 'rgba(10,18,32,.5)', 2)
          + `<path d="M112 21.6 l7 -1.4 q3 -.4 3 2 l0 1 z" fill="#1f2937" opacity=".8"/>`
          + linea([[206, 81.4], [228, 81.4]], 'rgba(10,18,32,.35)', 1.4);
      }
    },


    hatch: {
      suelo: 89, ruedaR: 15, ejeT: 58, ejeF: 176, yB: 82.6, archR: 17.5, ancho: 80,
      contorno: [
        ['M', 26, 82.6],
        ['C', 23, 79, 22.5, 74, 24, 69],
        ['C', 27, 64, 32, 60, 38, 56],
        ['C', 40, 52, 42, 48, 44, 44],
        ['C', 50, 38, 57, 31, 63, 27],
        ['C', 65, 26, 67, 25.2, 70, 24.8],
        ['C', 84, 23.8, 106, 23.4, 136, 24.4],
        ['C', 142, 25, 150, 38, 156, 52.2],
        ['C', 176, 53.4, 196, 54.4, 205, 55.6],
        ['C', 210, 57, 214, 60.5, 214.5, 66],
        ['C', 215, 73, 214, 79, 211.5, 82.6],
        ['L', 191.25, 82.6],
        ['AR', 176, 74, 17.5, 82.6],
        ['L', 73.25, 82.6],
        ['AR', 58, 74, 17.5, 82.6],
        ['L', 26, 82.6]
      ],
      parabrisas: [['M', 136, 24.4], ['C', 142, 25, 150, 38, 156, 52.2]],
      trasera: [['M', 44, 44], ['C', 50, 38, 57, 31, 63, 27], ['L', 70, 24.8]],
      vidrios: [
        [[148, 47.8], [110, 47], [110, 27.9], [134, 27.5]],
        [[105, 46.9], [70, 46.4], [82, 28.4], [110, 28.3]],
        [[65, 46.2], [58, 46], [73, 28.6], [78, 28.5]]
      ],
      puertas: [[[149, 48.4], [146, 82.6]], [[110, 47.2], [107, 82.6]], [[67, 46.4], [66, 58]]],
      manijas: [[116, 54], [76, 53.8]],
      espejo: [148, 47.4, false],
      faroF: [[204, 55], [213.6, 58.4], [214.6, 64], [204.5, 61]],
      faroT: [[28, 56], [33, 50], [44, 45], [46, 51], [32, 60]],
      parrilla: [[208, 65.5], [215, 68], [214.8, 73.5], [208.5, 71.5]],
      cintura: [[149, 48], [110, 47.2], [105, 47.1], [70, 46.6], [58, 46.2]],
      combustible: [46, 57.5, 4.2],
      extras(g, c) {
        return linea([[210, 59.6], [150, 58], [80, 57.4], [44, 58.4]], 'rgba(255,255,255,.32)', 1.4)
          + linea([[211, 61.6], [150, 60], [80, 59.4], [45, 60.4]], 'rgba(10,18,32,.18)', 1.6)
          + `<path d="M47 43 L59 31" stroke="rgba(10,18,32,.55)" stroke-width="1.8" stroke-linecap="round"/>`
          + `<path d="M60 29.6 L70 25.4" stroke="${claro(c, .25)}" stroke-width="3.4" stroke-linecap="round" opacity=".85"/>`
          + `<circle cx="46" cy="57.5" r="4.2" fill="none" stroke="${LINEA}" stroke-width="1.2"/>`
          + linea([[26, 80.8], [40, 80.8]], 'rgba(10,18,32,.5)', 2)
          + linea([[200, 81.4], [212, 81.4]], 'rgba(10,18,32,.35)', 1.4);
      }
    },


    suv: {
      suelo: 93, ruedaR: 16, ejeT: 59, ejeF: 179, yB: 82.5, archR: 18.5, ancho: 83, alto: true,
      contorno: [
        ['M', 20, 82.5],
        ['C', 17, 79, 16.5, 73.5, 17, 67],
        ['C', 17.5, 59, 19, 52, 22, 47],
        ['C', 26, 46.4, 31, 45.8, 37, 45],
        ['C', 44, 38, 51, 30, 58, 22.4],
        ['C', 60, 21.4, 63, 20.4, 66, 19.8],
        ['C', 69, 19.2, 72, 19, 76, 19],
        ['C', 94, 18.5, 118, 18.4, 140, 19],
        ['C', 146, 19.6, 154, 36, 160, 53.5],
        ['C', 180, 54.4, 198, 55, 207, 56.6],
        ['C', 212, 57.8, 215, 60.6, 215.4, 66],
        ['C', 215.8, 73, 215, 79, 212.5, 82.5],
        ['L', 196.66, 82.5],
        ['AR', 179, 77, 18.5, 82.5],
        ['L', 76.66, 82.5],
        ['AR', 59, 77, 18.5, 82.5],
        ['L', 20, 82.5]
      ],
      parabrisas: [['M', 140, 19], ['C', 146, 19.6, 154, 36, 160, 53.5]],
      trasera: [['M', 37, 45], ['C', 44, 38, 51, 30, 58, 22.4], ['L', 66, 19.8]],
      vidrios: [
        [[152, 47.4], [112, 46.6], [112, 26.4], [139, 25.8]],
        [[106, 46.4], [76, 46], [88, 27.4], [112, 26.6]],
        [[71, 45.9], [55, 45.8], [72, 27], [76, 26.9]]
      ],
      puertas: [[[153, 48.4], [150, 82.5]], [[112, 47], [109, 82.5]], [[79, 46], [78, 82.5]]],
      manijas: [[118, 54], [84, 53.6]],
      espejo: [152, 47.8, false],
      faroF: [[204, 54.6], [214.6, 58.4], [215.4, 64.4], [204.5, 61.4]],
      faroT: [[20, 52], [25, 47], [37, 44.6], [40, 50], [24, 57]],
      parrilla: [[209, 64.6], [215.6, 67.4], [215.4, 73], [209.5, 70.8]],
      cintura: [[153, 47.6], [112, 46.8], [106, 46.6], [76, 46.2], [55, 46]],
      combustible: [44, 57, 4.4],
      extras(g, c) {
        return `
          ${arcoGuardabarros(59, 77, 18.5, 82.5)}${arcoGuardabarros(179, 77, 18.5, 82.5)}
          ${linea([[80, 17.2], [138, 16.6]], '#27303e', 3)}
          ${linea([[84, 17.2], [84, 19.4]], '#27303e', 3)}${linea([[134, 16.6], [134, 19]], '#27303e', 3)}
          ${linea([[82, 79.4], [150, 79.4]], 'rgba(10,18,32,.55)', 4)}
          ${linea([[210, 60.4], [150, 58.8], [70, 58], [26, 60]], 'rgba(255,255,255,.3)', 1.4)}
          ${linea([[211, 62.4], [150, 60.8], [70, 60], [27, 62]], 'rgba(10,18,32,.18)', 1.6)}
          <circle cx="44" cy="57" r="4.4" fill="none" stroke="${LINEA}" stroke-width="1.2"/>
          ${linea([[196, 81], [212, 81]], 'rgba(10,18,32,.4)', 2)}
          ${linea([[26, 76.6], [46, 76.6]], 'rgba(10,18,32,.45)', 2)}`;
      }
    },


    pickup: {
      suelo: 92, ruedaR: 16, ejeT: 62, ejeF: 186, yB: 83.6, archR: 19, ancho: 75,
      contorno: [
        ['M', 14, 76],
        ['C', 11.5, 73, 11, 68, 12, 62],
        ['C', 13, 57, 14, 54, 16, 52.5],
        ['L', 90, 51.5],
        ['C', 92, 50, 93, 47.5, 93.5, 44],
        ['C', 95, 36, 96, 26, 96.5, 20.5],
        ['C', 110, 19.4, 132, 19.2, 150, 19.8],
        ['C', 158, 20.6, 166, 36, 172, 54],
        ['C', 188, 54.6, 204, 55.4, 212, 56.8],
        ['C', 219, 57.8, 223, 60, 225, 64],
        ['C', 226.5, 70, 226, 77, 224, 80],
        ['C', 222.5, 82.5, 221, 83.6, 218, 83.6],
        ['L', 203.4, 83.6],
        ['AR', 186, 76, 19, 83.6],
        ['L', 92, 83.6],
        ['L', 92, 76],
        ['L', 81, 76],
        ['AR', 62, 76, 19, 76],
        ['L', 14, 76]
      ],
      parabrisas: [['M', 150, 19.8], ['C', 158, 20.6, 166, 36, 172, 54]],
      trasera: [['M', 93.5, 44], ['C', 95, 36, 96, 26, 96.5, 20.5]],
      vidrios: [
        [[167, 49], [130, 48.2], [131, 26.4], [150, 25.8]],
        [[125, 48.1], [99, 47.4], [104, 27], [126, 26.5]]
      ],
      puertas: [[[166, 49.6], [163, 83.6]], [[128, 48.4], [125, 83.6]], [[96, 47.6], [94, 83.6]]],
      manijas: [[134, 55.4], [102, 54.8]],
      espejo: [165, 48.6, true],
      faroF: [[214, 54], [225, 58], [226, 66], [214.5, 62]],
      faroT: [[12.5, 56], [17, 52], [26, 50.8], [26, 58], [14, 62]],
      parrilla: [[224, 66], [227.4, 68.4], [226.8, 76.5], [223.6, 74.4]],
      cintura: [[167, 49.2], [128, 48.6], [96, 47.8]],
      combustible: [86, 60, 4.4],
      extras(g, c) {
        return `
          ${arcoGuardabarros(62, 76, 19, 76, true)}${arcoGuardabarros(186, 76, 19, 83.6, true)}
          ${linea([[17, 54.6], [88, 53.6]], 'rgba(255,255,255,.4)', 2)}
          ${linea([[90, 52.4], [90, 76]], LINEA, 1.4)}
          ${linea([[25, 52.6], [23, 76]], LINEA, 1.4)}
          ${linea([[14, 72.4], [90, 71.4]], 'rgba(10,18,32,.35)', 1.4)}
          ${linea([[96, 84.4], [163, 84.4]], '#1f2937', 5)}
          ${linea([[210, 60.6], [176, 58.8]], 'rgba(255,255,255,.35)', 1.6)}
          ${linea([[212, 68.4], [226, 68.4]], 'rgba(10,18,32,.4)', 1.6)}
          <circle cx="86" cy="60" r="4.4" fill="none" stroke="${LINEA}" stroke-width="1.2"/>
          ${linea([[99, 84.6], [160, 84.6]], 'rgba(255,255,255,.16)', 1.4)}
          ${linea([[30, 84.4], [44, 84.4]], '#1f2937', 4)}`;
      }
    },


    van: {
      suelo: 95, ruedaR: 15, ejeT: 57, ejeF: 183, yB: 87.7, archR: 17.5, ancho: 79,
      contorno: [
        ['M', 12, 87.7],
        ['C', 9.5, 83, 9, 76, 10, 68],
        ['C', 11, 60, 12, 54, 13.5, 50],
        ['C', 16, 42, 20, 30, 24, 22],
        ['C', 26, 18, 28, 16, 31, 15.2],
        ['C', 60, 13.6, 120, 13.2, 168, 14.4],
        ['C', 176, 15, 186, 34, 196, 56],
        ['C', 204, 57, 210, 58, 215, 60],
        ['C', 220, 61.6, 224, 64, 224.5, 70],
        ['C', 225, 78, 224.5, 84, 222, 87.7],
        ['L', 198.7, 87.7],
        ['AR', 183, 80, 17.5, 87.7],
        ['L', 72.7, 87.7],
        ['AR', 57, 80, 17.5, 87.7],
        ['L', 12, 87.7]
      ],
      parabrisas: [['M', 168, 14.4], ['C', 176, 15, 186, 34, 196, 56]],
      trasera: [['M', 13.5, 50], ['C', 16, 42, 20, 30, 24, 22], ['L', 31, 15.2]],
      vidrios: [
        [[190, 54], [142, 53], [146, 26.5], [171, 26]],
        [[137, 52.9], [80, 52.2], [84, 27.4], [142, 26.9]],
        [[75, 52.1], [36, 51.4], [46, 28.6], [80, 28]]
      ],
      puertas: [[[191, 55], [188, 87.7]], [[142, 53.2], [139, 87.7]], [[78, 52.4], [75, 87.7]]],
      manijas: [[146, 60.4], [100, 59.8]],
      espejo: [189, 53.6, true],
      faroF: [[215, 60], [224.6, 64], [225, 71], [215.5, 67]],
      faroT: [[12, 54], [17, 50], [27, 49], [27, 58], [13.5, 62]],
      parrilla: [[221, 71], [225.4, 73], [225, 80], [221.4, 78]],
      cintura: [[191, 54.2], [142, 53.2], [137, 53.1], [75, 52.3], [36, 51.6]],
      combustible: [66, 62, 4.4],
      extras(g, c) {
        return `
          ${linea([[80, 64.4], [140, 64]], 'rgba(10,18,32,.55)', 1.8)}
          ${linea([[40, 71.4], [190, 70.6]], 'rgba(10,18,32,.4)', 3)}
          ${linea([[14, 84.4], [10, 84.4]], 'rgba(10,18,32,.4)', 2)}
          ${linea([[198, 58.6], [150, 57.4], [60, 56.6], [18, 58]], 'rgba(255,255,255,.28)', 1.4)}
          ${linea([[199, 60.6], [150, 59.4], [60, 58.6], [19, 60]], 'rgba(10,18,32,.16)', 1.6)}
          <circle cx="66" cy="62" r="4.4" fill="none" stroke="${LINEA}" stroke-width="1.2"/>
          ${linea([[30, 86.4], [46, 86.4]], 'rgba(10,18,32,.45)', 2)}
          ${linea([[204, 86.4], [220, 86.4]], 'rgba(10,18,32,.45)', 2)}`;
      }
    }
  };


  function linea(pts, color, w) {
    return `<path d="${dLine(pts)}" fill="none" stroke="${color}" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round"/>`;
  }

  function arcoGuardabarros(cx, cy, r, yB, cerrar) {
    const al = Math.asin((yB - cy) / r);
    const x0 = cx + r * Math.cos(al), y0 = yB;
    const x1 = cx - r * Math.cos(al);
    return `<path d="M${x0.toFixed(1)} ${y0} A ${r} ${r} 0 1 0 ${x1.toFixed(1)} ${y0}" fill="none" stroke="#1c2431" stroke-width="${cerrar ? 3.6 : 3.2}" stroke-linecap="round" opacity=".9"/>`;
  }




  function pintar(tipo, color, marca) {
    const s = SPECS[tipo] || SPECS.sedan;
    const g = 'v' + (++UID);
    const cont = muestrear(s.contorno, 2.2);
    const cxT = s.ejeT, cxF = s.ejeF;
    const cxSombra = Math.round((s.ejeT + s.ejeF) / 2);
    const rxSombra = Math.round((s.ejeF - s.ejeT) / 2 + 46);

    const cuerpo = dPath(cont);
    const brazos = (tipo === 'suv' || tipo === 'pickup' || tipo === 'van') ? 6 : 5;




    const clip = `<clipPath id="${g}c"><path d="${cuerpo}"/></clipPath>`;

    let sombra = `<ellipse cx="${cxSombra}" cy="${s.suelo + 3.5}" rx="${rxSombra}" ry="5.5" fill="url(#${g}s)"/>`;
    let ruedas = rueda(cxT, s.suelo - s.ruedaR, s.ruedaR, brazos, g)
      + rueda(cxF, s.suelo - s.ruedaR, s.ruedaR, brazos, g);
    let capa = '';


    const fal = cont.filter(p => p[1] >= s.yB - 5);
    if (fal.length > 4) capa += `<path d="${dLine(fal)}" fill="none" stroke="rgba(6,10,18,.4)" stroke-width="5" stroke-linecap="round"/>`;

    s.puertas.forEach(p => { capa += linea(p, LINEA, 1.3); });

    s.vidrios.forEach(v => { capa += cristal(v, g); });

    capa += linea(s.cintura, 'rgba(233,240,248,.85)', 1.6);
    capa += linea(s.cintura, 'rgba(10,18,32,.35)', 0.8);

    capa += (s.extras ? s.extras(g, color) : '');

    s.manijas.forEach(m => { capa += manija(m[0], m[1], 9); });

    capa += faro(s.faroF, g, false);
    capa += faro(s.faroT, g, true);

    capa += `<path d="${dPath(s.parrilla)}" fill="#131a25" stroke="rgba(8,14,24,.7)" stroke-width="1.1" stroke-linejoin="round"/>`;
    for (let i = 0; i < 3; i++) {
      const y0 = s.parrilla[0][1] + (i + 1) * 2.6;
      capa += `<path d="M${(s.parrilla[0][0] + 1.4).toFixed(1)} ${y0.toFixed(1)} L${(s.parrilla[1][0] - 1.2).toFixed(1)} ${(y0 + 1.4).toFixed(1)}" stroke="#7d8b9e" stroke-width="1.1" stroke-linecap="round" opacity=".75"/>`;
    }

    const sup = cont.filter(p => p[1] <= s.suelo - 42);
    if (sup.length > 6) {
      capa += `<path d="${dLine(sup)}" fill="none" stroke="rgba(255,255,255,.5)" stroke-width="2.2" stroke-linecap="round" opacity=".55"/>`;
    }

    let out = sombra + ruedas;
    out += `<path d="${cuerpo}" fill="url(#${g}p)" stroke="rgba(8,14,24,.72)" stroke-width="1.8" stroke-linejoin="round"/>`;
    out += sombraArco(cxT, s.suelo - s.ruedaR, s.archR - 1.4);
    out += sombraArco(cxF, s.suelo - s.ruedaR, s.archR - 1.4);
    out += `<g clip-path="url(#${g}c)">${capa}</g>`;
    out += espejo(s.espejo[0], s.espejo[1], s.espejo[2]);

    return defs(g, color, brazos) + clip + out;
  }




  function carroSVG(marca, modelo, placa) {
    const tipo = estiloDe(marca, modelo);
    const color = colorVehiculo(marca, placa);
    return `<svg class="veh-car" viewBox="0 0 240 112" role="img" aria-label="Vista lateral del vehículo">${pintar(tipo, color, marca)}</svg>`;
  }
  window.vehCarSVG = function (marca, placa) { return carroSVG(marca, '', placa); };


  window.vehPerfil = function (v) {
    v = v || {};
    const tipo = estiloDe(v.marca, v.modelo);
    const s = SPECS[tipo] || SPECS.sedan;
    return {
      tipo,
      ancho: s.ancho,
      suelo: s.suelo,
      cinturaY: (s.cintura && s.cintura[0][1]) || null,
      contorno: muestrear(s.contorno, 1.6),
      vidrios: s.vidrios.map(g => g.map(p => [p[0], p[1]])),
      parabrisas: muestrear(s.parabrisas, 2),
      trasera: muestrear(s.trasera, 2),
      manijas: s.manijas,
      espejo: s.espejo,
      puertas: s.puertas,
      cintura: s.cintura,
      ruedas: [{ x: s.ejeT, y: s.suelo - s.ruedaR, r: s.ruedaR }, { x: s.ejeF, y: s.suelo - s.ruedaR, r: s.ruedaR }],
      faroF: s.faroF, faroT: s.faroT, parrilla: s.parrilla,
      color: colorVehiculo(v.marca, v.placa),
      marca: v.marca || '', modelo: v.modelo || '', placa: v.placa || ''
    };
  };


  function btn3D(v, chica) {
    const d = `data-marca="${esc(v.marca || '')}" data-modelo="${esc(v.modelo || '')}" data-placa="${esc(v.placa || '')}" data-anio="${esc(v.anio || '')}" data-genero="${esc(v.genero || '')}"`;
    return chica
      ? `<button type="button" class="ag-veh-3d ag-veh-3d-sm" ${d} title="Ver en 3D">3D</button>`
      : `<button type="button" class="ag-veh-3d" ${d}>Ver en 3D</button>`;
  }
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }


  function previewHTML(v) {
    const m = marcaDe(v.marca);
    const placa = ((v.placa || '').trim() || '___-___').toUpperCase();
    const detalle = [v.modelo, v.anio].filter(Boolean).join(' · ') || 'Completa los datos';
    return `<div class="ag-veh-preview">
      <div class="ag-veh-render">${carroSVG(v.marca, v.modelo, v.placa)}</div>
      <div class="ag-veh-side">
        <span class="ag-veh-emblem" title="${m ? m.nombre : 'Marca libre'}">${emblemaSVG(v.marca)}</span>
        <strong>${m ? m.nombre : (v.marca || 'Marca libre')}</strong>
        <span class="ag-plate">${placa}</span>
        <small>${detalle}</small>
        ${btn3D(v)}
      </div>
    </div>`;
  }
  window.vehPreviewHTML = previewHTML;


  function miniHTML(v) {
    const m = marcaDe(v.marca);
    const placa = (v.placa || '—').toUpperCase();
    const sub = [m ? m.nombre : v.marca, v.modelo, v.anio].filter(Boolean).join(' · ');
    return `<div class="ag-veh-mini">${carroSVG(v.marca, v.modelo, v.placa)}
      <div><span class="ag-plate ag-plate-sm">${placa}</span>
      <small>${sub || 'Sin datos'}</small></div>
      ${btn3D(v, true)}
    </div>`;
  }
  window.vehMiniHTML = miniHTML;


  function poblarMarcas(sel, actual) {
    if (!sel) return;
    const cur = (actual || '').trim();
    const conocida = normalizarMarca(cur);
    let html = `<option value="">— Sin marca —</option>`;
    html += MARCAS.map(x => `<option value="${x.nombre}"${conocida === x.id ? ' selected' : ''}>${x.nombre}</option>`).join('');
    const libre = cur && !conocida;
    html += `<option value="__otra"${libre ? ' selected' : ''}>Otra marca…</option>`;
    sel.innerHTML = html;
    sel.dataset.libre = libre ? '1' : '';
    if (libre) sel.dataset.valorLibre = cur;
  }
  window.vehPoblarMarcas = poblarMarcas;
  window.vehNormalizarMarca = normalizarMarca;
  window.vehEstilo = estiloDe;
})();
