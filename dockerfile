# Utiliza la imagen base de OpenJDK 17
FROM openjdk:17-jdk-slim


# Puerto que la aplicación usará
EXPOSE 8080

# Ruta del JAR en el contenedor
ARG JAR_FILE=target/hefesto-0.0.2-SNAPSHOT.jar

# Copia el JAR en el contenedor
COPY ${JAR_FILE} app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java","-jar","/app.jar"]
