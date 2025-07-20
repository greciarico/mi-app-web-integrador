package com.example.DyD_Natures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class DyDNaturesApplication {
	@PostConstruct
	public void init(){
	// Fija el timezone de toda la JVM a America/Lima
	TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DyDNaturesApplication.class, args);
	}
}
