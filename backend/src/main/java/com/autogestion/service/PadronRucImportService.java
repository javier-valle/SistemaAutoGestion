package com.autogestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Carga el Padrón Reducido RUC de SUNAT (dato público, sin API key) en la tabla padron_ruc.
 * Fuente: archivo local (padron.ruc.archivo) o descarga desde padron.ruc.url.
 * Lee en streaming (el archivo pesa cientos de MB) y escribe por lotes.
 */
@Service
@Slf4j
public class PadronRucImportService {

    private static final int LOTE = 5000;

    private final JdbcTemplate jdbc;

    @Value("${padron.ruc.archivo:}")
    private String archivo;

    @Value("${padron.ruc.url:https://www2.sunat.gob.pe/padron_reducido_ruc.zip}")
    private String url;

    @Value("${padron.ruc.solo-activos:false}")
    private boolean soloActivos;

    @Value("${padron.ruc.importar-al-iniciar:false}")
    private boolean importarAlIniciar;

    private final AtomicBoolean corriendo = new AtomicBoolean(false);
    private volatile String resultado = "sin ejecutar";
    private volatile long procesados = 0;

    public PadronRucImportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciar() {
        if (importarAlIniciar && tablaVacia()) {
            log.info("Padrón RUC vacío: iniciando importación automática");
            iniciarAsync();
        }
    }

    /** @return false si ya hay una importación en curso. */
    public boolean iniciarAsync() {
        if (!corriendo.compareAndSet(false, true)) return false;
        Thread t = new Thread(this::ejecutar, "padron-ruc-import");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public Map<String, Object> estado() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enCurso", corriendo.get());
        m.put("procesadosEnEstaCorrida", procesados);
        m.put("ultimoResultado", resultado);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM padron_ruc", Long.class);
        m.put("filasEnTabla", total == null ? 0L : total);
        return m;
    }

    private void ejecutar() {
        procesados = 0;
        resultado = "en curso";
        try {
            long n = importar();
            resultado = "ok: " + n + " RUC procesados";
            log.info("Importación del padrón RUC terminada: {} filas", n);
        } catch (Exception e) {
            resultado = "error: " + e.getMessage();
            log.error("Importación del padrón RUC falló", e);
        } finally {
            corriendo.set(false);
        }
    }

    private boolean tablaVacia() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT 1 FROM padron_ruc LIMIT 1) t", Integer.class);
        return n == null || n == 0;
    }

    private long importar() throws IOException {
        boolean vacia = tablaVacia();
        try (InputStream crudo = abrirFuente();
             InputStream datos = descomprimirSiHaceFalta(crudo);
             BufferedReader br = new BufferedReader(new InputStreamReader(datos, StandardCharsets.ISO_8859_1), 1 << 16)) {

            String linea = br.readLine();
            if (linea == null) throw new IOException("El archivo del padrón está vacío");

            Columnas cols = new Columnas();
            boolean primeraEsDato = linea.split("\\|", -1)[0].trim().matches("\\d{11}");
            if (!primeraEsDato) {
                cols = Columnas.desdeEncabezado(linea);
                linea = br.readLine();
            }

            List<Object[]> lote = new ArrayList<>(LOTE);
            long total = 0;
            for (; linea != null; linea = br.readLine()) {
                Object[] fila = cols.fila(linea, soloActivos);
                if (fila == null) continue;
                lote.add(fila);
                total++;
                if (lote.size() >= LOTE) {
                    guardar(lote, vacia);
                    lote.clear();
                    procesados = total;
                }
            }
            if (!lote.isEmpty()) guardar(lote, vacia);
            procesados = total;
            return total;
        }
    }

    /** Tabla vacía: INSERT directo (rápido). Con datos: UPDATE y, si no existía, INSERT. */
    private void guardar(List<Object[]> lote, boolean vacia) {
        if (vacia) {
            jdbc.batchUpdate("INSERT INTO padron_ruc (ruc, razon_social, estado, condicion, direccion) VALUES (?,?,?,?,?)", lote);
            return;
        }
        List<Object[]> updates = new ArrayList<>(lote.size());
        for (Object[] f : lote) updates.add(new Object[]{f[1], f[2], f[3], f[4], f[0]});
        int[] r = jdbc.batchUpdate("UPDATE padron_ruc SET razon_social=?, estado=?, condicion=?, direccion=? WHERE ruc=?", updates);
        List<Object[]> nuevos = new ArrayList<>();
        for (int i = 0; i < r.length; i++) if (r[i] == 0) nuevos.add(lote.get(i));
        if (!nuevos.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO padron_ruc (ruc, razon_social, estado, condicion, direccion) VALUES (?,?,?,?,?)", nuevos);
        }
    }

    private InputStream abrirFuente() throws IOException {
        if (archivo != null && !archivo.isBlank() && Files.isRegularFile(Path.of(archivo))) {
            log.info("Padrón RUC: leyendo archivo local {}", archivo);
            return Files.newInputStream(Path.of(archivo));
        }
        log.info("Padrón RUC: descargando desde {}", url);
        HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
        con.setConnectTimeout(15_000);
        con.setReadTimeout(120_000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("User-Agent", "AutoGestion/1.0");
        if (con.getResponseCode() != 200) {
            throw new IOException("No se pudo descargar el padrón (HTTP " + con.getResponseCode()
                    + "). Descárgalo a mano y configura PADRON_RUC_ARCHIVO.");
        }
        return con.getInputStream();
    }

    private static InputStream descomprimirSiHaceFalta(InputStream in) throws IOException {
        BufferedInputStream b = new BufferedInputStream(in, 1 << 16);
        b.mark(4);
        int a = b.read();
        int c = b.read();
        b.reset();
        if (a == 'P' && c == 'K') {
            ZipInputStream z = new ZipInputStream(b);
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.isDirectory()) return z;
            }
            throw new IOException("El ZIP del padrón no contiene archivos");
        }
        return b;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase().trim();
    }

    private static String corta(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.equals("-")) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    /** Posiciones de columnas del padrón (se detectan por nombre; si no hay encabezado, orden clásico de SUNAT). */
    private static final class Columnas {
        int ruc = 0, nombre = 1, estado = 2, condicion = 3;
        int tipoVia = -1, nombreVia = -1, numero = -1, interior = -1, lote = -1, manzana = -1, km = -1;

        static Columnas desdeEncabezado(String encabezado) {
            Columnas c = new Columnas();
            String[] h = encabezado.split("\\|", -1);
            for (int i = 0; i < h.length; i++) {
                String n = norm(h[i]);
                if (n.equals("RUC")) c.ruc = i;
                else if (n.startsWith("NOMBRE O RAZON")) c.nombre = i;
                else if (n.contains("ESTADO")) c.estado = i;
                else if (n.contains("CONDICION")) c.condicion = i;
                else if (n.equals("TIPO DE VIA")) c.tipoVia = i;
                else if (n.equals("NOMBRE DE VIA")) c.nombreVia = i;
                else if (n.equals("NUMERO")) c.numero = i;
                else if (n.equals("INTERIOR")) c.interior = i;
                else if (n.equals("LOTE")) c.lote = i;
                else if (n.equals("MANZANA")) c.manzana = i;
                else if (n.equals("KILOMETRO")) c.km = i;
            }
            return c;
        }

        /** [ruc, razon_social, estado, condicion, direccion] o null si la fila no sirve. */
        Object[] fila(String linea, boolean soloActivos) {
            String[] p = linea.split("\\|", -1);
            if (p.length <= Math.max(Math.max(ruc, nombre), Math.max(estado, condicion))) return null;
            String r = p[ruc].trim();
            if (!r.matches("\\d{11}")) return null;
            String est = corta(p[estado], 50);
            if (soloActivos && !norm(est).startsWith("ACTIVO")) return null;
            return new Object[]{r, corta(p[nombre], 300), est, corta(p[condicion], 50), direccion(p)};
        }

        private String direccion(String[] p) {
            StringBuilder sb = new StringBuilder();
            add(sb, p, tipoVia, "");
            add(sb, p, nombreVia, "");
            add(sb, p, numero, "NRO ");
            add(sb, p, interior, "INT ");
            add(sb, p, lote, "LT ");
            add(sb, p, manzana, "MZ ");
            add(sb, p, km, "KM ");
            String d = sb.toString().trim();
            return d.isEmpty() ? null : (d.length() > 500 ? d.substring(0, 500) : d);
        }

        private static void add(StringBuilder sb, String[] p, int idx, String prefijo) {
            if (idx < 0 || idx >= p.length) return;
            String v = p[idx].trim();
            if (v.isEmpty() || v.equals("-")) return;
            if (sb.length() > 0) sb.append(' ');
            sb.append(prefijo).append(v);
        }
    }
}
