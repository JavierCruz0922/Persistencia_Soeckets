package com.mycompany.sokects1;

/** Punto de entrada del proyecto Maven/NetBeans para elegir servidor o cliente. */
public class Sokects1 {
    /** Evita crear instancias de la clase de arranque. */
    private Sokects1() { }
    /**
     * Inicializa o ejecuta la operación con los datos indicados.
     * @param args servidor o cliente para abrir directamente ese modo; vacío muestra el menú
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            String modo = args.length == 0 ? "" : args[0];
            if (modo.isEmpty()) {
                Object[] opciones = {"Servidor", "Cliente", "Cancelar"};
                int seleccion = javax.swing.JOptionPane.showOptionDialog(null,
                    "Seleccione qué desea ejecutar", "Sokects1", javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
                if (seleccion < 0 || seleccion == 2) return;
                modo = seleccion == 0 ? "servidor" : "cliente";
            }
            if ("servidor".equalsIgnoreCase(modo)) new Servidor().setVisible(true);
            else if ("cliente".equalsIgnoreCase(modo)) new LoginCliente().setVisible(true);
            else javax.swing.JOptionPane.showMessageDialog(null, "Modo válido: servidor o cliente");
        });
    }
}
