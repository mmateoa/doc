# CHAMB-IA — Especificación maestra de producto y desarrollo Android

**Lema:** Dale vía a tus chambitas.  
**Estado:** Requisitos consolidados para implementación; no implica que estén desarrollados.  
**Fecha de consolidación:** 11 de noviembre de 2026.  
**Aplicación:** Android nativa, Java. **Package actual:** `mx.chambia.app` (confirmar antes de publicar).  
**Proyecto heredado:** `ChambIA_Android_Java_v0.1.zip` (base de referencia, no versión de producción).

> **Objetivo de este documento:** Ser la fuente de verdad para Codex. Implementar lo indicado, verificar las funciones existentes antes de reutilizarlas y no declarar completado nada que no haya sido compilado, probado y evidenciado.

## 1. Visión y límites

CHAMB-IA permite a personas que tienen uno o varios pequeños negocios, trabajan por cuenta propia o realizan actividades comerciales informales **preparar información de productos y servicios, generar materiales visuales profesionales y distribuirlos** por WhatsApp, otras apps, almacenamiento del celular, USB para televisiones, tabletas e impresión (incluidas lonas, con limitaciones de imprenta claramente explicadas).

Usuarios objetivo: fonditas, pizzerías, taquerías, tortillerías, panaderías, cafeterías, comerciantes de tianguis, artesanos, talleres mecánicos, barberías, oficios y servicios a domicilio, entre otros. No exigir negocio formal, establecimiento físico ni cuenta en línea.

**Flujo esencial:** elegir negocio → seleccionar qué vender/ofrecer desde un catálogo precargado → ajustar contenido, variantes y precios → continuar preparando cuando se desee → revisar → aplicar plantilla → vista previa → generar materiales → guardar localmente / exportar a USB / compartir. También permitir un anuncio rápido sin catálogo.

**Fuera de alcance en la primera versión:** POS, pedidos online, facturación CFDI, contabilidad, inventarios complejos, gestión de empleados, agenda, CRM, pagos del cliente, red social, publicación automática en redes, app dedicada de Android TV, backend de sincronización de datos privados, IA generativa obligatoria, traducción automática, MP4, hosting público de menús y QR público funcional sin servicio web. No convertir el producto en un ERP ni en Canva.

## 2. Prioridades de entrega

- **P0 — Producto base utilizable:** proyecto compilable; arquitectura mantenible; onboarding; multinegocio; catálogo local precargado; catálogo propio; preparación con autoguardado; plantillas gratuitas renderizadas; vista previa; JPG/PNG/PDF; almacenamiento local; compartir; historial; texto accesible y accesibilidad de interfaz; pruebas del recorrido básico.
- **P1 — Lanzamiento ampliado:** variantes/unidades/paquetes (comida corrida, pizzas por tamaño); agotados; catálogo remoto versionado; USB real con SAF; imágenes de calidad; mejoras de impresión; TTS narrado a archivo; diseños premium + compras verificadas; AdMob con consentimiento; Analytics/Crashlytics; experiencia tablet.
- **P2 — Evolución explícitamente posterior:** MP4, modo escaparate permanente en tablet, QR a visor web/audio público, impresión profesional CMYK/PDF-X/PDF-UA certificada, subtítulos de video, traducción automática, IA generativa, programación remota de TV.

P0/P1 son prioridades de implementación, no permiso para omitir su diseño en el modelo de datos. Los criterios de seguridad, accesibilidad y privacidad aplican desde el inicio.

## 3. Tecnología y arquitectura

- Java como lenguaje principal; AndroidX, Material Components, Fragments y Navigation Component; MVVM con `ViewModel`/`LiveData` y repositorios; Room con migraciones explícitas; RecyclerView; WorkManager para tareas persistentes; Gson/Moshi para JSON; Canvas/Paint/TextPaint/StaticLayout para renderizar; `PdfDocument`; Storage Access Framework (SAF); `FileProvider` y Sharesheet; `TextToSpeech`; Google Mobile Ads, Google Play Billing y Firebase en sus etapas correspondientes.
- Usar versiones de SDK, AGP, Gradle y bibliotecas **compatibles entre sí y vigentes**, comprobadas al implementar; documentar requisitos. El proyecto heredado declara Android SDK 35, minSdk 24, Gradle 8.10.2/JDK 17, pero esas versiones no son intocables. No inventar garantías de compatibilidad.
- `MainActivity` será contenedor de navegación, no una clase monolítica para formularios, Room, exportación y TTS. Paquetes/capas sugeridos: `core`, `business`, `catalog`, `publication`, `template`, `render`, `export`, `accessibility`, `monetization`, `analytics`.
- **Offline-first:** edición, catálogos ya descargados, plantillas locales y exportación deben funcionar sin conectividad. La nube solo distribuye datos compartidos y verifica derechos cuando procede. No subir información privada del negocio sin acción/consentimiento explícitos.
- Optimizar para móviles económicos, tabletas y uso prolongado. Procesamiento pesado fuera del hilo UI, progreso y cancelación; controlar memoria en rasterización de gran formato.
- Cadena de pruebas por entrega: `assembleDebug`, pruebas unitarias, pruebas instrumentadas cuando el entorno lo permita y revisión manual en Android; si falta SDK/emulador/credenciales declarar con exactitud qué no pudo verificarse.

## 4. Modelo multinegocio

- Crear, editar, seleccionar, archivar, reactivar y eliminar negocios (eliminar con confirmación y decisión explícita sobre archivos). Recordar último negocio.
- Datos opcionales: nombre comercial, giro(s), modalidades (local fijo, ambulante/tianguis, domicilio, móvil, internet, temporal), logotipo, eslogan, teléfono/WhatsApp, dirección o zona de atención, redes, horario. Solicitar mínimos para iniciar.
- Cada negocio mantiene catálogo, precios, jornadas, publicaciones, borradores, configuraciones de diseño y accesibilidad, favoritos, historial, archivos; **aislamiento absoluto** entre negocios.
- Un negocio puede mezclar giros (p. ej., tortillería + tamales + café), y una persona puede gestionar distintos negocios en horarios diferentes.

## 5. Sectores y modalidades de contenido

Ejemplos iniciales de sector: fonditas/cocinas económicas, pizzerías, taquerías, tortillerías, cafeterías/panaderías, bebidas/postres, restaurantes, talleres mecánicos, vulcanizadoras/autolavados, barberías/estéticas, artesanos, comercio y tianguis, oficios y servicios a domicilio, otro. El giro no debe imponer flujos incompatibles.

Modalidades **combinables** por negocio: (1) menú/oferta del día, (2) carta o catálogo permanente, (3) lista de precios, (4) promoción/anuncio independiente, (5) cartel informativo. Jornadas opcionales para desayunos/comidas/cenas o ventas de diferentes horarios. Entrada directa para crear anuncio sin catálogo.

**Regla UX clave:** la cocinera decide durante la mañana qué preparar, agrega progresivamente y **solo al final decide publicar**. No forzar creación de un “menú terminado” al abrir la app.

## 6. Catálogo maestro en nube y catálogos locales

- Incluir un **catálogo esencial dentro del APK**; paquetes completos descargables desde Google Cloud Storage (archivos JSON por país/idioma/sector/región, con `manifest.json`, versión, tamaño, integridad y compatibilidad).
- Sectores con ejemplos comunes: guisados, guarniciones, bebidas; pizzas, tamaños y complementos; tortilla/masa/totopos por unidad; servicios de mantenimiento/frenos/diagnóstico; artesanías, ropa, etc.
- Permitir seleccionar múltiples productos precargados, filtrar/buscar, incorporar elementos de otros sectores, personalizar y crear elementos propios. El precio **nunca** debe tratarse como precio de mercado vigente: dejar vacío/por confirmar, o marcar los ejemplos inequívocamente.
- Al incorporar un ítem maestro, crear un ítem propio del negocio con referencia opcional al ID maestro, **sin sobrescribir** personalizaciones en futuras actualizaciones.
- No copiar sin permiso imágenes, textos protegidos ni fotografías comerciales de terceros. Fotos opcionales, propias o licenciadas, claramente ilustrativas cuando aplique.
- Gestión de descargas/errores/offline con caché y actualización optativa; no bloquear trabajo si falla la nube.

## 7. Catálogo de negocio y preparación

Campos por artículo: ID, negocio, tipo producto/servicio, nombre, descripción, categoría(s), fotografías opcionales, orden, unidad, modalidad de precio (fijo, desde, por kg/litro/pieza/orden/paquete, a cotizar, sin precio visible), variantes con precios independientes, estado disponible/agotado/no incluido/oculto, y etiquetas.

- Combos/paquetes con precio total e ítems/opciones; **comida corrida** con guisados elegibles, complementos, bebida y precio general; pizzas con tamaños y extras; servicios con precio “desde”/cotización.
- La **oferta de publicación** es distinta del catálogo completo: elegir elementos, ajustar orden/visibilidad y guardar borrador automáticamente. Mostrar estados y permitir ocultar o señalar agotados.
- Reutilizar menú de ayer, otro día o una publicación anterior. No alterar históricamente publicaciones ya generadas al cambiar un precio del catálogo: almacenar instantáneas/versiones de publicación.
- Publicación rápida: texto/promoción/horarios/contacto con o sin catálogo.

## 8. UX y navegación

**Marca visual:** `CHAMB-IA`, lema **“Dale vía a tus chambitas.”** No usar “Mi Vitrina” como nombre visible. La denominación no implica que la app ya utilice IA; no hacer afirmaciones engañosas de funciones IA.

Navegación propuesta: **Inicio**, **Catálogo**, **Publicaciones**, **Diseños**; selector de negocio en cabecera, ajustes aparte.

- **Inicio:** tarjeta visual del borrador o publicación vigente, progreso de selección, acciones claras Continuar preparación / Crear anuncio / Preparar publicación, accesos a catálogos y reutilización. Titular adaptado al sector: “¿Qué cocinaremos hoy?”, “¿Qué servicios anunciaremos?”, etc.
- **Catálogo:** categorías, búsqueda, chips de filtros, tarjetas editables con precio/unidad/estado; sección “Explorar catálogo precargado” y selección múltiple; edición rápida sin perder contexto.
- **Publicaciones:** borradores, historial por negocio/fecha/jornada, duplicar, regenerar, compartir; indicar si el archivo original ya no existe.
- **Diseños:** galerías visuales de plantillas gratuitas/premium/compradas, previsualización con datos reales y favorita; ventas no deben bloquear flujo básico.
- **Preparación:** permitir múltiples sesiones, borradores, resumen y revisión antes de publicar. **Vista previa** de formatos TV/redes/impresión, selección de plantilla y accesibilidad.
- **Exportar:** opciones **guardar en celular**, **USB** y **compartir**; destinos (TV, redes, impresión/lona) con formatos adecuados, sin pedir al usuario que comprenda resoluciones técnicas.
- No basta una UI de listas y botones. Diseñar un sistema visual coherente: componentes Material, tarjetas con jerarquía, iconos, previsualizaciones reales, buen contraste, estados vacíos/error, loading, interacciones táctiles cómodas, navegación de regreso y layouts especiales para tableta. Asegurar que UI y funcionalidades corresponden, no pantallas simuladas.

## 9. Motor de plantillas ligeras

- **JSON declarativo** (no HTML arbitrario ni código ejecutable) más recursos compartidos: figuras Canvas, SVG sanitizado/convertido por motor compatible, vectores, tipografías con licencia, texturas WebP cuando haga falta y fotografías opcionales independientes.
- Esquema versionado con `templateId`, `version`, `minRendererVersion`, layouts por destino, tokens de contenido, reglas de tipografía, colores, áreas, márgenes, paginación, componentes de encabezado/productos/precios/contacto, reglas de accesibilidad. Validar JSON, tamaños, referencias y ZIP; no ejecutar código remoto.
- Reproducir con **componentes reconstruidos** el menú mexicano aprobado: fondo crema/textura sutil, flores mexicanas vectoriales, encabezado vino, listón verde, lista en dos columnas, precios destacados y pie decorativo. La imagen aprobada es **referencia**, no se usará como fondo completo con texto fijo.
- Garantizar salida legible para nombres largos, 4/8/16+ productos, precios diferentes, ausencia de fotos, retrato/paisaje. Si no cabe, paginar o crear múltiples imágenes; no reducir textos indefinidamente ni cortar precios.
- Composición común para vista previa y exportaciones; la salida final no es una captura de pantalla.
- La plantilla comprada habilita los formatos que soporte; no vender el mismo diseño varias veces solo por cambiar orientación.

## 10. Exportación, formatos y almacenamiento

- **JPG/PNG:** horizontal TV 16:9 (Full HD inicial, 4K opcional), vertical para estados/redes, cuadrado para publicaciones, otros tamaños configurados.
- **PDF:** carta, A4, oficio cuando aplique, carteles y dimensiones personalizadas/lona; usar texto y vectores cuando sea viable. Para lonas, márgenes/sangrado configurables, aviso si fotos insuficientes, límites de memoria y aclaración de que PDF básico no garantiza CMYK/PDF-X ni conformidad profesional.
- **TV sin app:** una o varias imágenes numeradas para USB; carpeta `TV/` sin archivos ajenos. Algunas TVs no repiten imágenes o no aceptan formatos específicos: incluir guía breve, no prometer universalidad. MP4 se reserva para versión posterior.
- **Local:** carpeta por negocio, fecha, publicación y destino (`CELULAR/`, `TV/`, `IMPRESION/`, `ACCESIBLE/`), accesible mediante MediaStore o SAF según el caso. **USB:** permitir seleccionar volumen/directorio vía SAF si Android/OTG lo reconocen, informar sobre permisos, errores, espacio e integridad de escritura. No asumir que todos los móviles soportan OTG.
- **Compartir:** Sharesheet/FileProvider con URIs correctas, sin asumir presencia de WhatsApp; compartir varios archivos cuando plataforma lo permita.
- **Historial:** referencias a archivos y versiones, duplicación, regeneración si archivo externo fue borrado, y conservación de contenido editable incluso después de borrar exportaciones.

## 11. Accesibilidad para comerciantes y clientes (función gratuita)

- Los datos estructurados son fuente única de texto visual y narración; **no usar OCR** para leer el menú generado por la propia aplicación.
- Alternativas del cliente: imagen de alto contraste/letras grandes, **TXT estructurado legible por lector de pantalla**, narración de productos/precios y versión de lectura sencilla. El audio nunca reemplaza la versión escrita; imágenes JPG/PNG no son equivalentes a texto accesible.
- App Android accesible con TalkBack: etiquetas, orden de enfoque, tamaños de texto, contraste, botones, estados y mensajes. Usar WCAG 2.2 AA como orientación aplicable sin declarar conformidad sin auditoría.
- TTS mediante `android.speech.tts.TextToSpeech`, motores/voces instalados y detección de offline real; voz, idioma, velocidad, inclusiones y correcciones de pronunciación configurables por negocio. Generar guion natural con categorías, productos, montos leídos correctamente, variantes y disponibilidad; revisar guion antes de sintetizar.
- `synthesizeToFile` + `UtteranceProgressListener`; exportar **formato realmente producido por el motor** (p. ej., WAV/PCM cuando corresponda), nunca renombrar a MP3 sin codificador. Detectar ausencia de voz, errores, segmentación de textos largos y actualización de audio si cambió la publicación. Ofrecer reproducir, detener, compartir y guardar archivo.
- Añadir `ACCESIBLE/menu.txt`, versión de alto contraste y audio si disponible a la carpeta de publicación; conservar equivalencia textual para personas sordas. QR público que apunte a audio/texto solo en versión futura con hosting web y consentimiento.
- Idioma inicial español de México; cambiar voz no implica traducción. No exigir internet ni cobrar por accesibilidad esencial.

## 12. Monetización, privacidad y métricas

- **Freemium:** negocios múltiples, catálogos, plantillas básicas, JPG/PNG/PDF, guardar/compartir/USB y accesibilidad esencial gratis. Monetización de diseño mediante plantillas/paquetes premium como compras únicas de contenido digital en Google Play Billing; vista previa, adquisición, descarga y restauración con derecho validado; Cloud Run mínimo si se requiere validación server-side. Evitar confiar en `premium=true` local.
- AdMob **solo en pantallas administrativas móviles**, nunca incrustado en publicaciones exportadas ni sobre vistas de exposición/TV. Banners discretos; intersticiales si se habilitan, poco frecuentes y solo en transiciones naturales; no durante edición, accesibilidad, exportación ni compra. Usar test IDs durante desarrollo y gestión de consentimiento/privacy según regulación/plataforma.
- Firebase Analytics/Crashlytics en integración posterior sin enviar nombres, teléfonos, precios particulares ni información privada del negocio en eventos; medir uso agregado de catalogación, plantillas, generación, exportación y errores.
- Sin cuenta obligatoria, sin sincronización remota de negocios por defecto. Seguridad en secretos, JSON/ZIP, URIs, permisos, compradores y respaldos. Definir comportamiento de eliminación/archivo y política de privacidad.

## 13. Entidades y persistencia

Modelo sugerido (ajustar después de diseñar relaciones y migraciones): `Business`, `BusinessOperatingMode`, `MasterCatalog`, `MasterCatalogVersion`, `MasterItem`, `BusinessItem`, `ItemVariant`, `ItemUnit`, `Bundle`, `BundleComponent`, `Publication`, `PublicationItemSnapshot`, `PublicationVersion`, `Template`, `TemplateAsset`, `TemplateEntitlement`, `ExportRecord`, `DownloadRecord`, `NarrationSettings`, `NarrationScript`, `AccessibilityExport`, `BusinessSettings`.

- Room con FKs e índices para consultas por `businessId`, fecha, estado, catálogo. Migraciones explícitas, sin `fallbackToDestructiveMigration` en producción.
- Separar catálogo maestro, copia del negocio, contenido de publicación, plantilla visual y exportación.
- Autoguardado de borradores; restaurar selección/último negocio después de matar proceso o reiniciar. Historial inmutable de versiones exportadas.

## 14. Internacionalización

- Recursos visibles en `strings.xml`, inicialmente español e inglés, con catálogos y voces preferentemente `es-MX`; arquitectura preparada para país/moneda/idioma adicionales. Formatear precios, unidades y fechas por región. No traducir automáticamente platillos regionales sin revisión.

## 15. Escenarios mínimos verificables

1. **Fondita:** crear negocio, añadir 6 guisados precargados sin volver a escribirlos, fijar precios y comida corrida, trabajar por etapas, crear versión TV y WhatsApp, JPG, PDF, TXT y audio; marcar agotado y regenerar.
2. **Pizzería:** variaciones por tamaño, precios independientes, combo, carta multipágina, guardar y compartir.
3. **Tortillería:** precio por kilo/paquete, lista de precios clara, cartel impreso.
4. **Taller:** servicios precargados y precios “desde”/“a cotizar”, anuncio y PDF.
5. **Tianguis:** combinar catálogos, anuncio rápido, promoción vertical, cartel imprimible.
6. **Multinegocio:** alternar dos negocios y comprobar aislamiento de productos, plantillas, borradores e historial.
7. **Offline:** reiniciar sin internet y continuar borrador; renderizar, exportar y compartir con recursos instalados.
8. **USB:** exportar a dispositivo OTG compatible; verificar carpeta y archivos; error si no hay permiso/espacio/dispositivo.
9. **Accesibilidad:** TalkBack en flujos principales, texto accesible, TTS disponible/no disponible, audios obsoletos tras cambiar precios.
10. **Plantilla:** textos largos y muchos productos sin recortes, preview coherente con exportación, recursos faltantes y JSON corrupto manejados.
11. **Premium:** compra de prueba, derecho, descarga, offline posterior y restauración; no habilitar acceso editando preferencias.
12. **Ciclo de vida:** rotación, proceso terminado, reabrir, compartir y cancelar trabajos sin pérdida de datos.

## 16. Criterios de aceptación y definición de “terminado”

- Un usuario nuevo puede crear negocio, incorporar sugerencias, preparar una publicación, aplicar diseño y exportar JPG/PNG/PDF **reales** sin registrarse ni utilizar internet (catálogo local).
- Cambiar de negocio nunca mezcla datos. Al reabrir la app, los borradores siguen allí.
- PDF e imágenes existen, se abren, no recortan texto y respetan estructura. Archivos comparten URIs válidas; la UI informa errores verificablemente.
- Los diseños profesionales utilizan plantillas dinámicas, no una imagen de fondo con nombres/precios fijos.
- Accesibilidad gratuita operativa y no solo switches de interfaz.
- Código Java mantenible, sin Activity monolítica ni funciones falsas. El build debe compilar; documentar dependencias, configuraciones, pruebas y límites.
- No declarar integrados AdMob/Billing/Cloud/Firebase sin código funcional, credenciales/configuración y verificación real. Si no hay credenciales, entregar implementación preparada y señalar bloqueos claramente.

## 17. Diagnóstico de la base v0.1 heredada

El ZIP de referencia contiene principalmente `MainActivity.java`, `data/{AppDao,AppDatabase,Business,Item,Publication,SeedCatalog}`, `design/TemplateEngine.java`, `export/ExportManager.java`, siete JSON de catálogos, tres JSON de plantillas y recursos mínimos. **No fue compilado ni probado en la entrega inicial**, y su propio README reconoce que faltan nube, compras, AdMob, Firebase, paquetes/variantes, PDF profesional, recursos visuales avanzados y pruebas automatizadas. No asumir que la funcionalidad declarada realmente funciona sin comprobarla.

- **Conservar** piezas correctas de Room, plantillas, exportación y JSON, pero probarlas y migrarlas a arquitectura MVVM.
- **Refactorizar** `MainActivity` en componentes funcionales con navegación, Fragments, ViewModels y repositorios.
- **Rediseñar** visualmente la app antes de integrar monetización. La UX anterior fue considerada demasiado básica.
- **No rellenar** con clases vacías o archivos innecesarios para aparentar proyecto grande: entregar valor real y pruebas.

## 18. Instrucciones operativas para Codex

1. Leer este documento, el README y el código existente; hacer una **matriz requisito ↔ código ↔ estado** (`implementado`, `parcial`, `pendiente`, `bloqueado`) con evidencias, sin inventar.
2. Determinar si conviene refactorizar o reemplazar partes, conservando datos y compatibilidad en lo posible. No borrar trabajo válido sin motivo.
3. Confirmar/crear wrapper Gradle válido y compilar. Corregir errores de sincronización antes de avanzar. Documentar lo que dependa de la máquina local.
4. Crear propuesta UX visual de alta fidelidad para **Inicio, Catálogo, Preparación, Diseños, Vista previa, Accesibilidad y Exportación**, adaptable a negocio y tableta; conservar principios del flujo aprobado.
5. Implementar **vertical slices** completas, primero: multinegocio + catálogo precargado + preparación + plantilla mexicana + JPG/PNG/PDF + guardar/compartir. Luego variantes/comida corrida, USB, accesibilidad y remotos/compras/ads.
6. En cada corte: compilar, probar, indicar archivos modificados, mostrar evidencia (capturas cuando sea posible) y actualizar `IMPLEMENTATION_STATUS.md` con criterios comprobados/no comprobados.
7. Nunca reemplazar acciones reales con mensajes “simulado” sin etiquetar demo; no declarar producción si faltan permisos, credenciales, compras, pruebas o accesibilidad.
8. Mantener los requisitos de este documento; documentar decisiones y cualquier cambio de alcance. Priorizar calidad visual y fiabilidad por encima del número de pantallas.

### Primer encargo concreto a Codex

> Inspecciona el repositorio Android de CHAMB-IA y `CHAMBIA_REQUIREMENTS.md`. Compara exhaustivamente el código actual con esta especificación. Genera `IMPLEMENTATION_STATUS.md` y un plan de refactorización basado en entregas compilables. Empieza implementando una arquitectura Java MVVM con navegación, Room y UX profesional; entrega un flujo completamente funcional para la fondita (negocio → catálogo precargado → preparación progresiva → plantilla mexicana dinámica → vista previa → guardar/compartir JPG, PNG y PDF). Compila y prueba; reporta sin ambigüedades lo que realmente quedó verificado y lo que sigue pendiente. No borres las demás funcionalidades existentes sin revisión.

---

**Regla final:** CHAMB-IA existe para facilitar la comunicación comercial de las *chambitas* de sus usuarios. La interfaz debe ser accesible, moderna y directa, y los materiales generados deben ser atractivos, legibles y reutilizables. La complejidad interna nunca debe convertirse en complejidad para el comerciante.
