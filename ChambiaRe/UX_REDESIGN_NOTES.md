# CHAMB-IA — Rediseño UX parcial (12 noviembre 2026)

Este paquete parte de `Chambia_CodigoFuente.zip`. No se alteraron las entidades Room, DAO, motor de exportación ni catálogos.

## Modificaciones implementadas
- Tema naranja comercial (`#C2410C`) y acento violeta (`#7C3AED`), fondo cálido y mayor contraste de botones.
- Barra inferior reemplazada por `BottomNavigationView` con iconos y etiquetas (sin botones horizontales de texto partidos).
- Inicio: muestra resumen compacto y vista previa; elimina la lista larga de artículos repetidos.
- Catálogo: filas compactas con selección, categoría/precio y acción de edición identificada; RecyclerView sin altura fija interna.
- Diseños: galería de dos columnas, selección y una sola vista previa grande del diseño activo.
- Se conserva la lógica de catálogo, borradores, accesibilidad y exportación existente.

## Alcance y limitaciones
- Primera iteración de interfaz, no rediseño completo de todas las pantallas. Preparación, exportación e historial siguen pendientes de revisión visual.
- Todavía se utiliza UI programática en varios componentes. Es conveniente migrar progresivamente a XML/ViewBinding, sin romper las operaciones existentes.
- Los tres diseños conservan su motor gráfico original: aquí cambia la galería, no la composición de las plantillas.
- No se ha ejecutado una compilación Gradle ni pruebas de dispositivo en este entorno: verificar `assembleDebug` y los flujos de navegación en Android Studio.
- No se incluyen cambios en AdMob, Google Play Billing ni backend.

## Pruebas sugeridas
1. Sincronizar Gradle y compilar `assembleDebug`.
2. Verificar navegación inferior en equipo físico con gestos y tamaño de letra grande.
3. Abrir el catálogo y seleccionar artículos; confirmar que el borrador persiste al cambiar de pestaña.
4. Cambiar diseño y verificar vista previa/exportación.
5. Alternar entre dos negocios y comprobar el aislamiento de datos.
6. Comprobar pantalla en tamaño pequeño y tablet.
