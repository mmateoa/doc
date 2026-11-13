# CHAMB-IA — Reporte de implementación UX/UI

Fecha: 12 de noviembre de 2026 (America/Mexico_City). Proyecto recibido: `ChambIA.zip`, directorio `ChambiaRe`, Android Java, paquete `mx.chambia.app`.

## 1. Resultado y límites de la entrega

Se implementaron cambios funcionales en los seis lotes A–F: shell XML y navegación Material; Inicio compacto; catálogo separado de la oferta; preparación de comida corrida con agregado persistente; tres composiciones de diseño y vista previa paginada; historial, exportación por destino y accesibilidad. No es una sustitución de colores ni un prototipo de pantallas. **La aceptación completa de todos los escenarios del documento sigue siendo parcial**: esta entrega no certifica TalkBack, USB físico, motores TTS reales, gestos de Android 15, ni todos los escenarios comerciales de los otros giros.

El ZIP de entrada no se modificó. Se conservaron identificadores de paquete, nombre de la base `chambia.db`, tablas heredadas, catálogo propio, exportaciones con carpetas UUID y referencias de publicaciones. No se añade backend, publicidad, monetización ni una dependencia de internet para editar o generar.

### Hallazgos de la copia original

- `WorkspaceFragment` construía cabecera, navegación y controles en Java, y sustituía el fondo de `MaterialButton` con un `GradientDrawable`. Eso elimina el manejo de fondo de Material y puede interferir con tintes/estados. Se eliminó esa combinación y se verificó texto visible y contraste; no se afirma haber reproducido en este entorno el fallo exacto de las capturas históricas.
- Catálogo exponía casillas de selección del día por defecto. Incorporar sugerencias o crear un producto también lo incluía automáticamente en el borrador. Se retiraron esos efectos.
- No existían `FondaMenuFragment` ni `FondaSections` en este ZIP. Se implementó su responsabilidad mediante `MealPlan` y `MealEditor`; no se asumió funcionalidad por nombres citados en otro documento.
- Tres plantillas compartían el mismo layout y cambiaban principalmente colores. Ahora hay menú mexicano con marco y flores, lista minimalista de una columna y composición editorial con tipografía serif y línea lateral.

## 2. Matriz requisito → implementación → evidencia

`Implementado` describe código y las comprobaciones indicadas, no certificación general de dispositivos. `Parcial` identifica una limitación concreta.

| Lote / requisito | Implementación y archivos principales | Evidencia | Estado |
|---|---|---|---|
| A · Botones y contraste | `values/styles.xml`, `color/button_*.xml`, tokens claros/oscuros; botones Material conservan su fondo administrado | `VisualContractTest`: contraste ≥4.5 para texto principal/secundario y primario en ambos temas; estados normal/pulsado/deshabilitado | Implementado |
| A · Navegación e insets | `fragment_workspace.xml`, `menu/root_navigation.xml`, vectores `ic_*`, `WorkspaceFragment`; cuatro destinos, barra oculta en tareas, Atrás y raíz sin acumular pestañas | Inflación XML en 320/393/800 dp, escalas 1/1.3/1.5/2; recorrido instrumentado y capturas | Parcial: falta dispositivo API 35 con gestos y teclado |
| B · Inicio | `fragment_home.xml`, `HomeFragment`, `PreviewSurface`: resumen comercial, miniatura acotada, continuar/vista previa y acciones reales | Capturas de Inicio; prueba de entrar a preparación y recuperar borrador | Implementado |
| B · Cambio de negocio | Selector compacto abre bottom sheet; crea/administra, selección persistente y borradores por negocio | Pruebas instrumentadas de separación y recuperación | Implementado; objetivo de dos toques no medido formalmente |
| C · Catálogo separado | `CatalogFragment`, `CatalogItemAdapter`, `row_catalog_item.xml`; selección solo al abrir “Agregar a esta publicación” | Pruebas de independencia catálogo/oferta y captura de filas sin casillas en administración | Implementado |
| C · Precarga | `dialog_catalog_suggestions.xml`, `WorkspaceDialogs`: búsqueda, categorías, selección múltiple, seleccionar resultados y contador; copia transaccional por negocio | Compilación y disponibilidad local de semillas; recorrido manual descrito abajo | Implementado; no se integra descarga remota |
| C · Precios/variantes | Editor preservado y recursos centralizados; precios del catálogo y del borrador independientes | `EditingRulesTest`, `PriceLabelTest`, pruebas de snapshots | Implementado; editor de variantes sigue siendo textual, no tabla por tamaño |
| D · Paquete real | `publication/MealPlan.java`, `ui/MealEditor.java`, `editor_meal.xml`, `row_meal_section.xml`: precio en centavos, revisión fija al pie, publicación sin precio explícita, seis secciones iniciales, secciones adicionales y a la carta, con nombre/orden/visibilidad/obligatoriedad/cardinalidad/política y IDs de opciones | `MealPlanTest` y pruebas de persistencia | Implementado |
| D · Instantáneas e inclusión | Agregado JSON versionado dentro de la fila Room del borrador, guardado atómico con sus artículos; copia en `businessJson` al generar; elementos incluidos ocultan su precio, ejecutivo con precio independiente conserva el suyo; suplemento es una política distinta | Tests de paquete $110 y ejecutivo $145, sin etiquetas administrativas en los materiales | Implementado |
| D · Modelo de secciones | Reordenar hacia arriba, ocultar/mostrar, requeridas/opcionales, incluido/precio propio/suplemento/no ofrecido; selección explícita por sección, agotados sin borrar catálogo | Validación y round trip de agregado | Implementado; también permite agregar secciones y un bloque a la carta; suplementos por opción incluida pendientes |
| D · Migraciones | Room 7→8 añade `drafts.mealJson`; 8→9 añade `publications.eventsJson`; cadena histórica 1→9 preservada | SQLite/Room JVM con esquemas 7 y 8; prueba instrumentada desde versión 1; archivo anterior conserva sus bytes | Implementado |
| D/E · Composición agrupada | `TemplateEngine`: misma medición de texto para preview, JPG/PNG y PDF vectorial; agrupación, repetición de encabezado al saltar columna/página, letras grandes con paginación | Android JVM: 0/1/5/20/40 artículos × tres plantillas × cuatro proporciones × dos modos; emulador: PDF abierto con `PdfRenderer` | Implementado para los casos probados |
| E · Galería | `fragment_designs.xml`, `row_template.xml`, `DesignsFragment`: RecyclerView de dos columnas, tres en tablet; Todos/Gratis/Favoritos, detalle real, aplicar y favorito por negocio | Capturas y compilación; miniaturas con el compositor real | Implementado; no hay plantilla fotográfica ni catálogo remoto/premium |
| E · Personalizar y preview | `fragment_preview.xml`, `dialog_template.xml`: formato, páginas, detalle ampliado, editar contenido, alto contraste, aplicar sin borrar borrador | Render Android y capturas | Parcial: ampliación en diálogo, sin zoom con gesto ni editor visual libre |
| F · Publicaciones | `fragment_history.xml`, `HistoryFragment`: En preparación/Generadas, abrir, duplicar a nuevo borrador, compartir, regenerar y eliminar referencia con confirmación; aviso si faltan archivos | Versiones originales no cambian al editar; pruebas de archivos inmutables | Implementado; miniaturas desde el snapshot histórico, sin consultar el borrador actual |
| F · Exportación guiada | `fragment_export.xml`, `ExportFragment`, `WorkspaceExports`: destino primero, formato recomendado, opciones avanzadas; JPG de TV numerados, PDF, TXT y Sharesheet existentes conservados | Archivos reales generados/abiertos en emulador y JVM | Implementado; resumen indica opciones/formato/destino, no estima aún el número exacto de archivos |
| F · Copia local/USB | SAF conservado; cuenta copias terminadas, informa fallo parcial y reintenta solo los archivos restantes; eventos de copia separados de abrir Sharesheet | Código compilado; no se sustituye por un éxito simulado | Parcial: no se ha probado memoria OTG ni fallo real por extracción/falta de espacio |
| F · Accesibilidad/TTS | `fragment_accessibility.xml`, guion agrupado, reproducir/detener, velocidad, selección de voz offline, síntesis real segmentada y validación RIFF/WAVE conservadas; TXT gratuito | Pruebas de guion y segmentación; revisión del manejo de voz no disponible | Parcial: sin motor instalado para verificar audio audible/archivo en este emulador; no hay lectura sencilla independiente |
| Arquitectura | ViewBinding y SavedStateHandle en pantallas y filas; ListAdapter/DiffUtil en catálogo, galería e historial; `WorkspaceDialogs` y `WorkspaceExports` separan operaciones de composición; repositorio serializado y WorkManager conservados | Compilación Java/Room y pruebas | Implementado; algunos formularios heredados continúan programáticos y el adaptador de seis secciones usa notificación completa |
| Adaptación | Margen/ancho de lectura acotado a 840dp en tablet y galería de tres columnas; controles XML adaptables | Inflación XML y capturas donde se enumeran | Parcial: no hay panel doble de catálogo/preview |

## 3. Comprobaciones ejecutadas

### Compilación por lote

JDK Temurin 17, Gradle wrapper 8.13, AGP 8.9.2, compile/target SDK 35, minSdk 24. El entorno inicial tenía JRE sin compilador y no tenía SDK; se prepararon herramientas locales. Las primeras descargas requirieron configurar el proxy y CA del entorno para Java; esos ajustes **no se incluyen como configuración del proyecto**. El `local.properties` de esta máquina no se entrega.

| Entrega | Comando Gradle efectivo | Resultado final de la etapa | Registro |
|---|---|---|---|
| A | `./gradlew assembleDebug testDebugUnitTest --console=plain --max-workers=2` | BUILD SUCCESSFUL; 15 pruebas JVM | `verification/ux/A-build.log` |
| B | Igual | BUILD SUCCESSFUL; 15 pruebas JVM | `verification/ux/B-build.log` |
| C | Igual | BUILD SUCCESSFUL; 15 pruebas JVM | `verification/ux/C-build.log` |
| D | Igual, con Robolectric/JSON agregados | BUILD SUCCESSFUL; 26 pruebas JVM | `verification/ux/D-build.log` |
| E | Igual | BUILD SUCCESSFUL; 26 pruebas JVM | `verification/ux/E-build.log` |
| F | Igual | BUILD SUCCESSFUL; 26 pruebas JVM | `verification/ux/F-build.log` |
| Regresión final | `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest --console=plain --max-workers=2` | BUILD SUCCESSFUL; 30 pruebas JVM | `verification/ux/final-build.log`, `TEST-*.xml` |

Las pruebas Android de JVM usan Robolectric 4.14.1, API 34 y gráficos nativos para imágenes. No se confunden con un dispositivo. `PdfDocument` no está implementado en esta versión de Robolectric; la prueba de PDF se trasladó al framework del emulador, sin un mock que fingiera un archivo correcto.

### Emulador

Android 9 / API 28 x86_64, emulador 35.2.10, render SwiftShader, CPU por software (sin KVM). Se instalaron los dos APK y se ejecutó:

```sh
adb install -r -t app-debug.apk
adb install -r -t app-debug-androidTest.apk
adb shell am instrument -w -r mx.chambia.app.test/androidx.test.runner.AndroidJUnitRunner
```

En esta máquina se transportaron `push`, `pm install` y `am instrument` directamente al servicio ADB local del emulador mediante un cliente Python: el servidor ADB estándar intentaba escribir en un HOME de solo lectura. Los APK y el runner son los reales. El registro incluye los estados y el resultado del runner, no una inferencia a partir de compilar.

**Resultado capturado: `OK (6 tests)`**, con pruebas de Room, catálogo/oferta, variantes/snapshots, generación JPG/PNG/PDF, PDF paginado de comida corrida en seis destinos, precio persistente al reabrir Activity y separación entre negocios. La primera captura del cliente de pruebas agotó su espera, pero Logcat confirmó 6/6; una segunda ejecución completa produjo el resultado explícito del runner. El emulador mostró un ANR de System UI durante el arranque sin aceleración; se esperó y se desactivaron animaciones para la ejecución estable. No se atribuye ese ANR al código de CHAMB-IA ni se usa este entorno para medir rendimiento.

La instalación del emulador usa únicamente datos ficticios de pruebas. Ninguna captura ni archivo de muestra se inyecta en el APK como dato de producción.

### Casos cubiertos y límites

- Paquete Rosita Sazón con entradas, segundos, fuertes, ejecutivo $145, bebida/postre configurados y paquete $110. La prueba de edición cambia el paquete a $120, reabre y comprueba el valor; por eso algunas capturas muestran $120.
- JPG/PNG decodificados y dimensiones comprobadas; PDF abierto por `PdfRenderer` en Android real; TXT sin “Precio por confirmar”.
- 0, 1, 5, 20 y 40 artículos con nombres largos, tres plantillas, TV/vertical/cuadrado/carta, normal/alto contraste.
- Migración desde esquema 1 en emulador; desde 7 y 8 mediante Room/SQLite en JVM. Catálogos, borradores, publicaciones y archivo anterior preservados.
- Contraste de tokens claros/oscuros y texto de botón distinto del fondo en los estados normal, presionado y deshabilitado. No equivale a una auditoría WCAG/TalkBack.
- Inflación de pantallas a 320/393/800dp y escalas 100/130/150/200%. Inflar no demuestra ausencia de recortes: se adjuntan capturas reales por separado.

## 4. Evidencia visual y de archivos

Las imágenes de `verification/ux/TV-*_menu.*`, `REDES-*` e `IMPRESIÓN-*` son salidas reales del compositor Android nativo de las pruebas JVM, con datos ficticios. Los PDF de muestra obtenidos del emulador se identifican por separado. No son maquetas dibujadas para aparentar un resultado.

Las capturas reales disponibles están en `verification/ux/screenshots/`; su nombre indica pantalla, ancho y escala. Incluyen el árbol de accesibilidad XML capturado cuando estuvo disponible. No existen capturas originales equivalentes tomadas en este entorno: la evidencia “antes” consiste en el ZIP intacto y el diagnóstico contra su código, sin inventar imágenes históricas.

## 5. Integridad y migraciones

- 7→8: `ALTER TABLE drafts ADD COLUMN mealJson TEXT NOT NULL DEFAULT '{}'`.
- 8→9: `ALTER TABLE publications ADD COLUMN eventsJson TEXT NOT NULL DEFAULT '[]'`.
- Las dos nuevas migraciones son aditivas. Se conserva la cadena anterior 1→7, incluida su conversión de borradores.
- `MealPlan` versión JSON 1 conserva orden, nombres, reglas y referencias a artículos del snapshot del borrador. Este agregado y `snapshotJson` se guardan en **la misma fila**, por lo que no hay escrituras separadas que dejen medio paquete.
- `{}` conserva un borrador antiguo como oferta normal: no activa comida corrida ni inventa un precio. El usuario habilita el nuevo modo explícitamente.
- `business.mealJson` es `@Ignore` en Room y solo transporta la composición del borrador o snapshot a preview/export. Los snapshots de publicaciones incluyen su propia copia. Regenerar usa esa copia, no los precios actuales del catálogo.
- Compartir abre una aplicación externa; el evento se llama `SHARE_SHEET_OPENED`, **no** “enviado” o “compartido con éxito”. `COPIED_LOCAL`, `COPIED_USB` y `COPY_PARTIAL` se registran por separado tras las operaciones de copia.
- No se llama `fallbackToDestructiveMigration`, no se cambia `chambia.db`, ni se recorren/borran carpetas exportadas durante una actualización.

## 6. Pendientes con siguiente acción

| Prioridad | Pendiente / alcance exacto | Siguiente acción |
|---|---|---|
| P0 verificación | API 35/gestos, teclado real, TalkBack, foco y regreso de segundo plano en teléfono | Ejecutar matriz manual en dispositivo; comprobar insets, lectura y controles con fuentes grandes |
| P1 verificación | USB OTG y errores de espacio/permiso/extracción; Sharesheet con aplicaciones receptoras reales | Copiar a un volumen compatible, desconectar durante copia y verificar recuperación sin duplicar archivos ya terminados |
| P1 verificación | Voz TTS offline y archivo WAV audible | Instalar voz es-MX, probar escuchar/detener/síntesis y motor sin soporte; verificar bytes y reproducción |
| P1 UX | Reordenar opciones con interacción dedicada | Añadir interacción directa de orden; el estado de scroll, página y destino usa SavedStateHandle y búsqueda/categoría se recuerdan por negocio |
| P1 UX | Suplementos específicos por guisado incluido | Añadir precio extra por opción incluida; ya existen secciones adicionales y a la carta con precio independiente |
| P1 UX | Editor visual de variantes por tamaño, onboarding en pasos  | Sustituir formularios heredados conservando sus validadores y snapshots |
| P1 UX | Lectura sencilla independiente y pronunciación de montos en palabras en TXT | Añadir transformación de guion revisable; hoy TXT estructurado y TTS usan montos legibles por el motor, sin prometer PDF/UA |
| P2 | Catálogo/preview en doble panel tablet, zoom por gesto y animaciones refinadas | Validar layout de dos paneles y estados de navegación |
| Fuera de esta revisión | Descargas GCS, premium/Billing, anuncios/Firebase, fotografías/licencias y backend | Integraciones de producto separadas; no se añaden botones de compra o descarga ficticia |

No se afirma completado el escenario integral de pizzería/tortillería/taller/tianguis: se preservaron sus catálogos y modos de precio y se probaron reglas de variantes/“desde”/cotización; faltan recorridos de extremo a extremo por cada giro y dispositivos representativos.

## 7. Riesgos y uso de la entrega

- El APK de revisión está firmado con la clave debug de este entorno. **Para conservar una instalación existente, compila el código con tu firma habitual; no desinstales la app para resolver un conflicto de firma.** Una copia del proyecto no es un respaldo de la base privada del teléfono.
- Room 9 no se puede abrir con el APK antiguo sin una estrategia explícita de downgrade. Conserva respaldo antes de actualizar una instalación con datos importantes.
- El motor rechaza un solo artículo/cabecera tan largo que no cabe ni en una página; no lo recorta silenciosamente. El editor debe abreviar esos casos extremos.
- TTS depende del motor instalado. USB depende de Android/OTG y permisos. Las copias externas que el usuario haya borrado no se pueden recuperar, pero las instantáneas permiten regenerar.
- La salida PDF conserva texto y vectores del compositor; no se certifica PDF/UA, CMYK o PDF/X.
- Algunas etiquetas de dominio y formularios heredados conservan español en Java por compatibilidad; queda pendiente completar recursos en inglés y sustituir identificadores persistidos por códigos independientes del idioma.

## 8. Cómo revisar en Android Studio

1. Conserva una copia del proyecto actual. Abre el directorio `ChambiaRe` de esta entrega y deja que Android Studio genere su `local.properties`; usa JDK 17 y SDK 35.
2. Ejecuta `gradlew.bat assembleDebug testDebugUnitTest` y, con emulador/dispositivo disponible, `gradlew.bat connectedDebugAndroidTest`.
3. Crea una fondita → Catálogo → Explorar catálogo → selecciona sugerencias → incorporar. El borrador permanece vacío hasta elegir qué ofrecer.
4. Hoy → Continuar preparando → Elegir productos. Activa “Preparar como comida corrida”, define $110 y asigna opciones a cada sección. Para ejecutivo activa su sección, política con cargo adicional, y edita su precio en la oferta ($145).
5. Revisa menú → Diseños → abre una miniatura → favorito/aplicar. Cambia entre TV, WhatsApp, cuadrado e impresión. Comprueba páginas y genera materiales.
6. Guarda en una carpeta del celular o abre la hoja de compartir. Revisa En preparación/Generadas, duplica sin modificar el original y prueba regenerar si falta un archivo.
7. Cambia de negocio y vuelve para verificar tus borradores. Prueba TTS/USB únicamente con las capacidades disponibles del dispositivo.

Archivos cambiados: [UX_CHANGED_FILES.md](UX_CHANGED_FILES.md). Esquemas Room: `app/schemas/mx.chambia.app.data.AppDatabase/{7,8,9}.json`. Código y pruebas incluidos en el proyecto.

SHA-256 del ZIP original conservado: `267e2a30485260e9fa0181437b79efafa7690881cf5286937800dc40d0e53c28`.

## 9. Regresión final después de los últimos ajustes

La compilación final (`assembleDebug assembleDebugAndroidTest testDebugUnitTest`) terminó con **BUILD SUCCESSFUL y 30 pruebas JVM sin fallos**. La prueba de dominio adicional verifica que a la carta pueda coexistir con comida corrida y conservar un precio independiente. Se conserva también el eslogan del negocio en el pie del compositor.

Después de corregir el estado de página se ejecutó la clase `RedesignFlowTest` completa:

```sh
adb shell am instrument -w -r -e class mx.chambia.app.RedesignFlowTest mx.chambia.app.test/androidx.test.runner.AndroidJUnitRunner
```

**Resultado: `OK (3 tests)`, 67.782 segundos.** Registro: `verification/ux/instrumentation-final3.log`.

- PDF real de comida corrida en TV, vertical, cuadrado, carta, A4 y oficio, abierto por PdfRenderer; archivos anteriores conservan sus bytes al editar el contenido.
- Precio autoguardado, cierre/reapertura de Activity y cambio de negocio sin mezclar datos.
- Página 2 de vista previa conservada después de recrear Activity. Se corrigió el guardado inmediato de página/destino y el test consulta el control visible, en vez de retener una referencia que pudiera haber sido reemplazada durante la carga inicial.

No se ocultan los intentos anteriores: una ejecución de 7 pruebas se abortó por caída de `system_server`, precedida de SIGSEGV en el servicio de radio `rild` y error de propiedad en GnssLocationProvider (`instrumentation-aborted7.log`, `emulator-radio-crash.log`). Se desactivó el servicio de radio en este emulador de pruebas y la última clase terminó sin fallos. Otra ejecución de tres casos descubrió la incidencia de página antes de la corrección (`rotation-failure-before-fix.log`). No se cuentan ninguno de esos intentos como una aprobación completa.

Los seis casos instrumentados de la regresión anterior habían pasado completos; tras los últimos ajustes se volvió a ejecutar la clase de tres casos afectada. No se afirma que los siete hayan pasado juntos en el APK final.

Los PDF de `verification/ux/device-files/` provienen del framework del emulador; `menu.txt` es el texto de la misma oferta. Algunas muestras llevan “Otro nombre” porque la prueba cambia ese dato para comprobar que no modifica el PDF generado antes. No se distribuyen estos datos dentro del APK de producción.

La revisión de fuentes grandes y tablets conserva únicamente capturas con contenido visible. Capturas de pantalla de carga vacía se descartaron. La aprobación visual en Android 15 con gestos, TalkBack y USB/TTS reales sigue pendiente tal como indica la matriz.

### Inventario de capturas entregadas

- `verification/ux/screenshots/catalog-393-font130.png`
- `verification/ux/screenshots/designs-393-font130.png`
- `verification/ux/screenshots/history-393-font130.png`
- `verification/ux/screenshots/home-320-font130.png`
- `verification/ux/screenshots/home-393-font130.png`
- `verification/ux/screenshots/meal-393-font130.png`

Las capturas a 320dp solo se incluyen si el árbol de UI confirmó la pantalla renderizada. No se completó la matriz visual de tablet, 411dp, 200%, modo oscuro real y navegación gestual; permanece pendiente. La inflación XML y los tests de contraste no sustituyen esa revisión.

### Entrega

La ruta de salida configurada `/codex/browser/projectless/.../output` no pudo crearse por permisos del entorno. Se preparó la descarga en `/workspace/output` como alternativa. El proyecto de código no incluye cachés, SDK/JDK, `local.properties`, bases privadas ni claves de firma.

### Ajuste visual final en teléfono estrecho

La inspección de 320dp / fuente 130% encontró el CTA parcialmente fuera del área visible. Se redujo únicamente la miniatura de Inicio a 64dp para anchos de hasta 360dp, conservando tamaño de texto. Después de este ajuste se volvió a compilar y ejecutar la suite JVM: **30/30 sin fallos**. En el emulador se reinstaló ese APK y se capturó el árbol UI de la pantalla renderizada: botón `[32,538][288,611]`, navegación desde `y=648`. El botón tiene 73dp de altura y queda completamente sobre la barra. Evidencias: `screenshots/home-320-font130.png`, `.xml` y `compact-bounds.json`; registro `compact-check.log`.

Las tres pruebas instrumentadas funcionales corresponden a la revisión inmediatamente anterior a este ajuste de altura. No se repitieron tras modificar únicamente la miniatura: se verificó la rama de 320dp con captura y límites reales de los controles, y la compilación/pruebas JVM del código final.

## Edicion publica para GitHub

Esta descarga conserva codigo, recursos, esquemas, pruebas, capturas y muestras ficticias. Se omiten los diagnosticos brutos y los XML de resultados de maquina para no publicar rutas locales, nombres de usuario ni identificadores del entorno. Los resultados de pruebas se resumen en este reporte. Los registros completos permanecen en el entorno privado de trabajo; sus referencias historicas aqui no implican que esos archivos esten incluidos en la descarga publica.

Descarga: https://github.com/mmateoa/doc/raw/refs/heads/master/entregas/ChambIA_UX_UI.zip
