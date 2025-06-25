package dk.digitalidentity.indberetning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages="dk.digitalidentity")
public class OS2indberetningApplication {

	public static void main(String[] args) {
		SpringApplication.run(OS2indberetningApplication.class, args);
	}

}
