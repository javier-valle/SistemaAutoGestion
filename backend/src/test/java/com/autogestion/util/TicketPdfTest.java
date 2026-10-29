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
        assertTrue(pdf.contains("BOLETA DE VENTA ELECTRONICA"));
        assertTrue(pdf.contains("B001-000001"));
        assertTrue(pdf.contains("20123456786"), "RUC del emisor");
        assertTrue(pdf.contains("DNI 45123698"), "Documento del adquirente");
        assertTrue(pdf.contains("S/ 80.00"), "Importe total");
        assertTrue(pdf.contains("OCHENTA CON 00/100 SOLES"));
        assertTrue(pdf.contains("2/10/2026 15:30"), "Fecha de emisión");
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
    void comprobanteAnuladoLoDiceEnElTicket() {
        var c = comprobante();
        c.setEstado("ANULADO");
        c.setMotivoAnulacion("Error de tipeo");
        String pdf = new String(TicketPdf.generar(c), StandardCharsets.ISO_8859_1);
        assertTrue(pdf.contains("COMPROBANTE ANULADO"));
        assertFalse(pdf.contains("PAGO REGISTRADO"));
    }
}
