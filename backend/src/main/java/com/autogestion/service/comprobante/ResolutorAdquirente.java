package com.autogestion.service.comprobante;

import com.autogestion.config.EmpresaProperties;
import com.autogestion.dto.ComprobanteEmitirRequest;
import com.autogestion.entity.Cliente;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.TipoComprobante;
import com.autogestion.entity.TipoDocumento;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.ClienteRepository;
import com.autogestion.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * SRP: única razón de cambio = reglas del adquirente por tipo de comprobante.
 * El orquestador ({@code ComprobanteService}) solo pide el snapshot validado.
 */
@Component
@RequiredArgsConstructor
public class ResolutorAdquirente {

    private final ClienteRepository clienteRepository;
    private final EmpresaProperties empresa;

    /** Valida adquirente según tipo de comprobante y devuelve el snapshot a imprimir. */
    public AdquirenteResuelto resolver(ComprobanteEmitirRequest req, OrdenTrabajo ot,
                                       TipoComprobante tipo, BigDecimal total) {
        Cliente cliente;
        if (req.getClienteId() != null) {
            cliente = clienteRepository.findById(req.getClienteId())
                    .orElseThrow(() -> new BusinessException("El cliente elegido no existe.", "clienteId",
                            "Búscalo de nuevo por documento.", org.springframework.http.HttpStatus.NOT_FOUND));
        } else {
            cliente = ot.getCotizacion().getDiagnostico().getRecepcion().getVehiculo().getCliente();
        }
        String tipoDoc = req.getTipoDoc() != null && !req.getTipoDoc().isBlank()
                ? req.getTipoDoc().trim().toUpperCase()
                : (cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().name() : "DNI");
        String numDoc = DocumentoValidator.normalizar(req.getNumDoc() != null ? req.getNumDoc() : cliente.getDocumento());
        String nombre = req.getNombre() != null && !req.getNombre().isBlank() ? req.getNombre().trim() : cliente.nombreFiscal();
        String direccion = req.getDireccion() != null && !req.getDireccion().isBlank()
                ? req.getDireccion().trim() : cliente.getDireccion();

        if (tipo == TipoComprobante.FACTURA) {
            if (!"RUC".equals(tipoDoc) || !DocumentoValidator.rucValido(numDoc)) {
                throw new BusinessException("La factura exige un RUC válido con dígito verificador.", "numDoc",
                        DocumentoValidator.mensajeError(TipoDocumento.RUC, numDoc));
            }
            if (nombre == null || nombre.isBlank()) {
                throw new BusinessException("La factura exige la razón social.", "nombre",
                        "Es el nombre legal de la empresa cliente.");
            }
            if (direccion == null || direccion.isBlank()) {
                throw new BusinessException("La factura exige la dirección fiscal.", "direccion",
                        "Es el domicilio legal del RUC.");
            }
            return new AdquirenteResuelto(cliente, "RUC", numDoc, nombre, direccion);
        }



        if ("SIN_DOC".equals(tipoDoc) || numDoc.isEmpty() || "-".equals(numDoc)) {
            if (total.compareTo(empresa.getBoletaUmbralDni()) > 0) {
                throw new BusinessException("Boleta mayor a S/ " + empresa.getBoletaUmbralDni()
                        + " exige DNI del cliente.", "numDoc",
                        "Pide el DNI (8 dígitos) o emite con documento.");
            }
            return new AdquirenteResuelto(null, "SIN_DOC", "-", "CLIENTES VARIOS", null);
        }
        TipoDocumento td = TipoDocumento.desde(tipoDoc);
        if (td == null || !DocumentoValidator.valido(td, numDoc)) {
            if (DocumentoValidator.rucValido(numDoc)) {
                td = TipoDocumento.RUC;
            } else {
                TipoDocumento inferido = inferirTipoDocumento(numDoc, cliente);
                if (inferido != null && DocumentoValidator.valido(inferido, numDoc)) {
                    td = inferido;
                }
            }
        }
        if (td == null || !DocumentoValidator.valido(td, numDoc)) {
            throw new BusinessException("Documento inválido para boleta.", "numDoc",
                    DocumentoValidator.mensajeError(td == null ? TipoDocumento.DNI : td, numDoc));
        }
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("Escribe el nombre del cliente.", "nombre",
                    "Nombres y apellidos como en su DNI.");
        }
        return new AdquirenteResuelto(cliente, td.name(), numDoc, nombre, direccion);
    }

    /** Tipo más plausible del número: primero el de la ficha del cliente, luego DNI. */
    private TipoDocumento inferirTipoDocumento(String numDoc, Cliente cliente) {
        TipoDocumento delCliente = cliente != null ? cliente.getTipoDocumento() : null;
        if (delCliente != null && delCliente != TipoDocumento.RUC
                && DocumentoValidator.valido(delCliente, numDoc)) {
            return delCliente;
        }
        if (DocumentoValidator.dniValido(numDoc)) return TipoDocumento.DNI;
        if (delCliente != null && delCliente != TipoDocumento.RUC) return delCliente;
        return TipoDocumento.DNI;
    }
}
