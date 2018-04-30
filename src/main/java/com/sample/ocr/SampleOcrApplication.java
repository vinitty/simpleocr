package com.sample.ocr;

import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class SampleOcrApplication {
	
	
	@Bean
	public TessBaseAPI tessBaseAPI() {
		TessBaseAPI  tessBaseAPI = new TessBaseAPI();
		if (tessBaseAPI.Init(getClass().getClassLoader().getResource("tessdata").getPath(), "ENG") != 0) {
            System.err.println("Could not initialize tesseract.");
            System.exit(1);
        }
		return tessBaseAPI;
	}

	public static void main(String[] args) {
        SpringApplication.run(SampleOcrApplication.class, args);
    }
}
