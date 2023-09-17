package com.hefesto.generator;

import com.squareup.javapoet.*;
import lombok.Data;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.hefesto.generator.JpaGenerator.*;

public class ResponseGenerator {

    private static final Map<String, TypeSpec.Builder> responseBuilders = new HashMap<>();
    private static final Map<String, Set<String>> oneToManyMap = new HashMap<>();
    private static final Map<String, String> skipManyToOne = new HashMap<>();
    public static void generateResponses(String sql, String path, String appName) {
        var appNameOg = appName;
        appName = path + "\\" + appName + "\\";
        var src = appName + "src\\";
        var main = src + "main\\";
        var java = main + "java\\";

        var createStatements = extractCreateStatements(sql);



        createStatements.forEach(createStatement -> {
            var tableName = extractTableName(createStatement);
            var fields = detectFields(createStatement);
            var references = detectReferences(createStatement);
            generateResponse(tableName, fields, references);
        });

        responseBuilders.forEach((name, builder) -> {
            if (oneToManyMap.containsKey(name)) {
                var packageName = "com." + appNameOg.toLowerCase() + ".rest.response";
                oneToManyMap.get(name).forEach(relation -> {
                    var foreignKeyName = toCamelCase(name.replace("Response", "")) + "Id";
                    skipManyToOne.put(relation.toLowerCase(), foreignKeyName);
                    var listType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(packageName, toPascalCase(toSnakeCase(relation)) + "Response"));
                    var fieldBuilder = FieldSpec.builder(listType, toCamelCase(toSnakeCase(relation)) + "s", Modifier.PRIVATE);
                    builder.addField(fieldBuilder.build());

                });
            }

            var javaFile = JavaFile.builder("com." + appNameOg.toLowerCase() + ".rest.response", builder.build()).build();
            try {
                javaFile.writeTo(Paths.get(java));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public static void generateResponse(String rawTableName, Map<String, String> fields, Map<String, String> references) {
        var dtoName = toPascalCase(rawTableName) + "Response";
        var classBuilder = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class);

        fields.forEach((fieldName, fieldType) -> {
            var javaType = getJavaType(fieldType);

             if (!references.containsKey(fieldName)) {
                var fieldBuilder = FieldSpec.builder(javaType, toCamelCase(fieldName), Modifier.PRIVATE);
                classBuilder.addField(fieldBuilder.build());
            } else {
                 var referenceTableName = references.get(fieldName);
                oneToManyMap.computeIfAbsent(toPascalCase(referenceTableName) + "Response", k -> new HashSet<>()).add(rawTableName);
            }
        });

        responseBuilders.put(dtoName, classBuilder);
    }




}
