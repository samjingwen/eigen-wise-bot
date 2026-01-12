package io.samjingwen.eigenwisebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EigenwisebotApplication {

	public static void main(String[] args) {
		SpringApplication.run(EigenwisebotApplication.class, args);
	}

}
