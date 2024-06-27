FROM gradle:6.1.1-jdk11  as build
RUN mkdir -p /home/gradle/cache_home
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
ENV USERNAME=$GITHUB_USERNAME TOKEN=$GITHUB_TOKEN
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle /home/gradle/sandbox/
WORKDIR /home/gradle/sandbox
RUN gradle clean build -i --stacktrace

FROM gradle:6.1.1-jdk11  as builder
COPY --from=build /home/gradle/cache_home /home/gradle/.gradle
WORKDIR /home/gradle/sandbox
COPY . .
RUN gradle --no-daemon shadowJar

FROM adoptopenjdk:11-jre-hotspot-bionic
RUN apt-get update && apt-get install -y  --no-install-recommends software-properties-common telnet iputils-ping net-tools procps wget curl vim unzip tzdata && rm -rf /var/lib/apt/lists/*
ENV RUN_ENV=${RUN_ENV} CLASS_PATH=${CLASS_PATH} TZ=Asia/Kolkata SANDBOX_HOME=/srv/webapps/bigbasket.com/sandbox NEWRELIC_ENABLED=${NEWRELIC_ENABLED}
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime  && echo ${TZ} > /etc/timezone
ENV HEAP_SIZE=${HEAP_SIZE}
WORKDIR $SANDBOX_HOME
COPY --from=builder /home/gradle/sandbox/build/libs/sandbox-0.0.1-SNAPSHOT-all.jar .
RUN curl -O "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.1.0/newrelic-java.zip" && unzip newrelic-java.zip && rm newrelic-java.zip
COPY newrelic/newrelic.yml newrelic/
RUN mkdir conf && touch conf/secret.properties
COPY conf $SANDBOX_HOME/conf/
ENTRYPOINT ["/bin/bash", "-c"]
EXPOSE 8080
CMD [ "java -Xms${HEAP_SIZE} -Xmx${HEAP_SIZE} -XX:+UseG1GC  -javaagent:${HULK_HOME}/newrelic/newrelic.jar -Dnewrelic.config.agent_enabled=${NEWRELIC_ENABLED}  -Dnewrelic.environment=${RUN_ENV} -cp  sandbox-0.0.1-SNAPSHOT-all.jar ${CLASS_PATH}"]