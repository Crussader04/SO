import java.util.*;

public class PlanificadorEngine {
    public List<Proceso> terminados = new ArrayList<>();
    public List<Proceso> bloqueados = new ArrayList<>();
    
    public List<String> ganttCPU = new ArrayList<>();
    public List<String> ganttIO = new ArrayList<>();
    public List<String> ganttListos = new ArrayList<>();

    public void ejecutarSimulacion(List<Proceso> inputProcesos, int umbralTiempo, int umbralPrioridad) {
        limpiarSimulacion();
        
        List<Proceso> pendientes = new ArrayList<>();
        for(Proceso p : inputProcesos) {
            // Clonamos pasando las listas (IMPORTANTE: crear nuevas listas para no modificar original)
            pendientes.add(new Proceso(p.id, p.burstTotal, p.prioridad, p.llegada, 
                                       new ArrayList<>(p.listaIniciosIO), 
                                       new ArrayList<>(p.listaDuracionesIO)));
        }

        List<Proceso> colaListos = new ArrayList<>();
        Proceso cpu = null;
        int reloj = 0;

        while (terminados.size() < inputProcesos.size() && reloj < 10000) {
            
            // Listas auxiliares para eventos de ESTE segundo (para graficar)
            List<Proceso> ingresosCPL = new ArrayList<>();
            List<Proceso> ingresosIO = new ArrayList<>(); 

            // 1. LLEGADAS
            Iterator<Proceso> it = pendientes.iterator();
            while (it.hasNext()) {
                Proceso p = it.next();
                if (p.llegada == reloj) {
                    colaListos.add(p);
                    p.envejeciendo = false;
                    ingresosCPL.add(p);
                    it.remove();
                }
            }

            // 2. RETORNO DE E/S
            if (!bloqueados.isEmpty()) {
                List<Proceso> vuelven = new ArrayList<>();
                for (Proceso p : bloqueados) {
                    p.tiempoEnIO++;
                    // Usamos duracionIOActual, que se cargó al irse a bloqueo
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
                    
                    // IMPORTANTE: Preparamos el siguiente índice para la próxima E/S
                    p.indiceIO++; 
                    
                    colaListos.add(p);
                    ingresosCPL.add(p); // Vuelve a CPL
                }
            }

            // 3. CHECK E/S EN CPU
            // Verificamos si al proceso actual le toca alguna E/S de su lista
            if (cpu != null && !cpu.enIO) {
                // ¿Le quedan E/S pendientes?
                if (cpu.indiceIO < cpu.listaIniciosIO.size()) {
                    int momentoIO = cpu.listaIniciosIO.get(cpu.indiceIO);
                    
                    // Si el tiempo ejecutado coincide con el hito de E/S
                    if (cpu.tiempoEjecutado == momentoIO) {
                        cpu.enIO = true;
                        
                        // Cargamos la duración correspondiente a ESTE índice
                        if (cpu.indiceIO < cpu.listaDuracionesIO.size()) {
                            cpu.duracionIOActual = cpu.listaDuracionesIO.get(cpu.indiceIO);
                        } else {
                            // Seguridad por si faltan datos
                            cpu.duracionIOActual = 1; 
                        }

                        bloqueados.add(cpu);
                        ingresosIO.add(cpu);
                        cpu = null;
                    }
                }
            }

            // 4. PLANIFICADOR
            colaListos.sort(Comparator.comparingInt((Proceso p) -> p.prioridad)
                                         .thenComparingInt(p -> p.llegada));

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
                    
                    // Restamos TODAS las E/S que haya hecho para tener la espera real en cola
                    int totalIO = 0;
                    // Sumamos solo las que completó (o sea, las anteriores a indiceIO)
                    // Como el índice avanza al volver, sumamos hasta indiceIO-1.
                    // O más fácil: TiempoEspera = Retorno - RáfagaTotal - (Suma de Duraciones EJECUTADAS)
                    // Pero tu fórmula simple (Retorno - BurstTotal) incluye el tiempo de IO como "no-espera" 
                    // si consideramos que espera es solo en cola de listos.
                    // Para ajustarlo exacto, restamos el tiempo total que pasó en IO:
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
                // Usamos duracionIOActual para calcular salida visual
                int tiempoSalida = reloj + p.duracionIOActual;
                sb.append("P").append(p.id).append("|")
                  .append(p.prioridad).append("|")
                  .append(p.burstRestante).append("|")
                  .append(tiempoSalida).append(" ");
            }
            ganttIO.add(sb.toString().trim());
        }

        // 3. CPL
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
    }
}