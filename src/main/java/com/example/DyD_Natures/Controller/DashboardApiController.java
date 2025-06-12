package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController // Indica que este controlador devuelve datos directamente (JSON, XML, etc.)
@RequestMapping("/api/dashboard") // Prefijo para todos los endpoints en este controlador
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats") // Este endpoint responderá a GET /api/dashboard/stats para los small boxes
    public Map<String, Object> getDashboardStats(@RequestParam(defaultValue = "monthly") String period) {
        return dashboardService.getDashboardStats(period);
    }

    @GetMapping("/chart-data") // ¡NUEVO! Este endpoint responderá a GET /api/dashboard/chart-data para el gráfico
    public Map<String, Object> getChartData() {
        return dashboardService.getSalesPurchasesChartData();
    }
}
