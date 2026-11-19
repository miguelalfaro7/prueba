# Plataforma de mantenimiento predictivo IoT

La aplicación simula diez sensores industriales. Kafka transporta lecturas y alertas,
RabbitMQ distribuye las decisiones del operador y PostgreSQL conserva el historial.
El stack local tiene 13 servicios y 15 contenedores, incluidas dos réplicas de cada worker.

## Arranque en Windows

1. Abra Docker Desktop con el motor de contenedores Linux y el backend WSL 2.
   Compruebe que `docker version` muestra cliente y servidor y que `docker compose version`
   responde. La primera compilación necesita conexión a los registros de imágenes, Maven Central y GitHub.
2. Desde la raíz, cree `.env` solo si todavía no existe y edite los valores de ejemplo.
   Use un usuario de RabbitMQ distinto de `guest` y contraseñas locales propias.
   Los usuarios de RabbitMQ y PostgreSQL se inicializan al crear sus volúmenes.
   Maven y Java 17 instalados en Windows solo son necesarios para ejecutar las pruebas unitarias fuera de Docker.

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   ```

3. Construya y levante el sistema desde la raíz.

   ```powershell
   docker compose up -d --build
   docker compose ps
   ```

4. Abra [dashboard/index.html](dashboard/index.html) en el navegador. Con los puertos
   predeterminados usa `ws://localhost:8081/ws/plant`, `ws://localhost:8082/ws/operator`,
   `http://localhost:8083/decide` y `http://localhost:8090/history`.
   Los servicios Kafka, ZooKeeper, RabbitMQ y PostgreSQL se comunican por la red interna de Compose.

Use `docker compose stop` para detener el proyecto y `docker compose start` para
volver a ejecutar sus contenedores. Si arrancó con `-p`, indique el mismo nombre
al consultar, detener o iniciar ese proyecto.

## Redes y credenciales

Kafka, ZooKeeper, RabbitMQ y PostgreSQL no publican puertos al host.
PostgreSQL solo pertenece a la red interna `database`. `history_service` y
`action_dispatcher` también pertenecen a esa red. El primero consulta y guarda
el historial, y el segundo usa PostgreSQL para la auditoría y la bandeja de
salida de decisiones. Los demás servicios permanecen en la red del proyecto.
El dashboard consulta el historial mediante HTTP en loopback. Complete las
contraseñas en `.env`; Git ignora ese archivo.

## Administración de PostgreSQL

```powershell
docker compose exec postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose exec postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose port postgres 5432
docker volume ls --filter 'label=com.docker.compose.volume=pgdata'
```

`docker compose port postgres 5432` debe indicar que el puerto no está publicado. Dentro de
`psql`, use `\dt` para listar tablas y `\q` para salir. El esquema de
`database/init.sql` se aplica al inicializar un volumen PostgreSQL vacío.
Para reutilizar los datos, conserve el mismo nombre de proyecto y el volumen
`<proyecto>_pgdata` en el mismo motor Docker. El nombre predeterminado es
`iot-industrial-local`; para otro proyecto use su nombre exacto con `-p`.
Antes de reutilizar un volumen de una versión anterior, compruebe el esquema
y las credenciales originales. No monte el mismo volumen en dos servidores
PostgreSQL activos.

Los cinco nombres lógicos de volumen son `pgdata`, `assignment_kafka_data`,
`assignment_rabbitmq_data`, `zookeeper_data` y `zookeeper_log`.
Kafka en modo ZooKeeper requiere sus volúmenes compatibles; el antiguo volumen
KRaft `kafka_data` no se puede reutilizar directamente. Para RabbitMQ, compruebe
también la identidad del nodo y el formato del volumen anterior.

## Despliegue histórico en EC2

El historial de Git documenta Docker Compose en EC2, PostgreSQL en contenedor
y acceso a los backends por reenvío de puertos SSM. La configuración histórica
usaba Kafka KRaft y un conjunto distinto de volúmenes. Para reutilizar este
Compose en EC2 hay que validar primero la migración de Kafka y los esquemas y
volúmenes existentes. El archivo actual define el stack local; los perfiles
o variables de puertos por sí solos no migran esos datos.

## Puertos alternativos

Establezca `IOT_PLANT_PORT`, `IOT_OPERATOR_PORT`, `IOT_DISPATCH_PORT` e
`IOT_HISTORY_PORT` en `.env` o en el entorno de la consola antes de arrancar.
Añada los mismos valores a la URL del HTML. Por ejemplo,
`dashboard/index.html?plant=28081&operator=28082&dispatch=28083&history=28090`.
La [guía del dashboard](dashboard/README.md) incluye cómo construir la URL en PowerShell.

## Verificación del sistema

Espere a que PostgreSQL y RabbitMQ indiquen `healthy` y a que las aplicaciones
Spring Boot terminen de iniciar. Kafka crea los topics de la aplicación con tres
particiones. Ejecute estos comandos para revisar los servicios y el flujo:

```powershell
docker compose ps
docker compose exec kafka kafka-topics --bootstrap-server kafka:19092 --describe
docker compose exec rabbitmq rabbitmq-plugins list -e -m
Invoke-RestMethod 'http://localhost:8090/history/readings?limit=5'
docker compose logs --tail 30 sensor_producer alert_detector alert_router actuator_worker maintenance_worker
```

En el dashboard deben cambiar las lecturas y aparecer alertas en la consola.
Elija una acción y verifique que aparezca en el historial y que el worker
correspondiente registre la simulación. `POST /decide` registra la decisión
antes del despacho. Repetir exactamente la solicitud devuelve `202` sin insertar
otra fila. Una acción distinta para la misma alerta devuelve `409`.

## Mantenimiento diferido

`RECONOCER_Y_ESPERAR_24H` se registra de inmediato y RabbitMQ entrega la orden de
mantenimiento tras **24 horas**. El publicador descuenta el tiempo transcurrido
desde el registro si hubo reintentos antes de llegar al broker.

Para comprobarlo en 15 segundos, abra otra ventana de PowerShell desde la raíz y
use un proyecto con sus propios puertos y volúmenes:

```powershell
$env:IOT_PLANT_PORT = '28081'
$env:IOT_OPERATOR_PORT = '28082'
$env:IOT_DISPATCH_PORT = '28083'
$env:IOT_HISTORY_PORT = '28090'
$env:IOT_MAINTENANCE_DELAY = '15s'
docker compose -p iot-delay-demo up --build -d
docker compose -p iot-delay-demo logs -f maintenance_worker
```

Abra el dashboard con los cuatro puertos anteriores, elija **Reconocer y esperar
24h** en una alerta de advertencia y compare la hora del historial con la del log
del worker. El texto de la acción sigue siendo el del ejercicio. Detenga la
prueba con `docker compose -p iot-delay-demo stop` y cierre esa consola para que las
variables de prueba dejen de aplicarse. El arranque normal usa `24h` por defecto.

Para ejecutar las suites unitarias con JDK 17 y Maven:

```powershell
Get-ChildItem services -Directory | ForEach-Object {
    mvn -f (Join-Path $_.FullName 'pom.xml') test
    if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas de $($_.Name)" }
}
```

Los ejemplos de mensajes están en [contracts/examples](contracts/examples).
La clave Kafka para las lecturas es `sensor_id`. RabbitMQ usa el exchange fanout
`human_alerts` y las colas `critical_actions_queue` y `maintenance_queue`.
