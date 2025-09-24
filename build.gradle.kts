import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "1.9.24"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

kotlin {
    // Compilador Kotlin usando JDK 21
    jvmToolchain(21)
}

java {
    // Tarefas Java usando JDK 21 (evita cair no JDK 24 da sua máquina)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
application {
    mainClass.set("br.com.casainteligente.mini.app.MainKt")
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dtelegram.bot.token=mudar",
        "-Dtelegram.chat.id=mudar"
    )
}


tasks.withType<KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.withType<JavaCompile>().configureEach {
    // Alinha o bytecode Java ao 21 (combina com o Kotlin)
    options.release.set(21)
}
