FROM amazoncorretto:17
ARG JAR_PATH=./build/libs
COPY ${JAR_PATH}/*.jar .
ENTRYPOINT ["java","-jar","CookieHouse-0.0.1-SNAPSHOT.jar"]