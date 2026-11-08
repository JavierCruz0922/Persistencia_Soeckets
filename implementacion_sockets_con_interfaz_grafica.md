# Implementación de sockets con interfaz gráfica en Sokects1

Esta guía adapta el documento original al proyecto Maven existente en NetBeans. Se conservan sus paquetes, nombres de clases y archivos `.form`.

## Requisitos y ejecución en NetBeans

1. Abre la carpeta `Sokects1` mediante **File > Open Project**.
2. Selecciona un **JDK 17 o posterior** para el proyecto. El equipo tiene JDK 26; el `pom.xml` genera bytecode compatible con Java 17.
3. Ejecuta **Clean and Build**. Maven necesita Internet la primera vez para descargar sus plugins.
4. Pulsa **Run Project (F6)** y selecciona **Servidor**. El registro debe indicar `Servidor iniciado en puerto 59420`.
5. Haz clic derecho en `com.mycompany.sokects1.LoginCliente` y elige **Run File (Shift+F6)**. Usa `127.0.0.1`, puerto `59420` y un nombre.
6. Repite el paso anterior con otro nombre para abrir un segundo cliente. Escribe un mensaje y pulsa **Enter**.
7. Escribe `chao` o cierra la ventana para salir. Cierra el servidor para desconectar todos sus clientes y liberar el puerto.

También puedes ejecutar directamente `com.mycompany.sokects1.Servidor` con **Run File**. `ChatCliente.main` abre el formulario de conexión para evitar iniciar un chat sin una sesión.

## Correspondencia con el documento original

| Documento de referencia | Clase real del proyecto | Responsabilidad |
| --- | --- | --- |
| `chat.Servidor` | `Chat.Servidor` | Escuchar conexiones y retransmitir mensajes |
| `chat.ManejadorCliente` | `Chat.Listener` | Leer el nombre y atender un cliente |
| `chat.Cliente` | `Chat.Cliente` | Conectar, enviar y escuchar |
| `chat.ServidorGUI` | `com.mycompany.sokects1.Servidor` | Mostrar el registro del servidor |
| `chat.ClienteLoginGUI` | `com.mycompany.sokects1.LoginCliente` | Validar y solicitar la conexión |
| `chat.ClienteChatGUI` | `com.mycompany.sokects1.ChatCliente` | Mostrar el historial y enviar texto |
| Punto de entrada | `com.mycompany.sokects1.Sokects1` | Elegir servidor o cliente al pulsar F6 |

**Importante:** el paquete existente se llama `Chat`, con mayúscula. La clase lógica `Chat.Servidor` y la ventana `com.mycompany.sokects1.Servidor` tienen el mismo nombre simple; la ventana usa el nombre completo para distinguirlas.

## Formularios de NetBeans

Los controles, propiedades, distribución y eventos están declarados en cada archivo `.form` y en los bloques `GEN-BEGIN` correspondientes del `.java`. Las tres ventanas se pueden abrir, visualizar y editar en la pestaña **Design** de NetBeans. Usa el árbol **Navigator** para seleccionar componentes dentro de paneles y barras de desplazamiento.

La lógica de sockets permanece fuera de `initComponents()`. NetBeans puede regenerar ese método sin eliminar la conexión, los observadores ni los manejadores de eventos. El servidor no abre puertos durante el diseño.

## Lógica de comunicación y ajustes

- TCP usa líneas de texto UTF-8. La primera línea es el nombre; las siguientes son mensajes.
- El servidor envía a los demás clientes `nombre--> mensaje`; el remitente muestra su mensaje localmente.
- `Listener` tiene un constructor con su nombre correcto. El original declaraba `ManejadorCliente` dentro de `Listener`, lo que impedía compilar.
- La colección de conexiones admite acceso concurrente. Cerrar el servidor cierra también los sockets aceptados, incluidos los que aún no enviaron su nombre.
- La conexión del cliente se realiza con `SwingWorker` y un límite de cinco segundos; la interfaz permanece disponible mientras conecta.
- Los eventos recibidos se aplican mediante `SwingUtilities.invokeLater`. Se notifica la desconexión tanto ante errores como ante fin normal del flujo.
- Se validan nombre, dirección y puerto. Si falla la conexión, el formulario permite corregir y reintentar.
- El cierre de una ventana libera sus recursos sin terminar otras ventanas con `System.exit`.
- Las clases, constructores y métodos incluyen Javadoc; los parámetros se documentan con `@param`, y se agregan `@return` y `@throws` donde aplican.

## Ejecución desde una terminal

Desde la carpeta del proyecto, con Java y Maven configurados:

```powershell
mvn clean package
java -jar target\Sokects1-1.0-SNAPSHOT.jar servidor
```

En otras dos terminales, una por cliente:

```powershell
java -jar target\Sokects1-1.0-SNAPSHOT.jar cliente
```

Si omites el argumento, aparece el selector de modo. También puedes usar `mvn exec:java` para abrirlo.

## Verificación y Javadoc

```powershell
.\verificar.ps1 -Jdk 'C:\Program Files\Java\jdk-26.0.2.1'
$fuentes = @(Get-ChildItem src\main\java -Recurse -Filter '*.java' | ForEach-Object FullName)
javadoc -encoding UTF-8 -charset UTF-8 -d target\apidocs @fuentes
```

La prueba independiente usa sockets reales y verifica mensajes en ambos sentidos, acentos, salida `chao`, desconexión local, cierre del servidor, puerto ocupado, liberación del puerto y reinicio. No necesita bibliotecas de pruebas. El índice de la documentación HTML queda en `target/apidocs/index.html`.

## Problemas habituales

- **Conexión rechazada:** inicia primero el servidor y verifica dirección y puerto.
- **Address already in use:** ya hay un servidor en el puerto 59420. Cierra la instancia anterior antes de abrir otra.
- **Dos equipos:** usa en el cliente la IP local del equipo servidor y permite Java/TCP 59420 en su firewall. `127.0.0.1` solo sirve para clientes en el mismo equipo.
- **No llegan mensajes:** abre al menos dos clientes; el historial de cada cliente también muestra sus propios mensajes.

Esta implementación corresponde a un chat académico de texto en una red de confianza: no incluye autenticación, cifrado ni persistencia de mensajes.

## Acciones de ejecución de NetBeans

El archivo `nbactions.xml` fija la clase principal de **Run Project (F6)** en `com.mycompany.sokects1.Sokects1` y configura las acciones de depuración y perfilado. Evita que NetBeans intente ejecutar literalmente `${packageClassName}` al no haber un archivo seleccionado.

Para abrir varias instancias también están disponibles las acciones **Ejecutar servidor** y **Ejecutar cliente** desde el menú contextual del proyecto, en **Run Maven / Ejecutar Maven**. **Run File (Shift+F6)** conserva la selección de la clase que se está editando.

Si NetBeans conserva una ejecución anterior, detén esa ejecución y vuelve a pulsar **F6**. Si no actualiza las acciones, cierra y vuelve a abrir el proyecto. El `pom.xml` mantiene el mismo punto de entrada para Maven y para el manifiesto del JAR.
