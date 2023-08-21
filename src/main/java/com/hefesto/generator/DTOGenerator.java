package com.hefesto.generator;

import com.squareup.javapoet.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static com.hefesto.generator.JpaGenerator.*;

public class DTOGenerator {

    private static final Map<String, TypeSpec.Builder> dtoBuilders = new HashMap<>();
    private static final Map<String, String> referenceMap = new HashMap<>();
    private static final Map<String, Set<String>> oneToManyMap = new HashMap<>();

    public static void generateDTOs() {
        String sql;
        try {
            sql = new String(Files.readAllBytes(Paths.get("C:\\workspace\\hefesto\\src\\main\\resources\\db\\migration\\test.sql")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var createStatements = extractCreateStatements(sql);

        // Generate all DTOs
        createStatements.forEach(createStatement -> {
            var tableName = extractTableName(createStatement);
            var fields = detectFields(createStatement);
            var references = detectReferences(createStatement);
            generateDTO(tableName, fields, references);
        });

        // Finalize DTO generation
        dtoBuilders.forEach((name, builder) -> {
            if (oneToManyMap.containsKey(name)) {
                oneToManyMap.get(name).forEach(relation -> {
                    var listType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("com.hefesto.dto", relation + "DTO"));
                    var fieldBuilder = FieldSpec.builder(listType, toCamelCase(toSnakeCase(relation)) + "s", Modifier.PRIVATE);
                    builder.addField(fieldBuilder.build());
                });
            }

            var javaFile = JavaFile.builder("com.hefesto.dto", builder.build()).build();
            try {
                javaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static String extractTableName(String createStatement) {
        var pattern = Pattern.compile("CREATE TABLE (\\w+)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(createStatement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("No table name found in the provided CREATE statement.");
    }


    public static void generateDTO(String rawTableName, Map<String, String> fields, Map<String, String> references) {
        var dtoName = toPascalCase(rawTableName) + "DTO";
        var classBuilder = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class);

        fields.forEach((fieldName, value) -> {
            var javaType = getJavaType(value);
            var camelCaseFieldName = toCamelCase(fieldName);
            var fieldBuilder = FieldSpec.builder(javaType, camelCaseFieldName, Modifier.PRIVATE);

            if (references.containsKey(fieldName)) {
                var referenceDtoName = toPascalCase(references.get(fieldName)) + "DTO";
                var referenceType = ClassName.get("com.hefesto.dto", referenceDtoName);
                fieldBuilder = FieldSpec.builder(referenceType, toCamelCase(toSnakeCase(camelCaseFieldName.replace("Id", ""))), Modifier.PRIVATE);
                referenceMap.put(dtoName, referenceDtoName);

                oneToManyMap.computeIfAbsent(referenceDtoName, k -> new HashSet<>()).add(dtoName.replace("DTO", ""));            }

            classBuilder.addField(fieldBuilder.build());
        });

        dtoBuilders.put(dtoName, classBuilder);
    }

}

