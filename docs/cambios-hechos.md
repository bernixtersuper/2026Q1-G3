# MenuQR — Cambios e implementación realizada

Documento de registro del estado actual de la solución respecto a la propuesta de TP1, los requisitos de TP4 y el feedback recibido en correcciones parciales.

**Última actualización:** junio 2026

---

## 1. Resumen ejecutivo

MenuQR es una plataforma multi-tenant para menús digitales vía QR. La implementación actual funciona de punta a punta: panel admin, menú público, backend en AWS, analítica, pipeline ML asíncrono y despliegue automatizado con Terraform + GitHub Actions.

El producto **funciona y está bien encaminado** (feedback de corrección). Lo pendiente principal apunta a **potenciar analytics** y **optimizar el esquema de DynamoDB** (ver [propuestas-mejoras.md](./propuestas-mejoras.md)).

---

## 2. Alineación con la propuesta de TP1

| Funcionalidad propuesta (TP1) | Estado | Implementación |
|-------------------------------|--------|----------------|
| Panel de administración del menú | ✅ Hecho | SPA admin (React/Vite) + API Quarkus protegida con Cognito |
| Menú interactivo vía QR | ✅ Hecho | SPA menú público + slug por restaurante |
| Filtros por restricciones alimentarias | ✅ Hecho | Tags dietéticos + eventos `FILTER_USED` |
| Analítica de interacción | ✅ Parcial | Dashboard con KPIs, gráficos y tiempo real; mejorable (ver propuestas) |
| Segmentación / ML | ✅ Parcial | Pipeline SQS + Lambda; modelo de popularidad por ítem (MREC), no clustering completo |

---

## 3. Cumplimiento de requisitos TP4

| Requisito TP4 | Estado | Evidencia |
|---------------|--------|-----------|
| Diagrama fiel a la implementación | ✅ | `Architecture.png` + Terraform en `terraform/` |
| Implementación completa de componentes propuestos | ✅ | VPC, ECS, RDS, DynamoDB, S3, Cognito, Lambda, SQS, EventBridge |
| Elasticidad / desacople (SQS o SNS) | ✅ | Cola `ml-training-queue` → Lambda worker |
| Autenticación con Cognito | ✅ | User Pool + client SPA admin; JWT en endpoints `/api/admin/*` |
| Terraform | ✅ | Infraestructura principal en Terraform (evaluado en entrega 3) |

### Rubrica TP4 — cobertura actual

| Criterio (peso) | Situación actual |
|-----------------|------------------|
| Teoría Cloud / Presentación (40%) | Arquitectura multi-tier, servicios managed, justificación documentada en README |
| Diagrama (10%) | Diagrama actualizado con VPC, AZs, endpoints, pipeline ML |
| Cognito y Seguridad (20%) | Cognito + JWT; RDS privado + Secrets Manager + RDS Proxy TLS; buckets privados |
| Buen uso de SQS/SNS (10%) | SQS con batch failures parciales; orquestador desacoplado del worker |
| Calidad y completitud (20%) | Flujo funcional admin → menú → eventos → analytics → ML |

---

## 4. Infraestructura AWS implementada

### Red y compute

- **VPC** `172.30.0.0/16` con 2 AZs: subnets públicas, privadas (app) y de datos.
- **NAT Gateway** uno por AZ (`one_nat_gateway_per_az = true`) para salida HA desde subnets privadas.
- **ALB** público → **ECS Fargate** (2 tareas, puerto 8080) en subnets privadas.
- **VPC Endpoints**: S3 y DynamoDB (gateway); Secrets Manager, SQS, ECR API/DKR (interface).

### Datos

- **RDS PostgreSQL 18** Multi-AZ (`db.t4g.micro`) — datos transaccionales multi-tenant.
- **RDS Proxy** con TLS obligatorio y credenciales en Secrets Manager.
- **DynamoDB** `menuqr-events` — eventos de interacción (PK/SK por tenant + timestamp).
- **S3 privado**: imágenes de menú y modelos ML (versionado + cifrado).
- **S3 público**: hosting estático de SPAs admin y menú (website hosting).

### Integración y ML

- **EventBridge** (cron diario) → **Lambda orquestador** (VPC) → encola jobs en **SQS**.
- **Lambda worker** (sin VPC) consume SQS, agrega eventos DynamoDB y sube artefacto MREC a S3.
- **ECR** para imágenes del backend; scan on push; lifecycle policy (conserva las 10 imágenes más recientes).

### Seguridad e identidad

- **Cognito User Pool**: email/password, política de contraseñas, tokens con expiración configurada.
- **LabRole** (AWS Academy): rol único para ECS, Lambda y RDS Proxy (restricción del lab).
- Security groups con reglas explícitas ALB → Fargate → Proxy → RDS.

### CI/CD y estado

- **Terraform remote state**: S3 + DynamoDB locks (`terraform/bootstrap/`).
- **GitHub Actions**: deploy completo, tfsec, validate/plan, terraform-docs.
- Scripts: `deploy.sh`, `deploy-backend.sh`, `deploy-frontends.sh`.

---

## 5. Cambios y decisiones técnicas relevantes

### Backend (Quarkus)

- Autenticación admin vía **Cognito access token** (`@Authenticated` en recursos admin).
- Bootstrap de sesión con **ID token** verificado (`/api/auth/session`).
- **Multi-tenancy** resuelto en `TenantRequestFilter` a partir del `sub` de Cognito.
- Imágenes en bucket S3 **privado** (sin acceso público); lectura vía **URLs prefirmadas** (TTL 1 h). Eliminado el proxy público `/api/media`.
- **CORS** restringido en producción a las URLs de las SPAs (admin + menú en S3); dev local usa `localhost:5173/5174` por defecto.
- Credenciales DB desde **Secrets Manager** con cache configurable.
- Recomendaciones consumen modelo **MREC** generado por el pipeline ML en S3.

### Frontend

- **Admin SPA**: gestión de menú, pedidos, tema, analytics con gráficos (Recharts).
- **Menú SPA**: vista pública, filtros, tracking de eventos, recomendaciones.
- Deploy estático a S3 con variables de entorno (`VITE_API_URL`, Cognito).

### Analytics implementado

**Tipos de evento registrados** (`EventType`):

- `MENU_VIEW`, `ITEM_VIEW`, `SECTION_VIEW`, `FILTER_USED`

**Esquema DynamoDB actual:**

```
PK = TENANT#{tenantId}
SK = EVENT#{timestamp}#{eventId}
```

Atributos: `eventType`, `sessionId`, `itemId`, `sectionId`, `metadata`, `timestamp`.

**Métricas expuestas en el dashboard admin:**

- KPIs: vistas 30d, vistas hoy, sesiones únicas, profundidad promedio de sesión.
- Gráfico diario de vistas (menú vs ítems).
- Heatmap hora × día de la semana.
- Ranking top 10 ítems con tasa de vista y flag *trending*.
- Uso de filtros dietéticos.
- Engagement por sección.
- Panel en tiempo real (buckets de 5 min, últimos 60 min).

**Pipeline ML:**

- Orquestador lista tenants desde PostgreSQL y encola un mensaje SQS por tenant/día.
- Worker agrega `ITEM_VIEW` del día en DynamoDB y genera `recommendations/{tenantId}/model.bin`.

---

## 6. Feedback de corrección — estado

| Observación del corrector | Estado |
|---------------------------|--------|
| El producto funciona y vienen bien encaminados | ✅ Confirmado |
| Aprovechar y potenciar la parte de analytics | 🔄 En progreso — dashboard base existe; faltan optimizaciones de consulta y profundidad analítica |
| Repensar esquemas de DynamoDB según gráficos y métricas | ⏳ Pendiente — esquema actual es genérico; filtrado por `eventType` se hace en memoria |

---

## 7. Decisiones de diseño aceptadas (Learner Lab)

### Tráfico HTTP (sin HTTPS)

**Decisión:** ALB y SPAs en S3 website hosting usan HTTP. No es deuda técnica pendiente en este entorno.

**Motivo:**

- Route 53 en el lab **no permite registrar dominios** → no hay hostname propio para validar un certificado ACM público.
- El ALB expone un DNS `*.elb.amazonaws.com` que no controlamos; ACM no emite certificados para ese dominio.
- Los website endpoints de S3 son HTTP por naturaleza; CloudFront (alternativa habitual para HTTPS sin dominio propio) **no figura** en el listado de servicios del Learner Lab.
- ACM está disponible en el lab, pero sin dominio verificable no cierra el circuito HTTPS de punta a punta.

**Para la defensa TP4:** documentar como **limitación consciente del entorno académico**, no como omisión. En producción se usaría CloudFront + ACM o ALB 443 con dominio propio.

### IAM con LabRole único

**Decisión:** ECS, Lambda y RDS Proxy usan el rol preexistente `LabRole`. No es deuda técnica ni algo a “corregir” en TP4.

**Motivo (Learner Lab):**

- IAM tiene **acceso extremadamente limitado**: no se pueden crear usuarios, grupos ni roles custom (salvo service-linked roles).
- AWS Academy **exige** `LabRole` en ECS task definitions, Lambdas, RDS Proxy, etc.; crear roles propios falla o no está permitido.
- El propio listado del lab indica: *“Attach the existing LabRole to any function that you create if that function will need permissions…”*

**Para la defensa TP4:** explicar que en producción se separarían roles por servicio (least privilege: task role ECS ≠ role Lambda worker). En el lab, la segmentación de permisos se compensa parcialmente con **security groups** y recursos en subnets privadas.

---

## 8. Resiliencia — estado actual

### Ya implementado

| Mecanismo | Dónde | Efecto |
|-----------|-------|--------|
| **2 Availability Zones** | VPC, subnets, ALB | Compute y balanceo distribuidos |
| **ECS Fargate × 2 tareas** | `desired_count = 2` | Backend sobrevive caída de una tarea |
| **ALB health checks** | `/q/health/ready` | Tráfico solo a tareas sanas |
| **RDS PostgreSQL Multi-AZ** | `multi_az = true` | Failover automático de BD |
| **RDS Proxy** | Entre ECS/Lambda y RDS | Pooling; menos agotamiento de conexiones |
| **Backups RDS** | `backup_retention_period = 7` | Point-in-time recovery operativo |
| **Snapshot final en destroy** | `skip_final_snapshot = false` | Respaldo al destruir infra |
| **SQS + worker desacoplado** | Pipeline ML | Fallos parciales no bloquean el cron entero |
| **`ReportBatchItemFailures`** | Lambda worker | Reintentos por mensaje, no por lote entero |
| **NAT Gateway × AZ** | `one_nat_gateway_per_az = true` | Salida a internet sin SPOF por zona |
| **VPC endpoints** | S3, DynamoDB, ECR, Secrets, SQS | Tráfico AWS sin depender del NAT |

### Pendiente (mejora real de resiliencia — ver propuestas)

| Mecanismo | Prioridad | Impacto |
|-----------|-----------|---------|
| **DLQ** cola ML | P1 | No perder jobs fallidos en silencio |
| **Alarmas CloudWatch** | P2 | Detectar fallos del pipeline y 5xx en ALB |
| **Auto Scaling ECS** | P2 | Elasticidad en picos (consigna TP4) |
| **Circuit breaker** en deploy ECS | P2 | Rollback automático de deploys rotos |

### Evolución en producción (no aplica al lab)

- Auto Scaling con techo mayor + métricas custom (RPS)
- Interface endpoints adicionales (`logs`, `cognito-idp`)
- Multi-región / DR (fuera de alcance TP)

**Para la defensa TP4:** distinguir *“qué resiliencia tenemos hoy”* vs *“qué haríamos con presupuesto prod”*.

---

## 9. Deuda técnica conocida (registrada, no resuelta aún)

Estos puntos están identificados pero **no forman parte de lo ya implementado**. Detalle y plan en [propuestas-mejoras.md](./propuestas-mejoras.md):

- Sin DLQ en la cola ML.
- Sin auto scaling en ECS.
- Sin circuit breaker en deploy ECS.
- Sin alarmas CloudWatch declaradas en Terraform.
- DynamoDB sin TTL ni GSI para consultas analíticas eficientes.
- Endpoints públicos de eventos sin rate limiting.

---

## 10. Referencias

- Propuesta original: [propuestaTP1.txt](./propuestaTP1.txt)
- Consigna y rubrica TP4: [ConsignaTP4.txt](./ConsignaTP4.txt)
- Feedback recibido: [Corecciones.txt](./Corecciones.txt)
- Restricciones AWS Academy: [ListadoServiciosLearnerLab.txt](./ListadoServiciosLearnerLab.txt)
- Propuestas de mejora: [propuestas-mejoras.md](./propuestas-mejoras.md)
