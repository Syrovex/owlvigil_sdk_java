import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "com.owlvigil"
version = providers.gradleProperty("version").getOrElse("0.2.0")

repositories {
    mavenCentral()
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { into("META-INF") }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("owlvigil.project.version", project.version.toString())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("OwlVigil Java SDK")
                description.set("Java SDK for OwlVigil Gateway and Management APIs")
                url.set("https://github.com/Syrovex/owlvigil_sdk_java")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("Syrovex")
                        name.set("OwlVigil")
                    }
                }
                scm {
                    url.set("https://github.com/Syrovex/owlvigil_sdk_java")
                    connection.set("scm:git:https://github.com/Syrovex/owlvigil_sdk_java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Syrovex/owlvigil_sdk_java.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "MavenCentral"
            url = uri(providers.gradleProperty("mavenCentralUrl").getOrElse(
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"))
            credentials {
                username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
            }
        }
    }
}

val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY")
val signingPassword = providers.environmentVariable("GPG_PRIVATE_KEY_PASSWORD")
if (signingKey.isPresent) {
    signing {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications["mavenJava"])
    }
}
