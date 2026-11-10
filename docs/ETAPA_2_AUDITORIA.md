# Etapa 2 — auditoría previa (28/09/2026)

Checkout comprobado: Android nativo Java/XML; no es el ZIP Next.js de referencia. No se encontraron AGENTS.md ni instrucciones adicionales en el proyecto o sus padres. Árbol Git limpio al iniciar.

## Hallazgos antes de editar
- Bienvenida de una sola pantalla, sin avance ni persistencia; proporción fija poco adaptable.
- Login real por email conservado; Google y recuperación carecen de servicio Android. Presentar disponibilidad honestamente.
- Más no abría Inventario ni Finanzas. Ganado ya tiene ruta real de consulta.
- CRUD ausente: ejemplos por fragmento, sin lotes ni repositorio compartido; estados se pierden entre módulos. Dashboard no usa la fuente demo.
- Detalles solo lectura, sin formularios agrupados ni validación. No existen endpoints móviles de escritura ni permisos de escritura cargados.
- Hojas de 90% de altura fija, padding IME más adjustResize: posible doble descuento de teclado. Revisar en emulador.
- Pantallas anchas sin límite de lectura; listas y formularios requieren adaptación nativa a ventanas grandes.
- Splash conserva una devolución de llamada después de destruir la vista. Corregir ciclo de vida de UI.
- Proteger restauración manual del RecyclerView 1.1.0 y overlay FrameLayout del Dashboard, regresiones anteriores.

## Contratos preservados
LoginViewModel, SessionManager, SupabaseConfig, SupabaseApiService y modelos de dominio quedan intactos. Consultas de tareas, lotes, rodeos, finanzas y calendario existentes. Sin consulta de animales/inventario ni escrituras Android. Login real actualmente guarda organización nula: no alterar autenticación para disimularlo. No inferir permisos.

## Compatibilidad comprobada
AGP 8.5.2, Gradle 8.7, Kotlin 1.9.24, compile/target SDK 34, mínimo 27, Java fuente 8. No se agregarán dependencias. Koala 2024.1.1 admite AGP 8.5; AGP 8.5 requiere Gradle 8.7/JDK 17 y soporta API 34. JDK local disponible 21.0.3.1.

Fuentes: https://developer.android.com/build/releases/about-agp y https://developer.android.com/build/releases/agp-8-5-0-release-notes

## Estrategia
Repositorio demo en memoria, aislado por sesión y observado entre pantallas; nunca suplanta una consulta fallida. CRUD de tareas/lotes/rodeos/animales solo en demo. Formularios y detalles compartidos sobre los mismos campos existentes, sin cambiar contratos. Consultas reales siguen en su ViewModel. Navegación y pantallas secundarias con estados honestos. Verificación manual en emulador más verificaciones existentes.
