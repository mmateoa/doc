# CHAMB-IA — Especificación maestra de UX/UI para implementación con Codex

**Versión:** UX/UI 1.0 · 12 de noviembre de 2026  
**Proyecto:** Android nativo Java · paquete actual `mx.chambia.app`  
**Marca:** **CHAMB-IA** · **«Dale vía a tus chambitas»**  
**Naturaleza:** Especificación ejecutable para **modificar el proyecto existente**, no para crear un prototipo ni cambiar solamente la paleta.  
**Documentos relacionados:** `CHAMBIA_REQUIREMENTS.md`, `IMPLEMENTATION_STATUS.md`, `FONDA_MEXICO_UX_V03.md`. En caso de conflicto, este documento prevalece **únicamente en UX/UI y flujo de preparación**, no en requisitos de seguridad, integridad ni compatibilidad.

---

## 0. Instrucción principal para Codex

Lee este documento completo, inspecciona el código existente y sus recursos, genera un mapa de diferencias, **implementa cambios funcionales y visuales reales** de forma incremental y prueba cada entrega. No vuelvas a entregar una interfaz programática de botones grandes, ni una mera sustitución de colores. No sustituyas la app ni borres la base Room; conserva los datos instalados y las exportaciones existentes. Usa Java y AndroidX/Material Components. No declares nada terminado sin compilación, comprobación y evidencia. Si falta infraestructura para ejecutar pruebas, documenta con precisión qué se verificó y qué no.

**Prioridad de esta intervención:** (P0) navegación/legibilidad/contraste; (P0) separación catálogo y preparación; (P0) comida corrida mexicana con estructura comercial real; (P1) Inicio y galería de diseños; (P1) publicación/exportación/accesibilidad; (P2) refinamiento de tabletas, estados complejos y animaciones discretas.

## 1. Problema identificado en la versión actual

Basado en las capturas de uso y en la estructura del ZIP `ChambIA_Fonda_UX_v0.3.zip`:

- Inicio muestra un bloque de marca enorme, selector de negocio, mensajes repetidos, tarjeta y una vista previa que vuelven a mostrar el mismo contenido. Obliga a desplazarse para llegar a las acciones importantes.
- Hay **botones naranjas sin texto visible**; es un defecto P0 de legibilidad que debe investigarse en estilos/tints/textColor/estados. No se resuelve únicamente cambiando un color hexadecimal.
- Navegación inferior con iconos poco claros, etiquetas rotas o superpuestas; posible conflicto con `WindowInsets`, fuentes escaladas y barra de gestos.
- Catálogo presenta tarjetas de altura excesiva, botones «Editar catálogo» de ancho completo por producto y una selección que puede confundirse con el menú del día.
- Diseños presenta previsualizaciones apiladas a tamaño completo, con poca diferencia estructural entre plantillas.
- La fondita muestra doce guisados con «Precio por confirmar» como si fueran doce ofertas individuales, en vez de una **comida corrida** con precio de paquete y opciones por tiempo.
- Las pantallas actuales enfatizan la captura de datos, no el resultado: publicar una oferta atractiva por WhatsApp, TV, USB o papel.

**Resultado esperado:** una app reconocible como herramienta creativa/comercial, que permita a una cocinera preparar el día progresivamente, a una tortillería modificar tarifas y a un mecánico anunciar servicios, sin pantallas sobrecargadas.

## 2. Principios UX no negociables

1. **Primero la tarea, luego el diseño:** «¿Qué ofrecerás hoy?» antes de «¿Cómo se verá?».
2. **La preparación es un borrador:** no equivale a publicar ni modifica publicaciones antiguas.
3. **Un dato, múltiples salidas:** el mismo contenido produce anuncios, carteles, audio, imágenes y PDF.
4. **Sin captura repetitiva:** productos de catálogos sectoriales, favoritos, menús anteriores y precios por negocio.
5. **Decisiones reversibles:** cambiar producto o plantilla no borra información ni acciones anteriores; confirmación para acciones destructivas.
6. **Progreso visible:** indicar preparación, revisión, listo y última publicación, sin bloquear el trabajo por información opcional.
7. **Contenido y diseño separados:** los colores de CHAMB-IA no se transfieren a las piezas publicitarias salvo petición del usuario.
8. **Multinegocio real:** selector persistente y aislamiento de catálogos, borradores, plantillas favoritas y materiales.
9. **Sin obligar a conectarse:** funcionalidad principal local; catálogo básico integrado; descarga de otros paquetes cuando haya red.
10. **Inclusión desde el origen:** pantalla accesible para quien administra y materiales accesibles para sus clientes.
11. **Evitar fricción comercial:** plantillas gratuitas suficientes, compras opcionales, nunca interrumpir exportación o edición con anuncios.
12. **No simular prestaciones:** PDF, TTS, guardado y USB deben funcionar de verdad o aparecer explícitamente como pendientes.

## 3. Público y recorridos de referencia

| Persona / actividad | Qué necesita al abrir | Acción frecuente | Resultado |
|---|---|---|---|
| Cocinera de fonda | Decidir lo que preparará en diferentes momentos | Elegir opciones de comida corrida, precio de paquete y publicar al estar lista | Menú WhatsApp, TV, texto y audio |
| Dueño de pizzería | Mantener una carta relativamente estable | Cambiar precios por tamaño, combos y promoción | Carta y promoción |
| Tortillería | Precios por kg, paquete o pieza | Modificar tarifa y regenerar | Cartel legible para mostrador y TV |
| Taller mecánico | Servicios y precios desde / por cotizar | Seleccionar servicios y contacto | Anuncio / lista de servicios |
| Vendedor de tianguis | Productos variados por jornada | Reutilizar publicación y cambiar ubicación cuando corresponda | Cartel, WhatsApp, lona |
| Persona con dos negocios | Cambiar del negocio de mañana al de noche | Cambiar perfil sin cerrar app | Información separada |

**Tareas medibles para pruebas:** seleccionar un menú precargado, definir precio total, crear anuncio con un diseño favorito, compartir imagen, preparar una carpeta USB, generar y escuchar versión narrada, cambiar de negocio y volver al borrador.

## 4. Arquitectura de información y navegación

### 4.1 Cuatro destinos principales

Usar `BottomNavigationView` Material estándar con **iconos vectoriales inequívocos**, etiquetas legibles y una sola selección activa:

1. **Hoy** (casa/calendario del día): borrador y acciones inmediatas. «Inicio» es aceptable si ya existen cadenas; preferir «Hoy» en perfiles de jornada.
2. **Catálogo** (cuadrícula de productos/caja): inventario comercial reutilizable, **NO** selección del borrador por defecto.
3. **Publicaciones** (documentos superpuestos): borradores, publicados e historial/exportaciones.
4. **Diseños** (paleta/pincel): galería de plantillas y favoritas.

**No usar emojis como iconos de navegación final.** Usar VectorDrawable Material/ilustración vectorial propia con tamaño 24dp, coherencia de trazos, `contentDescription` y estados seleccionados. Barra de 72–80dp aprox. incluyendo insets cuando aplique; icono de 24dp, etiqueta de 11–12sp (mínimo legible con ampliación). Evitar texto dividido en «Catálog/o», solapamiento y recortes: probar a 320dp de ancho, 360dp y 411dp, fuente del sistema 1.0/1.3/1.5 y navegación gestual/3 botones. Si cuatro etiquetas no caben con texto ampliado, seguir patrones accesibles de NavigationBar en lugar de reducir fuente arbitrariamente.

### 4.2 Jerarquía de pantallas secundarias

- `Hoy` → **Preparar publicación** → **Revisar contenido** → **Vista previa** → **Exportar/Compartir** → **Resultado**.
- `Hoy` → **Crear anuncio rápido** (sin catálogo obligatorio) → **Vista previa** → **Exportar**.
- `Catálogo` → **Explorar catálogo precargado** → **Seleccionar varios** → **Incorporar** → **Editar producto**.
- `Publicaciones` → borradores/historial → abrir / duplicar / actualizar / compartir de nuevo.
- `Diseños` → galería → detalle de plantilla (una vista grande) → personalizar → aplicar.
- Cabecera → **Cambiar negocio** / Crear / Editar / Archivar.
- Vista previa → **Opciones accesibles** → guion / voz / versión alta legibilidad / texto.

La barra inferior solo aparece en destinos de primer nivel. Las pantallas de tarea usan barra superior con **Atrás, título y acción contextual**, sin apilar dos barras de navegación. Respetar el estado al volver.

### 4.3 Patrón de navegación

- `MainActivity` contiene `NavHostFragment` y navegación raíz; no múltiples pantallas construidas a mano dentro de una clase masiva.
- Navegación declarada en `res/navigation`, rutas con argumentos tipados o identificadores seguros.
- `OnBackPressedDispatcher`: volver un paso; si hay cambios guardados automáticamente, no mostrar aviso engañoso de pérdida; antes de salir de flujos no persistentes, confirmar.
- Preservar scroll y filtros al regresar; no recrear toda la vista ni perder el texto tecleado.
- Cambiar negocio lleva al **Hoy** del nuevo negocio, conservando el estado del anterior.
- No ubicar acciones de «Agregar negocio», «Nuevo anuncio» y «Editar catálogo» como tres FAB simultáneos.

## 5. Sistema visual de CHAMB-IA

### 5.1 Paleta semántica (clara)

| Token semántico | Hex | Uso |
|---|---|---|
| `brandPrimary` | `#C2410C` | CTA principal, marca, selección importante |
| `brandPrimaryPressed` | `#9A3412` | Pulsado / variación fuerte |
| `brandPrimarySoft` | `#FFF0E6` | Selecciones suaves / estados activos |
| `brandSecondary` | `#7C3AED` | Diseño creativo, premium y acentos selectivos |
| `brandSecondarySoft` | `#F3E8FF` | Etiqueta premium y superficie secundaria |
| `accentPromotion` | `#FACC15` | Descuentos/promociones, **nunca** texto blanco encima |
| `background` | `#FFFBF7` | Fondo cálido sin saturación |
| `surface` | `#FFFFFF` | Tarjetas y diálogos |
| `surfaceMuted` | `#F3F4F6` | Campos y secciones secundarias |
| `textPrimary` | `#1F2937` | Texto principal |
| `textSecondary` | `#4B5563` | Ayuda y metadatos |
| `divider` | `#E5E7EB` | Líneas discretas |
| `success` | `#166534` | Exportación completa/estado listo |
| `warning` | `#92400E` | Falta precio, revisión |
| `error` | `#B91C1C` | Fallos, eliminación |

**Regla de contraste:** para texto normal objetivo WCAG AA ≥4.5:1, texto grande ≥3:1, componentes no textuales ≥3:1 donde corresponda. Confirmar los pares en código y pruebas; no asumir que naranja claro `#F97316` con texto blanco pasa. El CTA base usa **`#C2410C` con blanco** después de validación. Nunca usar texto naranja oscuro sobre naranja oscuro ni asignar `textColor` de acuerdo con un atributo incompleto de `MaterialButton`.

**Modo oscuro:** definir una paleta semántica `values-night` propia (no invertir directamente colores del anuncio). Usar `colorOnPrimary`, `colorOnSurface`, `colorSurface`, `colorOutline`, `colorError`; preservar las plantillas comerciales como son.

### 5.2 Tipografía

- Usar **Roboto** del sistema (o familia de fuente con licencia y soporte de español), sin descargar fuentes nuevas para la UI del MVP. `sans-serif`/Roboto y `sans-serif-medium` son suficientes.
- Escala sugerida: título de pantalla 24sp semibold, encabezado de sección 18sp semibold, tarjeta principal 18–20sp, cuerpo 16sp, secundaria 14sp, etiqueta/complementaria 12–13sp. Precio destacado 18–22sp cuando aplique.
- Evitar encabezados de 32–36sp en cascada («CHAMB-IA», «Hola», «Tu negocio...») ocupando media pantalla. Marca compacta en barra; el contenido es protagonista.
- Respetar `fontScale` y `sp`; evitar limitar texto a una línea sin un patrón alternativo accesible. Preferir truncamiento solo de metadatos secundarios.
- Números alineados y moneda en formato local; usar `NumberFormat` y no concatenar indiscriminadamente `$`.

### 5.3 Espaciado, tamaños y esquinas

- Unidad de espaciado de 4dp; paddings frecuentes 8/12/16/24dp.
- Margen lateral de contenido 16dp (20–24dp en tablet).
- Separación entre secciones 20–24dp; entre filas 8dp.
- Tarjeta estándar radio 12–16dp, sombra sutil o borde de 1dp, **no ambos exagerados**.
- Botón principal alto 48–52dp, radio 12dp. Controles táctiles objetivo ≥48×48dp.
- Filas de catálogo 64–80dp si cabe nombre y precio; **no tarjetas de 160–200dp para un solo producto**.
- Botón de icono contextual 48×48dp; no convertir cada acción de edición en un botón ancho.
- FAB solo para acción contextual evidente cuando sea necesario, sin tapar contenido ni controles.

### 5.4 Componentes reutilizables

Crear componentes XML + ViewBinding (y clases Java reutilizables) para:

- `BusinessSwitcher` compacto (avatar/icono giro + nombre + chevron).
- `StatusChip`: borrador, listo, publicado, agotado, pendiente de precio, premium.
- `PublicationSummaryCard`: tipo, estado, fecha/turno, elementos, miniatura y CTA.
- `CatalogItemRow`: selección opcional según contexto, nombre, categoría, precio/etiqueta, editar.
- `SectionChoiceCard`: encabezado por tiempo, contador y chips de platillos elegidos.
- `TemplateThumbnailCard`: miniatura, nombre, chip gratis/premium, seleccionado/favorito.
- `PreviewSurface`: contenedor proporcional 16:9/9:16/carta.
- `EmptyStateView`: ilustración ligera + texto + acción.
- `Loading/Error/OfflineNotice`: mensajes sobrios y accionables.
- `AccessibilityOptions`: texto/audio/contraste sin duplicar el menú.

Usar `RecyclerView` + `ListAdapter`/`DiffUtil`; los cambios de selección actualizan el elemento específico. Evitar volver a crear toda la UI y perder foco/cursor.

### 5.5 Iconografía e ilustraciones

- Iconos Material vectoriales o equivalentes de trazo consistente: casa, caja/catálogo, documento/publicación, paleta/diseño, añadir, editar, duplicar, compartir, descargar, USB, TV, imprimir, reproducir audio, accesibilidad.
- No usar símbolo de «ojo» para Inicio ni iconos de reloj para Publicaciones si inducen ambigüedad.
- Ilustraciones pequeñas solo para estados vacíos y bienvenida, en SVG/VectorDrawable propios; evitar fotos de stock como relleno.
- Texto de botones **con etiqueta visible**; iconos acompañan, no sustituyen la acción crítica.
- La identidad «CHAMB-IA» puede usar isotipo propio; no llenar todas las tarjetas con el logotipo.

### 5.6 Movimiento y respuesta

- Microtransiciones 150–250ms para selección, cambios de estado y apertura de hojas; respetar «Eliminar animaciones» del sistema.
- Skeleton o indicador en descargas/renderización >~300ms; cancelación real cuando proceda.
- `Snackbar` para confirmaciones breves con «Deshacer» cuando sea reversible, no mensajes permanentes en la cabecera.
- Diálogos de confirmación solo para eliminación irreversible, sobrescritura/exportación potencialmente destructiva o salida con cambios no persistidos.

## 6. Pantallas: especificación detallada

### 6.1 Bienvenida y alta inicial (solo primera vez)

**Pantalla 1 — Bienvenida:** CHAMB-IA, eslogan pequeño, ilustración de publicaciones (TV, WhatsApp, papel), CTA **«Crear mi negocio»** y acción **«Explorar demostración»** solo si está claramente etiquetada y no simula datos persistentes. Dos frases de valor como máximo.

**Pantalla 2 — Tipo de negocio:** categorías visuales (Alimentos, Oficios, Comercio, Servicios, Otro) y giros buscables; no exigir elegir de una lista interminable. Sugerir giro inicial, editable después.

**Pantalla 3 — Perfil mínimo:** nombre comercial (requerido), modalidad (local/tianguis/domicilio/online, opcional), WhatsApp opcional. Logo, ubicación, eslogan y redes se pueden completar después.

**Pantalla 4 — Precarga:** «Ya tenemos productos habituales de tu negocio» → lista con selección múltiple por categoría, búsqueda, `Seleccionar todos` acotado a categoría y CTA fijo **«Agregar N a mi negocio»**. Precios **en blanco/pendientes** salvo que usuario los confirme. Opción «Empezar sin catálogo».

**Resultado:** entrar al negocio con borrador vacío o datos del catálogo; no marcar automáticamente todos los guisados como «lo que cocinaremos hoy».

### 6.2 Cabecera global y cambio de negocio

Barra superior compacta:

- Izquierda: marca breve o icono de app (no repetir eslogan en cada pantalla).
- Centro/abajo: nombre del negocio con icono de giro y flecha.
- Derecha: menú contextual/configuración; «+ negocio» dentro del selector o icono separado con etiqueta accesible.

Al tocar selector: bottom sheet con negocios recientes, categoría, estado y opciones **Cambiar / Agregar negocio / Administrar negocios**. No usar spinner gigante de formulario. Persistir selección. Ofrecer accesos rápidos «Mañana»/«Noche» solo como etiquetas opcionales de negocio/jornada, no asumir dos negocios son una misma actividad.

### 6.3 Hoy — portada de trabajo

**Objetivo:** continuar rápidamente la jornada y llegar a preparar/publicar.

Jerarquía vertical recomendada (visible sin gran desplazamiento):

1. Saludo contextual discreto: **«¿Qué ofrecerás hoy?»**; debajo solo subtítulo de giro (fondita «Prepara la comida del día»).
2. **Tarjeta de borrador** con chip `En preparación` / `Listo para publicar`, nombre de jornada y resumen: «Comida corrida · 3 platos fuertes · precio pendiente» (no «12 productos o servicios» sin contexto).
3. CTA primario **«Continuar preparando»**; secundario **«Vista previa»**. Miniatura 16:9 pequeña dentro de tarjeta o carrusel, no un cartel gigante más lista gigante.
4. Acciones rápidas de 2 columnas o icon buttons con etiqueta: **«Crear anuncio»**, **«Usar publicación anterior»**, **«Explorar catálogo»** si se necesita.
5. Actividad reciente opcional: una publicación, fecha y accesos **Compartir / Duplicar**.
6. AdMob banner discreto solo si no interrumpe y sin tapar navegación.

**No mostrar** doce productos, botones huecos sin texto, eslogan repetido, estado «Borrador guardado» permanente ni tres encabezados consecutivos.

### 6.4 Catálogo — administrar, NO publicar

**Barra:** título «Mi catálogo», búsqueda `Buscar productos o servicios`, chips categorías horizontales, icono de orden/filtro.

**Contenido:** `RecyclerView` compacto. Cada fila:

- Nombre en primera línea.
- Categoría y precio/estado en segunda.
- Icono editar a la derecha; menú adicional opcional para eliminar/duplicar.
- Selección de catálogo no equivale a inclusión en borrador; solo si se llega mediante selector para preparar una publicación, mostrar casillas con título «Agregar a esta publicación».

**Acciones:** botón contextual de **«Explorar catálogo precargado»**, `+ Agregar producto` y bulk add. Eliminar «Editar catálogo» repetido dentro de cada tarjeta.

**Estados:** sin productos, sin resultados, offline sin paquete descargado, precio pendiente, producto disponible/no disponible. Acceso a edición de precio con teclado numérico y modalidad: fijo, desde, por unidad, a cotizar, no mostrar.

**Regla:** cuando un producto del catálogo cambie de precio, no modificar silenciosamente PDFs/publicaciones ya generados; el borrador activo solo se sincroniza si el usuario aplica/acepta el cambio según política establecida.

### 6.5 Preparación universal (editor de publicación)

Usar estructura de **pasos orientativos**, no formularios interminables:

**Paso 1 Contenido** → **Paso 2 Revisar** → **Paso 3 Diseño** → **Paso 4 Publicar**. El progreso puede ser una línea discreta y no debe forzar rehacer pasos al editar una sección.

Para negocios con catálogo permanente: elegir categorías, productos, variantes, promos, orden y mensajes. Para anuncios rápidos: título, texto, CTA, fotografía opcional, contacto; no obligar catálogo. Guardado automático con estado breve «Guardado» y timestamp solo si aporta valor.

La persona puede abandonar y regresar sin perder trabajo. Mostrar al reabrir «Continuar borrador»; si crea otro, ofrecer duplicar/crear nuevo, sin sobrescribir.

### 6.6 Preparación específica de FONDITA MEXICANA (P0)

**Modelo comercial correcto:** la oferta del día es una **comida corrida/paquete con precio propio**, compuesta por tiempos y opciones, más platos ejecutivos u opciones a la carta. No exigir precio individual para sopa/segundo si van incluidos. La estructura siguiente es un **perfil editable**, no una norma universal para todas las fondas.

**Pantalla «Arma tu comida corrida»**:

- Cabecera: «Comida corrida» + campo precio del paquete (ej. `$110`) + etiqueta **«Incluye»**.
- Sección **Entrada — elige una**: propone 2 opciones, pero permite 0..N (ej. sopa de fideo, consomé de pollo). Botón pequeño «Cambiar/Agregar opciones» y chips elegidos.
- Sección **Segundo tiempo — elige uno**: propone 2 opciones, configurable (arroz rojo, espagueti).
- Sección **Plato fuerte — elige uno**: N guisados, ej. pollo en mole, enchiladas verdes, chicharrón en salsa verde; permitir extras por guisado solo si negocio los usa.
- Sección **Plato ejecutivo** (opcional e independiente): uno o más ejecutivos con precio de paquete propio o suplemento claramente explicado; no confundir con el guisado básico.
- Sección **Bebida**: incluida (agua del día), a elegir, no incluida o precio adicional; indicar tipo y política.
- Sección **Postre**: incluido, opcional con cargo, no ofrecido; se puede omitir por completo.
- Sección **Extras y observaciones**: tortillas, salsas, horario, «Hasta agotar existencias», etc. sin campos obligatorios.
- CTA fijo de tarea **«Revisar menú»**; lista por sección plegable para ahorrar scroll.

**Estados por sección:** incompleta, lista, opcional. Si faltan datos esenciales antes de publicar (p. ej. sin plato fuerte o precio paquete no confirmado), mostrar resumen de correcciones con enlaces directos. Permitir **«Publicar sin precios»** explícitamente sin imprimir «Precio por confirmar» doce veces. Si el precio de paquete está establecido, no solicitar precio para elementos incluidos.

**El anuncio generado debe agrupar**: nombre negocio, título/fecha, **Comida corrida — $110**, «Entrada (elige una)», «Segundo tiempo (elige uno)», «Plato fuerte (elige uno)», **Ejecutivo — desde $145** cuando aplique, «Incluye agua» / «Postre incluido» cuando aplique. Mantener categorías y opciones visibles. Evitar en la imagen final la etiqueta administrativa «precio por confirmar».

**Importante:** algunos establecimientos ofrecen menú de dos, tres o cuatro tiempos o no venden comida corrida. Permitir configurar secciones, nombres, orden, cardinalidad («elige una / hasta dos»), y ocultarlas. Además contemplar **«Platillos a la carta»** como modo separado o coexistente.

**Ejemplo de prueba** (datos ficticios):

- Comida corrida: $110.
- Entrada: sopa de fideo / consomé.
- Segundo: arroz rojo / espagueti.
- Plato fuerte: pollo en mole / enchiladas verdes / chicharrón salsa roja.
- Ejecutivo: milanesa con guarnición $145.
- Bebida: agua de jamaica incluida.
- Postre: gelatina opcional incluida según configuración.

**Comportamiento de agotados:** si se agota una opción, marcarla y regenerar anuncio; en TV por USB se deberá reemplazar manualmente el contenido. No eliminarla del catálogo.

**Diseño visual del menú:** títulos y precios dominantes, secciones agrupadas, tipografía legible desde distancia de TV, máximo de contenido por página parametrizable; al exceder generar imagen 2/3 en lugar de comprimir todo.

### 6.7 Otros giros: adaptaciones mínimas

**Pizzería:** producto con tamaños y precios por variante; tabla por tamaño en plantilla apta, combos y promociones; carta estable.

**Tortillería:** productos por kg/medio kg/paquete; lista de precios grandes, unidades junto al precio, sin iconos decorativos invasivos.

**Taller:** servicio, descripción corta, precio fijo/«Desde $»/«Cotización», teléfono/WhatsApp, imágenes opcionales; diseño que priorice contacto y servicios.

**Tianguis/ambulante:** catálogo por jornada, precio de oferta, ubicación opcional y variable, carteles verticales listos para impresión.

**Multinegocio:** todas las pantallas muestran giro y negocio correcto sin mezclar datos; el cambio no publica ni cancela trabajos.

### 6.8 Galería de Diseños — herramienta de monetización

- Pantalla inicial: filtros `Todos | Gratis | Premium | Mis diseños | Favoritos` (usar scroll horizontal de chips).
- Cuadrícula 2 columnas teléfono, 3–4 en tablet según ancho; tarjetas con preview proporcionada, nombre, precio/«Gratis», y estado «En uso».
- Diferencias **compositivas** reales entre diseños: minimalista de precio grande, folclórico decorado, fotográfico, lista de precios, elegante; no solo el mismo JSON con colores diferentes.
- Tap → **Detalle** con vista grande adaptada al negocio y botones **«Aplicar»** / **«Personalizar»** / **«Comprar»** según derecho. Permitir vista previa de premium sin desbloquear exportación ilegítima.
- Diseño favorito por negocio; compra de plantilla asociada a usuario/compra conforme reglas, no a una sola fondita necesariamente.
- Carga de thumbnails bajo demanda/caché; evitar render 4K en galería.
- Plantillas básicas funcionales sin red; estado descargando / error / reintentar / versión incompatible.

### 6.9 Vista previa y composición

**Pantalla «Así se verá tu publicación»**:

- Superficie protagonista, centrada y respetando proporciones reales.
- Selector `TV 16:9 | WhatsApp 9:16 | Cuadrado 1:1 | Impresión` con iconos claros y etiquetas.
- Zoom opcional, cambio de página para menús largos, contador `1 de 2`.
- Debajo botones: **«Editar contenido»**, **«Cambiar diseño»**, **«Accesibilidad»**.
- CTA **«Generar materiales»**.
- Alertas de legibilidad y precios pendientes; no producir PDFs que muestran placeholders administrativos salvo consentimiento explícito.
- Preview y export deben compartir **el mismo modelo de composición y salto de línea** (evitar diferencias entre lo visto y lo obtenido).

### 6.10 Publicaciones / historial

- Segmentar `En preparación | Generadas` o chips `Todas | Borradores | Compartidas`.
- Tarjetas compactas con miniatura, título, negocio, fecha, destino y estado.
- Acciones: `Abrir`, `Duplicar`, `Compartir`, `Regenerar`, menú secundario (Eliminar confirmando).
- Páginas vacías con CTA «Crear publicación».
- La edición de una publicación anterior crea **nueva versión** o borrador, no altera silenciosamente un archivo previamente compartido.
- Archivos borrados por fuera: estado «Archivo no disponible» + «Regenerar» si datos/plantilla están presentes.

### 6.11 Exportación — lenguaje cotidiano

Pantalla de selección **«¿Dónde quieres mostrarlo?»**:

1. **Celular y WhatsApp** — formatos recomendados sin tecnicismos iniciales.
2. **Televisión / USB** — JPG Full HD y secuencia numerada; opción compatible si el dispositivo ofrece acceso SAF.
3. **Imprimir** — PDF carta/A4/oficio; poster/lona como formato específico con advertencia de resolución.
4. **Texto / Audio accesible** — mostrar como opción adicional gratuita.

**No exigir seleccionar primero JPG/PNG/PDF** para poder compartir: recomendar formato según destino y revelar «Opciones avanzadas» a demanda. Dar resumen antes de escribir: `3 archivos · Carpeta Cocina Rosita / 2026-11-12 / Comida`.

- Local: guardado en carpeta definida por usuario, luego hoja «Compartir por WhatsApp u otra aplicación».
- USB: SAF elige destino; mostrar «Conecta una memoria USB compatible» si no hay, sin fallar ni asumir OTG; confirmación real con número de archivos.
- TV: nombres `01_menu.jpg`, `02_bebidas.jpg`, `03_promociones.jpg`; imágenes 16:9; la app NO instala nada en TV ni promete actualización inalámbrica.
- PDF: render/vector cuando proceda; impresión de lona requiere tamaño y alerta sobre fotografías insuficientes.
- Errores: no espacio, permiso denegado, destino retirado, cancelado, fallo parcial, exportación exitosa. Incluir `Reintentar` donde sea seguro.
- No mostrar publicidad durante escritura o hoja de compartir.

### 6.12 Accesibilidad y narración de publicaciones

Función gratuita y transversal. En `Vista previa` y `Exportar` ofrecer **«Hacer accesible»** con:

- **«Escuchar menú/anuncio»**: guion ordenado por secciones, voz TTS compatible, botones reproducir/detener, velocidad, correcciones de pronunciación; archivo de audio si el motor lo permite.
- **«Texto para lectores de pantalla»**: TXT/representación estructurada; ordenar Entrada → Segundo → Fuerte → Ejecutivo → Bebida → Postre; expresar precios en palabras.
- **«Letras grandes»**: plantilla de alto contraste, con paginación, no solo aumento que cause recorte.
- **«Lectura sencilla»**: frases cortas con información esencial intacta.

Probar TalkBack, escalado tipográfico, enfoque, orden de lectura, hints en controles, tiempos de carga y contraste. No afirmar que un JPG o PDF rasterizado es plenamente accesible. Si TTS offline no existe, mostrar alternativa textual y explicar limitación; nunca simular un WAV/MP3 generado.

## 7. Modelo de estados UX y validaciones

### 7.1 Estados de publicación

`BORRADOR` → `LISTO_PARA_REVISAR` → `GENERADO` → `COMPARTIDO/EXPORTADO` (registrar destinos y versiones; compartir y exportar son sucesos distintos). Cambiar contenido después de generar crea versión pendiente de regenerar, no modifica archivos anteriores.

### 7.2 Estados comerciales

- Catálogo: activo / archivado; precio confirmado / no definido / no mostrar.
- Oferta del día: incluido / no incluido / agotado / oculto.
- Secciones del paquete: requerida / opcional / completa / incompleta.
- Plantilla: integrada / descargable / descargada / premium sin derecho / adquirida / incompatible.
- Exportación: pendiente / generando / terminada / parcial / fallida / cancelada.

### 7.3 Validaciones y mensajes

- Precio negativo o excesivo: «Revisa el precio» al lado del campo, no snackbar genérico.
- Precio paquete ausente: «Falta definir el precio de la comida corrida» + acción `Agregar precio` o `Publicar sin precio`.
- Sin platillos: «Agrega al menos una opción para continuar».
- Selección vacía al generar: indicar qué agregar y volver al paso correcto.
- Sin internet: «Puedes seguir preparando y publicando con los recursos descargados»; solo acciones de nube se deshabilitan con explicación.
- Paquete incompleto: informe agrupado por sección, no doce errores repetidos.
- Nombre de negocio extenso: ajustar en vista previa dentro de límites sin recortar o desbordar.

## 8. Microcopy oficial y tono de voz

Tono cercano, útil y respetuoso; no infantilizar ni asumir que todos los negocios son fonditas.

| Contexto | Texto preferido | Evitar |
|---|---|---|
| Portada de fondita | «¿Qué cocinaremos hoy?» | «12 productos o servicios seleccionados» |
| Estado | «Menú en preparación» | «Borrador guardado en este dispositivo» persistente |
| Acción | «Continuar preparando» | «Continuar preparación» si se siente técnico |
| Catálogo | «Mis productos» / «Explorar sugerencias» | «Editar catálogo» en cada fila |
| Paquete | «Comida corrida · $110» | Desglose individual de precios incluidos |
| Precio | «Por definir» (solo editor) | «Precio por confirmar» en cartel final |
| Vista previa | «Así se verá tu anuncio» | «Renderizar publicación» |
| TV | «Preparar para televisión» | «Exportación SAF de imágenes» |
| Accesibilidad | «Escuchar menú» | «Sintetizar TTS» |
| Éxito | «Tu menú está listo para compartir» | «Proceso 200 OK» |

No incrustar textos en Java; centralizar en `strings.xml`, plurales y recursos localizados.

## 9. Distribución adaptable y compatibilidad

### Teléfonos

- 320dp, 360dp, 393dp, 411dp y 480dp; vertical. No scroll horizontal de página; chips pueden desplazar **dentro** de su fila.
- Barra de navegación inferior estable. CTAs dentro de `CoordinatorLayout`/contenedor con insets, sin tapar contenido; cuando teclado aparece, contenido editable debe permanecer visible.
- Landscape mínimo funcional, sin cortar botón exportar.

### Tabletas

- `sw600dp+`: pane de catálogo izquierda y selección/vista previa derecha cuando ayude; galería 3–4 columnas.
- No estirar tarjeta de 350dp a toda la tableta sin criterio; ancho de lectura max ~720–840dp y contenido centrado.

### Accesibilidad de interfaces

- Fuente sistema 100%, 130%, 150%, 200% cuando resulte razonable; pantallas con scroll y botones sin truncamientos graves.
- TalkBack: navegar cada fila como una unidad semántica o controles explícitos; estado de check seleccionado anunciado.
- Teclado físico/selector externo y navegación por gestos.
- `touch target` ≥48dp, colores accesibles y descripción de iconos.

## 10. Modelo de implementación Android recomendado

### 10.1 Evitar refactorización destructiva

Partir de las clases existentes, comprobadas en el ZIP v0.3:

- `MainActivity.java`, `WorkspaceFragment.java`, `WorkspaceViewModel.java`.
- `HomeFragment.java`, `CatalogFragment.java`, `PreparationFragment.java`, `FondaMenuFragment.java`, `FondaSections.java`.
- `DesignsFragment.java`, `PreviewFragment.java`, `HistoryFragment.java`, `ExportFragment.java`, `AccessibilityFragment.java`.
- `data/` con `AppDatabase`, `AppDao`, `Business`, `Item`, `Publication`, `Draft`, etc.
- `design/TemplateEngine.java`, `export/ExportManager.java`, `publication/DraftEditor.java`.
- `res/navigation/main.xml`, `res/layout/activity_main.xml`, `res/values/styles.xml`.

**No asumir que esas clases están listas o que el nombre asegura su comportamiento**; inspeccionar dependencias y flujos reales antes de editarlas.

### 10.2 Cambios concretos por área

- Refactorizar `WorkspaceFragment` (actualmente demasiado concentrado): mantener lógica común mínima; extraer layout y adaptadores por pantalla. Reducir creación de widgets mediante Java, evitar funciones `renderAll()` que reconstruyen pantallas completas.
- Crear layouts XML reutilizables y `ViewBinding`. `RecyclerView` con adaptadores para productos, secciones y plantillas.
- Consolidar `BottomNavigationView` con `menu` XML e iconos vectoriales específicos; seleccionar destino con NavController/estado consistente.
- Centralizar `colors.xml`, `dimens.xml`, `styles.xml`, `values-night/colors.xml`, `strings.xml`; usar temas/atributos semánticos y estilos.
- `FondaMenuFragment` debe editar un **modelo de paquete/tiempos** persistente. No basta categorizar `Item` con `FondaSections.normalize` sin guardar precio del paquete, cardinalidad, reglas de inclusión y opciones elegidas.
- Extender entidades Room con migración segura si faltan: `MealPackage`, `MealSection`, `MealSectionOption` o equivalente normalizado; vincular a publicación/borrador y negocio; snapshot al generar.
- `TemplateEngine`: aceptar grupos y secciones además de lista plana, render a resoluciones distintas, paginar, precios de paquete, ejecutivo opcional y textos accesibles. Mantener compatibilidad con plantillas antiguas mediante versión.
- `ExportManager`: generar solo archivos actuales; no incluir placeholders administrativos; conservar SAF y formato real de audio.
- `AccessibilityFragment`: narrar **estructura comercial**, no concatenar lista plana de items.
- No introducir un backend ni monetización nueva durante la revisión de UX salvo corregir interferencias existentes.

### 10.3 Arquitectura de estado

- `ViewModel` por pantalla o flujo complejo; repositorios para Room y catálogo; una fuente de verdad por negocio/borrador.
- `SavedStateHandle` para estado efímero (selección de filtro/página/scroll) y Room para borradores persistentes.
- Escrituras en hilo apropiado; observar cambios; debounce de campo precio según necesidad.
- Transacciones para actualizar paquetes con sus opciones, no dejar borradores a medias.
- Todas las UI de edición usan los mismos validadores del dominio, no reglas contradictorias.

## 11. Orden de implementación y entregables verificables

### Lote A — Correcciones P0 de legibilidad/navegación

- Resolver botones sin texto; identificar causa real en estilos de Material y estados.
- Barra inferior con iconos adecuados y textos sin saltos ni solapamientos.
- Insets correctos y controles táctiles accesibles.
- Theme tokens día/noche, botones primario/secundario correctos.

**Criterio:** capturas a 320/393dp con escala 1.3 y navegación gestual; ningún botón vacío, texto sobrepuesto ni contenido cubierto.

### Lote B — Inicio / cabecera / selector de negocio

- Tarjeta compacta, CTAs bien jerarquizados, acciones rápidas reales.
- Miniatura pequeña y contador de sección, sin repetir 12 filas.
- Cambio de negocio aislado; conserva borradores.

**Criterio:** comenzar preparación desde Inicio en ≤2 toques y cambiar de negocio en ≤2 toques (objetivos de prueba, no garantías de tiempo).

### Lote C — Catálogo vs preparación

- Separar pantallas/estados; listas compactas, multi-selección en contexto, edición de precios legible.
- No asociar «en mi catálogo» con «hoy lo ofrezco».

**Criterio:** selección del día no modifica permanente/accidentalmente el catálogo; ~4–5 filas visibles en 393dp según configuración.

### Lote D — Comida corrida mexicana

- Implementar precio de paquete y seis secciones configurables (entrada, segundo, fuerte, ejecutivo, bebida y postre), incluyendo opcionalidad.
- Borrador persistente, revisar errores, exportar anuncio agrupado.

**Criterio:** generar imagen y PDF donde aparezca `Comida corrida $110`, secciones y precios de ejecutivo sin doce etiquetas de precio pendiente.

### Lote E — Diseños / vista previa

- Galería, detalle y personalización; 2 columnas de thumbnails.
- Variantes estructurales; zoom/paginación y formatos.

**Criterio:** seleccionar diseño sin perder borrador, y previsualización fiel al archivo final.

### Lote F — Publicaciones, exportaciones, audio

- Historial y versiones, destino guiado, compartir/USB, accesibilidad TTS/texto.
- Pruebas de fallos y permisos.

**Criterio:** flujo real de compartir JPG, guardar PDF, exportar a USB compatible, narración de paquete mediante motor disponible.

Cada lote debe entregar: **lista de archivos cambiados, capturas antes/después o evidencia equivalente, compilación `assembleDebug`, pruebas unitarias/instrumentadas que se hayan ejecutado, fallos conocidos y pendientes**. No presentar ejecución real si no se efectuó.

## 12. Checklist de aceptación de UX/UI (QA)

### Global

- [ ] El nombre CHAMB-IA y el negocio activo son reconocibles sin ocupar excesivo espacio.
- [ ] Naranja/violeta usados semánticamente; contraste texto/fondo verificado.
- [ ] Todos los botones muestran texto legible en estado normal, presionado, deshabilitado y cargando.
- [ ] Ningún CTA importante queda oculto por barra inferior, teclado o anuncios.
- [ ] Barra inferior con cuatro iconos correctos, etiquetas legibles, destino activo inequívoco.
- [ ] Textos centralizados en recursos, sin emojis como iconos de navegación.
- [ ] Funciona con TalkBack, escalado 1.3/1.5 y modo oscuro.
- [ ] Sin cambios destructivos en Room; migraciones definidas y probadas.

### Inicio y multinegocio

- [ ] Inicio solo presenta resumen y acciones, no catálogo entero.
- [ ] «Continuar preparando» abre borrador correcto, no lo reinicia.
- [ ] Cambiar negocio no mezcla productos, diseños ni historial.
- [ ] Sin internet se puede editar y generar con recursos locales.

### Catálogo

- [ ] Filas compactas, precio visible, edición con icono.
- [ ] Búsqueda, filtros y precarga operativos.
- [ ] Agregar catálogo maestro copia al negocio sin sobrescribir personalizaciones futuras.
- [ ] Seleccionar oferta del día no elimina o archiva producto maestro.

### Fonda

- [ ] Precio de comida corrida único y editable.
- [ ] Entrada, segundo, fuerte, ejecutivo, bebida, postre configurables.
- [ ] Dos opciones sugeridas de entrada/segundo, pero no límite rígido.
- [ ] Ejecutivo con precio propio sin confusión.
- [ ] «Incluye bebida/postre» explícito y correcto.
- [ ] Producto agotado no desaparece del catálogo.
- [ ] Anuncio agrupado por tiempos y sin «Precio por confirmar» repetido.
- [ ] Salida 16:9, 9:16 y PDF sin recortes; paginación si excede.

### Diseños y publicación

- [ ] Galería de miniaturas, plantillas diferenciadas estructuralmente.
- [ ] Vista previa usa datos verdaderos del borrador.
- [ ] Guardar en celular/USB/compartir funciona según destino y permisos.
- [ ] Audio y texto accesibles basados en la estructura del menú.
- [ ] Archivos históricos no se sobrescriben silenciosamente.

## 13. Escenarios de prueba obligatorios

**A. Fondita:** crear «Rosita Sazón», elegir dos entradas, dos segundos, tres fuertes; comida corrida $110, ejecutivo $145, agua incluida y postre opcional; guardar; cerrar/reabrir; diseño mexicano; vista TV; JPG y PDF; voz; regenerar al agotarse un guisado.

**B. Pizzería:** tres tamaños/precios, combo y promoción; plantilla distinta; exportar a WhatsApp.

**C. Tortillería:** precio por kg/medio kg, cartel legible desde distancia y PDF sin fotos.

**D. Taller:** servicio «Desde $650» y «Cotizar», sin sustitución errónea por $0; compartir.

**E. Multinegocio:** cambiar Rosita Sazón ↔ Taller González, conservar borradores y plantilla favorita; publicar por separado.

**F. Robustez:** cero productos, 1, 5, 20 y 40 productos; títulos largos; fuente grande; sin internet; sin motor TTS; sin USB; permisos denegados; regreso de segundo plano; rotación.

**G. Regresión:** abrir una instalación previa de v0.2/v0.3 (o BD de prueba equivalente), migrar sin perder negocios, precios ni historial; verificar que los archivos exportados antiguos siguen accesibles donde estén.

## 14. Fuera del alcance de este rediseño

No agregar: POS, pedidos, facturación, inventarios, CRM, cobranza, asistente IA en la nube, MP4 final, publicación automática a redes, app Android TV separada. No cambiar la marca, nombre del paquete o IDs de productos de Play Billing sin instrucción explícita. El rediseño **sí** debe mantener la preparación para catálogos remotos, plantillas ligeras, contenido accesible y múltiples negocios.

## 15. Formato de reporte final que Codex debe producir

Al completar cada lote, crear/actualizar `docs/UX_IMPLEMENTATION_REPORT.md` con:

1. **Resumen:** qué problema UX se resolvió y cómo se comprueba.
2. **Matriz:** requisito → implementación → archivos → evidencia → estado (`Implementado/Parcial/Pendiente`).
3. **Capturas:** teléfonos y tableta donde sea posible, mostrando Inicio, Catálogo, Fonda y Diseños.
4. **Pruebas:** comando exacto, resultado exacto, fecha y entorno; no marcar probado si no se ejecutó.
5. **Migraciones:** esquema anterior/nuevo y resultado de pruebas.
6. **Pendientes:** incidencias con gravedad P0/P1/P2 y siguiente acción concreta.
7. **Riesgos:** dependencias externas, TTS, USB, Play Billing, AdMob, seguridad de plantillas.

**Criterio final de éxito:** la app debe verse y comportarse como una herramienta de creación de anuncios para microemprendedores, no como un panel administrativo; una fondita debe poder preparar una comida corrida por tiempos y generar un anuncio profesional sin capturar doce precios individuales ni perder tiempo navegando por tarjetas gigantes.

---

## Prompt breve de arranque para pegar en Codex

> Lee `CHAMBIA_REQUIREMENTS.md` y `CHAMBIA_UX_UI_ESPECIFICACION_CODEX_v1.md`. Inspecciona el proyecto Java actual, el tema Material, `WorkspaceFragment`, navegación, catálogos, `FondaMenuFragment` y `TemplateEngine`. Crea una matriz de diferencias y aplica el rediseño **real** por lotes A–F descritos en la especificación, empezando por corregir botones naranjas sin texto, navegación inferior y la pantalla Inicio. A continuación separa Catálogo/Preparación y convierte el flujo de fondita en comida corrida con precio de paquete, tiempos, opciones, ejecutivo, bebida y postre; adapta el renderizador y las exportaciones. Conserva Room y las funciones existentes; implementa migraciones seguras. No hagas solo cambios de color. Compila, ejecuta pruebas que permita el entorno y entrega `docs/UX_IMPLEMENTATION_REPORT.md` con archivos cambiados, evidencias y pendientes honestos.
