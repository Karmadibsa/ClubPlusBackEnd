# ---- Étape 1: Construire l'application avec Maven ----
# Utiliser une image Docker qui contient Maven et JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Définir le répertoire de travail dans le conteneur pour cette étape
WORKDIR /app

# Copier le fichier pom.xml pour télécharger les dépendances en premier (optimisation du cache Docker)
COPY pom.xml .

# (Si vous utilisez le wrapper Maven (mvnw), décommentez et adaptez les lignes suivantes)
# COPY .mvn/ .mvn
# COPY mvnw .
# RUN ./mvnw dependency:go-offline

# S'il n'y a pas de wrapper, et que vous avez copié seulement pom.xml, téléchargez les dépendances.
# Cela peut être fait automatiquement par la commande package, mais pour une meilleure gestion du cache :
RUN mvn dependency:go-offline -B

# Copier le reste du code source de l'application
COPY src ./src

# Construire l'application et créer le fichier .jar.
# -DskipTests saute l'exécution des tests unitaires pour accélérer la construction de l'image.
# Vous pouvez les enlever si vous voulez que les tests s'exécutent pendant la construction de l'image.
RUN mvn package -DskipTests -B


# ---- Étape 2: Créer l'image finale ----
# Partir d'une image Java JRE légère (OpenJDK 17) car nous n'avons plus besoin de Maven
FROM openjdk:17-jdk-slim

# Définir le répertoire de travail dans le conteneur pour l'image finale
WORKDIR /app

# Copier uniquement le fichier .jar construit depuis l'étape 'builder'
# Le JAR se trouvera dans /app/target/ à l'intérieur de l'étape 'builder'
COPY --from=builder /app/target/*.jar application.jar

# AJOUTEZ CETTE LIGNE pour copier le fichier .env
# Assurez-vous que le fichier .env existe bien à la racine de votre projet backend
# (C:\Users\mompe\Desktop\Cours\Club plus\ClubPlusBackEnd\.env)
COPY .env .

# Exposer le port sur lequel votre application Spring Boot écoute (par défaut 8080)
EXPOSE 8080

# Commande pour démarrer votre application Spring Boot lorsque le conteneur se lance
# Le / devant application.jar n'est pas nécessaire si WORKDIR est /app
ENTRYPOINT ["java", "-jar", "application.jar"]
