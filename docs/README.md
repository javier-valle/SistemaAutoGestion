# Documentación — AutoGestión

Esta carpeta reúne la documentación de apoyo del sistema y el informe del Avance de
Proyecto Final 3. Las capturas, diagramas y registros que lo respaldan están en
`evidencia/`.

| Documento | Para qué sirve | A quién está dirigido |
| :-- | :-- | :-- |
| [GUIA_DESARROLLO.md](GUIA_DESARROLLO.md) | Arquitectura, capas, decisiones técnicas y cómo extender el código | Quien mantiene el sistema |
| [MANUAL_USUARIO.md](MANUAL_USUARIO.md) | Manual vigente: qué es el sistema, cómo entrar y qué hace cada rol | Personal del taller |
| [Manual_Usuario_AutoGestion.docx](Manual_Usuario_AutoGestion.docx) | El mismo manual en Word, con portada, índice, tablas e imágenes para imprimir o entregar | Personal del taller y entrega académica |
| [Manual_Usuario_AutoGestion.pdf](Manual_Usuario_AutoGestion.pdf) | Versión PDF del mismo manual, para enviar o imprimir sin depender de Word | Personal del taller y entrega académica |
| [MANUAL_USUARIO_extendido.md](MANUAL_USUARIO_extendido.md) | Versión ampliada anterior (reglas de negocio, solución de problemas, FAQ y glosario). Se conserva como referencia; no se exporta | Desarrollo |
| [permisos.md](permisos.md) | Matriz de permisos por rol y dónde se aplican | Desarrollo y auditoría |
| [politicas-README.md](politicas-README.md) | Marco legal de datos personales y estado técnico | Administración del taller |

El `.docx` se genera desde `MANUAL_USUARIO.md` y el `.pdf` se exporta desde ese mismo `.docx`
con Word; ninguno se edita a mano, para que el `.md`, el `.docx` y el `.pdf` no se
contradigan. Las capturas que usa el manual están en `assets/capturas/`.

## Evidencia del informe

`docs/evidencia/` guarda las capturas y los registros que respaldan el Avance de Proyecto
Final 3: Javadoc, salida de la suite de pruebas, diagramas (modelo de datos, arquitectura,
cronograma, flujo del proceso), capturas de pantalla y evidencia del repositorio en GitHub.
Se versiona en Git a propósito: es parte del entregable, no material temporal.
