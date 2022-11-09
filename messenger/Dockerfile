FROM azul/zulu-openjdk:17

RUN mkdir -p /opt/messenger/messenger
WORKDIR /opt/messenger/messenger

COPY pom.xml mvnw ./
COPY .mvn ./.mvn

RUN ./mvnw dependency:resolve

COPY src ./src

COPY frontend/themes ./frontend/themes
COPY frontend/index.html ./frontend
COPY package.json tsconfig.json types.d.ts webpack.config.js ./

COPY wait-for-it.sh docker-entrypoint.sh ./
RUN ["chmod", "+x", "docker-entrypoint.sh"]
RUN ["chmod", "+x", "wait-for-it.sh"]

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["./mvnw", "spring-boot:run"]