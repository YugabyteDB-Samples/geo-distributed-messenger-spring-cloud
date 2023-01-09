FROM maven:3.8.3-openjdk-17

ARG build_type=dev

RUN mkdir -p /opt/messenger/config-server
WORKDIR /opt/messenger/config-server

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

RUN mvn clean package -P$build_type -DskipTests

CMD ["java", "-jar", "/opt/messenger/config-server/target/config-server-1.0.0-SNAPSHOT.jar"]