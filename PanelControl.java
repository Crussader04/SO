import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PanelControl extends JPanel {
    private JLabel lblTEP;   // Tiempo de Espera Promedio
    private JLabel lblTEjeP; // Tiempo de Ejecución/Retorno Promedio
    private JLabel lblResp;  // Tiempo de Respuesta (Opcional, pero útil)

    public PanelControl() {
        // Diseño: Centrado con separación
        setLayout(new FlowLayout(FlowLayout.CENTER, 30, 15));
        setBackground(new Color(245, 245, 245)); // Fondo gris suave
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY)); // Línea superior

        // Fuente grande y negrita para que destaque
        Font fuenteEtiqueta = new Font("Arial", Font.BOLD, 14);
        Font fuenteValor = new Font("Arial", Font.BOLD, 18);
        Color colorValor = new Color(0, 100, 0); // Verde oscuro profesional

        // --- TEP (Tiempo Espera Promedio) ---
        JPanel pnlTEP = new JPanel(new BorderLayout());
        pnlTEP.setOpaque(false);
        JLabel titleTEP = new JLabel("TEP (Espera): ");
        titleTEP.setFont(fuenteEtiqueta);
        lblTEP = new JLabel("0.0");
        lblTEP.setFont(fuenteValor);
        lblTEP.setForeground(colorValor);
        pnlTEP.add(titleTEP, BorderLayout.WEST);
        pnlTEP.add(lblTEP, BorderLayout.CENTER);

        // --- TEjeP (Tiempo Ejecución/Retorno Promedio) ---
        JPanel pnlTEjeP = new JPanel(new BorderLayout());
        pnlTEjeP.setOpaque(false);
        JLabel titleTEjeP = new JLabel("TEjeP (Retorno): ");
        titleTEjeP.setFont(fuenteEtiqueta);
        lblTEjeP = new JLabel("0.0");
        lblTEjeP.setFont(fuenteValor);
        lblTEjeP.setForeground(colorValor);
        pnlTEjeP.add(titleTEjeP, BorderLayout.WEST);
        pnlTEjeP.add(lblTEjeP, BorderLayout.CENTER);

        // Agregamos al panel principal
        add(pnlTEP);
        add(new JSeparator(SwingConstants.VERTICAL)); // Línea separadora vertical
        add(pnlTEjeP);
    }

    public void actualizarPromedios(List<Proceso> terminados) {
        if (terminados == null || terminados.isEmpty()) return;

        double sumEspera = 0;
        double sumRetorno = 0;

        for (Proceso p : terminados) {
            sumEspera += p.tiempoEspera;
            sumRetorno += p.tiempoRetorno;
        }

        int n = terminados.size();
        double tep = sumEspera / n;
        double tejep = sumRetorno / n;

        // Actualizamos los textos con 2 decimales para mayor precisión
        lblTEP.setText(String.format("%.2f ms", tep));
        lblTEjeP.setText(String.format("%.2f ms", tejep));
    }
}