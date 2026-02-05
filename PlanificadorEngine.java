import java.util.*;

public class PlanificadorEngine {
    public List<Proceso> terminados = new ArrayList<>();
    public List<Proceso> bloqueados = new ArrayList<>();
    
    public List<String> ganttCPU = new ArrayList<>();
    public List<String> ganttIO = new ArrayList<>();
    public List<String> ganttListos = new ArrayList<>();

    // --- NUEVO: Contador global para gestionar el orden de llegada a CPL ---
    private int contadorOrden = 0;

    public void ejecutarSimulacion(List<Proceso> inputProcesos, int umbralTiempo, int umbralPrioridad) {
        limpiarSimulacion();
        
        List<Proceso> pendientes = new ArrayList<>();
        for(Proceso p : inputProcesos) {
            pendientes.add(new Proceso(p.id, p.burstTotal, p.prioridad, p.llegada, 
                                       new ArrayList<>(p.listaIniciosIO), 
                                       new ArrayList<>(p.listaDuracionesIO)));
        }

        List<Proceso> colaListos = new ArrayList<>();
        Proceso cpu = null;
        int reloj = 0;

        while (terminados.size() < inputProcesos.size() && reloj < 10000) {
            
            List<Proceso> ingresosCPL = new ArrayList<>();
            List<Proceso> ingresosIO = new ArrayList<>(); 

            // 1. LLEGADAS (Nuevos Procesos)
            // Se procesan PRIMERO para que obtengan un número de orden menor
            Iterator<Proceso> it = pendientes.iterator();
            while (it.hasNext()) {
                Proceso p = it.next();
                if (p.llegada == reloj) {
                    // Asignamos turno actual
                    p.ordenLlegadaCPL = contadorOrden++;
                    
                    colaListos.add(p);
                    p.envejeciendo = false;
                    ingresosCPL.add(p);
                    it.remove();
                }
            }

            // 2. RETORNO DE E/S (Procesos Desbloqueados)
            // Se procesan DESPUÉS, obteniendo un número de orden mayor que los nuevos
            if (!bloqueados.isEmpty()) {
                List<Proceso> vuelven = new ArrayList<>();
                for (Proceso p : bloqueados) {
                    p.tiempoEnIO++;
                    if (p.tiempoEnIO >= p.duracionIOActual) {
                        vuelven.add(p);
                    }
                }
                for(Proceso p : vuelven) {
                    bloqueados.remove(p);
                    p.enIO = false;
                    p.tiempoEnIO = 0;
                    p.tiempoEsperandoEnCola = 0;
                    p.envejeciendo = false;
                    p.indiceIO++; 
                    
                    // Asignamos turno (será mayor que el de los nuevos que entraron arriba)
                    p.ordenLlegadaCPL = contadorOrden++;
                    
                    colaListos.add(p);
                    ingresosCPL.add(p); 
                }
            }

            // 3. CHECK E/S EN CPU
            if (cpu != null && !cpu.enIO) {
                if (cpu.indiceIO < cpu.listaIniciosIO.size()) {
                    int momentoIO = cpu.listaIniciosIO.get(cpu.indiceIO);
                    if (cpu.tiempoEjecutado == momentoIO) {
                        cpu.enIO = true;
                        if (cpu.indiceIO < cpu.listaDuracionesIO.size()) {
                            cpu.duracionIOActual = cpu.listaDuracionesIO.get(cpu.indiceIO);
                        } else {
                            cpu.duracionIOActual = 1; 
                        }
                        bloqueados.add(cpu);
                        ingresosIO.add(cpu);
                        cpu = null;
                    }
                }
            }

            // 4. PLANIFICADOR
            // MODIFICADO: Usamos 'ordenLlegadaCPL' para el desempate en lugar de 'llegada' original
            colaListos.sort(Comparator.comparingInt((Proceso p) -> p.prioridad)
                                         .thenComparingInt(p -> p.ordenLlegadaCPL));

            if (!colaListos.isEmpty()) {
                if (cpu == null) {
                    cpu = colaListos.remove(0);
                    cpu.envejeciendo = false;
                } else {
                    if (colaListos.get(0).prioridad < cpu.prioridad) {
                        colaListos.add(cpu);
                        cpu = colaListos.remove(0);
                        cpu.envejeciendo = false;
                    }
                }
            }

            // 5. AGING (Envejecimiento)
            for (Proceso p : colaListos) {
                if (p.prioridad > umbralPrioridad || p.envejeciendo) {
                    p.tiempoEsperandoEnCola++;
                    if (p.tiempoEsperandoEnCola >= umbralTiempo) {
                        p.prioridad = Math.max(0, p.prioridad - 1); 
                        p.tiempoEsperandoEnCola = 0;
                        p.envejeciendo = true;
                    }
                } else {
                    p.tiempoEsperandoEnCola = 0;
                }
            }

            // REGISTRO 
            registrarGantt(reloj, cpu, ingresosIO, ingresosCPL);

            // EJECUCIÓN
            if (cpu != null) {
                if (cpu.tiempoRespuesta == -1) cpu.tiempoRespuesta = reloj - cpu.llegada;
                cpu.burstRestante--;
                cpu.tiempoEjecutado++;

                if (cpu.burstRestante == 0) {
                    cpu.tiempoFin = reloj + 1;
                    cpu.tiempoRetorno = cpu.tiempoFin - cpu.llegada;
                    cpu.tiempoEspera = cpu.tiempoRetorno - cpu.burstTotal; 
                    
                    int totalIO = 0;
                    for(int k=0; k < cpu.indiceIO && k < cpu.listaDuracionesIO.size(); k++){
                         totalIO += cpu.listaDuracionesIO.get(k);
                    }
                    cpu.tiempoEspera -= totalIO;

                    terminados.add(cpu);
                    cpu = null;
                }
            }
            reloj++;
        }
    }

    private void registrarGantt(int reloj, Proceso cpu, List<Proceso> ingresosIO, List<Proceso> ingresosCPL) {
        // 1. CPU
        if (cpu != null) ganttCPU.add("P" + cpu.id + "|" + cpu.prioridad + "|" + cpu.burstRestante);
        else ganttCPU.add("-");
        
        // 2. CE/S
        if (ingresosIO.isEmpty()) ganttIO.add("-");
        else {
            StringBuilder sb = new StringBuilder();
            for(Proceso p : ingresosIO) {
                int tiempoSalida = reloj + p.duracionIOActual;
                sb.append("P").append(p.id).append("|")
                  .append(p.prioridad).append("|")
                  .append(p.burstRestante).append("|")
                  .append(tiempoSalida).append(" ");
            }
            ganttIO.add(sb.toString().trim());
        }

        // 3. CPL
        // Aquí ordenamos visualmente también por orden de llegada para que coincida con la lógica
        ingresosCPL.sort(Comparator.comparingInt(p -> p.ordenLlegadaCPL));
        
        if (ingresosCPL.isEmpty()) ganttListos.add("-");
        else {
            StringBuilder sb = new StringBuilder();
            for(Proceso p : ingresosCPL) {
                sb.append("P").append(p.id).append("|").append(p.prioridad).append("|").append(p.burstRestante).append(" ");
            }
            ganttListos.add(sb.toString().trim());
        }
    }

    private void limpiarSimulacion() {
        terminados.clear(); bloqueados.clear();
        ganttCPU.clear(); ganttIO.clear(); ganttListos.clear();
        contadorOrden = 0; // Reiniciamos el contador de orden
    }
}