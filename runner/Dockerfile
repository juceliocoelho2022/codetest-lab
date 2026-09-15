FROM maven:3.9.11-eclipse-temurin-21

WORKDIR /opt/cache
COPY pom.xml ./pom.xml
RUN mkdir -p src/test/java \
    && printf '%s\n' \
       'import org.junit.jupiter.api.Test;' \
       'import static org.junit.jupiter.api.Assertions.assertTrue;' \
       'class CacheWarmupTest { @Test void warmsSurefireProvider() { assertTrue(true); } }' \
       > src/test/java/CacheWarmupTest.java \
    && mvn -q test \
    && rm -rf src target

WORKDIR /workspace
ENTRYPOINT ["mvn", "-o", "-q", "test", "-Dstyle.color=never"]
