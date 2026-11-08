package Chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Atiende una conexión: nombre en la primera línea y mensajes posteriores. */
public class Listener implements Runnable {
    /** Estado o recurso interno: socket. */
    private final Socket socket;
    /** Estado o recurso interno: servidor. */
    private final Servidor servidor;
    /** Estado o recurso interno: out. */
    private volatile PrintWriter out;
    /** Estado o recurso interno: nombreUsuario. */
    private volatile String nombreUsuario;

    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param socket conexión aceptada por el servidor
     * @param servidor coordinador de los clientes
     */
    public Listener(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    /** Lee hasta recibir chao, fin del flujo o un error de conexión. */
    @Override public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String nombre = in.readLine();
            if (nombre == null || nombre.trim().isEmpty()) return;
            nombreUsuario = nombre.trim();
            servidor.registrarLog("Usuario '" + nombreUsuario + "' conectado");
            servidor.transmitirMensaje(nombreUsuario + " se ha unido al chat.", this);
            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                if ("chao".equalsIgnoreCase(mensaje.trim())) break;
                servidor.transmitirMensaje(nombreUsuario + "--> " + mensaje, this);
            }
        } catch (IOException ex) {
            if (!socket.isClosed()) servidor.registrarLog("Conexión perdida: " + ex.getMessage());
        } finally {
            servidor.removerCliente(this);
            cerrar();
            if (nombreUsuario != null) {
                servidor.registrarLog("El usuario '" + nombreUsuario + "' abandonó");
                servidor.transmitirMensaje("Servidor: " + nombreUsuario + " abandonó el chat.", this);
            }
        }
    }

    /** Procesa el dato indicado.
     * @param mensaje línea que se enviará al cliente identificado
     */
    public void enviarMensaje(String mensaje) {
        PrintWriter salida = out;
        if (nombreUsuario != null && salida != null) salida.println(mensaje);
    }

    /** Cierra el socket y desbloquea cualquier lectura pendiente. */
    public void cerrar() {
        try { socket.close(); }
        catch (IOException ex) { servidor.registrarLog("Error al cerrar: " + ex.getMessage()); }
    }
}
