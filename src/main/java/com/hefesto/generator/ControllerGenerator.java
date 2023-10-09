package com.hefesto.generator;

import com.squareup.javapoet.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.hefesto.generator.JpaGenerator.toPascalCase;

public class ControllerGenerator {

    public static void generateControllers(String sql, String path, String appName) {

        var fullPath = path + "\\" + appName + "\\";
        var src = fullPath + "src\\";
        var main = src + "main\\";
        var java = main + "java\\";

        var createStatements = extractCreateStatements(sql);

        createStatements.forEach(createStatement -> {


            var controllerClassName = toPascalCase(createStatement) + "Controller";
            var responseEntityName = toPascalCase(createStatement) + "Response";

            var responseEntityType = ParameterizedTypeName.get(
                    ClassName.get("org.springframework.http", "ResponseEntity"),
                    ClassName.get("com." + appName.toLowerCase() + ".rest.response", responseEntityName)
            );

            var controllerClass = TypeSpec.classBuilder(controllerClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                            .addMember("value", "{$S, $S, $S, $S}", "/meat/" + createStatement, "/fish/" + createStatement, "/frozen/" + createStatement, "/frnv/" + createStatement)
                            .build())
                    .addMethod(generateCreateMethod(toPascalCase(createStatement), responseEntityType))
                    .addMethod(generateReadMethod(toPascalCase(createStatement), responseEntityType))
                    .addMethod(generateUpdateMethod(toPascalCase(createStatement), responseEntityType))
                    .addMethod(generateDeleteMethod(toPascalCase(createStatement)))
                    .addMethod(generateListMethod(toPascalCase(createStatement), responseEntityType))
                    .build();

            var packagePath = "com." + appName.toLowerCase() + ".controller";

            try {
                var javaFile = JavaFile.builder(packagePath, controllerClass).build();
                javaFile.writeTo(Paths.get(java));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    private static MethodSpec generateCreateMethod(String entityName, TypeName responseEntityType) {
        return MethodSpec.methodBuilder("create" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(responseEntityType)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                .addStatement("return null")
                .build();
    }


    private static MethodSpec generateReadMethod(String entityName, TypeName responseEntityType) {
        return MethodSpec.methodBuilder("get" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(responseEntityType)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                .addParameter(ParameterSpec.builder(String.class, "code")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestParam"))
                        .build())
                .addStatement("return null")
                .build();
    }

    private static MethodSpec generateListMethod(String entityName, TypeName responseEntityType) {
        var returnType = ParameterizedTypeName.get(
                ClassName.get(Page.class),
                responseEntityType
        );

        return MethodSpec.methodBuilder("list" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                        .addMember("value", "$S", "/all")
                        .build())
                .addParameter(Pageable.class, "pageable")
                .addStatement("return null")
                .build();
    }


    private static MethodSpec generateUpdateMethod(String entityName, TypeName responseEntityType) {
        return MethodSpec.methodBuilder("update" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(responseEntityType)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PutMapping"))
                .addStatement("return null")
                .build();
    }

    private static MethodSpec generateDeleteMethod(String entityName) {
        return MethodSpec.methodBuilder("delete" + entityName)
                .addModifiers(Modifier.PUBLIC)
                .returns(Boolean.class)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping"))
                .addStatement("return null")
                .build();
    }

    private static List<String> extractCreateStatements(String sql) {
        var pattern = Pattern.compile("CREATE TABLE (\\w+)", Pattern.CASE_INSENSITIVE);
        var entities = new ArrayList<String>();
        var matcher = pattern.matcher(sql);

        while (matcher.find()) {
            entities.add(matcher.group(1));
        }

        return entities;
    }

}

