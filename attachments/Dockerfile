FROM maven:3.8.3-openjdk-17

ARG build_type=dev

RUN mkdir -p /opt/messenger/attachments
WORKDIR /opt/messenger/attachments

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

RUN mvn clean package -P$build_type -DskipTests

COPY wait-for-it.sh docker-entrypoint.sh ./
RUN ["chmod", "+x", "docker-entrypoint.sh"]
RUN ["chmod", "+x", "wait-for-it.sh"]

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["java", "-jar", "/opt/messenger/attachments/target/attachments-1.0.0-SNAPSHOT.jar"]