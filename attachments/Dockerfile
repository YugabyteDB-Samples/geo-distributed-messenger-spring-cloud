FROM azul/zulu-openjdk:17

RUN mkdir -p /opt/messenger/attachments
WORKDIR /opt/messenger/attachments

COPY pom.xml mvnw ./
COPY .mvn ./.mvn

RUN ./mvnw dependency:resolve

COPY src ./src

CMD ["./mvnw", "spring-boot:run"]