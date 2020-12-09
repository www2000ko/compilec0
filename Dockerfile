# gradle 好大
FROM gradle:jdk14
COPY ./src/* /app/
# 编译程序
WORKDIR /app/
RUN javac *.java