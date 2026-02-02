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
            pendientes.add(new Proceso(p.id, p.burstTotal, p.prioridad, p.llegada, p.inicioIO, p.duracionIO));
        }

        List<Proceso> colaListos = new ArrayList<>();
        Proceso cpu = null;
        int reloj = 0;

        while (terminados.size() < inputProcesos.size() && reloj < 10000) {
            
            // Listas auxiliares para eventos de ESTE segundo
            List<Proceso> ingresosCPL = new ArrayList<>();
            List<Proceso> ingresosIO = new ArrayList<>(); // <-- NUEVA LISTA

            // 1. LLEGADAS
            Iterator<Proceso> it = pendientes.iterator();
            while (it.hasNext()) {
                Proceso p = it.next();
                if (p.llegada == reloj) {
                    colaListos.add(p);
                    ingresosCPL.add(p);
                    it.remove();
                }
            }

            // 2. RETORNO DE E/S
            if (!bloqueados.isEmpty()) {
                List<Proceso> vuelven = new ArrayList<>();
                for (Proceso p : bloqueados) {
                    p.tiempoEnIO++;
                    if (p.tiempoEnIO >= p.duracionIO) {
                        vuelven.add(p);
                    }
                }
                for(Proceso p : vuelven) {
                    bloqueados.remove(p);
                    p.enIO = false;
                    p.tiempoEnIO = 0;
                    p.tiempoEsperandoEnCola = 0; 
                    colaListos.add(p);
                    ingresosCPL.add(p); // Vuelve a CPL
                }
            }

            // 3. CHECK E/S EN CPU (Aquí detectamos el ingreso a IO)
            if (cpu != null && !cpu.enIO && cpu.duracionIO > 0) {
                if (cpu.tiempoEjecutado == cpu.inicioIO) {
                    cpu.enIO = true;
                    bloqueados.add(cpu);
                    ingresosIO.add(cpu); // <-- REGISTRAMOS EL EVENTO
                    cpu = null;
                }
            }

            // 4. PLANIFICADOR
            colaListos.sort(Comparator.comparingInt((Proceso p) -> p.prioridad)
                                         .thenComparingInt(p -> p.llegada));

            if (!colaListos.isEmpty()) {
                if (cpu == null) {
                    cpu = colaListos.remove(0);
                } else {
                    if (colaListos.get(0).prioridad < cpu.prioridad) {
                        colaListos.add(cpu);
                        // Opcional: Si quieres ver el reingreso por expropiación:
                        // ingresosCPL.add(cpu); 
                        cpu = colaListos.remove(0);
                    }
                }
            }

            // 5. AGING
            for (Proceso p : colaListos) {
                if (p.prioridad > umbralPrioridad) {
                    p.tiempoEsperandoEnCola++;
                    if (p.tiempoEsperandoEnCola >= umbralTiempo) {
                        p.prioridad--;
                        p.tiempoEsperandoEnCola = 0;
                    }
                } else {
                    p.tiempoEsperandoEnCola = 0; 
                }
            }

            // REGISTRO (Pasamos el reloj y la nueva lista ingresosIO)
            registrarGantt(reloj, cpu, ingresosIO, ingresosCPL);

            // EJECUCIÓN
            if (cpu != null) {
                if (cpu.tiempoRespuesta == -1) cpu.tiempoRespuesta = reloj - cpu.llegada;
                cpu.burstRestante--;
                cpu.tiempoEjecutado++;

                if (cpu.burstRestante == 0) {
                    cpu.tiempoFin = reloj + 1;
                    cpu.tiempoRetorno = cpu.tiempoFin - cpu.llegada;
                    cpu.tiempoEspera = cpu.tiempoRetorno - cpu.burstTotal - cpu.duracionIO;
                    terminados.add(cpu);
                    cpu = null;
                }
            }
            reloj++;
        }
    }

    // Método actualizado para recibir reloj e ingresosIO
    private void registrarGantt(int reloj, Proceso cpu, List<Proceso> ingresosIO, List<Proceso> ingresosCPL) {
        // 1. CPU (Formato estándar 3 partes)
        if (cpu != null) ganttCPU.add("P" + cpu.id + "|" + cpu.prioridad + "|" + cpu.burstRestante);
        else ganttCPU.add("-");
        
        // 2. CE/S (NUEVO FORMATO 4 partes: ID|Prio|Rafaga|TiempoSalida)
        if (ingresosIO.isEmpty()) ganttIO.add("-");
        else {
            StringBuilder sb = new StringBuilder();
            for(Proceso p : ingresosIO) {
                // Calculamos cuándo saldrá: tiempo actual + duración
                int tiempoSalida = reloj + p.duracionIO;
                sb.append("P").append(p.id).append("|")
                  .append(p.prioridad).append("|")
                  .append(p.burstRestante).append("|")
                  .append(tiempoSalida).append(" ");
            }
            ganttIO.add(sb.toString().trim());
        }

        // 3. CPL (Formato estándar 3 partes)
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