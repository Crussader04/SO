public class Proceso {
    public int id;
    public int burstTotal;
    public int burstRestante;
    public int prioridad;
    public int llegada;
    
    // E/S
    public int inicioIO;
    public int duracionIO;
    public boolean enIO = false;
    public int tiempoEnIO = 0;
    
    // Control interno
    public int tiempoEsperandoEnCola = 0; // Para Aging
    public int tiempoEjecutado = 0;       // NUEVO: Para saber exactamente cuándo ir a IO

    // Métricas finales
    public int tiempoFin = 0;
    public int tiempoRetorno = 0;
    public int tiempoEspera = 0;
    public int tiempoRespuesta = -1;

    public Proceso(int id, int burst, int prioridad, int llegada, int inicioIO, int duracionIO) {
        this.id = id;
        this.burstTotal = burst;
        this.burstRestante = burst;
        this.prioridad = prioridad;
        this.llegada = llegada;
        this.inicioIO = inicioIO;
        this.duracionIO = duracionIO;
    }
}