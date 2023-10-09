package com.hefesto.generator;


 import lombok.extern.java.Log;
 import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@Log
public class FileUploadController {

    @PostMapping("/upload")
    public ResponseEntity<Boolean> uploadFile(@RequestParam("file") MultipartFile file,
                                              @RequestParam("basePackage") String basePackage,
                                              @RequestParam("appName") String appName) throws IOException {

        if (file.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if(basePackage.isEmpty()) throw new IllegalArgumentException("El basePackage está vacío");
        if(appName.isEmpty()) throw new IllegalArgumentException("El appName está vacío");

        var content = new String(file.getBytes(), StandardCharsets.UTF_8);

        log.info("GRAPHANA ME VES !????");


        //DirectoryGenerator.createAllDirectories(basePackage, appName);
        //JpaGenerator.generateJpaEntities(content, basePackage, appName);
        //DTOGenerator.generateDTOs(content, basePackage, appName);
        //ResponseGenerator.generateResponses(content, basePackage, appName);
        //ControllerGenerator.generateControllers(content, basePackage, appName);

        return ResponseEntity.ok(true);
    }
}
