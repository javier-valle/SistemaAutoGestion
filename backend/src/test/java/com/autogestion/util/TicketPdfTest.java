package com.autogestion.util;

import com.autogestion.dto.ComprobanteResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El PDF del ticket lo generaba el navegador; ahora lo arma Java.
 * Estas pruebas validan la estructura del archivo (cabecera, xref coherente,
 * cierre) y que los datos fiscales clave aparezcan en el texto.
 */
class TicketPdfTest {

    private ComprobanteResponseDTO comprobante() {
        return ComprobanteResponseDTO.builder()
                .id(7L)
                .tipo("BOLETA")
                .serie("B001")
                .numero(1)
                .folio("B001-000001")
                .fechaEmision(LocalDateTime.of(2026, 10, 2, 15, 30))
                .moneda("PEN")
                .formaPago("CONTADO")
                .metodoPago("EFECTIVO")
                .estado("EMITIDO")
                .emisorRuc("20123456786")
                .emisorRazonSocial("TALLER SAN MARTIN S.A.C.")
                .emisorNombreComercial("Taller San Martín")
                .emisorDireccion("Av. Universitaria 123, San Martin de Porres, Lima")
                .clienteTipoDoc("DNI")
                .clienteNumDoc("45123698")
                .clienteNombre("Juan Pérez")
                .clienteDireccion("Los Olivos 123")
                .totalGravado(new BigDecimal("67.7966"))
                .igv(new BigDecimal("12.2034"))
                .total(new BigDecimal("80.00"))
                .totalLetras("OCHENTA CON 00/100 SOLES")
                .observacion("Placa ABC-123 - OT #5")
                .qrContenido("20123456786|B001|1|80.00|20261002|")
                .detalle(List.of(ComprobanteResponseDTO.DetalleDTO.builder()
                        .item(1).codigo("S1").descripcion("Cambio de aceite y filtro")
                        .unidadMedida("ZZ").cantidad(BigDecimal.ONE)
                        .importeTotal(new BigDecimal("80.00")).build()))
                .build();
    }

    @Test
    void generaUnPdfConEstructuraValida() {
        byte[] bytes = TicketPdf.generar(comprobante());
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(pdf.startsWith("%PDF-1.4"), "Debe empezar con la cabecera PDF");
        assertTrue(pdf.endsWith("%%EOF"), "Debe cerrar con %%EOF");

        int i = pdf.lastIndexOf("startxref");
        assertTrue(i > 0, "Debe declarar startxref");
        int xref = Integer.parseInt(pdf.substring(i + "startxref".length()).trim()
                .split("\\s+")[0]);
        assertEquals("xref", pdf.substring(xref, xref + 4),
                "El offset de startxref debe apuntar a la tabla xref");


        String entradas = pdf.substring(xref);
        int off1 = Integer.parseInt(entradas.split("\n")[3].trim().split(" ")[0]);
        assertEquals("1 0 obj", pdf.substring(off1, off1 + "1 0 obj".length()),
                "El offset del objeto 1 debe ser correcto");
    }

    @Test
    void incluyeLosDatosFiscalesDelComprobante() {
        String pdf = new String(TicketPdf.generar(comprobante()), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("BOLETA DE VENTA ELECTRÓNICA"));
        assertTrue(pdf.contains("B001-000001"));
        assertTrue(pdf.contains("RUC: 20123456786"), "RUC del emisor");
        assertTrue(pdf.contains("(DNI)"), "Tipo de documento del adquirente");
        assertTrue(pdf.contains("(45123698)"), "Número de documento del adquirente");
        assertTrue(pdf.contains("S/ 80.00"), "Importe total");
        assertTrue(pdf.contains("SON: OCHENTA CON 00/100 SOLES"));
        assertTrue(pdf.contains("02/10/2026 15:30"), "Fecha de emisión");
    }

    @Test
    void usaLaEstructuraDeLaRepresentacionImpresa() {
        String pdf = new String(TicketPdf.generar(comprobante()), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("::: BOLETA DE VENTA ELECTRÓNICA - Impresión :::"));
        assertTrue(pdf.contains("Valor"), "Cabecera de la columna de valor unitario");
        assertTrue(pdf.contains("Unitario\\(*\\)"), "Los paréntesis viajan escapados en el PDF");
        assertTrue(pdf.contains("Importe de"));
        assertTrue(pdf.contains("Venta\\(**\\)"));
        assertTrue(pdf.contains("Op. Gravada :"));
        assertTrue(pdf.contains("Importe Total :"));
        assertTrue(pdf.contains("Tipo de Moneda"));
        assertTrue(pdf.contains("SOLES"));
        assertTrue(pdf.contains("\\(*\\) Sin impuestos"), "Nota al pie de la tabla");
        assertTrue(pdf.contains("representación impresa"), "Texto legal enmarcado");
    }

    @Test
    void laFacturaCambiaElTituloYElDetalleLargoNoSeSale() {
        var c = comprobante();
        c.setTipo("FACTURA");
        c.setDetalle(java.util.stream.IntStream.rangeClosed(1, 45).mapToObj(i ->
                ComprobanteResponseDTO.DetalleDTO.builder()
                        .item(i)
                        .codigo("P" + i)
                        .descripcion("Repuesto de freno delantero con pastillas y discos")
                        .unidadMedida("NIU")
                        .cantidad(BigDecimal.ONE)
                        .valorUnitario(new BigDecimal("10.00"))
                        .importeTotal(new BigDecimal("11.80"))
                        .build()).toList());
        String pdf = new String(TicketPdf.generar(c), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("FACTURA ELECTRÓNICA"));
        assertFalse(pdf.contains("BOLETA DE VENTA ELECTRÓNICA"));
        assertTrue(pdf.contains("Continúa en la hoja siguiente"),
                "45 líneas no caben en una hoja: debe paginar");
        assertTrue(pdf.lines().filter(l -> l.contains("/Type /Page ")).count() >= 2,
                "Debe repartir el detalle en varias páginas");
    }

    @Test
    void textoConParentesisNoRompeElFlujo() {
        var c = comprobante();
        c.setClienteNombre("Servicio (general) Ñuñoir");
        c.setObservacion("Cliente llamó a las 5:30 (mañana)");
        String pdf = new String(TicketPdf.generar(c), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("\\(general\\)"), "Los paréntesis deben venir escapados");
        assertTrue(pdf.contains("Ñuñoir"), "Las tildes/ñ viajan en WinAnsi");
    }

    @Test
    void elQrSeDibujaComoVectoresDentroDelPdf() {
        String pdf = new String(TicketPdf.generar(comprobante()), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("Escanea el QR"), "El documento debe invitar a escanear el código");
        int modulos = pdf.split(" re f", -1).length - 1;
        assertTrue(modulos > 80,
                "Un QR 33x33 deja decenas de módulos pintados; se encontraron " + modulos);
    }

    @Test
    void sinContenidoDeQrNoSeDibujaNada() {
        var c = comprobante();
        c.setQrContenido(null);
        String pdf = new String(TicketPdf.generar(c), StandardCharsets.ISO_8859_1);
        assertFalse(pdf.contains("Escanea el QR"));
    }

    @Test
    void comprobanteAnuladoLoDiceEnElTicket() {
        var c = comprobante();
        c.setEstado("ANULADO");
        c.setMotivoAnulacion("Error de tipeo");
        String pdf = new String(TicketPdf.generar(c), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("COMPROBANTE ANULADO"));
        assertFalse(pdf.contains("PAGO REGISTRADO"));
    }
}
