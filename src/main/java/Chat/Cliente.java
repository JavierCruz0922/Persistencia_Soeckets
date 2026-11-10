package Chat;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cliente TCP de una sesión; comunica los eventos mediante un observador. */
public class Cliente {
    /** Estado o recurso interno: ip. */
    private final String ip;
    /** Estado o recurso interno: puerto. */
    private final int puerto;
    /** Estado o recurso interno: usuario. */
    private final String usuario;
    /** Estado o recurso interno: observer. */
    private final ClienteObserver observer;
    /** Estado o recurso interno: cerrado. */
    private final AtomicBoolean cerrado = new AtomicBoolean();
    /** Estado o recurso interno: socket. */
    private volatile Socket socket;
    /** Estado o recurso interno: out. */
    private volatile PrintWriter out;

    /** Eventos recibidos desde el hilo de escucha, fuera del hilo de Swing. */
    public interface ClienteObserver {
        /** Procesa el dato indicado.
     * @param mensaje línea recibida del servidor
     */
        void onMensajeRecibido(String mensaje);
        /** Notifica exactamente una vez el cierre de una sesión conectada. */
        void onDesconectado();
    }

    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param ip dirección o nombre del servidor
     * @param puerto puerto TCP entre 1 y 65535
     * @param usuario nombre visible, sin saltos de línea
     * @param observer receptor de mensajes y desconexiones
     */
    public Cliente(String ip, int puerto, String usuario, ClienteObserver observer) {
        if (ip == null || ip.trim().isEmpty()) throw new IllegalArgumentException("Escriba la IP del servidor");
        if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException("Puerto entre 1 y 65535");
        if (usuario == null || usuario.trim().isEmpty() || usuario.contains("\n") || usuario.contains("\r"))
            throw new IllegalArgumentException("Escriba un nombre válido");
        this.ip = ip.trim(); this.puerto = puerto; this.usuario = usuario.trim();
        this.observer = java.util.Objects.requireNonNull(observer);
    }

    /**
     * Conecta y espera hasta cinco segundos la aceptación del nombre por el servidor.
     * @throws IOException si falla la conexión, el servidor rechaza el nombre o no confirma el registro
     */
    public synchronized void conectar() throws IOException {
        if (socket != null || cerrado.get()) throw new IOException("La sesión ya fue utilizada");
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, puerto), 5000);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(usuario);
            if (out.checkError()) throw new IOException("No se pudo enviar el nombre");
            socket.setSoTimeout(5000);
            String respuesta = in.readLine();
            if (!"OK".equals(respuesta)) {
                if (respuesta != null && respuesta.startsWith("ERROR: ")) {
                    throw new IOException(respuesta.substring("ERROR: ".length()));
                }
                throw new IOException("El servidor no confirmó el registro del usuario.");
            }
            socket.setSoTimeout(0);
            new Thread(() -> escuchar(in), "cliente-escucha").start();
        } catch (IOException ex) {
            cerrado.set(true);
            try { socket.close(); } catch (IOException ignored) { }
            throw ex;
        }
    }

    /** Procesa el dato indicado.
     * @param in flujo UTF-8 que contiene los mensajes del servidor
     */
    private void escuchar(BufferedReader in) {
        try {
            String mensaje;
            while ((mensaje = in.readLine()) != null) observer.onMensajeRecibido(mensaje);
        } catch (IOException ignored) {
            // El cierre local también interrumpe readLine.
        } finally { desconectar(); }
    }

    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param mensaje línea de texto que se enviará
     * @throws IOException si la conexión está cerrada o falla la escritura
     */
    public void enviarMensaje(String mensaje) throws IOException {
        if (cerrado.get() || out == null) throw new IOException("Cliente desconectado");
        if (mensaje == null || mensaje.contains("\n") || mensaje.contains("\r"))
            throw new IOException("El mensaje debe ocupar una línea");
        out.println(mensaje);
        if (out.checkError()) { desconectar(); throw new IOException("No se pudo enviar el mensaje"); }
    }

    /** Cierra la conexión; el servidor reconoce la salida por el fin del flujo. */
    public void desconectar() {
        if (!cerrado.compareAndSet(false, true)) return;
        try { if (socket != null) socket.close(); }
        catch (IOException ignored) { }
        observer.onDesconectado();
    }

    /** Devuelve el dato de la sesión.
     * @return nombre visible del usuario
     */
    public String getUsuario() { return usuario; }
}
