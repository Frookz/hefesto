package com.hefesto.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.lang.model.element.Modifier;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
public class DirectoryGenerator {

    public static void createAllDirectories(String basePackagePath, String appName) {
        appName += "/";
        var src = appName + "src/";
        var main = src + "main/";
        var test = src + "test/";
        var java = main + "java/";
        var com = java + "com/";
        var app = com + appName.toLowerCase() + "/";
        var resources = main + "resources/";
        var directories = Arrays.asList(appName,
                                        src,
                                        main,
                                        test,
                                        java,
                                        com,
                                        app,
                                        resources,
                                        app + "controller",
                                        app + "jpa",
                                        app + "dto",
                                        app + "service",
                                        app + "rest",
                                        app + "rest/response",
                                        app + "rest/request",
                                        app + "business",
                                        app + "config",
                                        app + "exceptions",
                                        app + "repositories");
            
        directories.forEach(dir -> {
            try {
                createDirectory(basePackagePath, dir);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        var currentProjectPath = System.getProperty("user.dir");

        // Copiar los archivos y carpetas adicionales
        copyFileToProject("pom.xml", basePackagePath + "/" + appName);
        copyFileToProject("mvnw", basePackagePath + "/" + appName);
        copyFileToProject("mvnw.cmd", basePackagePath + "/" + appName);
        copyFileToProject("compose.yaml", basePackagePath + "/" + appName);

        copyDirectoryToProject(".mvn", basePackagePath + "/" + appName);

        updateAndCopyApplicationYml(currentProjectPath, basePackagePath + "/" + appName + "/src/main/resources/");

        updatePomElements(basePackagePath + "/" + appName + "pom.xml", appName);

        generateMainClass(appName.replace("/", ""), basePackagePath + "\\" + appName);
    }

    public static void updatePomElements(String filePath, String newName) {
        try {
            var inputFile = new File(filePath);
            var dbFactory = DocumentBuilderFactory.newInstance();
            var dBuilder = dbFactory.newDocumentBuilder();
            var doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            updateTagContent(doc, "name", newName.replace("/", ""));
            updateTagContent(doc, "groupId", "com." + newName.toLowerCase().replace("/", ""));
            updateTagContent(doc, "artifactId", newName.toLowerCase().replace("/", ""));

            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            var source = new DOMSource(doc);
            var result = new StreamResult(new File(filePath));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateTagContent(Document doc, String tagName, String newContent) {
        var nList = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nList.getLength(); i++) {
            var node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getParentNode().getNodeName().equals("project")) {
                node.setTextContent(newContent);
                break;
            }
        }
    }
    private static void updateAndCopyApplicationYml(String sourceProjectPath, String destinationProjectPath) {
        var sourcePath = Paths.get(sourceProjectPath, "src/main/resources/application.yml");
        var destinationPath = Paths.get(destinationProjectPath, "application.yml");
        try {
            var content = new String(Files.readAllBytes(sourcePath));
            var random = new Random();
            int randomPort = random.nextInt(8999 - 8081 + 1) + 8081;
            content = content.replace("8080", String.valueOf(randomPort));
            Files.write(destinationPath, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyFileToProject(String fileName, String destinationProjectPath) {
        var sourcePath = Paths.get(fileName);
        var destinationPath = Paths.get(destinationProjectPath, fileName);

        try {
            Files.copy(sourcePath, destinationPath);
            log.info(fileName + " has been copied to the new project.");
        } catch (IOException e) {
            log.info("An error occurred while copying " + fileName);
            e.printStackTrace();
        }
    }

    private static void copyDirectoryToProject(String directoryName, String destinationProjectPath) {
        var sourceDirectory = Paths.get(directoryName);
        var destinationDirectory = Paths.get(destinationProjectPath, directoryName);

        try {
            Files.walk(sourceDirectory).forEach(source -> {
                try {
                    var destination = Paths.get(destinationDirectory.toString(), source.toString().substring(sourceDirectory.toString().length()));
                    Files.copy(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            log.info(directoryName + " has been copied to the new project.");
        } catch (IOException e) {
            log.info("An error occurred while copying directory " + directoryName);

        }
    }

    private static void createDirectory(String basePath, String dirName) throws IOException {
        var path = Paths.get(basePath, dirName.split("/"));
        if (Files.exists(path)) {
            deleteDirectory(path);
        }
        Files.createDirectories(path);
        log.info("Directory created: " + path);
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

    public static void generateMainClass(String appName, String basePackagePath) {
        var springBootApplication = ClassName.get(SpringBootApplication.class);
        var springApplication = ClassName.get(SpringApplication.class);
        var slf4j = ClassName.get(Slf4j.class);

        var mainMethod = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addStatement("$T.run($N.class, args)", springApplication, appName)
                .build();

        var mainClass = TypeSpec.classBuilder(appName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(springBootApplication)
                .addAnnotation(slf4j)
                .addMethod(mainMethod)
                .build();

        var lowerCase = appName.toLowerCase();
        var javaFile = JavaFile.builder("com." + lowerCase, mainClass)
                .build();

        try {
            var filePath = basePackagePath.replace("/", "") + "\\" + "src" + "\\main" + "\\java";
            javaFile.writeTo(Path.of(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
