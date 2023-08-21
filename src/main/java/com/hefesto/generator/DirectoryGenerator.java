package com.hefesto.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class DirectoryGenerator {

    public static void createAllDirectories(String basePackagePath) {

        var directories = Arrays.asList("controller",
                                        "jpa",
                                        "dto",
                                        "service",
                                        "rest",
                                        "rest/response",
                                        "rest/request",
                                        "business",
                                        "config",
                                        "exceptions",
                                        "repositories");

        directories.forEach(dir -> {
            try {
                createDirectory(basePackagePath, dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void createDirectory(String basePath, String dirName) throws IOException {
        var path = Paths.get(basePath, dirName.split("/"));
        if (Files.exists(path)) {
            deleteDirectory(path);
        }
        Files.createDirectories(path);
        System.out.println("Directory created: " + path);
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.newDirectoryStream(path).forEach(child -> {
                try {
                    deleteDirectory(child);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Files.delete(path);
    }
}
