plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.buildtools.native") version "0.11.3"
}

group = "io.samjingwen"
version = "0.0.1-SNAPSHOT"
description = "Eigen Wise Telegram Bot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	implementation("org.telegram:telegrambots-longpolling:9.2.1")
	implementation("org.telegram:telegrambots-client:9.2.1")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<Exec>("generateImages") {
	description = "Generate images from LaTeX files"
	group = "build"

	commandLine("bash", "${projectDir}/util/generate_images.sh")
	workingDir(projectDir)
}

tasks.named("processResources") {
	dependsOn("generateImages")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
