package com.lcoprobe.acbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

/**
 * @author Osvaldo Martini
 */
// tag::code[]
@SpringBootApplication
@Configuration
public class LCOProbeApplication {

	public static void main(String...args) {
		SpringApplication.run(LCOProbeApplication.class, args);
	}
}
// end::code[]