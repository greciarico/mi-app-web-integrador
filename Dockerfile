# === Etapa de compilación ===
# Usa una imagen base que incluya Maven y un JDK de Java 17
# Puedes ajustar la versión de Java si tu proyecto usa otra (ej. 11, 21)
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia los archivos de configuración de Maven (pom.xml y .mvn)
# Esto aprovecha el cache de Docker: si solo cambias el código, Maven no re-descarga las dependencias.
COPY pom.xml .
COPY .mvn .mvn/

# Copia el código fuente de tu aplicación
COPY src ./src

# Ejecuta el comando Maven para limpiar, compilar y empaquetar la aplicación en un JAR.
# -DskipTests es opcional, pero acelera el build si ya ejecutaste tus tests localmente.
RUN mvn clean package -DskipTests

# === Etapa de ejecución ===
# Usa una imagen más ligera que solo tenga el Java Runtime Environment (JRE) de Java 17
# Esto reduce el tamaño de la imagen final del Docker, lo cual es más eficiente para producción.
FROM eclipse-temurin:21-jre-jammy

# Establece el directorio de trabajo para la aplicación ejecutándose
WORKDIR /app

# Copia el archivo JAR compilado desde la etapa de 'build' a esta etapa
# El `*.jar` asegura que copie el archivo JAR generado, sin importar su nombre completo (ej. mi-app-0.0.1-SNAPSHOT.jar)
COPY --from=build /app/target/*.jar app.jar

# Expone el puerto en el que tu aplicación Spring Boot escuchará.
# Por defecto, Spring Boot usa 8080. Si lo cambiaste en application.properties, ajústalo aquí.
EXPOSE 8080

# Define el comando que se ejecutará cuando el contenedor se inicie
# Esto iniciará tu aplicación Spring Boot.
ENTRYPOINT ["java", "-jar", "app.jar"]
