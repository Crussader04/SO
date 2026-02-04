import java.util.List;
import java.util.ArrayList;

public class Proceso {
    public int id;
    public int burstTotal;
    public int burstRestante;
    public int prioridad;
    public int llegada;
    
    // --- CAMBIOS PARA MÚLTIPLES E/S ---
    public List<Integer> listaIniciosIO;    // Lista de momentos en que debe ir a E/S (ej: 2, 4)
    public List<Integer> listaDuracionesIO; // Lista de duraciones correspondientes (ej: 3, 5)
    public int indiceIO = 0;                // Para saber cuál E/S toca (0 para la primera, 1 para la segunda...)
    public int duracionIOActual = 0;        // Cuánto dura la E/S que está ocurriendo AHORA mismo
    // ----------------------------------

    public boolean enIO = false;
    public int tiempoEnIO = 0;
    
    public int tiempoEsperandoEnCola = 0; 
    public int tiempoEjecutado = 0;       
    public boolean envejeciendo = false;  

    public int tiempoFin = 0;
    public int tiempoRetorno = 0;
    public int tiempoEspera = 0;
    public int tiempoRespuesta = -1;

    // Constructor actualizado para recibir Listas
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