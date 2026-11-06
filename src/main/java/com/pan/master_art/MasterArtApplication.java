package com.pan.master_art;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("com.pan.master_art.entity")
@EnableScheduling
public class MasterArtApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterArtApplication.class, args);
    }

}
