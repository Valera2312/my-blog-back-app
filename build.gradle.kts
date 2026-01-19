plugins {
    id("java")
    id("war")
}

val springVersion = "7.0.2"
val jakartaServletVersion = "6.1.0"
val jacksonVersion = "2.17.2"
var mapstructVersion = "1.6.3"


group = "ru.valera"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.springframework:spring-jdbc:${springVersion}")
    implementation("org.springframework:spring-context:${springVersion}")
    implementation("org.springframework:spring-webmvc:${springVersion}")
    testImplementation("org.springframework:spring-test:${springVersion}")

    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

    compileOnly("jakarta.servlet:jakarta.servlet-api:${jakartaServletVersion}")

    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("org.postgresql:postgresql:42.7.8")
    testRuntimeOnly("com.h2database:h2:2.4.240")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testRuntimeOnly("jakarta.servlet:jakarta.servlet-api:${jakartaServletVersion}")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}