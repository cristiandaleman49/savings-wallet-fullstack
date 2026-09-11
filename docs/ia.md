# AI-Assisted Development

## 1. Objetivo

La inteligencia artificial se utilizó como herramienta de apoyo durante el desarrollo de **Savings Wallet**, principalmente para acelerar la implementación, generar y revisar pruebas, analizar problemas técnicos, proponer alternativas y preparar documentación.

La responsabilidad sobre la arquitectura, el alcance, las decisiones técnicas, la revisión del código y la aceptación final de los cambios permaneció en el desarrollador.

El proceso seguido fue iterativo:

**propuesta/asistencia de IA → implementación → revisión humana → pruebas → validación manual cuando correspondía → aceptación o modificación**

---

## 2. Herramientas utilizadas

### Cline

Cline se utilizó como agente de implementación dentro del entorno de desarrollo. Sus responsabilidades durante el proceso incluyeron:

- inspeccionar el código existente;
- implementar funcionalidades a partir de instrucciones;
- crear y modificar pruebas;
- ejecutar la suite de tests;
- analizar errores de compilación y ejecución;
- reportar los cambios realizados y sus resultados.

El uso de Cline fue incremental: las funcionalidades se dividieron en tareas pequeñas para poder revisar cada cambio antes de continuar.

### ChatGPT

ChatGPT se utilizó como apoyo para:

- diseño y revisión de arquitectura;
- análisis de trade-offs;
- definición de límites del MVP;
- diseño de prompts para Cline;
- revisión de cambios;
- análisis de errores;
- preparación de documentación y sustentación técnica.

---

## 3. Prompts y forma de trabajo

Los prompts utilizados siguieron una estrategia de alcance controlado. En lugar de solicitar una implementación completa de una sola vez, se definieron tareas concretas.

Algunos ejemplos de tareas asistidas fueron:

- implementar el Value Object `Money`;
- implementar el Aggregate Root `SavingsGoal`;
- implementar los casos de uso;
- crear el Repository y el adapter de persistencia;
- implementar la API REST;
- implementar `GoalCompleted` y SSE;
- implementar la integración SSE en Angular;
- implementar el diálogo de finalización;
- investigar y corregir errores de `Broken pipe`;
- corregir el lifecycle del stream SSE;
- preparar documentación técnica.

Los prompts también incluyeron restricciones explícitas como:

- no modificar capas fuera del alcance de la tarea;
- no introducir tecnologías innecesarias;
- ejecutar tests después de los cambios;
- revisar `git diff --check`;
- no realizar commits o pushes hasta que el cambio fuera revisado.

Esto permitió mantener control sobre el alcance de cada modificación.

---

## 4. Agente / sub-agente

Cline cumplió el rol de **agente de implementación**: recibió una tarea concreta, inspeccionó el contexto del proyecto, realizó cambios, ejecutó validaciones y reportó resultados.

No se utilizó una arquitectura de múltiples sub-agentes independientes. La colaboración se realizó principalmente entre:

**Desarrollador ↔ ChatGPT ↔ Cline**

ChatGPT actuó principalmente como apoyo de diseño/revisión, mientras Cline ejecutó tareas de implementación y validación dentro del proyecto.

---

## 5. Co-creación

| Etapa | Asistencia de IA | Responsabilidad del desarrollador |
|---|---|---|
| Arquitectura inicial | Análisis de alternativas y trade-offs | Selección de Modular Monolith + Hexagonal |
| Dominio | Propuesta/implementación de `Money` y `SavingsGoal` | Definición y validación de invariantes |
| Persistencia | Implementación de JPA/SQLite y adapters | Decisión de SQLite y aislamiento mediante Repository |
| REST | DTOs, controllers y tests | Validación del contrato y reglas |
| Realtime | Implementación de SSE y eventos | Decisión de SSE frente a WebSockets |
| Angular | Servicios, estado, componentes y tests | Validación del flujo UX y arquitectura |
| Completion Dialog | Implementación e integración | Validación del comportamiento esperado |
| Broken Pipe | Análisis y corrección incremental | Revisión de la solución y límites del manejo de errores |
| SSE lifecycle | Diagnóstico del problema de navegación | Decisión de mover ownership al App Shell |
| Documentación | Generación y estructuración | Revisión final y precisión técnica |

La IA no fue considerada como autoridad sobre el diseño. Cada cambio relevante fue sometido a pruebas y revisión antes de incorporarlo.

---

## 6. Código generado/asistido vs. decisiones humanas

### Código generado o asistido por IA

La IA participó en la generación o modificación de diferentes piezas, incluyendo:

- código de dominio;
- casos de uso;
- adapters de persistencia;
- controllers y DTOs;
- tests unitarios e integración;
- servicios Angular;
- componentes Angular;
- integración SSE;
- manejo de errores SSE;
- documentación.

Esto no implica que cada línea fuera aceptada sin revisión.

### Decisiones y validaciones humanas

Las decisiones técnicas principales fueron definidas y/o validadas durante el proceso de desarrollo:

- Java 21;
- Spring Boot;
- Angular;
- Modular Monolith;
- Hexagonal Architecture;
- separación Domain / Application / Infrastructure;
- `SavingsGoal` como Aggregate Root;
- `Money` como Value Object;
- Repository Pattern;
- Observer / Publish-Subscribe;
- Adapter Pattern;
- SSE en lugar de WebSockets;
- SQLite para el MVP;
- `BigDecimal` para cantidades monetarias;
- persistir antes de publicar `GoalCompleted`;
- mantener el stream SSE en el App Shell;
- no introducir microservicios, Kafka o Redis para este alcance.

La diferencia fundamental fue:

> **La IA podía proponer o implementar; el desarrollador decidía qué debía formar parte de la solución y validaba el resultado.**

---

## 7. Decisiones rechazadas o modificadas

### 7.1 SSE en lugar de WebSockets

Durante el diseño se evaluó la necesidad de comunicación realtime.

La solución seleccionada fue **Server-Sent Events**, porque el requerimiento principal consiste en notificaciones:

**server → client**

Los comandos y consultas continúan utilizando REST.

WebSockets habría permitido comunicación bidireccional, pero no era necesaria para este caso y habría introducido una solución más compleja para el requerimiento planteado.

**Decisión:** utilizar SSE.

**Razón:** menor complejidad y ajuste directo al patrón de comunicación requerido.

---

### 7.2 Evitar sobreingeniería distribuida

Durante el análisis arquitectónico se consideraron alternativas como microservicios, brokers y mecanismos de mensajería durable.

No se incorporaron Kafka, Redis ni una arquitectura de microservicios al MVP.

La razón fue deliberada:

- el dominio es pequeño;
- la prueba no requiere despliegue distribuido;
- el objetivo es demostrar separación de responsabilidades, reglas de negocio, persistencia y realtime;
- introducir infraestructura distribuida habría aumentado considerablemente la complejidad operacional.

Estas alternativas se mantienen como posibles evoluciones si el sistema necesitara escalabilidad o entrega durable de eventos.

---

### 7.3 Iteración sobre el manejo de Broken Pipe

Durante la ejecución real apareció:

`java.io.IOException: Broken pipe`

El problema mostró que una conexión SSE muerta podía provocar que una contribución ya persistida terminara afectando la request HTTP.

La primera solución fue revisada y endurecida para tratar la entrega SSE como **best-effort**:

- el error se maneja en el boundary de `SseEmitter.send()`;
- el emitter muerto se elimina del registro;
- un cliente defectuoso no afecta a otros;
- la operación de negocio no debe fallar por una desconexión SSE.

Posteriormente apareció un segundo problema:

`HttpMessageNotWritableException`

Spring intentaba procesar el error mediante `GlobalExceptionHandler` y serializar `ApiError` mientras la respuesta ya tenía:

`Content-Type: text/event-stream`

Esto llevó a una segunda iteración específica del lifecycle async de SSE, evitando que los errores de una conexión SSE intentaran convertirse en respuestas REST.

**Resultado:** 110 tests backend en verde y validación manual del flujo completo.

---

### 7.4 SSE ownership: Dashboard → App Shell

Durante la integración frontend se detectó un problema de lifecycle.

Inicialmente el Dashboard era responsable de:

- abrir EventSource;
- suscribirse al stream;
- cerrar EventSource al destruirse.

Esto provocaba que al navegar:

`/savings-goals → /savings-goals/:id/contribute`

el Dashboard se destruyera y el stream SSE se cerrara antes de que pudiera llegar el evento de finalización.

La solución fue modificar el ownership:

**Dashboard → App Shell**

El App Shell mantiene:

- la conexión SSE;
- la suscripción;
- el estado del evento de finalización;
- el diálogo.

Así, el evento puede llegar independientemente de la ruta activa.

Esta fue una decisión basada en el comportamiento real observado durante la integración.

---

## 8. Debugging como proceso de co-creación

### Problema 1 — Broken Pipe

**Problema:** un cliente SSE desconectado provocaba un error de escritura.

**Análisis:** la entrega SSE es independiente de la operación de negocio.

**Solución:** tratar la entrega como best-effort y eliminar el emitter muerto.

**Validación:** tests de publisher y suite completa.

---

### Problema 2 — `HttpMessageNotWritableException`

**Problema:** `ApiError` intentaba escribirse sobre una respuesta `text/event-stream`.

**Análisis:** el error SSE estaba entrando en el mecanismo global de excepciones REST durante un async dispatch.

**Solución:** manejo específico del lifecycle SSE y protección del handler global para ese escenario.

**Validación:** pruebas MVC/integración y suite completa.

---

### Problema 3 — pérdida del evento al navegar

**Problema:** el Dashboard destruía el EventSource al navegar al formulario de contribución.

**Análisis:** el evento `GoalCompleted` no tiene replay y podía producirse mientras el Dashboard estaba desmontado.

**Solución:** mover la conexión y el estado del evento al App Shell.

**Validación:** tests de integración frontend y prueba manual del flujo:

**contribuir → 100%/COMPLETED → diálogo de meta completada**

---

## 9. Validación humana

Los cambios no se consideraron terminados únicamente porque Cline reportara éxito.

El proceso de validación incluyó:

1. revisión de los cambios;
2. revisión de responsabilidades arquitectónicas;
3. ejecución de tests;
4. `git diff --check`;
5. validación manual de funcionalidades críticas;
6. commits incrementales;
7. push al repositorio después de la revisión.

Resultados documentados durante el desarrollo:

- Backend: **110 tests, 0 failures, 0 errors**.
- Frontend: **67 tests/specs**, además de validación TypeScript y build.
- Flujo E2E de completación: validado manualmente.

No se declara un porcentaje de cobertura porque no se midió una métrica de cobertura como parte de la prueba.

---

## 10. Principios utilizados para el desarrollo asistido por IA

Se siguieron estos principios:

- **IA como asistente, no como autoridad.**
- Validar cambios mediante tests.
- Preferir cambios pequeños e incrementales.
- No aceptar cambios arquitectónicos sin revisión.
- Evitar sobreingeniería.
- Mantener separación de responsabilidades.
- Evitar dependencias innecesarias.
- Documentar explícitamente las limitaciones del MVP.
- Diferenciar código generado de decisiones arquitectónicas.
- Revisar manualmente los problemas que aparecen durante la ejecución real.

Un ejemplo claro fue SSE: la implementación inicial funcionaba en pruebas aisladas, pero la integración real reveló problemas de conexión y lifecycle. El comportamiento observado llevó a nuevas iteraciones en lugar de asumir que la primera implementación era correcta.

---

## 11. Conclusión

La IA permitió acelerar diferentes etapas del proyecto: implementación, generación de pruebas, debugging, análisis y documentación.

Sin embargo, el proceso mantuvo una separación clara entre **asistencia de implementación** y **responsabilidad técnica**.

Las decisiones sobre arquitectura, alcance, patrones, tecnología, trade-offs y aceptación final de los cambios fueron revisadas por el desarrollador.

El resultado es un proceso de desarrollo asistido por IA en el que:

**IA propone/implementa → desarrollador revisa → tests validan → comportamiento real confirma → se acepta o modifica.**
