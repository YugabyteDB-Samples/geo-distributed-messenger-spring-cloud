FROM azul/zulu-openjdk:17

RUN mkdir -p /opt/messenger/attachments
WORKDIR /opt/messenger/attachments

COPY pom.xml mvnw ./
COPY .mvn ./.mvn

RUN ./mvnw dependency:resolve

COPY src ./src

COPY wait-for-it.sh docker-entrypoint.sh ./
RUN ["chmod", "+x", "docker-entrypoint.sh"]
RUN ["chmod", "+x", "wait-for-it.sh"]

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["./mvnw", "spring-boot:run"]