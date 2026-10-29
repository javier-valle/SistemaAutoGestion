package com.autogestion.service;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Límite en memoria para la consulta pública de documentos.
 * Evita usar el portal como oráculo para enumerar DNIs. Nginx ya limita por IP,
 * pero el backend también se protege si se accede directo al puerto 8080.
 *
 * <p>Usa <b>Guava</b> ({@link Cache}) para que las IPs inactivas se expulsen solas
 * (tamaño máximo + expiración), en vez de la limpieza manual anterior, y
 * <b>Commons Lang3</b> ({@link StringUtils}) para normalizar la IP.
 *
 * <p>Es por instancia: con varias réplicas cada una tiene su propio contador.
 * Para producción distribuida, mover el contador a Redis o a la capa Nginx.
 */
@Component
public class ConsultaDocumentoRateLimit {

    private static final int MAX_IPS_EN_MEMORIA = 10_000;

    private final int maxPorVentana;
    private final long ventanaMs;
    private final Cache<String, Deque<Long>> accesos;

    public ConsultaDocumentoRateLimit(
            @Value("${reniec.consulta.max-por-ventana:12}") int maxPorVentana,
            @Value("${reniec.consulta.ventana-minutos:10}") long ventanaMinutos) {
        this.maxPorVentana = Math.max(1, maxPorVentana);
        long minutos = Math.max(1, ventanaMinutos);
        Preconditions.checkArgument(minutos <= 24 * 60, "La ventana no puede superar 24 horas");
        this.ventanaMs = minutos * 60_000;
        this.accesos = CacheBuilder.newBuilder()
                .maximumSize(MAX_IPS_EN_MEMORIA)
                .expireAfterAccess(minutos, TimeUnit.MINUTES)
                .build();
    }

    /** true = puede consultar; false = superó el límite. */
    public boolean permitir(String ip) {
        String clave = StringUtils.defaultIfBlank(ip, "desconocida");
        long ahora = System.currentTimeMillis();

        Deque<Long> marcas;
        try {
            marcas = accesos.get(clave, ArrayDeque::new);
        } catch (ExecutionException e) {

            return false;
        }
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
}
