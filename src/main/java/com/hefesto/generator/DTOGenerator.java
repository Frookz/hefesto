package com.hefesto.generator;

import com.squareup.javapoet.*;
import lombok.Data;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            var oneToOneRefs = detectOneToOneReferences(createStatement);
            if (!oneToOneRefs.isEmpty()) {
                references.putAll(oneToOneRefs);
            }
            generateDTO(tableName, fields, references, oneToOneRefs);
        });

        // Finalize DTO generation
        dtoBuilders.forEach((name, builder) -> {
            if (oneToManyMap.containsKey(name)) {
                oneToManyMap.get(name).forEach(relation -> {
                    var listType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("com.hefesto.dto", relation + "Dto"));
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


    public static void generateDTO(String rawTableName, Map<String, String> fields, Map<String, String> references, Map<String, String> oneToOneRefs) {
        var dtoName = toPascalCase(rawTableName) + "Dto";
        var classBuilder = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class);

        fields.forEach((fieldName, value) -> {
            var javaType = getJavaType(value);
            var camelCaseFieldName = toCamelCase(fieldName);
            var fieldBuilder = FieldSpec.builder(javaType, camelCaseFieldName, Modifier.PRIVATE);

            // If this field is a reference, modify its type and add it to the reference map.
            if (references.containsKey(fieldName)) {
                if (oneToOneRefs.containsKey(fieldName)) { // Check if it's a one-to-one reference
                    var referenceDtoName = toPascalCase(oneToOneRefs.get(fieldName)) + "Dto";
                    var referenceType = ClassName.get("com.hefesto.dto", referenceDtoName);
                    fieldBuilder = FieldSpec.builder(referenceType, toCamelCase(toSnakeCase(camelCaseFieldName.replace("Id", ""))), Modifier.PRIVATE);
                } else {
                    // Existing code for handling one-to-many
                    var referenceDtoName = toPascalCase(references.get(fieldName)) + "Dto";
                    var referenceType = ClassName.get("com.hefesto.dto", referenceDtoName);
                    fieldBuilder = FieldSpec.builder(referenceType, toCamelCase(toSnakeCase(camelCaseFieldName.replace("Id", ""))), Modifier.PRIVATE);
                    referenceMap.put(dtoName, referenceDtoName);
                    oneToManyMap.computeIfAbsent(referenceDtoName, k -> new HashSet<>()).add(dtoName.replace("Dto", ""));
                }
            }

            // Add the field to the class
            classBuilder.addField(fieldBuilder.build());
        });

        dtoBuilders.put(dtoName, classBuilder);
    }

}

