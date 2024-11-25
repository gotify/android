import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.File
import java.net.URI

plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.hidetake.swagger.generator") version "2.19.2"
}

fun download(url: String, filename: String) {
    URI(url).toURL().openConnection().let { conn ->
        File(filename).outputStream().use { out ->
            conn.inputStream.use { inp ->
                inp.copyTo(out)
            }
        }
    }
}

tasks.register("downloadSpec") {
    val gotifyVersion = "master"
    val url = "https://raw.githubusercontent.com/gotify/server/$gotifyVersion/docs/spec.json"
    val buildDir = project.layout.buildDirectory.get()
    val specLocation = buildDir.file("gotify.spec.json").asFile.absolutePath
    doFirst {
        buildDir.asFile.mkdirs()
        download(url, specLocation)
    }
}

swaggerSources {
    create("swagger") {
        setInputFile(file("$projectDir/build/gotify.spec.json"))
        code.apply {
            language = "java"
            configFile = file("$projectDir/swagger.config.json")
            outputDir = file("$projectDir/client")
        }
    }
}

dependencies {
    "swaggerCodegen"("io.swagger.codegen.v3:swagger-codegen-cli:3.0.63")
}

tasks.named("generateSwaggerCode").dependsOn("downloadSpec")
