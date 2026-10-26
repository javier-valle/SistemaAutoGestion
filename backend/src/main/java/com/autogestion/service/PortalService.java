package com.autogestion.service;

import com.autogestion.entity.*;
import com.autogestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private final PortalAccesoLogRepository portalAccesoLogRepository;
    private final OtpCodigoRepository otpCodigoRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final Notificador notificador;

    public boolean validarIdentificacionModoA(String documento, String tipoDocumento, String placa) {
        // Modo A: DNI/RUC + placa. Solo lectura.
        // Respuesta genérica. Sin revelar existencia.
        TipoDocumento td = TipoDocumento.desde(tipoDocumento);
        boolean documentoExiste = td != null && clienteRepository.findByTipoDocumentoAndDocumento(td, documento).isPresent();
        boolean placaExiste = vehiculoRepository.findByPlaca(placa).isPresent();
        boolean valido = documentoExiste && placaExiste;
        // Registramos con hash (simulado) para evitar PII en logs.
        String docHash = hashSimple(documento);
        portalAccesoLogRepository.save(PortalAccesoLog.builder()
                .documentoHash(docHash)
                .resultado(valido ? "IDENTIFICADO" : "FALLIDO")
                .tipo("A")
                .ipHash(hashSimple("127.0.0.1"))
                .build());
        return valido;
    }

    public void solicitarOtp(String documento, String tipoDocumento) {
        TipoDocumento td = TipoDocumento.desde(tipoDocumento);
        String tipoStr = td != null ? td.name() : tipoDocumento;
        // Genera OTP de 6 dígitos, hasheado, expira en 5 min.
        String codigoPlano = String.format("%06d", (int)(Math.random() * 1_000_000));
        String hashCodigo = hashSimple(codigoPlano);
        otpCodigoRepository.deleteAllInBatch(); // simplificación: limpiar anteriores
        otpCodigoRepository.save(OtpCodigo.builder()
                .documento(documento)
                .tipoDocumento(tipoStr)
                .codigo(codigoPlano)
                .hashCodigo(hashCodigo)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .intentos(0)
                .usado(false)
                .build());
        notificador.enviar(hashSimple(documento), "WHATSAPP", "OTP_PORTAL", "Tu código es: " + codigoPlano);
    }

    public boolean verificarOtp(String documento, String tipoDocumento, String codigo) {
        TipoDocumento td = TipoDocumento.desde(tipoDocumento);
        String tipoStr = td != null ? td.name() : tipoDocumento;
        Optional<OtpCodigo> otpOpt = otpCodigoRepository.findByDocumentoAndTipoDocumentoAndUsadoFalseAndExpiraEnAfter(documento, tipoStr, LocalDateTime.now());
        if (otpOpt.isEmpty()) {
            return false;
        }
        OtpCodigo otp = otpOpt.get();
        otp.setIntentos(otp.getIntentos() + 1);
        if (otp.getIntentos() > 3) {
            otp.setUsado(true);
            otpCodigoRepository.save(otp);
            return false;
        }
        if (!hashSimple(codigo).equals(otp.getHashCodigo())) {
            otpCodigoRepository.save(otp);
            return false;
        }
        otp.setUsado(true);
        otpCodigoRepository.save(otp);
        return true;
    }

    private String hashSimple(String input) {
        return java.util.UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }
}
