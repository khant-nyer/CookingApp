package com.chef.william;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CookingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(CookingAppApplication.class, args);
	}

}
