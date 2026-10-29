package com.autogestion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite en memoria para la consulta pública de documentos.
 * Evita usar el portal como oráculo para enumerar DNIs. Nginx ya limita por IP,
 * pero el backend también se protege si se accede directo al puerto 8080.
 *
 * Es por instancia: con varias réplicas cada una tiene su propio contador.
 * Para producción distribuida, mover el contador a Redis o a la capa Nginx.
 */
@Component
public class ConsultaDocumentoRateLimit {

    private final int maxPorVentana;
    private final long ventanaMs;
    private final Map<String, Deque<Long>> accesos = new ConcurrentHashMap<>();

    public ConsultaDocumentoRateLimit(
            @Value("${reniec.consulta.max-por-ventana:12}") int maxPorVentana,
            @Value("${reniec.consulta.ventana-minutos:10}") long ventanaMinutos) {
        this.maxPorVentana = Math.max(1, maxPorVentana);
        this.ventanaMs = Math.max(1, ventanaMinutos) * 60_000;
    }

    /** true = puede consultar; false = superó el límite. */
    public boolean permitir(String ip) {
        String clave = (ip == null || ip.isBlank()) ? "desconocida" : ip;
        long ahora = System.currentTimeMillis();
        if (accesos.size() > 10_000) limpiar(ahora);

        Deque<Long> marcas = accesos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && marcas.peekFirst() <= ahora - ventanaMs) {
                marcas.pollFirst();
            }
            if (marcas.size() >= maxPorVentana) {
                return false;
            }
            marcas.addLast(ahora);
            return true;
        }
    }

    private void limpiar(long ahora) {
        accesos.entrySet().removeIf(e -> {
            Deque<Long> d = e.getValue();
            synchronized (d) {
                return d.isEmpty() || d.peekLast() <= ahora - ventanaMs;
            }
        });
    }
}
