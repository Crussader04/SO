import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        setTitle("Planificador de prioridades con envejecimiento");
        setSize(1100, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // --- 1. CONFIGURACIÓN (ARRIBA) ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Configuración de la REGLA
        panelNorte.add(new JLabel("Si Prioridad >= "));
        txtUmbralPrio = new JTextField("7", 3); // Valor por defecto del ejemplo
        panelNorte.add(txtUmbralPrio);
        
        panelNorte.add(new JLabel(" entonces mejorar la prioridad cada:"));
        txtUmbralTiempo = new JTextField("5", 3); // Valor por defecto del ejemplo
        panelNorte.add(txtUmbralTiempo);
        panelNorte.add(new JLabel("ms"));

        // Botones
        JButton btnAdd = new JButton("+");
        JButton btnDel = new JButton("-");
        JButton btnEjecutar = new JButton("Ejecutar Simulación");
        btnEjecutar.setBackground(new Color(255, 140, 0));
        btnEjecutar.setForeground(Color.BLACK);

        panelNorte.add(Box.createHorizontalStrut(20)); // Espacio
        panelNorte.add(btnAdd); panelNorte.add(btnDel); panelNorte.add(btnEjecutar);
        add(panelNorte, BorderLayout.NORTH);

        // --- 2. TABLA ---
        String[] col = {"Proceso", "T.Llegada", "Prioridad", "O E/S", "Duración", "Ráfaga CPU", "T.Esp", "T.Ret"};
            // Hacemos un modelo donde las dos últimas columnas (6 y 7) NO sean editables
            modeloTabla = new DefaultTableModel(col, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // Columnas 0..5 editables (entrada del usuario). 6 (T.Esp) y 7 (T.Ret) son calculadas.
                    return column >= 0 && column <= 5;
                }
            };
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
        btnSalir.setForeground(Color.BLACK);
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

        // --- LÓGICA MODIFICADA PARA LEER GUIONES (2-4) ---
        btnEjecutar.addActionListener(e -> {
            try {
                if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();

                java.util.List<Proceso> listaInput = new ArrayList<>();
                int umbralT = Integer.parseInt(txtUmbralTiempo.getText());
                int umbralP = Integer.parseInt(txtUmbralPrio.getText());

                for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                    String nombre = modeloTabla.getValueAt(i, 0).toString();
                    int id = Integer.parseInt(nombre.replace("P", "").trim());
                    int llegada = Integer.parseInt(modeloTabla.getValueAt(i, 1).toString());
                    int prio = Integer.parseInt(modeloTabla.getValueAt(i, 2).toString());
                    int rafaga = Integer.parseInt(modeloTabla.getValueAt(i, 5).toString());
                    
                    // Parseamos los guiones
                    String strInicioIO = modeloTabla.getValueAt(i, 3).toString();
                    String strDuracionIO = modeloTabla.getValueAt(i, 4).toString();

                    List<Integer> inicios = parsearEntrada(strInicioIO);
                    List<Integer> duraciones = parsearEntrada(strDuracionIO);

                    listaInput.add(new Proceso(id, rafaga, prio, llegada, inicios, duraciones));
                }

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
                JOptionPane.showMessageDialog(this, "Error en los datos (revisa formato 2-4 o números solos).");
            }
        });
    }

    // --- NUEVO MÉTODO AUXILIAR ---
    private java.util.List<Integer> parsearEntrada(String texto) {
        java.util.List<Integer> lista = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty() || texto.trim().equals("-")) {
            return lista; 
        }
        // Si pone 0, lo ignoramos para no generar IOs fantasmas, o lo tratamos como vacío
        if (texto.trim().equals("0")) return lista;

        String[] partes = texto.split("-");
        for (String p : partes) {
            try {
                lista.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException e) {
                // Si hay error, ignoramos
            }
        }
        return lista;
    }
}