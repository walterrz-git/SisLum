# SisLum

Prototipo de escritorio para el centro de medicina estética Lumina, desarrollado en Java con Swing y MySQL para el trabajo práctico 3.

Implementa el circuito **Registrar turno → Registrar atención → Registrar cobro**, con alta y búsqueda de pacientes, consulta de agenda e historia clínica. Los cobros se pueden consultar por fecha del turno, paciente y estado Pendiente, Pagado o Todos.

## Contenido

- [app](app/README.md): proyecto Maven, código Java, formularios de NetBeans, scripts SQL e instrucciones de ejecución.
- [entorno](entorno/README.md): utilidades del entorno local en macOS.
- [futuros-casos-de-uso](futuros-casos-de-uso/README.md): material separado para funcionalidades posteriores; no forma parte del proyecto Maven activo.

## Ejecutar

Se requiere Java 17 o superior, Maven o NetBeans con soporte Maven y MySQL. Las dependencias Java se descargan desde Maven.

1. Ejecutar en MySQL los archivos `app/sql/01_esquema.sql`, `02_registrar_turno.sql` y `03_registrar_cobro.sql`, en ese orden.
2. Configurar `SISLUM_DB_URL`, `SISLUM_DB_USER` y `SISLUM_DB_PASSWORD` con los datos de la conexión. La URL predeterminada apunta a la base `SisLum` en `127.0.0.1:3306`.
3. Abrir la carpeta `app` en NetBeans y ejecutar **Run Project**. Desde una terminal con las variables configuradas también se puede usar `cd app` y `mvn compile exec:java`.

La clase principal es `ar.com.sislum.SisLum`. Las instrucciones completas se encuentran en [app/README.md](app/README.md).

## Alcance

Esta etapa no incluye login, roles, administración de catálogos, configuración individual de horarios, bloqueos ni feriados. Los profesionales y tratamientos iniciales son ficticios; no se incluyen datos de pacientes ni una copia de la base local.

Las credenciales y los archivos generados permanecen fuera del repositorio.
