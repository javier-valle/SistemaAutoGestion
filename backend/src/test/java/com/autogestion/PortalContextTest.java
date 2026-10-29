package com.autogestion;

import com.autogestion.entity.*;
import com.autogestion.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PortalContextTest {

    @Autowired
    private PortalAccesoLogRepository portalLogRepo;
    @Autowired
    private CitaRepository citaRepo;
    @Autowired
    private EncuestaSatisfaccionRepository encuestaRepo;
    @Autowired
    private ConsentimientoRepository consentimientoRepo;
    @Autowired
    private NotificacionRepository notificacionRepo;

    @Test
    void contextoArrancaYRepositoriosPortalExisten() {
        assertNotNull(portalLogRepo);
        assertNotNull(citaRepo);
        assertNotNull(encuestaRepo);
        assertNotNull(consentimientoRepo);
        assertNotNull(notificacionRepo);
    }
}
