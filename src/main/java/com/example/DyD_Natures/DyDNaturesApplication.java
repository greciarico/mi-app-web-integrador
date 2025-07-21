package com.example.DyD_Natures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class DyDNaturesApplication {
	@PostConstruct
	public void init(){

	TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DyDNaturesApplication.class, args);
	}
}
