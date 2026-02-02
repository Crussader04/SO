import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class VentanaPrincipal extends JFrame {
    private DefaultTableModel modeloTabla;
    private JTable tabla;
    // DOS campos de configuración ahora
    private JTextField txtUmbralTiempo;
    private JTextField txtUmbralPrio;
    
    private PanelControl panelTotales;
    private PanelGantt panelGantt;
    private PlanificadorEngine engine = new PlanificadorEngine();

    public VentanaPrincipal() {
        setTitle("Planificador Avanzado - Regla: Si P > X, mejorar cada Y ms");
        setSize(1100, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // --- 1. CONFIGURACIÓN (ARRIBA) ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Configuración de la REGLA
        panelNorte.add(new JLabel("Si Prioridad > "));
        txtUmbralPrio = new JTextField("7", 3); // Valor por defecto del ejemplo
        panelNorte.add(txtUmbralPrio);
        
        panelNorte.add(new JLabel(" entonces mejorar cada (ms):"));
        txtUmbralTiempo = new JTextField("5", 3); // Valor por defecto del ejemplo
        panelNorte.add(txtUmbralTiempo);

        // Botones
        JButton btnAdd = new JButton("+");
        JButton btnDel = new JButton("-");
        JButton btnEjecutar = new JButton("Ejecutar Simulación");
        btnEjecutar.setBackground(new Color(255, 140, 0));
        btnEjecutar.setForeground(Color.WHITE);

        panelNorte.add(Box.createHorizontalStrut(20)); // Espacio
        panelNorte.add(btnAdd); panelNorte.add(btnDel); panelNorte.add(btnEjecutar);
        add(panelNorte, BorderLayout.NORTH);

        // --- 2. TABLA ---
        String[] col = {"Proceso", "T.Llegada", "Prioridad", "O E/S", "Duración", "Ráfaga CPU", "T.Esp", "T.Ret"};
        modeloTabla = new DefaultTableModel(col, 0);
        tabla = new JTable(modeloTabla);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // --- 3. GANTT ---
        panelGantt = new PanelGantt();
        JScrollPane scrollGantt = new JScrollPane(panelGantt);
        scrollGantt.setPreferredSize(new Dimension(900, 250));
        scrollGantt.setBorder(BorderFactory.createTitledBorder("Diagramas de Gantt"));
        
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), scrollGantt);
        split.setDividerLocation(200);
        add(split, BorderLayout.CENTER);

        // --- 4. TOTALES ---
        panelTotales = new PanelControl();
        JButton btnSalir = new JButton("Salir");
        btnSalir.setBackground(Color.RED);
        btnSalir.setForeground(Color.WHITE);
        btnSalir.addActionListener(e -> System.exit(0));
        
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelTotales, BorderLayout.CENTER);
        panelSur.add(btnSalir, BorderLayout.EAST);
        add(panelSur, BorderLayout.SOUTH);

        // --- ACCIONES ---
        btnAdd.addActionListener(e -> {
            int id = modeloTabla.getRowCount() + 1;
            modeloTabla.addRow(new Object[]{"P"+id, "0", "1", "0", "0", "5", "", ""});
        });

        btnDel.addActionListener(e -> {
            if(tabla.getSelectedRow() != -1) modeloTabla.removeRow(tabla.getSelectedRow());
        });

        btnEjecutar.addActionListener(e -> {
            try {
                if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();

                java.util.List<Proceso> listaInput = new ArrayList<>();
                // Leemos los DOS umbrales
                int umbralT = Integer.parseInt(txtUmbralTiempo.getText());
                int umbralP = Integer.parseInt(txtUmbralPrio.getText());

                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    String nombre = modeloTabla.getValueAt(i, 0).toString();
                    int id = Integer.parseInt(nombre.replace("P", "").trim());
                    int llegada = Integer.parseInt(modeloTabla.getValueAt(i, 1).toString());
                    int prio = Integer.parseInt(modeloTabla.getValueAt(i, 2).toString());
                    int inicioIO = Integer.parseInt(modeloTabla.getValueAt(i, 3).toString());
                    int duracionIO = Integer.parseInt(modeloTabla.getValueAt(i, 4).toString());
                    int rafaga = Integer.parseInt(modeloTabla.getValueAt(i, 5).toString());

                    listaInput.add(new Proceso(id, rafaga, prio, llegada, inicioIO, duracionIO));
                }

                // Pasamos ambos umbrales al motor
                engine.ejecutarSimulacion(listaInput, umbralT, umbralP);

                // Actualizar Tabla
                for (Proceso p : engine.terminados) {
                    for(int i=0; i<modeloTabla.getRowCount(); i++) {
                        if(modeloTabla.getValueAt(i, 0).toString().equals("P"+p.id)) {
                            modeloTabla.setValueAt(p.tiempoEspera, i, 6);
                            modeloTabla.setValueAt(p.tiempoRetorno, i, 7);
                        }
                    }
                }
                
                panelTotales.actualizarPromedios(engine.terminados);
                panelGantt.setDatos(engine.ganttCPU, engine.ganttIO, engine.ganttListos);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error en los datos (revisa celdas vacías).");
            }
        });
    }
}