# ===== Etapa 1: build =====
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia só o pom.xml primeiro pra aproveitar cache de camada do Docker —
# as dependências só são baixadas de novo se o pom.xml mudar, não a cada
# alteração de código-fonte (builds seguintes ficam bem mais rápidos).
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ===== Etapa 2: runtime =====
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root — não roda a aplicação como root dentro do container
RUN addgroup -S investai && adduser -S investai -G investai
USER investai

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]