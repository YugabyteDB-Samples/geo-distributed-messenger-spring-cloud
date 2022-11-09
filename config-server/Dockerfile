FROM azul/zulu-openjdk:17

RUN mkdir -p /opt/messenger/discovery-server
WORKDIR /opt/messenger/discovery-server

COPY pom.xml mvnw ./
COPY .mvn ./.mvn

RUN ./mvnw dependency:resolve

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]