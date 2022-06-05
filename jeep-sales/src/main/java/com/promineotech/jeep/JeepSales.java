package com.promineotech.jeep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.promineotech.ComponentScanMarker;

@SpringBootApplication(scanBasePackages = {"com.promineotech"})

public class JeepSales {
 
	public static void main(String[] args) {
	  SpringApplication.run(JeepSales.class, args);

	}

}
