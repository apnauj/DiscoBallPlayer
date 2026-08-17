# DiscoBallPlayer

Proyecto universitario — aplicación de escritorio en Java.

## Estructura del proyecto

- `model/` — Entidades del dominio
- `structures/` — Estructuras de datos implementadas manualmente
- `service/` — Lógica de negocio
- `repository/` — Persistencia de datos
- `controller/` — Puente entre la UI y los servicios
- `ui/` — Interfaz gráfica
- `util/` — Clases utilitarias
- `exception/` — Excepciones personalizadas

## Cómo ejecutar

```
mvn clean install
mvn exec:java -Dexec.mainClass="com.juanpa.discoballplayer.Main"
```
