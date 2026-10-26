package com.autogestion.service;

public interface Notificador {
    void enviar(String destinatarioHash, String canal, String plantilla, String contenido);
}
