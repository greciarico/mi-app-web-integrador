package com.example.DyD_Natures.Config;

import com.example.DyD_Natures.Service.TurnoCajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SchedulerConfig {

    private final TurnoCajaService turnoCajaService;

    @Autowired
    public SchedulerConfig(TurnoCajaService turnoCajaService) {
        this.turnoCajaService = turnoCajaService;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Lima")
    public void cerrarTurnosOlvidadosJob() {
        System.out.println("Iniciando tarea programada: Cierre automático de turnos olvidados...");
        turnoCajaService.cerrarTurnosOlvidadosAutomaticamente();
        System.out.println("Tarea programada de cierre automático finalizada.");
    }
}
