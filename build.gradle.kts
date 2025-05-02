plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "dev.timothyw"
version = "1.0.4"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.1")
    }

    implementation("org.jetbrains:marketplace-zip-signer:0.1.8")
}

intellijPlatform {
    publishing {
        token = providers.gradleProperty("jetBrainsMarketplaceToken")
    }

    /*
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    */
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    prepareSandbox {
        pluginName.set(project.name)
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("232")
//        untilBuild.set("242.*")
        version = project.version.toString()
        pluginDescription.set(file("src/main/resources/description.html").readText())
        changeNotes.set(file("src/main/resources/changelog.html").readText())
    }
}