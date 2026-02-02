import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PanelGantt extends JPanel {
    private List<String> historiaCPU;
    private List<String> historiaIO;
    private List<String> historiaListos;
    
    private final int ALTO_FILA = 70; 
    private final int ANCHO_CELDA = 50; 
    private final int MARGEN_IZQ = 80;
    private final int MARGEN_SUP = 30;

    public PanelGantt() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 300));
    }

    public void setDatos(List<String> cpu, List<String> io, List<String> listos) {
        this.historiaCPU = cpu;
        this.historiaIO = io;
        this.historiaListos = listos;
        
        if(cpu != null && !cpu.isEmpty()) {
            int anchoNecesario = MARGEN_IZQ + (cpu.size() * ANCHO_CELDA) + 50;
            setPreferredSize(new Dimension(Math.max(1000, anchoNecesario), 300));
            revalidate();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (historiaCPU == null || historiaCPU.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dibujarFila(g2, "CPL:", 0, historiaListos);
        dibujarFila(g2, "CE/S:", 1, historiaIO);
        dibujarFila(g2, "CPU:", 2, historiaCPU);
        
        int tiempoTotal = historiaCPU.size();
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        for(int t=0; t<=tiempoTotal; t++) {
            g2.drawString(String.valueOf(t), MARGEN_IZQ + (t * ANCHO_CELDA) - 3, MARGEN_SUP + (ALTO_FILA*3) + 15);
        }
    }

    private void dibujarFila(Graphics2D g, String titulo, int idx, List<String> datos) {
        int yBase = MARGEN_SUP + (idx * ALTO_FILA);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(titulo, 10, yBase + 40);
        g.setColor(new Color(220, 220, 220));
        g.drawLine(0, yBase + ALTO_FILA, getWidth(), yBase + ALTO_FILA);

        for (int t = 0; t < datos.size(); t++) {
            String rawVal = datos.get(t);
            int x = MARGEN_IZQ + (t * ANCHO_CELDA);
            int y = yBase + 5;
            
            if (!rawVal.equals("-") && !rawVal.isEmpty()) {
                String[] procesos = rawVal.split(" ");
                if (procesos.length > 1) {
                    dibujarCaja(g, x, y, "...", "", "", Color.LIGHT_GRAY); 
                } else {
                    // AQUÍ ESTÁ LA LÓGICA NUEVA
                    String[] partes = rawVal.trim().split("\\|");
                    
                    // Caso estándar: ID|Prio|Rafaga (CPU y CPL)
                    if (partes.length == 3) {
                        dibujarCaja(g, x, y, partes[0], partes[1], "R:" + partes[2], getColor(partes[0]));
                    } 
                    // Caso E/S: ID|Prio|Rafaga|TiempoSalida
                    else if (partes.length == 4) {
                        String rafaga = partes[2];
                        String salida = partes[3];
                        // Formato combinado para abajo: R:9 T:4
                        String infoAbajo = "R:" + rafaga + " T:" + salida;
                        dibujarCaja(g, x, y, partes[0], partes[1], infoAbajo, getColor(partes[0]));
                    }
                }
            } else {
                g.setColor(new Color(250, 250, 250));
                g.drawRect(x, y, ANCHO_CELDA, ALTO_FILA - 10);
            }
        }
    }
    
    private void dibujarCaja(Graphics2D g, int x, int y, String nombre, String prio, String infoAbajo, Color c) {
        int alto = ALTO_FILA - 10;
        g.setColor(c); g.fillRect(x, y, ANCHO_CELDA, alto);
        g.setColor(Color.BLACK); g.drawRect(x, y, ANCHO_CELDA, alto);
        
        // 1. PRIORIDAD (Arriba)
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        centrarTexto(g, prio, x, y + 15, ANCHO_CELDA);
        
        // 2. NOMBRE (Centro)
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        centrarTexto(g, nombre, x, y + 35, ANCHO_CELDA);
        
        // 3. INFO ABAJO (Ráfaga y/o Tiempo Salida)
        if(!infoAbajo.isEmpty()){
            g.setColor(Color.DARK_GRAY);
            // Usamos fuente un poco más pequeña para que quepa "R:XX T:YY"
            g.setFont(new Font("SansSerif", Font.PLAIN, 9)); 
            centrarTexto(g, infoAbajo, x, y + 54, ANCHO_CELDA);
        }
    }
    
    private void centrarTexto(Graphics2D g, String txt, int x, int y, int ancho) {
        if(txt == null || txt.isEmpty()) return;
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (ancho - fm.stringWidth(txt)) / 2;
        g.drawString(txt, tx, y);
    }

    private Color getColor(String id) {
        try {
            int n = Integer.parseInt(id.replaceAll("\\D+", ""));
            Color[] p = {new Color(173,216,230), new Color(255,182,193), new Color(144,238,144), 
                         new Color(255,255,224), new Color(221,160,221), new Color(255,160,122)};
            return p[(n-1)%p.length];
        } catch(Exception e) { return Color.LIGHT_GRAY; }
    }
}