plugins {
    id("java")
}

group = "dev.nautchkafe.arangodb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // ArangoDB stuff
    implementation("com.arangodb:arangodb-java-driver:7.3.11")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}