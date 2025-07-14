package com.example.DyD_Natures.Integration;

import com.example.DyD_Natures.Dto.api.ReniecDataDTO;
import com.example.DyD_Natures.Dto.api.SunatDataDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

@Component
public class PeruDataApiClient {

    private static final Logger logger = LoggerFactory.getLogger(PeruDataApiClient.class);
    private final RestTemplate restTemplate;

    @Value("${api.peru.base-url}")
    private String apiBaseUrl;

    @Value("${api.peru.token}")
    private String apiToken;

    public PeruDataApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<ReniecDataDTO> getReniecData(String dni) {
        String url = apiBaseUrl + "/reniec/dni?numero=" + dni + "&token=" + apiToken;
        try {
            logger.info("Consultando RENIEC (APIS.NET.PE) para DNI: {}", dni);
            ResponseEntity<ReniecDataDTO> response = restTemplate.getForEntity(url, ReniecDataDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Mantener esta validación para DNI, ya que el comportamiento de la API puede variar
                // entre retornar 200 con un DTO vacío o un mensaje de error.
                if (response.getBody().getNumeroDocumento() == null || response.getBody().getNumeroDocumento().isEmpty()) {
                    logger.warn("DNI {} no encontrado en RENIEC (APIS.NET.PE) o respuesta vacía.", dni);
                    return Optional.empty();
                }
                return Optional.of(response.getBody());
            } else {
                logger.warn("Reniec API (APIS.NET.PE) devolvió estado {} para DNI {}: {}", response.getStatusCode(), dni, response.getBody());
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error del cliente al consultar RENIEC para DNI {}: Status {} - {}", dni, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error inesperado al consultar RENIEC para DNI {}: {}", dni, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<SunatDataDTO> getSunatData(String ruc) {
        String url = apiBaseUrl + "/sunat/ruc/full?numero=" + ruc + "&token=" + apiToken;
        try {
            logger.info("Consultando SUNAT (APIS.NET.PE) para RUC: {}", ruc);
            ResponseEntity<SunatDataDTO> response = restTemplate.getForEntity(url, SunatDataDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // ***** CAMBIO AQUÍ *****
                // Dado que el JSON directo tiene el campo "ruc" y lo estás recibiendo,
                // significa que response.getBody().getRuc() NO DEBERÍA SER NULL O VACÍO aquí.
                // Esta línea de validación puede estar causando el problema si por alguna razón
                // el mapeo de Jackson falla internamente o el campo 'ruc' es el último en poblarse
                // y se verifica antes.
                // Si la API devuelve un 200 OK y un cuerpo no nulo, asumimos que los datos están ahí.
                // Si el RUC no fuera encontrado, esperaríamos un status diferente a 200 OK (ej. 404, 422)
                // o un JSON con un mensaje de error explícito (ej. {"error": "ruc no encontrado"}).
                // Si la API siempre devuelve el RUC incluso si el cuerpo es vacío, puedes dejarla
                // pero si el problema es que el DTO está bien pero esta línea falla, quítala.

                // Para depurar, si no quieres quitarla, déjala y añade los logs de depuración
                // que te mencioné en el paso 3 de la respuesta anterior, y verifica qué valor
                // tiene response.getBody().getRuc() justo antes de este if.
                // Si ese log muestra el RUC, entonces esta condición es redundante o mal aplicada.

                // Si confías en que la API te devuelve 200 OK SOLO cuando hay datos de RUC,
                // puedes simplificarlo a solo retornar el DTO:
                return Optional.of(response.getBody());
                // **********************
            } else {
                logger.warn("Sunat API (APIS.NET.PE) devolvió estado {} para RUC {}: {}", response.getStatusCode(), ruc, response.getBody());
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            logger.error("Error del cliente al consultar SUNAT para RUC {}: Status {} - {}", ruc, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error inesperado al consultar SUNAT para RUC {}: {}", ruc, e.getMessage());
            return Optional.empty();
        }
    }
}
