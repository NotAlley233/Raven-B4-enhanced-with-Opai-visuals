plugins {
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

group = "xyz.daikirai"
version = "1.0.0"

java { toolchain.languageVersion.set(JavaLanguageVersion.of(8)) }

loom {
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("clickgui.mixins.json")
    }
    mixin { defaultRefmapName.set("clickgui.refmap.json") }
    runConfigs {
        "client" {
            isIdeConfigGenerated = true
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker", "--mixin", "clickgui.mixins.json")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

val embed: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    embed(files("libs/mixin-0.7.11-SNAPSHOT.jar"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") { expand("version" to project.version) }
}
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.jar {
    from(embed.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF", "LICENSE.txt")
        exclude("META-INF/services/javax.annotation.processing.Processor")
    }
    manifest.attributes(
        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        "TweakOrder" to "0",
        "MixinConfigs" to "clickgui.mixins.json",
        "FMLCorePluginContainsFMLMod" to "true",
        "ForceLoadAsMod" to "true"
    )
}

tasks.named<org.gradle.jvm.tasks.Jar>("remapJar") { archiveFileName.set("RavenB4.jar") }

tasks.register<JavaExec>("opaiVisualCheck") {
    group = "verification"
    description = "Render Opai with a real hidden OpenGL context and check UI interaction and GL state restoration."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("xyz.daikirai.gui.opai.OpaiVisualCheck")
    args(layout.buildDirectory.dir("opai-verification").get().asFile.absolutePath)
}

// Test-only Forge mod on an isolated development client; never included in the release JAR.
if (providers.gradleProperty("opaiClientCheck").isPresent) {
    loom.runConfigs.named("client") {
        runDir("build/opai-client-check")
        programArgs("--width", "1280", "--height", "720")
    }
    // Loom 0.10's RunGameTask has an unannotated legacy `main` getter on Gradle 8.
    // Reuse its resolved launch configuration with a standard, validated JavaExec task.
    tasks.register<JavaExec>("opaiMinecraftCheck") {
        dependsOn("downloadAssets", tasks.testClasses)
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
        val client = tasks.named<JavaExec>("runClient").get()
        mainClass.set(client.javaClass.getMethod("getMain").invoke(client) as String)
        classpath = client.classpath + sourceSets.test.get().output
        args = client.args
        jvmArgs = client.jvmArgs
        val directory = layout.buildDirectory.dir("opai-client-check").get().asFile
        directory.mkdirs()
        workingDir(directory)
        systemProperty("raven.opai.clientCheck", "true")
    }
}
