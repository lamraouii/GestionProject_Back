package com.ensao.gestionprojet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GestionprojetApplication {


	public static void main(String[] args) {
		SpringApplication.run(GestionprojetApplication.class, args);
	}

}
