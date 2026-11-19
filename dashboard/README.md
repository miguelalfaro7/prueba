# Dashboard local

Desde la raíz ejecute `docker compose up -d --build` según la [guía de arranque](../README.md#arranque-en-windows). Abra
[index.html](index.html) en un navegador. El HTML se ejecuta localmente y se
conecta a cuatro servicios en loopback:

| Uso | Dirección predeterminada |
|---|---|
| Lecturas y estado de planta | `ws://localhost:8081/ws/plant` |
| Alertas y opciones del operador | `ws://localhost:8082/ws/operator` |
| Registro de decisiones | `http://localhost:8083/decide` |
| Consultas de historial | `http://localhost:8090/history` |

Si Compose publica en puertos alternativos, abra el HTML con sus cuatro
parámetros de consulta. Desde la raíz del proyecto, PowerShell puede construir
la dirección completa:

```powershell
$dashboard = ([Uri](Resolve-Path -LiteralPath dashboard/index.html).Path).AbsoluteUri
"$($dashboard)?plant=28081&operator=28082&dispatch=28083&history=28090"
```

Pegue la dirección resultante en el navegador. Una decisión diferida aparece
primero en el historial. Su ejecución se observa en los logs de `maintenance_worker`.
La espera se configura en Compose. El entorno normal usa 24 horas.
