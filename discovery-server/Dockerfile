FROM maven:3.8.3-openjdk-17

RUN mkdir -p /opt/messenger/discovery-server
WORKDIR /opt/messenger/discovery-server

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

RUN mvn clean package -DskipTests

CMD ["java", "-jar", "/opt/messenger/discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar"]