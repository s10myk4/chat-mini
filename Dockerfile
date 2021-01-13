FROM hseeberger/scala-sbt:8u265_1.4.4_2.12.12 AS builder

COPY ./build.sbt /opt/build.sbt
COPY ./project/plugins.sbt /opt/project/plugins.sbt
COPY ./project/BaseSettings.scala /opt/project/BaseSettings.scala
COPY ./read-api-server/src /opt/src
RUN cd /opt;sbt clean assembly
#sbt clean "project read-api-server" assembly


FROM flink:1.11.2-scala_2.12
RUN wget -P /opt/flink/lib/ https://repo.maven.apache.org/maven2/org/apache/flink/flink-sql-connector-kafka_2.12/1.11.2/flink-connector-kafka_2.12-1.11.2.jar; \
    wget -P /opt/flink/lib/ https://repo.maven.apache.org/maven2/org/apache/flink/flink-connector-jdbc_2.12/1.11.2/flink-connector-jdbc_2.12-1.11.2.jar; \
    wget -P /opt/flink/lib/ https://repo.maven.apache.org/maven2/org/apache/flink/flink-csv/1.11.2/flink-csv-1.11.2.jar; \
    wget -P /opt/flink/lib/ https://repo.maven.apache.org/maven2/mysql/mysql-connector-java/8.0.22/mysql-connector-java-8.0.22.jar;

COPY --from=builder /opt/target/scala-2.12/flink-playgrounds-scala.jar /opt/flink/usrlib/flink-playgrounds-scala.jar
