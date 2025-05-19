# Étape de build avec Maven et JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B # -DskipTests pour ne pas lancer les tests unitaires ici

# Étape d'exécution avec JRE 17
FROM eclipse-temurin:17-jre-alpine AS final
WORKDIR /app
# Copiez le JAR depuis l'étape de build
COPY --from=builder /app/target/*.jar application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]

