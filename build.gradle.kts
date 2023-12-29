plugins {
    application
    java

    //Gradle shadow plugin to make fatjar
    id("com.github.johnrengelman.shadow") version ("7.0.0")
}

group = "humanika.rafeki.james"
version = "2023.12.24"

repositories {
    mavenCentral()
}

application {
    mainClass.set("humanika.rafeki.james.James")
}

sourceSets {
    main {
        java.srcDir("src/main/java")
        java.srcDir("esparser/src/main/java")
    }
    all {
        dependencies {
            implementation("com.google.code.findbugs:jsr305:3.0.2")
            implementation("com.discord4j:discord4j-core:3.3.0-RC1")
            implementation("ch.qos.logback:logback-classic:1.2.3")
	    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
            implementation("com.squareup.okhttp3:okhttp")
            implementation("org.json:json:20231013")
            implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
            implementation("com.github.mpkorstanje:simmetrics-core:4.1.1")
        }
    }
}

/*
Configure the sun.tools.jar.resources.jar task for our main class and so that `./gradlew build` always makes the fatjar
This boilerplate is completely removed when using Springboot
 */
tasks.jar {
    manifest {
        attributes("Main-Class" to "humanika.rafeki.james.James")
    }

    finalizedBy("shadowJar")
}
