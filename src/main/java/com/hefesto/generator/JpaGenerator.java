package com.hefesto.generator;

import com.squareup.javapoet.*;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JpaGenerator {
    private static final Map<String, TypeSpec.Builder> entityBuilders = new HashMap<>();
    private static final Map<String, List<String>> oneToManyMap = new HashMap<>();
    private static final Set<String> manyToManyTables = new HashSet<>();
    private static final Set<String> oneToOneTables = new HashSet<>();
    private static final Map<String, String> oneToOneReferences = new HashMap<>();

    public static void generateJpaEntities() {
        String sql;
        try {
            sql = new String(Files.readAllBytes(Paths.get("C:\\workspace\\hefesto\\src\\main\\resources\\db\\migration\\test.sql")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var createStatements = extractCreateStatements(sql);
        manyToManyTables.addAll(detectManyToManyTables(sql));
        oneToOneTables.addAll(detectOneToOneTables(sql));

        createStatements.forEach(createStatement -> {
            var tableName = extractTableName(createStatement);
            var fields = detectFields(createStatement);
            var references = detectReferences(createStatement);
            generateJpaEntity(tableName, fields, references);
        });

        entityBuilders.forEach((name, builder) -> {
            if (oneToManyMap.containsKey(name)) {
                oneToManyMap.get(name).forEach(relation -> {
                    if (!fieldExists(builder, toCamelCase(toSnakeCase(relation)) + "s")) {
                        var listType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("com.hefesto.jpa", relation));
                        var fieldBuilder = FieldSpec.builder(listType, toCamelCase(toSnakeCase(relation)) + "s", Modifier.PRIVATE)
                                .addAnnotation(AnnotationSpec.builder(OneToMany.class)
                                        .addMember("mappedBy", "$S", toCamelCase(name).toLowerCase())
                                        .build());
                        builder.addField(fieldBuilder.build());
                    }
                });
            }

            var javaFile = JavaFile.builder("com.hefesto.jpa", builder.build()).build();
            try {
                javaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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

        if (isManyToMany(rawTableName)) {
            var entities = rawTableName.split("_");
            var firstEntity = entities[0];
            var secondEntity = entities[1];

            if (!fieldExists(classBuilder, "id")) {
                classBuilder.addField(FieldSpec.builder(ClassName.get("com.hefesto.jpa", tableName + "Id"), "id", Modifier.PRIVATE)
                        .addAnnotation(EmbeddedId.class)
                        .build());
            }

            if (!fieldExists(classBuilder, firstEntity.toLowerCase())) {
                classBuilder.addField(FieldSpec.builder(ClassName.get("com.hefesto.jpa", toPascalCase(firstEntity)), firstEntity.toLowerCase(), Modifier.PRIVATE)
                        .addAnnotation(ManyToOne.class)
                        .addAnnotation(JoinColumn.class)
                        .addAnnotation(AnnotationSpec.builder(MapsId.class)
                                .addMember("value", "$S", firstEntity.toLowerCase() + "Id")
                                .build())
                        .build());
            }
            if (!fieldExists(classBuilder, secondEntity.toLowerCase())) {
                classBuilder.addField(FieldSpec.builder(ClassName.get("com.hefesto.jpa", toPascalCase(secondEntity)), secondEntity.toLowerCase(), Modifier.PRIVATE)
                        .addAnnotation(ManyToOne.class)
                        .addAnnotation(JoinColumn.class)
                        .addAnnotation(AnnotationSpec.builder(MapsId.class)
                                .addMember("value", "$S", secondEntity.toLowerCase() + "Id")
                                .build())
                        .build());
            }

            var keyClass = generateEmbeddedIdClass(tableName, firstEntity, secondEntity);
            var keyJavaFile = JavaFile.builder("com.hefesto.jpa", keyClass).build();
            try {
                keyJavaFile.writeTo(Paths.get("C:\\workspace\\hefesto\\src\\main\\java"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fields.forEach((fieldName, value) -> {
                var javaType = getJavaType(value);
                var camelCaseFieldName = toCamelCase(fieldName);
                var fieldBuilder = FieldSpec.builder(javaType, camelCaseFieldName, Modifier.PRIVATE)
                        .addAnnotation(Column.class);

                if ("id".equalsIgnoreCase(camelCaseFieldName)) {
                    fieldBuilder.addAnnotation(Id.class);
                }

                if (references.containsKey(fieldName)) {
                    if (oneToOneReferences.containsKey(fieldName)) {
                        var referenceType = ClassName.get("com.hefesto.jpa", toPascalCase(references.get(fieldName)));
                        fieldBuilder = FieldSpec.builder(referenceType, toCamelCase(toSnakeCase(camelCaseFieldName.replace("Id", ""))), Modifier.PRIVATE)
                                .addAnnotation(OneToOne.class)
                                .addAnnotation(JoinColumn.class);
                    } else {
                        var referenceType = ClassName.get("com.hefesto.jpa", toPascalCase(references.get(fieldName)));
                        fieldBuilder = FieldSpec.builder(referenceType, toCamelCase(toSnakeCase(camelCaseFieldName.replace("Id", ""))), Modifier.PRIVATE)
                                .addAnnotation(ManyToOne.class)
                                .addAnnotation(JoinColumn.class);
                        oneToManyMap
                                .computeIfAbsent(toPascalCase(references.get(fieldName)), k -> new ArrayList<>())
                                .add(tableName);
                    }
                }

                classBuilder.addField(fieldBuilder.build());
            });
        }

        entityBuilders.put(tableName, classBuilder);
    }

    public static List<String> detectOneToOneTables(String sql) {
        var pattern = Pattern.compile("(\\w+)\\s+UUID\\s+UNIQUE REFERENCES\\s+(\\w+)\\s*\\(id\\)", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(sql);
        var oneToOneTables = new ArrayList<String>();
        while (matcher.find()) {
            oneToOneTables.add(matcher.group(1));
        }
        return oneToOneTables;
    }

    private static void addManyToOneField(TypeSpec.Builder classBuilder, String entityName) {
        String fieldName = entityName.toLowerCase();
        if (fieldExists(classBuilder, fieldName)) {
            var fieldBuilder = FieldSpec.builder(ClassName.get("com.hefesto.jpa", toPascalCase(entityName)), fieldName, Modifier.PRIVATE)
                    .addAnnotation(ManyToOne.class)
                    .addAnnotation(JoinColumn.class)
                    .addAnnotation(AnnotationSpec.builder(MapsId.class)
                    .addMember("value", "$S", fieldName + "Id")
                    .build());

            classBuilder.addField(fieldBuilder.build());
        }
    }

    private static boolean fieldExists(TypeSpec.Builder builder, String fieldName) {
        return builder.fieldSpecs.stream().anyMatch(field -> field.name.equals(fieldName));
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

    private static TypeSpec generateEmbeddedIdClass(String tableName, String firstEntity, String secondEntity) {
        return TypeSpec.classBuilder(tableName + "Id")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Serializable.class)
                .addAnnotation(Embeddable.class)
                .addAnnotation(Getter.class)
                .addAnnotation(Setter.class)
                .addAnnotation(EqualsAndHashCode.class)
                .addField(FieldSpec.builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L", "1L")
                        .build())
                .addField(FieldSpec.builder(UUID.class, firstEntity.toLowerCase() + "Id", Modifier.PRIVATE)
                        .build())
                .addField(FieldSpec.builder(UUID.class, secondEntity.toLowerCase() + "Id", Modifier.PRIVATE)
                        .build())
                .build();
    }

    public static String toPascalCase(String s) {
        return Arrays.stream(s.split("_"))
                .map(word -> {
                    String singularWord = word.endsWith("s")
                            ? word.substring(0, word.length() - 1)
                            : word;
                    return singularWord.substring(0, 1).toUpperCase() + singularWord.substring(1).toLowerCase();
                })
                .collect(Collectors.joining());
    }

    public static String toCamelCase(String s) {
        var words = s.split("_");
        var result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1).toLowerCase());
        }
        return result.toString();
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
            if (sql.contains(matcher.group(1) + " UUID UNIQUE REFERENCES")) {
                oneToOneReferences.put(matcher.group(1), matcher.group(2));
            }
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

    public static String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Arrays.stream(input.split("(?=[A-Z])"))
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"));
    }
}
