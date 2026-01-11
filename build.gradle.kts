plugins {
    id("java")
}

val springVersion = "7.0.2"
val jettyVersion = "12.1.5"
val jakartaServletVersion = "6.1.0"
val jacksonVersion = "2.17.2"
val slf4jVersion = "2.0.17"
var mapstructVersion = "1.6.3"


group = "ru.valera"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mapstruct:mapstruct-processor:${mapstructVersion}")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.springframework:spring-jdbc:${springVersion}")
    implementation("org.springframework:spring-context:${springVersion}")
    implementation("org.springframework:spring-webmvc:${springVersion}")

    implementation("org.eclipse.jetty:jetty-server:${jettyVersion}")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:${jettyVersion}")
    implementation("org.eclipse.jetty.ee10:jetty-ee10-webapp:${jettyVersion}")

    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

    compileOnly("jakarta.servlet:jakarta.servlet-api:${jakartaServletVersion}")

    implementation("org.slf4j:slf4j-simple:${slf4jVersion}")

    implementation("org.postgresql:postgresql:42.7.8")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.10.0")
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