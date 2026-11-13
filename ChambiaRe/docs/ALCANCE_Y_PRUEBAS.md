# Alcance y pruebas actuales

El diagnóstico v0.1 queda reemplazado por [la matriz actual](../IMPLEMENTATION_STATUS.md). No hay una APK verificada.

Pruebas JVM: `PriceLabelTest` (5) y `PublicationSnapshotTest` (2). Las siete pasan al ejecutarlas directamente con JUnit. Pruebas Android escritas: `OfflineFlowTest`, persistencia/aislamiento de dos negocios, 17 productos con nombres largos, páginas completas, bitmap y archivos PDF/JPG/PNG/TXT; no ejecutadas por bloqueo de build/ADB.

Antes de aceptar el primer corte, compilar con wrapper, ejecutar unitarias e instrumentadas y revisar en Android:

1. Crear fondita, añadir seis guisados precargados, definir precios/unidades; cerrar, reabrir y continuar.
2. Alternar fondita/taller y comprobar catálogo, borrador, plantilla, contraste e historial aislados.
3. Exportar TV y vertical con nombres largos y 17 artículos; abrir todas las imágenes y PDF, cotejar TXT y precios, revisar páginas y medidas Carta.
4. Cambiar de negocio/precio mientras se genera; comprobar snapshot y carpeta del negocio original.
5. Compartir todos los archivos con dos apps distintas y probar cancelación del selector de destino.
6. Borrar un archivo privado de una publicación de prueba; comprobar aviso y regeneración desde snapshot sin cambiar la versión anterior.
7. Guardar con SAF y dispositivo OTG; comprobar carpeta TV sin TXT, ACCESIBLE/menu.txt separado, tamaño de cada archivo y permisos/espacio/dispositivo ausente.
8. Probar TalkBack, escalado de texto, contraste, foco, rotación, regreso y tabletas.
9. Probar TTS instalado/no instalado, voz de red, escuchar/detener, límite de texto, errores y cabecera WAV válida. No aprobar segmentación/configuración porque aún faltan.
10. Migrar una base real v1 y v3; comprobar datos y validación Room. Archivar/reactivar y eliminar con ambas decisiones de archivos.

Variantes/comida corrida, compras y escenarios remotos no son pruebas aprobables todavía: su implementación está pendiente. Ver [modelo previsto](DATA_MODEL.md).
