package com.hefesto.generator;

import com.squareup.javapoet.*;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JpaGenerator {
    private static final Map<String, TypeSpec.Builder> entityBuilders = new HashMap<>();
    private static final Map<String, List<String>> oneToManyMap = new HashMap<>();
    private static final Set<String> manyToManyTables = new HashSet<>();
    private static final Set<String> oneToOneTables = new HashSet<>();

    public static void generateJpaEntities(String sql, String path, String appName) {
        var appOg = appName;
        appName = path + "\\" + appName + "\\";
        var src = appName + "src\\";
        var main = src + "main\\";
        var java = main + "java\\";

        var createStatements = extractCreateStatements(sql);
        manyToManyTables.addAll(detectManyToManyTables(sql));
        oneToOneTables.addAll(detectOneToOneTables(sql));

        createStatements.forEach(createStatement -> {
            var tableName = extractTableName(createStatement);
            var fields = detectFields(createStatement);
            var references = detectReferences(createStatement);
            var oneToOneRefs = detectOneToOneReferences(createStatement);
            if (!oneToOneRefs.isEmpty()) {
                references.putAll(oneToOneRefs);
            }
            generateJpaEntity(tableName, fields, references, oneToOneRefs, java, appOg);
        });

        for (var entry : entityBuilders.entrySet()) {
            var name = entry.getKey();
            var builder = entry.getValue();
            if (oneToManyMap.containsKey(name)) {
                for (var relation : oneToManyMap.get(name)) {
                    if (!fieldExists(builder, toCamelCase(toSnakeCase(relation)) + "s")) {
                        var listType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("com." + appOg.toLowerCase() + ".jpa", relation));
                        var fieldBuilder = FieldSpec.builder(listType, toCamelCase(toSnakeCase(relation)) + "s", Modifier.PRIVATE)
                                .addAnnotation(AnnotationSpec.builder(OneToMany.class)
                                        .addMember("mappedBy", "$S", toCamelCase(name).toLowerCase())
                                        .build());
                        builder.addField(fieldBuilder.build());
                    }
                }
            }

            var javaFile = JavaFile.builder("com."+ appOg.toLowerCase() + ".jpa", builder.build()).build();
            try {

                var filePath = path + "\\" + appOg + "\\src" + "\\main" + "\\java";
                javaFile.writeTo(Paths.get(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void generateJpaEntity(String rawTableName, Map<String, String> fields, Map<String, String> references, Map<String, String> oneToOneRefs, String path, String appName) {
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
                classBuilder.addField(FieldSpec.builder(ClassName.get("com."+ appName.toLowerCase() + ".jpa", tableName + "Id"), "id", Modifier.PRIVATE)
                        .addAnnotation(EmbeddedId.class)
                        .build());
            }

            if (!fieldExists(classBuilder, firstEntity.toLowerCase())) {
                classBuilder.addField(FieldSpec.builder(ClassName.get("com."+ appName.toLowerCase() + ".jpa", toPascalCase(firstEntity)), firstEntity.toLowerCase(), Modifier.PRIVATE)
                        .addAnnotation(ManyToOne.class)
                        .addAnnotation(JoinColumn.class)
                        .addAnnotation(AnnotationSpec.builder(MapsId.class)
                                .addMember("value", "$S", firstEntity.toLowerCase() + "Id")
                                .build())
                        .build());
            }

            if (!fieldExists(classBuilder, secondEntity.toLowerCase())) {
                classBuilder.addField(FieldSpec.builder(ClassName.get("com."+ appName.toLowerCase() + ".jpa", toPascalCase(secondEntity)), secondEntity.toLowerCase(), Modifier.PRIVATE)
                        .addAnnotation(ManyToOne.class)
                        .addAnnotation(JoinColumn.class)
                        .addAnnotation(AnnotationSpec.builder(MapsId.class)
                                .addMember("value", "$S", secondEntity.toLowerCase() + "Id")
                                .build())
                        .build());
            }

            var keyClass = generateEmbeddedIdClass(tableName, firstEntity, secondEntity);
            var keyJavaFile = JavaFile.builder("com."+ appName.toLowerCase() + ".jpa", keyClass).build();
            try {
                keyJavaFile.writeTo(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }

            addManyToManyField(firstEntity, tableName, secondEntity , appName.toLowerCase());
            addManyToManyField(secondEntity, tableName, firstEntity , appName.toLowerCase());

        }  else {
            fields.forEach((fieldName, value) -> {
                var javaType = getJavaType(value);
                var camelCaseFieldName = toCamelCase(fieldName);
                var fieldBuilder = FieldSpec.builder(javaType, camelCaseFieldName, Modifier.PRIVATE)
                        .addAnnotation(Column.class);

                if ("id".equalsIgnoreCase(camelCaseFieldName)) {
                    fieldBuilder.addAnnotation(Id.class);
                }

                if (references.containsKey(fieldName)) {
                    if (oneToOneRefs.containsKey(fieldName)) {
                        var referenceType = ClassName.get("com."+ appName.toLowerCase() + ".jpa", toPascalCase(oneToOneRefs.get(fieldName)));
                        var name = toCamelCase(toSnakeCase(fieldName.replace("_id", "")));
                        fieldBuilder = FieldSpec.builder(referenceType, name, Modifier.PRIVATE)
                                .addAnnotation(OneToOne.class)
                                .addAnnotation(JoinColumn.class);
                    } else {
                        var referenceType = ClassName.get("com."+ appName.toLowerCase() + ".jpa", toPascalCase(references.get(fieldName)));
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

    public static Map<String, String> detectOneToOneReferences(String createStatement) {
        Map<String, String> oneToOneReferences = new HashMap<>();

        // Regular expression to capture the structure (case-insensitive)
        var patternStr = "(?i)(\\w+\\s+UUID\\s+UNIQUE\\s+REFERENCES\\s+\\w+\\s*\\(\\s*\\w+\\s*\\))";
        var pattern = Pattern.compile(patternStr);

        // Clean up the statement
        var cleanedStatement = createStatement.replace("\r", "").replace("\n", "").replaceAll("\\s+", " ");

        var matcher = pattern.matcher(cleanedStatement);

        while (matcher.find()) {
            var fullMatch = matcher.group(1);

            // Extract field name and referenced table
            var parts = fullMatch.split("\\s+");
            var fieldName = parts[0];
            var referencedTable = parts[parts.length - 2]; // Get the table name from "REFERENCES table_name (column)"

            oneToOneReferences.put(fieldName, referencedTable);
        }

        return oneToOneReferences;
    }

    private static void addManyToManyField(String mainEntity, String relatedEntity, String junctionTable, String appName) {
        if (entityBuilders.containsKey(toPascalCase(mainEntity))) {
            var builder = entityBuilders.get(toPascalCase(mainEntity));
            if (!fieldExists(builder, relatedEntity + "s")) {
                var setOfRelatedEntity = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("com."+ appName.toLowerCase() + ".jpa", toPascalCase(toSnakeCase(relatedEntity))));
                var relatedEntityField = FieldSpec.builder(setOfRelatedEntity, toCamelCase(toSnakeCase(relatedEntity)) + "s", Modifier.PRIVATE)
                        .addAnnotation(ManyToMany.class)
                        .addAnnotation(AnnotationSpec.builder(JoinColumns.class)
                                .addMember("value", "{@$T(name=\"$L\"), @$T(name=\"$L\")}", JoinColumn.class, mainEntity.toLowerCase() + "_id", JoinColumn.class, junctionTable.toLowerCase() + "_id")
                                .build())
                        .build();
                builder.addField(relatedEntityField);
            }
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
            case "TIMESTAMP", "TIMESTAMP DEFAULT NOW()" -> ClassName.get(java.sql.Timestamp.class);
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
                        .addAnnotation(Serial.class)
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
        var pattern = Pattern.compile("(\\w+)\\s+(UUID|TEXT|VARCHAR\\(\\d+\\)|CHAR\\(\\d+\\)|INT4|INT8|FLOAT8|FLOAT4|BOOL|DATE|TIMESTAMP(\\(\\d*\\))?\\s*(DEFAULT\\s+NOW\\(\\))?(\\s+NOT\\s+NULL)?|NUMERIC|DECIMAL|BYTEA|JSON|JSONB|CITEXT)", Pattern.CASE_INSENSITIVE);
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
            if (!sql.contains(matcher.group(1) + " UUID UNIQUE REFERENCES")) {
                references.put(matcher.group(1), matcher.group(2));
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
