# Propuesta UX y aplicación actual

[Maqueta visual de siete flujos](UX_PROPOSAL.svg). Es una propuesta estática, no captura del dispositivo. Se conserva el verde de la interfaz heredada y la composición crema/vino/verde del material mexicano; la marca siempre es CHAMB-IA.

Inicio pone el borrador por delante de publicar. Catálogo permite búsqueda y selección progresiva, con precio sin confirmar explícito. Preparación no obliga a terminar una jornada. Diseños muestra contenido propio renderizado. Vista previa conserva relación de aspecto, destino y páginas. Accesibilidad ofrece texto antes que audio. Exportación separa destino, formato y acción, con copia a directorio SAF y envío por Sharesheet.

Implementado en la interfaz: botones Material, tarjetas de catálogo, búsqueda, miniaturas reales, aviso de progreso accesible, vistas de página, controles de 48dp y márgenes ampliados en tabletas. No se ha revisado visualmente en Android; no se presenta esta propuesta como rediseño final aprobado. Faltan RecyclerView, componentes separados por pantalla, layouts completos de tabletas, manejo fino de foco y auditoría TalkBack. Los textos siguen mayoritariamente en Java y requieren recursos ES/EN.
