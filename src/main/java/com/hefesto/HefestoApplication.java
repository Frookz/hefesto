package com.hefesto;

import com.hefesto.generator.DirectoryGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

@SpringBootApplication
@Slf4j
public class HefestoApplication {


    public static void main(String[] args) {
        SpringApplication.run(HefestoApplication.class, args);
    }
}
