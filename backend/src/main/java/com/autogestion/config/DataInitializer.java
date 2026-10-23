package com.autogestion.config;

import com.autogestion.dto.ProveedorRequest;
import com.autogestion.entity.*;
import com.autogestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Arranque: el ADMIN y las series siempre (infraestructura); los datos demo
 * (equipo, clientes, vehículos, catálogos) solo si app.seed-demo=true.
 * Las claves salen de propiedades/variables, nunca del código.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final UsuarioRepository usuarioRepository;
        private final ClienteRepository clienteRepository;
        private final VehiculoRepository vehiculoRepository;
        private final ServicioRepository servicioRepository;
        private final ProductoRepository productoRepository;
        private final SerieComprobanteRepository serieRepository;
        private final ProveedorRepository proveedorRepository;
        private final PasswordEncoder passwordEncoder;

        @Value("${app.seed-demo:true}")
        private boolean seedDemo;

        @Value("${app.admin.email:admin@sanmartin.pe}")
        private String adminEmail;

        @Value("${app.admin.password:Admin-2026*}")
        private String adminPassword;

        @Value("${app.seed.password.mecanico:Meca-2026*}")
        private String passMecanico;

        @Value("${app.seed.password.almacen:Alma-2026*}")
        private String passAlmacen;

        @Value("${app.seed.password.recepcionista:Recep-2026*}")
        private String passRecepcionista;

        @Override
        public void run(String... args) {
                // 1. Admin de arranque: siempre, para no quedarse sin acceso.
                if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
                        usuarioRepository.save(Usuario.builder()
                                        .nombre("Administrador")
                                        .nombres("Administrador").apellidos(null)
                                        .tipoDocumento(TipoDocumento.DNI).documento("45123789")
                                        .telefono("951238745").email(adminEmail)
                                        .passwordHash(passwordEncoder.encode(adminPassword))
                                        .rol("ADMIN").activo(true).build());
                        log.info("Admin de arranque creado ({})", adminEmail);
                }

                // 2. Series de comprobantes (infraestructura, no demo).
                if (serieRepository.findById("B001").isEmpty()) {
                        serieRepository.save(SerieComprobante.builder()
                                        .serie("B001").tipo(TipoComprobante.BOLETA).ultimoNumero(0).build());
                }
                if (serieRepository.findById("F001").isEmpty()) {
                        serieRepository.save(SerieComprobante.builder()
                                        .serie("F001").tipo(TipoComprobante.FACTURA).ultimoNumero(0).build());
                }

                // 3. El admin es genérico ("Administrador"): renombra instalaciones
                // viejas con nombre personal, sin tocar claves ni roles.
                renombrarAdmin(adminEmail);
                // Repara nombres genéricos de instalaciones viejas (sin tocar claves).
                repararNombre("mecanico1@sanmartin.pe", "Luis Alberto", "Ramírez Torres",
                                TipoDocumento.DNI, "40258963", "962458713", Especialidad.MOTOR);
                repararNombre("almacen@sanmartin.pe", "Pedro Pablo", "Quispe Mendoza",
                                TipoDocumento.DNI, "41369725", "945213698", null);
                repararNombre("recepcionista@sanmartin.pe", "Rosa Elena", "Chávez Paredes",
                                TipoDocumento.DNI, "43852196", "956321478", null);

                // 4. Demo: solo con bandera (en producción, false).
                if (seedDemo && clienteRepository.count() == 0) {
                        sembrarDemo();
                }
        }

        private void sembrarDemo() {
                log.info("Insertando datos semilla de demostración...");
                crearSiFalta("mecanico1@sanmartin.pe", "Luis Alberto", "Ramírez Torres",
                                TipoDocumento.DNI, "40258963", "962458713", "MECANICO",
                                Especialidad.MOTOR, passMecanico);
                crearSiFalta("jcastillo@sanmartin.pe", "Jorge Enrique", "Castillo Vargas",
                                TipoDocumento.DNI, "44258963", "973652148", "MECANICO",
                                Especialidad.FRENOS_SUSPENSION, passMecanico);
                crearSiFalta("mdelgado@sanmartin.pe", "Marco Antonio", "Delgado Ruiz",
                                TipoDocumento.DNI, "45521478", "984715236", "MECANICO",
                                Especialidad.ELECTRICIDAD, passMecanico);
                crearSiFalta("almacen@sanmartin.pe", "Pedro Pablo", "Quispe Mendoza",
                                TipoDocumento.DNI, "41369725", "945213698", "ALMACENERO",
                                null, passAlmacen);
                crearSiFalta("recepcionista@sanmartin.pe", "Rosa Elena", "Chávez Paredes",
                                TipoDocumento.DNI, "43852196", "956321478", "RECEPCIONISTA",
                                null, passRecepcionista);

                Cliente c1 = clienteRepository.save(Cliente.builder()
                                .nombre("Juan Pérez").telefono("951234567")
                                .email("juan.perez@gmail.com").documento("45123698")
                                .tipoDocumento(TipoDocumento.DNI)
                                .genero(Genero.MASCULINO)
                                .direccion("Jr. Los Pinos 245, San Martín de Porres, Lima").build());
                Cliente c2 = clienteRepository.save(Cliente.builder()
                                .nombre("María López").telefono("962345678")
                                .email("maria.lopez@hotmail.com").documento("40258741")
                                .tipoDocumento(TipoDocumento.DNI)
                                .genero(Genero.FEMENINO)
                                .direccion("Av. Las Palmeras 123, Los Olivos, Lima").build());
                Cliente c3 = clienteRepository.save(Cliente.builder()
                                .nombre("Carlos García").telefono("973456789")
                                .email("carlos.garcia@yahoo.com").documento("41369852")
                                .tipoDocumento(TipoDocumento.DNI)
                                .genero(Genero.MASCULINO)
                                .direccion("Av. Las Palmeras 123, Los Olivos, Lima").build());
                // Empresas de prueba con RUC válido (ficticios)
                Cliente c4 = clienteRepository.save(Cliente.builder()
                                .nombre("Repuestos El Sol S.A.C.").telefono("014251478")
                                .email("contacto@elsol.pe").documento("20123456786")
                                .tipoDocumento(TipoDocumento.RUC)
                                .razonSocial("Repuestos El Sol S.A.C.")
                                .direccion("Av. Los Rosales 450, San Martín de Porres, Lima").build());
                Cliente c5 = clienteRepository.save(Cliente.builder()
                                .nombre("Juan Carlos Pérez Núñez").telefono("987452136")
                                .email("jcperez@gmail.com").documento("10123456781")
                                .tipoDocumento(TipoDocumento.RUC)
                                .razonSocial("JUAN CARLOS PEREZ NUÑEZ")
                                .direccion("Jr. Las Flores 123, Lima").build());

                // 3. Proveedores (demo)
                Proveedor p1 = proveedorRepository.save(Proveedor.builder()
                        .nombre("Repuestos El Sol S.A.C.").ruc("20123456786")
                        .telefono("014251478").email("ventas@elsol.pe")
                        .direccion("Av. Los Rosales 450, San Martín de Porres, Lima").build());
                Proveedor p2 = proveedorRepository.save(Proveedor.builder()
                        .nombre("Distribuidora Autopartes Lima S.A.C.").ruc("20456789123")
                        .telefono("015551234").email("ventas@autoparteslima.pe")
                        .direccion("Av. Industrial 123, Ate, Lima").build());
                Proveedor p3 = proveedorRepository.save(Proveedor.builder()
                        .nombre("Hidráulicos Andes S.A.C.").ruc("20345678901")
                        .telefono("016543210").email("compras@hidraulicosandes.pe")
                        .direccion("Av. Industrial 789, Lurín, Lima").build());

                vehiculoRepository.save(Vehiculo.builder()
                                .cliente(c1).placa("ABC-123").marca("Toyota")
                                .modelo("Corolla").anio(2020).build());
                vehiculoRepository.save(Vehiculo.builder()
                                .cliente(c2).placa("DEF-456").marca("Hyundai")
                                .modelo("Accent").anio(2019).build());
                vehiculoRepository.save(Vehiculo.builder()
                                .cliente(c3).placa("GHI-789").marca("Nissan")
                                .modelo("Sentra").anio(2021).build());
                vehiculoRepository.save(Vehiculo.builder()
                                .cliente(c4).placa("SOL-001").marca("Toyota")
                                .modelo("Hilux").anio(2022).build());

                servicioRepository.save(Servicio.builder().nombre("Cambio de aceite")
                                .precioBase(new BigDecimal("80.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Alineación y balanceo")
                                .precioBase(new BigDecimal("120.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Cambio de frenos")
                                .precioBase(new BigDecimal("200.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Diagnóstico computarizado")
                                .precioBase(new BigDecimal("150.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Reparación de motor")
                                .precioBase(new BigDecimal("800.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Cambio de correa de distribución")
                                .precioBase(new BigDecimal("350.00")).build());
                servicioRepository.save(Servicio.builder().nombre("Lavado técnico").precioBase(new BigDecimal("50.00"))
                                .build());
                servicioRepository.save(Servicio.builder().nombre("Revisión de suspensión")
                                .precioBase(new BigDecimal("100.00")).build());

                productoRepository.save(Producto.builder().nombre("Aceite 5W-30 4L").tipo("INSUMO")
                                .precioUnitario(new BigDecimal("65.00")).stockActual(50).stockMinimo(10)
                                .costoUnitario(new BigDecimal("45.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Filtro de aceite").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("25.00")).stockActual(40).stockMinimo(8)
                                .costoUnitario(new BigDecimal("15.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Filtro de aire").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("35.00")).stockActual(30).stockMinimo(8)
                                .costoUnitario(new BigDecimal("20.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Pastillas de freno delanteras").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("120.00")).stockActual(20).stockMinimo(5)
                                .costoUnitario(new BigDecimal("70.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Pastillas de freno traseras").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("100.00")).stockActual(15).stockMinimo(5)
                                .costoUnitario(new BigDecimal("60.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Disco de freno").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("180.00")).stockActual(10).stockMinimo(3)
                                .costoUnitario(new BigDecimal("110.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Correa de distribución").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("90.00")).stockActual(8).stockMinimo(3)
                                .costoUnitario(new BigDecimal("55.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Líquido de frenos 500ml").tipo("INSUMO")
                                .precioUnitario(new BigDecimal("30.00")).stockActual(25).stockMinimo(5)
                                .costoUnitario(new BigDecimal("18.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Refrigerante 1L").tipo("INSUMO")
                                .precioUnitario(new BigDecimal("20.00")).stockActual(30).stockMinimo(8)
                                .costoUnitario(new BigDecimal("12.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Bujias (juego 4)").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("45.00")).stockActual(20).stockMinimo(5)
                                .costoUnitario(new BigDecimal("28.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Amortiguador delantero").tipo("REPUESTO")
                                .precioUnitario(new BigDecimal("250.00")).stockActual(6).stockMinimo(2)
                                .costoUnitario(new BigDecimal("150.00")).unidadMedida("NIU").build());
                productoRepository.save(Producto.builder().nombre("Aceite de transmisión ATF").tipo("INSUMO")
                                .precioUnitario(new BigDecimal("55.00")).stockActual(15).stockMinimo(5)
                                .costoUnitario(new BigDecimal("35.00")).unidadMedida("NIU").build());

                log.info("✅ Datos semilla insertados correctamente");
                log.info("   Clientes: 5 (3 DNI + 2 RUC) | Vehículos: 4");
                log.info("   Servicios: 8 | Productos: 12");
        }

        private void crearSiFalta(String email, String nombres, String apellidos,
                                  TipoDocumento tipo, String documento, String telefono,
                                  String rol, Especialidad especialidad, String clave) {
                if (usuarioRepository.findByEmail(email).isPresent()) return;
                usuarioRepository.save(Usuario.builder()
                                .nombre((nombres + " " + apellidos).trim())
                                .nombres(nombres).apellidos(apellidos)
                                .tipoDocumento(tipo).documento(documento)
                                .telefono(telefono).email(email)
                                .passwordHash(passwordEncoder.encode(clave))
                                .rol(rol).activo(true).especialidad(especialidad).build());
        }

        /** El admin de arranque es genérico: renombra instalaciones viejas sin tocar clave ni rol. */
        private void renombrarAdmin(String email) {
                usuarioRepository.findByEmail(email).ifPresent(u -> {
                        if ("Administrador".equals(u.getNombre()) && "Administrador".equals(u.getNombres())) return;
                        u.setNombre("Administrador");
                        u.setNombres("Administrador");
                        u.setApellidos(null);
                        usuarioRepository.save(u);
                        log.info("Admin renombrado a genérico ({})", email);
                });
        }

        /** Si el usuario aún tiene nombre genérico, lo actualiza sin tocar clave ni rol. */
        private void repararNombre(String email, String nombres, String apellidos,
                                   TipoDocumento tipo, String documento, String telefono,
                                   Especialidad especialidad) {
                usuarioRepository.findByEmail(email).ifPresent(u -> {
                        if (u.getNombres() != null && !u.getNombres().isBlank()) return;
                        u.setNombres(nombres);
                        u.setApellidos(apellidos);
                        u.setNombre((nombres + " " + apellidos).trim());
                        u.setTipoDocumento(tipo);
                        u.setDocumento(documento);
                        u.setTelefono(telefono);
                        if (especialidad != null && u.getEspecialidad() == null) {
                                u.setEspecialidad(especialidad);
                        }
                        usuarioRepository.save(u);
                        log.info("Nombre reparado para {}", email);
                });
        }
}
