# compile source
FROM amazoncorretto:21-alpine as build

WORKDIR /workspace/app

# prepare for build
COPY pom.xml pom.xml
COPY mvnw mvnw
COPY .mvn/ .mvn/

# build dependencies in separate layer (for caching purposes)
RUN ./mvnw dependency:go-offline -B

# copy source (this layer is rebuild everytime there are changes to code)
COPY src/ src/

# actually compile
COPY .git .git
RUN ./mvnw clean package -DskipTests

# now build deployment image
FROM amazoncorretto:21

VOLUME /tmp
ARG TARGET=/workspace/app/target
COPY --from=build ${TARGET}/indberetning-0.0.1-SNAPSHOT.jar .

COPY /deploy .
RUN chmod +x run.sh
EXPOSE 9090

ENTRYPOINT ["/bin/bash", "run.sh"]
