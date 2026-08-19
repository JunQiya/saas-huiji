# =============================================================================
# 星河·会记 后端 (Spring Boot 3.2 / Java 17) — 多阶段构建
# 构建: docker build -f deploy/server.Dockerfile -t huiji-server:1.0.0 .
# 基础镜像走 DaoCloud 加速(docker.io 国内直拉超时); 若有内网 ACR 镜像可自行替换
# =============================================================================
FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Maven 走阿里云镜像(国内直连 Maven Central 可能超时)
RUN mkdir -p /root/.m2 && \
    printf '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>\n' > /root/.m2/settings.xml
# 先拷 pom 拉依赖(利用构建缓存)
COPY server/pom.xml .
RUN mvn -q dependency:go-offline -B || true
COPY server/src ./src
RUN mvn -q -DskipTests -B package

# ---------- 运行阶段 ----------
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /build/target/huiji-server-1.0.0.jar app.jar

# ---- 安全加固: 非 root 运行(最小权限, 防容器逃逸提权) ----
RUN useradd -r -u 10001 -m appuser \
    && chown -R appuser:appuser /app
USER appuser

# 内存/CPU 限制配套: 容器 limit 内存 512m, JVM 堆上限 384m(其余留给非堆/元空间),
# MaxRAMPercentage 兜底防止 JVM 按宿主内存自适应导致 OOMKilled
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
