plugins {
	java
	`maven-publish`
}

group = "darvil"
version = "INDEV"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()

	maven {
		name = "github-mirror"
		url = uri("https://maven.pkg.github.com/fadeoffical/mirror")
		credentials {
			username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
			password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
		}
	}
}

dependencies {
	implementation("org.jetbrains:annotations:23.0.0")
	implementation("fade:mirror:0.0.4-beta.0+develop")
//	implementation("fade:mirror") {
//		version {
//			branch = "develop"
//		}
//	}
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

java {
	withJavadocJar()
	withSourcesJar()
}


publishing {
	repositories {
		maven {
			name = "github"
			url = uri("https://maven.pkg.github.com/darvil82/Lanat")
			credentials {
				username = System.getenv("CI_GITHUB_USERNAME")
				password = System.getenv("CI_GITHUB_PASSWORD")
			}
		}
	}

	publications {
		register<MavenPublication>("gpr") {
			from(components["java"])
			artifactId = rootProject.name
		}
	}
}

tasks.named<Test>("test") {
	useJUnitPlatform()
	filter {
		includeTestsMatching("Test*")
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
	options.encoding = "UTF-8"
}