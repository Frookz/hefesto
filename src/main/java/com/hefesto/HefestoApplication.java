package com.hefesto;

import com.hefesto.generator.DTOGenerator;
import com.hefesto.generator.DirectoryGenerator;
import com.hefesto.generator.JpaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

@SpringBootApplication
@Slf4j
public class HefestoApplication {


    public static void main(String[] args) {
        SpringApplication.run(HefestoApplication.class, args);

        StopWatch a = new StopWatch();
        a.start();
        var basePackagePath = "C:\\workspace\\hefesto\\src\\main\\java\\com\\hefesto";

        DirectoryGenerator.createAllDirectories(basePackagePath);

        JpaGenerator.generateJpaEntities();

        DTOGenerator.generateDTOs();
        a.stop();
        System.out.println(a.getTotalTimeMillis() + " MS");

    }
}
