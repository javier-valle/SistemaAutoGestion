package com.autogestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LogNotificador implements Notificador {
    @Override
    public void enviar(String destinatarioHash, String canal, String plantilla, String contenido) {
        log.info("[Notificador] Canal: {}, Plantilla: {}, DestinatarioHash: {}, Contenido: {}",
                canal, plantilla, destinatarioHash, contenido);
    }
}
