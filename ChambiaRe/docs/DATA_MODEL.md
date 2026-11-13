# Persistencia actual y límites

Base `chambia.db`, versión 7. Java + Room. Migraciones explícitas 1→2→3→4→5→6→7; sin fallback destructivo. No se han ejecutado las migraciones en Android en esta entrega.

- `Business`: identidad, nombre, giro principal, modalidades, contacto, eslogan, plantilla, archivo y perfil JSON opcional (zona/dirección, horario, redes, giros secundarios). Cabecera/jornada de publicación son campos `@Ignore`, capturados en la instantánea, no ajustes globales persistidos.
- `Item`: copia propia del negocio, referencia maestra opcional, nombre/categoría/unidad/descripción, precio/modalidad/estado; variantes editables con validación y contenido textual de paquete. `inDraft` es legado, la oferta actual ya no se calcula desde ese flag.
- `Draft`: ID independiente, businessId con FK/cascade e índice, nombre, modalidad, jornada, snapshot JSON ordenado y fecha de guardado. Preferencias por negocio recuerdan borrador activo; la migración conserva el borrador anterior como `active_<businessId>`.
- `Publication`: versión exportada inmutable con snapshot de contenido y negocio, plantilla, destino, contraste, fecha, texto y referencias privadas a archivos. Regenerar siempre inserta otra versión. Datos ausentes de instantáneas heredadas no pueden reconstruirse.
- `PublicationStore`: codec de negocio y creación de versiones; `PublicationSnapshot` copia profunda y codec de artículos; `DraftEditor` impide referencias compartidas o artículos ajenos.
- `ExportWorker`: solicitud JSON privada en `files/export_requests/<workId>.json`, con contenido/negocio capturados. WorkManager conserva el trabajo; resultado contiene ID de publicación y el historial conserva los archivos. Cancelación entre páginas. Se elimina la solicitud al finalizar el worker.
- Preferencias: último negocio, borrador/trabajo activo, contraste, opciones de audio; guion/voz/velocidad y favoritos separados por businessId. Borrado de negocio limpia los espacios específicos.

## Migraciones añadidas

- 4→5 añade `Item.variants` y `bundleContents` vacíos, conserva catálogo y snapshots viejos compatibles.
- 5→6 convierte el borrador único por negocio en borradores identificados, conservando su JSON y fecha; crea índice y FK.
- 6→7 añade perfil opcional vacío sin alterar datos existentes.
- `room.schemaLocation` y exportación de esquema están configurados. No hay esquema generado/verificado porque el build no alcanzó el procesador Room.

## Pendiente del modelo completo

FK de Item/Publication, entidades relacionales para variantes/componentes/combos/fotos/etiquetas y categorías múltiples; catálogos maestros/versiones/descargas; assets de plantilla/derechos comerciales; registros de URI externa y accesibilidad conjunta. Los campos de texto/JSON implementados no equivalen a esas entidades. TTS y SAF no son trabajos durables; no hay backend de datos privados. La prueba instrumentada desde v1 y la de varios borradores están escritas pero sin ejecutar.

## Rediseño UX/UI — Room 9 (12 nov 2026)

- `Draft.mealJson`: agregado versionado de comida corrida. Se guarda junto a `snapshotJson` en una sola fila: precio en centavos, consentimiento para omitir precios, notas y secciones ordenadas con IDs de opciones del snapshot. Políticas: incluido, precio independiente, suplemento, no ofrecido. Las secciones adicionales permiten a la carta sin quitar el paquete.
- `Business.mealJson` es `@Ignore`: transporta la composición al motor, no altera el esquema de negocios. La instantánea `Publication.businessJson` conserva su copia histórica.
- `Publication.eventsJson`: sucesos de apertura de hoja de compartir y copia a destinos, distintos del contenido inmutable. Abrir Sharesheet no confirma un envío.
- Migraciones aditivas 7→8 y 8→9; se conserva la cadena anterior. `{}` mantiene un borrador antiguo como oferta universal, sin activar un paquete ni inventar precios.
- Estado transitorio de pantalla: `WorkspaceViewModel.uiState` (`SavedStateHandle`) conserva scroll, página y destino. Room sigue siendo fuente de verdad del contenido por negocio y borrador.
