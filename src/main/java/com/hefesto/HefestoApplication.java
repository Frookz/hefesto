package com.hefesto;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HefestoApplication {

    private static final Map<String, TypeSpec.Builder> entityBuilders = new HashMap<>();
    private static final Map<String, List<String>> oneToManyMap = new HashMap<>();
    private static final Set<String> generatedEntities = new HashSet<>();  // Track generated entities

    public static void main(String[] args) {
        SpringApplication.run(HefestoApplication.class, args);
        String sql;
        try {
            sql = new String(Files.readAllBytes(Paths.get("C:\\workspace\\hefesto\\src\\main\\resources\\db\\migration\\test.sql")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var createStatements = extractCreateStatements(sql);

        // Generate ManyToMany entities first
        detectManyToManyTables(sql).stream()
                .filter(manyToManyTable -> manyToManyTable.split("_").length >= 2)
                .forEach(manyToManyTable -> {
                    var entities = manyToManyTable.split("_");
                    var manyToManyEntity = generateManyToManyEntity(manyToManyTable, entities[0], entities[1]);
                    var javaFile = JavaFile.builder("com.hefesto", manyToManyEntity).build();
                    try {
                        javaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
                        generatedEntities.add(manyToManyTable);  // Mark this entity as generated
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // Generate other entities
        createStatements.stream()
                .filter(statement -> !generatedEntities.contains(extractTableName(statement)))  // Skip already generated entities
                .forEach(createStatement -> {
                    var tableName = extractTableName(createStatement);
                    var fields = detectFields(createStatement);
                    var references = detectReferences(createStatement);
                    generateJpaEntity(tableName, fields, references);
                });

        // Finalize entity generation after adding OneToMany relationships
        entityBuilders.forEach((name, builder) -> {
            if (oneToManyMap.containsKey(name)) {
                oneToManyMap.get(name).forEach(relation -> {
                    var listType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get("com.hefesto", relation));
                    var fieldBuilder = FieldSpec.builder(listType, toCamelCase(relation) + "s", Modifier.PRIVATE)
                            .addAnnotation(AnnotationSpec.builder(OneToMany.class)
                                    .addMember("mappedBy", "$S", toCamelCase(name).toLowerCase())
                                    .build());
                    builder.addField(fieldBuilder.build());
                });
            }

            var javaFile = JavaFile.builder("com.hefesto", builder.build()).build();
            try {
                javaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static List<String> extractCreateStatements(String sql) {
        var pattern = Pattern.compile("CREATE TABLE (\\w+)\\s*\\(([^;]+)\\);", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(sql);
        var createStatements = new ArrayList<String>();
        while (matcher.find()) {
            createStatements.add(matcher.group(0));
        }
        return createStatements;
    }

    public static String extractTableName(String createStatement) {
        var pattern = Pattern.compile("CREATE TABLE (\\w+)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(createStatement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("No table name found in the provided CREATE statement.");
    }

    public static Map<String, String> detectFields(String sql) {
        var pattern = Pattern.compile("(\\w+)\\s+(UUID|TEXT|VARCHAR\\(\\d+\\)|CHAR\\(\\d+\\)|INT4|INT8|FLOAT8|FLOAT4|BOOL|DATE|TIMESTAMP\\(\\d*\\)?|NUMERIC|DECIMAL|BYTEA|JSON|JSONB|CITEXT)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(sql);
        var fields = new HashMap<String, String>();
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        return fields;
    }

    public static Map<String, String> detectReferences(String sql) {
        var pattern = Pattern.compile("(\\w+)\\s+UUID\\s+REFERENCES\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(sql);
        var references = new HashMap<String, String>();
        while (matcher.find()) {
            references.put(matcher.group(1), matcher.group(2));
        }
        return references;
    }

    public static List<String> detectManyToManyTables(String sql) {
        var pattern = Pattern.compile("CREATE TABLE (\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(sql);
        var manyToManyTables = new ArrayList<String>();
        while (matcher.find()) {
            if (isManyToMany(matcher.group(1))) {
                manyToManyTables.add(matcher.group(1));
            }
        }
        return manyToManyTables;
    }

    public static boolean isManyToMany(String tableName) {
        return tableName.split("_").length >= 2;
    }

    public static void generateJpaEntity(String rawTableName, Map<String, String> fields, Map<String, String> references) {
        var tableName = toPascalCase(rawTableName);
        var classBuilder = TypeSpec.classBuilder(tableName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Entity.class)
                .addAnnotation(Getter.class)
                .addAnnotation(Setter.class)
                .addAnnotation(AnnotationSpec.builder(Table.class)
                        .addMember("name", "$S", rawTableName.toLowerCase())
                        .build());

        fields.forEach((fieldName, value) -> {
            var javaType = getJavaType(value);
            var camelCaseFieldName = toCamelCase(fieldName);
            var fieldBuilder = FieldSpec.builder(javaType, camelCaseFieldName, Modifier.PRIVATE)
                    .addAnnotation(Column.class);

            if ("id".equalsIgnoreCase(camelCaseFieldName)) {
                fieldBuilder.addAnnotation(Id.class);
            }

            if (references.containsKey(fieldName)) {
                var referenceType = ClassName.get("com.hefesto", toPascalCase(references.get(fieldName)));
                fieldBuilder = FieldSpec.builder(referenceType, camelCaseFieldName.replace("Id", ""), Modifier.PRIVATE)
                        .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                        .addAnnotation(AnnotationSpec.builder(JoinColumn.class)
                                .addMember("name", "$S", fieldName)
                                .addMember("referencedColumnName", "$S", "id")
                                .build());

                oneToManyMap
                        .computeIfAbsent(toPascalCase(references.get(fieldName)), k -> new ArrayList<>())
                        .add(tableName);
            }

            classBuilder.addField(fieldBuilder.build());
        });

        entityBuilders.put(tableName, classBuilder);
    }


    public static TypeName getJavaType(String sqlType) {
        return switch (sqlType.toUpperCase()) {
            case "UUID" -> ClassName.get(UUID.class);
            case "TEXT", "VARCHAR(255)", "VARCHAR(15)", "CHAR(255)", "CITEXT" -> ClassName.get(String.class);
            case "INT4" -> TypeName.INT;
            case "INT8" -> TypeName.LONG;
            case "FLOAT8" -> TypeName.DOUBLE;
            case "FLOAT4" -> TypeName.FLOAT;
            case "BOOL" -> TypeName.BOOLEAN;
            case "DATE" -> ClassName.get(java.sql.Date.class);
            case "TIMESTAMP" -> ClassName.get(java.sql.Timestamp.class);
            default -> throw new IllegalArgumentException("Unsupported SQL type: " + sqlType);
        };
    }

    public static TypeSpec generateManyToManyEntity(String tableName, String firstEntity, String secondEntity) {
        tableName = toPascalCase(tableName);
        firstEntity = toPascalCase(firstEntity);
        secondEntity = toPascalCase(secondEntity);

        // Generate the Embeddable key class
        var keyClass = generateEmbeddedIdClass(tableName, firstEntity, secondEntity);

        // Save the Embeddable key class
        var keyJavaFile = JavaFile.builder("com.hefesto", keyClass).build();
        try {
            keyJavaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        var classBuilder = TypeSpec.classBuilder(tableName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Entity.class)
                .addAnnotation(AnnotationSpec.builder(Table.class).addMember("name", "$S", toSnakeCase(tableName)).build())
                .addField(FieldSpec.builder(ClassName.get("com.hefesto", tableName + "Id"), "id", Modifier.PRIVATE)
                        .addAnnotation(EmbeddedId.class)  // This is where @EmbeddedId should be
                        .build())
                .addField(FieldSpec.builder(ClassName.get("com.hefesto", firstEntity), toCamelCase(firstEntity), Modifier.PRIVATE)
                        .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                        .addAnnotation(AnnotationSpec.builder(JoinColumn.class)
                                .addMember("name", "$S", toSnakeCase(firstEntity) + "_id")
                                .build())
                        .build())
                .addField(FieldSpec.builder(ClassName.get("com.hefesto", secondEntity), toCamelCase(secondEntity), Modifier.PRIVATE)
                        .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                        .addAnnotation(AnnotationSpec.builder(JoinColumn.class)
                                .addMember("name", "$S", toSnakeCase(secondEntity) + "_id")
                                .build())
                        .build());

        return classBuilder.build();
    }

    private static TypeSpec generateEmbeddedIdClass(String tableName, String firstEntity, String secondEntity) {
        return TypeSpec.classBuilder(tableName + "Id")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Serializable.class)
                .addAnnotation(Embeddable.class)
                .addField(FieldSpec.builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L", "1L")
                        .addAnnotation(Serial.class)
                        .build())
                .addField(FieldSpec.builder(UUID.class, firstEntity.toLowerCase() + "Id", Modifier.PRIVATE)
                        .build())
                .addField(FieldSpec.builder(UUID.class, secondEntity.toLowerCase() + "Id", Modifier.PRIVATE)
                        .build())
                .build();
    }

    private static String toPascalCase(String s) {
        return Arrays.stream(s.split("_"))
                .map(word -> {
                    String singularWord = word.endsWith("s")
                            ? word.substring(0, word.length() - 1)
                            : word;
                    return singularWord.substring(0, 1).toUpperCase() + singularWord.substring(1).toLowerCase();
                })
                .collect(Collectors.joining());
    }


    private static String toCamelCase(String s) {
        var words = s.split("_");
        var result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1).toLowerCase());
        }
        return result.toString();
    }

    public static String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Arrays.stream(input.split("(?=[A-Z])"))
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"));
    }

}
