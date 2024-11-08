plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "dev.timothyw"
version = "1.0.3"

repositories {
    mavenCentral()
}

intellij {
    version.set("243-EAP-SNAPSHOT")
    type.set("IU")
    updateSinceUntilBuild.set(false)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("232")
//        untilBuild.set("242.*")
        version.set(project.version.toString())
        pluginDescription.set(file("src/main/resources/description.html").readText())
        changeNotes.set(file("src/main/resources/changelog.html").readText())
    }

    publishPlugin {
        token.set(project.findProperty("jetBrainsMarketplaceToken") as? String ?: "")
    }
}