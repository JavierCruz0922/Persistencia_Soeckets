# Sokects1: chat con sockets e interfaz gráfica

## Descripción del proyecto

Sokects1 es una aplicación de chat desarrollada en Java que permite la comunicación entre varios usuarios mediante sockets TCP. Utiliza una arquitectura cliente-servidor: un servidor central recibe las conexiones y distribuye los mensajes, mientras cada cliente dispone de una interfaz gráfica para conectarse y conversar.

Las ventanas están construidas con Swing y organizadas como formularios de NetBeans. El proyecto utiliza Maven para compilar el código y generar un archivo JAR ejecutable. La lógica de comunicación está separada de las ventanas para mantener responsabilidades claras.

## Funcionalidades

- Iniciar un servidor TCP en el puerto `59420`.
- Conectar varios clientes mediante la dirección del servidor, el puerto y un nombre de usuario.
- Rechazar nombres ya conectados sin distinguir mayúsculas y minúsculas; por ejemplo, `poli1` y `POLI1` corresponden al mismo nombre.
- Enviar mensajes de texto y recibir los mensajes de los demás participantes.
- Mostrar avisos de ingreso y salida de usuarios.
- Consultar las conexiones y desconexiones en el registro del servidor.
- Desconectarse escribiendo `chao` o cerrando la ventana del chat.
- Detectar el cierre del servidor y desactivar la entrada de mensajes del cliente.

## Tecnologías utilizadas

| Tecnología | Uso en el proyecto |
| --- | --- |
| Java 17 o posterior | Lenguaje y entorno de ejecución |
| Swing | Ventanas, controles y eventos de la interfaz gráfica |
| `ServerSocket` y `Socket` | Comunicación TCP entre servidor y clientes |
| Hilos de Java | Aceptar conexiones y escuchar mensajes sin bloquear las ventanas |
| `SwingWorker` | Realizar conexiones y envíos fuera del hilo de eventos |
| Maven | Compilar, empaquetar y ejecutar la aplicación |
| NetBeans | Desarrollo del código y edición visual de los formularios |
| Javadoc | Documentación de clases, constructores y métodos |

## Estructura principal

```text
Sokects1/
├── pom.xml
├── nbactions.xml
├── implementacion_sockets_con_interfaz_grafica.md
└── src/main/java/
    ├── Chat/
    │   ├── Cliente.java
    │   ├── Listener.java
    │   └── Servidor.java
    └── com/mycompany/sokects1/
        ├── Sokects1.java
        ├── LoginCliente.java
        ├── LoginCliente.form
        ├── ChatCliente.java
        ├── ChatCliente.form
        ├── Servidor.java
        └── Servidor.form
```

El paquete `Chat`, con mayúscula inicial, contiene la lógica de red. El paquete `com.mycompany.sokects1` contiene el punto de entrada y las ventanas. Existen dos clases llamadas `Servidor`; su paquete permite distinguir el servidor de red de su interfaz gráfica.

## Descripción de las clases

### `Chat.Servidor`

Administra el servidor TCP y mantiene el conjunto de conexiones activas. Usa una colección concurrente para permitir que los hilos de los clientes consulten y actualicen las conexiones.

Su constructor recibe el puerto de escucha y un observador de eventos. La interfaz interna `ServidorObserver` define `onLog(String mensaje)`, que permite comunicar información a la ventana sin introducir dependencias de Swing en la lógica de red.

| Método | Responsabilidad |
| --- | --- |
| `iniciar()` | Abre el puerto y comienza a aceptar clientes en un hilo independiente |
| `aceptar(ServerSocket actual)` | Crea un `Listener` para cada conexión aceptada |
| `detener()` | Cierra la escucha y las conexiones de los clientes |
| `transmitirMensaje(String mensaje, Listener remitente)` | Envía el mensaje a los clientes distintos del remitente |
| `removerCliente(Listener cliente)` | Retira la conexión y libera su nombre para un nuevo ingreso |
| `registrarUsuario(String nombre, Listener cliente)` | Reserva el nombre de forma atómica, sin distinguir mayúsculas y minúsculas |
| `registrarLog(String mensaje)` | Notifica un evento al observador |

### `Chat.Listener`

Representa la atención de un cliente dentro del servidor. Implementa `Runnable`, por lo que cada instancia se ejecuta en un hilo propio. Su constructor recibe el socket aceptado y la instancia de `Chat.Servidor` que coordina la conversación.

La primera línea recibida solicita el nombre de usuario. El servidor responde `OK` si puede reservarlo o `ERROR: ...` si el nombre ya está ocupado. Solo después de la aceptación, el manejador lee los mensajes y solicita al servidor su retransmisión. Al recibir `chao`, finalizar el flujo o producirse un error, retira al cliente, cierra su socket y comunica su salida cuando corresponde.

| Método | Responsabilidad |
| --- | --- |
| `run()` | Lee el nombre, procesa mensajes y gestiona el final de la sesión |
| `enviarMensaje(String mensaje)` | Escribe una línea hacia el cliente identificado |
| `cerrar()` | Cierra el socket y desbloquea las lecturas pendientes |

### `Chat.Cliente`

Gestiona una sesión TCP desde el lado del usuario. Su constructor recibe la dirección del servidor, el puerto, el nombre del usuario y un observador. Valida los datos y utiliza UTF-8 para intercambiar texto.

La interfaz interna `ClienteObserver` define `onMensajeRecibido(String mensaje)` y `onDesconectado()`. Estos eventos permiten que la ventana reciba mensajes y detecte el cierre de la conexión. Cada instancia corresponde a una sola sesión; para conectarse de nuevo se crea otra instancia.

| Método | Responsabilidad |
| --- | --- |
| `conectar()` | Conecta, envía el nombre y espera hasta cinco segundos la aceptación antes de iniciar el hilo de escucha |
| `escuchar(BufferedReader in)` | Lee las líneas del servidor y notifica al observador |
| `enviarMensaje(String mensaje)` | Envía una línea y detecta errores de escritura |
| `desconectar()` | Cierra la conexión y notifica su final una sola vez |
| `getUsuario()` | Devuelve el nombre visible del usuario |

### `com.mycompany.sokects1.Sokects1`

Es el punto de entrada del proyecto. Su método `main(String[] args)` inicia la interfaz en el hilo de eventos de Swing.

Si no recibe argumentos, muestra un selector para abrir el servidor o un cliente. Con el argumento `servidor` abre la ventana de registro; con `cliente` abre el formulario de conexión. Esta clase está configurada como principal en Maven, en las acciones del proyecto de NetBeans y en el manifiesto del JAR.

### `com.mycompany.sokects1.LoginCliente`

Es el formulario de conexión. Permite introducir la dirección del servidor, el puerto y el nombre del usuario. Sus valores iniciales son `127.0.0.1`, `59420` y `poli01`.

El botón **Aceptar** valida los datos e inicia la conexión mediante `SwingWorker`. Mientras se conecta, desactiva ese botón para impedir solicitudes repetidas. Si la conexión tiene éxito, muestra `ChatCliente` y cierra el formulario; si falla, informa el error y permite reintentar. **Cancelar** cierra el formulario cuando no hay una conexión en curso.

| Método | Responsabilidad |
| --- | --- |
| `initComponents()` | Inicializa los controles y eventos definidos en `LoginCliente.form` |
| `configurarInterfaz()` | Establece el botón predeterminado del formulario |
| `conectar()` | Valida los datos y coordina la conexión en segundo plano |
| `main(String[] args)` | Permite abrir directamente el formulario |

### `com.mycompany.sokects1.ChatCliente`

Es la ventana de conversación. Implementa `Chat.Cliente.ClienteObserver` y contiene el encabezado, el historial de mensajes y el campo de entrada.

El constructor con dirección, puerto y nombre prepara la sesión de red. Al pulsar **Enter**, envía el texto en segundo plano y lo añade al historial local cuando el envío termina correctamente. Los mensajes recibidos y los cambios de estado se aplican mediante `SwingUtilities.invokeLater`.

| Método | Responsabilidad |
| --- | --- |
| `initComponents()` | Inicializa los controles y eventos definidos en `ChatCliente.form` |
| `conectar()` | Abre la sesión de red y habilita la entrada cuando corresponde |
| `enviarMensaje()` | Procesa el texto escrito y reconoce `chao` como orden de salida |
| `onMensajeRecibido(String mensaje)` | Añade una línea al historial |
| `onDesconectado()` | Informa la desconexión y desactiva la entrada |
| `main(String[] args)` | Abre `LoginCliente` para solicitar los datos antes del chat |

Cerrar esta ventana también cierra su conexión.

### `com.mycompany.sokects1.Servidor`

Es la ventana de registro del servidor. Implementa `Chat.Servidor.ServidorObserver` y muestra los eventos de inicio, conexión, desconexión y errores de red.

Durante la ejecución crea una instancia de `Chat.Servidor` en el puerto `59420`. Su configuración evita iniciar la red cuando está activo el modo de diseño. Al cerrar la ventana, detiene el servidor y libera sus conexiones.

| Método | Responsabilidad |
| --- | --- |
| `initComponents()` | Inicializa los controles y eventos definidos en `Servidor.form` |
| `configurarInterfaz()` | Crea e inicia el servidor durante la ejecución |
| `onLog(String mensaje)` | Añade eventos al registro mediante el hilo de Swing |
| `main(String[] args)` | Permite iniciar directamente la ventana del servidor |

## Flujo de comunicación

1. El servidor abre el puerto `59420` y espera conexiones.
2. El usuario introduce sus datos en `LoginCliente`.
3. `Chat.Cliente` abre el socket y envía el nombre como primera línea.
4. El servidor crea un `Listener` para atender esa conexión y reserva el nombre. Si ya está conectado, devuelve un error y cierra únicamente la segunda conexión; el formulario permite elegir otro nombre. El chat se abre después de recibir la aceptación.
5. El cliente envía mensajes; el servidor los distribuye a los demás participantes con el formato `nombre--> mensaje`.
6. La ventana del remitente muestra su propio mensaje localmente.
7. Al salir un participante, el servidor retira su conexión y avisa a los demás. Al cerrar el servidor, los clientes detectan el final de la sesión.

## Formularios de NetBeans

Cada ventana tiene un archivo `.java` y un archivo `.form`. El `.form` describe los componentes, sus propiedades, su distribución y los eventos que administra el editor visual. El `.java` contiene el código generado y el comportamiento de la ventana.

Los controles están declarados en los formularios y se inicializan dentro de `initComponents()`. Para modificar su apariencia se utiliza la pestaña **Design** y el árbol **Navigator**. La lógica de red y los manejadores de eventos permanecen fuera de los bloques protegidos `GEN-BEGIN`, permitiendo que NetBeans regenere la interfaz.

## Configuración Maven y acciones del proyecto

El archivo `pom.xml` define el identificador `com.mycompany:Sokects1:1.0-SNAPSHOT`, el empaquetado JAR, la codificación UTF-8 y la compilación compatible con Java 17.

| Plugin | Versión | Función |
| --- | --- | --- |
| `maven-compiler-plugin` | `3.13.0` | Compilar el código Java |
| `maven-jar-plugin` | `3.4.2` | Generar el JAR con la clase principal en su manifiesto |
| `exec-maven-plugin` | `3.5.0` | Ejecutar la aplicación desde Maven y las acciones configuradas |

El archivo `nbactions.xml` establece `com.mycompany.sokects1.Sokects1` como clase principal al ejecutar el proyecto. También define las acciones **Ejecutar servidor** y **Ejecutar cliente** con las clases de sus respectivas ventanas.

## Ejecución en NetBeans

1. Abre la carpeta `Sokects1` con **File > Open Project**.
2. Configura un JDK 17 o posterior y ejecuta **Clean and Build**.
3. Pulsa **F6** y selecciona **Servidor**. El registro debe mostrar el inicio en el puerto `59420`.
4. Abre `LoginCliente.java` y pulsa **Shift+F6**, o utiliza la acción **Ejecutar cliente** del proyecto.
5. Introduce `127.0.0.1`, el puerto `59420` y un nombre de usuario.
6. Abre otro cliente con un nombre diferente para conversar. Escribe el mensaje y pulsa **Enter**.
7. Para salir, escribe `chao` o cierra la ventana. Cierra la ventana del servidor al terminar.

Las acciones personalizadas están disponibles desde el menú contextual del proyecto en **Run Maven / Ejecutar Maven**. Si NetBeans conserva acciones anteriores, cierra y vuelve a abrir el proyecto.

## Ejecución desde una terminal

Con Java y Maven configurados, ejecuta desde la carpeta del proyecto:

```powershell
mvn clean package
java -jar target\Sokects1-1.0-SNAPSHOT.jar servidor
```

En otra terminal, abre un cliente:

```powershell
java -jar target\Sokects1-1.0-SNAPSHOT.jar cliente
```

Repite el comando del cliente para abrir otros participantes. Sin argumentos, el JAR muestra el selector de modo. También puedes abrirlo con `mvn exec:java`.

## Documentación del código

Las clases, constructores y métodos utilizan comentarios Javadoc con el formato `/** ... */`. Las etiquetas `@param` describen los parámetros, `@return` los valores devueltos y `@throws` las excepciones declaradas cuando corresponde.

## Consideraciones de uso

- Inicia el servidor antes de conectar los clientes.
- Cada nombre solo puede tener una sesión activa, sin distinguir mayúsculas y minúsculas ni espacios en los extremos. El nombre vuelve a estar disponible cuando el servidor procesa la desconexión de su propietario.
- Usa `127.0.0.1` cuando el servidor y los clientes estén en el mismo equipo. Para otros equipos, utiliza la dirección del servidor en la red local.
- El puerto configurado en el cliente debe coincidir con el del servidor. Un puerto ocupado impide iniciar otra instancia de servidor en esa dirección.
- La comunicación entre equipos requiere acceso al puerto TCP `59420` en el servidor.
- La aplicación no incluye autenticación, cifrado ni almacenamiento del historial. Está orientada a la demostración académica de sockets y eventos de interfaz gráfica.
