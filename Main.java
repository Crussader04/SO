import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            // Intentamos poner el estilo del sistema operativo (Windows/Mac)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            new VentanaPrincipal().setVisible(true);
        });
    }
}   