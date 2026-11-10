package Chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/** Servidor TCP que retransmite mensajes sin depender de Swing. */
public class Servidor {
    /** Estado o recurso interno: puerto. */
    private final int puerto;
    /** Estado o recurso interno: observer. */
    private final ServidorObserver observer;
    /** Estado o recurso interno: clientes. */
    private final Set<Listener> clientes = ConcurrentHashMap.newKeySet();
    /** Nombres normalizados y la conexión propietaria de cada registro. */
    private final ConcurrentMap<String, Listener> usuarios = new ConcurrentHashMap<>();
    /** Estado o recurso interno: enEjecucion. */
    private boolean enEjecucion;
    /** Estado o recurso interno: socketServidor. */
    private ServerSocket socketServidor;

    /** Recibe los eventos del servidor desde sus hilos de red. */
    public interface ServidorObserver {
        /** Procesa el dato indicado.
     * @param mensaje evento que debe mostrarse en el registro
     */
        void onLog(String mensaje);
    }

    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param puerto puerto TCP entre 1 y 65535
     * @param observer receptor del registro
     */
    public Servidor(int puerto, ServidorObserver observer) {
        if (puerto < 1 || puerto > 65535) throw new IllegalArgumentException("Puerto inválido");
        this.puerto = puerto;
        this.observer = java.util.Objects.requireNonNull(observer);
    }

    /** Abre el puerto y acepta clientes en un hilo independiente. */
    public synchronized void iniciar() {
        if (enEjecucion) return;
        try {
            socketServidor = new ServerSocket(puerto);
            enEjecucion = true;
            ServerSocket actual = socketServidor;
            new Thread(() -> aceptar(actual), "servidor-aceptar").start();
            registrarLog("Servidor iniciado en puerto " + puerto);
        } catch (IOException ex) {
            registrarLog("No se pudo iniciar el servidor: " + ex.getMessage());
        }
    }

    /** Procesa el dato indicado.
     * @param actual socket de escucha de esta ejecución
     */
    private void aceptar(ServerSocket actual) {
        try {
            while (!actual.isClosed()) {
                Socket socket = actual.accept();
                synchronized (this) {
                    if (actual.isClosed()) { socket.close(); break; }
                    Listener cliente = new Listener(socket, this);
                    clientes.add(cliente);
                    new Thread(cliente, "servidor-cliente").start();
                }
            }
        } catch (IOException ex) {
            if (!actual.isClosed()) registrarLog("Error de red: " + ex.getMessage());
        } finally {
            synchronized (this) {
                if (socketServidor == actual) detener();
            }
        }
    }

    /** Cierra la escucha y todas las conexiones; permite iniciar de nuevo. */
    public synchronized void detener() {
        enEjecucion = false;
        if (socketServidor != null) {
            try { socketServidor.close(); }
            catch (IOException ex) { registrarLog(ex.getMessage()); }
        }
        for (Listener cliente : clientes) cliente.cerrar();
        clientes.clear();
        usuarios.clear();
    }

    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param mensaje texto completo para los destinatarios
     * @param remitente cliente que no debe recibir su propio mensaje
     */
    public void transmitirMensaje(String mensaje, Listener remitente) {
        for (Listener cliente : clientes) {
            if (cliente != remitente) cliente.enviarMensaje(mensaje);
        }
    }

    /** Procesa el dato indicado.
     * @param cliente conexión que terminó
     */
    public void removerCliente(Listener cliente) {
        clientes.remove(cliente);
        usuarios.forEach((nombre, propietario) -> {
            if (propietario == cliente) usuarios.remove(nombre, cliente);
        });
    }

    /**
     * Reserva un nombre de forma atómica, ignorando mayúsculas y minúsculas.
     * @param nombre nombre solicitado por el cliente
     * @param cliente conexión que solicita el registro
     * @return true si el nombre quedó reservado para esta conexión
     */
    public synchronized boolean registrarUsuario(String nombre, Listener cliente) {
        if (!enEjecucion || !clientes.contains(cliente)) return false;
        String clave = nombre.trim().toLowerCase(Locale.ROOT);
        return usuarios.putIfAbsent(clave, cliente) == null;
    }

    /** Procesa el dato indicado.
     * @param mensaje evento que se enviará al observador
     */
    public void registrarLog(String mensaje) { observer.onLog(mensaje); }
}
