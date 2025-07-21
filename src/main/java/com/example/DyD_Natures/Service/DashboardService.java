package com.example.DyD_Natures.Service;

import com.example.DyD_Natures.Repository.VentaRepository;
import com.example.DyD_Natures.Repository.UsuarioRepository;
import com.example.DyD_Natures.Repository.ClienteRepository;
import com.example.DyD_Natures.Repository.DocumentoCompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private DocumentoCompraRepository documentoCompraRepository; 

    public Map<String, Object> getDashboardStats(String period) {
        Map<String, Object> stats = new HashMap<>();

        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        switch (period) {
            case "today":
                startDate = LocalDate.now();
                break;
            case "weekly":
                startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                break;
            case "monthly":
                startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
                break;
            case "yearly":
                startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
                break;
            default:
                startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
                period = "monthly";
                break;
        }

        BigDecimal totalSales = ventaRepository.sumTotalByFechaRegistroBetweenAndEstado(startDate, endDate);
        Long numberOfSales = ventaRepository.countByFechaRegistroBetweenAndEstado(startDate, endDate);
        Long userRegistrations = usuarioRepository.countAllActiveUsers(); 
        Long clientRegistrations = clienteRepository.countAllActiveClients(); 


        stats.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        stats.put("numberOfSales", numberOfSales != null ? numberOfSales : 0L);
        stats.put("userRegistrations", userRegistrations != null ? userRegistrations : 0L);
        stats.put("clientRegistrations", clientRegistrations != null ? clientRegistrations : 0L);

        return stats;
    }


    public Map<String, Object> getSalesPurchasesChartData() {
        Map<String, Object> chartData = new HashMap<>();
        int currentYear = LocalDate.now().getYear();
        int lastYear = currentYear - 1;


        List<String> monthLabels = IntStream.rangeClosed(1, 12)
                .mapToObj(monthNum -> Month.of(monthNum).name().substring(0, 3).toUpperCase()) 
                .collect(Collectors.toList());
        chartData.put("labels", monthLabels);

        Map<Integer, BigDecimal> currentYearSalesMap = ventaRepository.findTotalSalesByMonthForYear(currentYear).stream()
                .collect(Collectors.toMap(
                        obj -> (Integer) obj[0], 
                        obj -> (BigDecimal) obj[1],
                        (existing, replacement) -> existing 
                ));

        Map<Integer, BigDecimal> currentYearPurchasesMap = documentoCompraRepository.findTotalPurchasesByMonthForYear(currentYear).stream() 
                .collect(Collectors.toMap(
                        obj -> (Integer) obj[0],
                        obj -> (BigDecimal) obj[1],
                        (existing, replacement) -> existing
                ));

        List<BigDecimal> currentYearSales = new ArrayList<>();
        List<BigDecimal> currentYearPurchases = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            currentYearSales.add(currentYearSalesMap.getOrDefault(i, BigDecimal.ZERO));
            currentYearPurchases.add(currentYearPurchasesMap.getOrDefault(i, BigDecimal.ZERO));
        }
        chartData.put("thisYearSales", currentYearSales);
        chartData.put("thisYearPurchases", currentYearPurchases);



        Map<Integer, BigDecimal> lastYearSalesMap = ventaRepository.findTotalSalesByMonthForYear(lastYear).stream()
                .collect(Collectors.toMap(
                        obj -> (Integer) obj[0],
                        obj -> (BigDecimal) obj[1],
                        (existing, replacement) -> existing
                ));

        Map<Integer, BigDecimal> lastYearPurchasesMap = documentoCompraRepository.findTotalPurchasesByMonthForYear(lastYear).stream() 
                .collect(Collectors.toMap(
                        obj -> (Integer) obj[0],
                        obj -> (BigDecimal) obj[1],
                        (existing, replacement) -> existing
                ));

        List<BigDecimal> lastYearSales = new ArrayList<>();
        List<BigDecimal> lastYearPurchases = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            lastYearSales.add(lastYearSalesMap.getOrDefault(i, BigDecimal.ZERO));
            lastYearPurchases.add(lastYearPurchasesMap.getOrDefault(i, BigDecimal.ZERO));
        }
        chartData.put("lastYearSales", lastYearSales);
        chartData.put("lastYearPurchases", lastYearPurchases);

        return chartData;
    }
}
