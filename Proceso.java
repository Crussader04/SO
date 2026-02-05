import java.util.List;

public class Proceso {
    public int id;
    public int burstTotal;
    public int burstRestante;
    public int prioridad;
    public int llegada;
    
    // --- CAMPO NUEVO PARA EL DESEMPATE ---
    public int ordenLlegadaCPL = 0; 
    // -------------------------------------

    // Listas para múltiples E/S
    public List<Integer> listaIniciosIO;    
    public List<Integer> listaDuracionesIO; 
    public int indiceIO = 0;                
    public int duracionIOActual = 0;        

    public boolean enIO = false;
    public int tiempoEnIO = 0;
    
    public int tiempoEsperandoEnCola = 0; 
    public int tiempoEjecutado = 0;       
    public boolean envejeciendo = false;  

    public int tiempoFin = 0;
    public int tiempoRetorno = 0;
    public int tiempoEspera = 0;
    public int tiempoRespuesta = -1;

    public Proceso(int id, int burst, int prioridad, int llegada, List<Integer> inicios, List<Integer> duraciones) {
        this.id = id;
        this.burstTotal = burst;
        this.burstRestante = burst;
        this.prioridad = prioridad;
        this.llegada = llegada;
        this.listaIniciosIO = inicios;
        this.listaDuracionesIO = duraciones;
    }
}