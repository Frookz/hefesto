package com.hefesto;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Log
public class HefestoApplication {

    public static void main(String[] args) {
        log.info("SE HA INICIADO");
        log.info("SE HA INICIADO2");
        SpringApplication.run(HefestoApplication.class, args);
    }
}
