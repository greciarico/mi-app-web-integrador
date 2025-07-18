package com.example.DyD_Natures.Controller;

import com.example.DyD_Natures.Service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController 
@RequestMapping("/api/dashboard") 
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats") 
    public Map<String, Object> getDashboardStats(@RequestParam(defaultValue = "monthly") String period) {
        return dashboardService.getDashboardStats(period);
    }

    @GetMapping("/chart-data") 
    public Map<String, Object> getChartData() {
        return dashboardService.getSalesPurchasesChartData();
    }
}
