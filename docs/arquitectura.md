# Arquitectura

## 1. Resumen

Savings Wallet es una aplicación Full Stack para la gestión de metas de ahorro.

El sistema permite:

- Crear metas de ahorro.
- Consultar metas.
- Registrar abonos.
- Validar reglas de negocio.
- Calcular el progreso.
- Detectar metas completadas.
- Notificar cambios mediante comunicación realtime.

---

## 2. Arquitectura

Se utiliza un **Modular Monolith con principios de Hexagonal Architecture**.

La solución separa:

```text
Domain
   ↓
Application
   ↓
Adapters
   ↓
Infrastructure
```

El objetivo principal es mantener las reglas de negocio independientes de frameworks, persistencia y mecanismos de comunicación.

### Estructura conceptual

```text
Angular
   │
REST / SSE
   │
   ▼
Adapters
   │
   ▼
Application
   │
   ▼
Domain
   │
   ▼
Ports
   │
   ▼
Infrastructure
```

---

## 3. Stack

### Backend

- Java 21
- Spring Boot
- Spring Data JPA
- SQLite
- Server-Sent Events (SSE)
- JUnit 5
- Mockito

### Frontend

- Angular
- TypeScript strict
- RxJS

---

## 4. Dominio

El agregado principal es:

```text
SavingsGoal
```

Una meta contiene:

- `id`
- `userId`
- `name`
- `targetAmount`
- `accumulatedAmount`
- `status`
- `createdAt`
- `updatedAt`

El modelo utiliza un Value Object `Money` para representar valores monetarios.

Los montos utilizan `BigDecimal`.

---

## 5. Reglas de negocio

Las reglas principales se mantienen dentro del dominio:

- El nombre de la meta es obligatorio.
- El objetivo debe ser mayor que cero.
- Los abonos deben ser mayores que cero.
- Un abono no puede superar el monto restante.
- Una meta completada no acepta nuevos abonos.
- Una meta pasa a `COMPLETED` al alcanzar el 100%.
- El monto acumulado nunca puede superar el objetivo.

Invariante:

```text
0 <= accumulatedAmount <= targetAmount
```

---

## 6. Casos de uso

El backend contempla inicialmente:

```text
CreateSavingsGoal
GetSavingsGoals
AddContribution
```

Los casos de uso coordinan las operaciones mientras las reglas de negocio permanecen en el dominio.

---

## 7. Persistencia

Se utiliza SQLite mediante Spring Data JPA.

El acceso a datos está abstraído mediante un Repository Port, permitiendo sustituir posteriormente la implementación de persistencia sin modificar las reglas principales del dominio.

Las entidades JPA se mantienen separadas de las entidades de dominio.

---

## 8. Comunicación realtime

Se utiliza **Server-Sent Events (SSE)**.

Cuando un abono completa una meta:

```text
SavingsGoal
     ↓
GoalCompleted
     ↓
Event Publisher
     ↓
SSE
     ↓
Angular
```

SSE fue seleccionado porque el requerimiento necesita principalmente comunicación server-to-client.

---

## 9. Patrones de diseño

### Repository

Abstrae el acceso a la persistencia:

```text
SavingsGoalRepository
        ↓
JpaSavingsGoalRepository
```

### Observer / Publish-Subscribe

Permite reaccionar a eventos de dominio como:

```text
GoalCompleted
```

### Adapter

Conecta el núcleo de la aplicación con tecnologías externas como REST, JPA y SSE.

---

## 10. Ownership

Cada meta está asociada a un `userId`.

El MVP no implementa autenticación ni autorización, ya que no forman parte del alcance funcional principal.

El modelo permite evolucionar posteriormente hacia un contexto de usuario autenticado.

---

## 11. Testing

Se implementan pruebas para:

- Reglas del dominio.
- Casos de uso.
- Endpoints principales.
- Flujos críticos del frontend.

El objetivo es validar tanto el comportamiento de negocio como la integración entre componentes.

---

## 12. Principios de diseño

La solución prioriza:

- Separation of Concerns.
- Dependency Inversion.
- Testability.
- Type Safety.
- Domain isolation.
- Simplicidad.
- Evolución incremental.

Se evita introducir complejidad arquitectónica que no esté justificada por los requisitos actuales.