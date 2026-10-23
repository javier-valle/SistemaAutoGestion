package com.autogestion;

import com.autogestion.controller.ReporteController;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ReporteControllerSecurityTest {

    @Test
    void inventarioPermiteAdminYAlmacenero() throws Exception {
        Method m = ReporteController.class.getMethod("inventarioResumen", java.time.LocalDate.class, java.time.LocalDate.class);
        RolesAllowed ra = m.getAnnotation(RolesAllowed.class);
        assertNotNull(ra, "inventario/resumen debe tener @RolesAllowed");
        assertTrue(
            java.util.Arrays.asList(ra.value()).contains("ALMACENERO"),
            "inventario/resumen debe permitir ALMACENERO"
        );
    }

    @Test
    void ingresosNoPermiteAlmacenero() throws Exception {
        Method m = ReporteController.class.getMethod("ingresos", java.time.LocalDate.class, java.time.LocalDate.class, String.class);
        RolesAllowed ra = m.getAnnotation(RolesAllowed.class);
        assertTrue(
            ra == null || java.util.Arrays.asList(ra.value()).contains("ADMIN"),
            "ingresos debe ser ADMIN (o sin anotación, heredando ADMIN)"
        );
        assertTrue(
            ra == null || !java.util.Arrays.asList(ra.value()).contains("ALMACENERO"),
            "ingresos NO debe permitir ALMACENERO"
        );
    }
}
