FROM maven:3.8.3-openjdk-17

ARG build_type=dev

RUN mkdir -p /opt/messenger/messenger
WORKDIR /opt/messenger/messenger

COPY pom.xml mvnw ./
COPY .mvn ./.mvn
COPY src ./src

COPY frontend/themes ./frontend/themes
COPY frontend/index.html ./frontend
COPY package.json tsconfig.json types.d.ts webpack.config.js ./

RUN mvn clean package -P$build_type -DskipTests

COPY wait-for-it.sh docker-entrypoint.sh ./
RUN ["chmod", "+x", "docker-entrypoint.sh"]
RUN ["chmod", "+x", "wait-for-it.sh"]

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["java", "-jar", "/opt/messenger/messenger/target/messenger-1.0.0-SNAPSHOT.jar"]