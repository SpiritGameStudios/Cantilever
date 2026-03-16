plugins {
	java
	alias(libs.plugins.fabric.loom)
	alias(libs.plugins.minotaur)
	alias(libs.plugins.shadow)
}

class ModInfo {
	val id = property("mod.id").toString()
	val group = property("mod.group").toString()
	val version = property("mod.version").toString()
}

val mod = ModInfo()

version = "${mod.version}+${libs.versions.minecraft.get()}"
group = mod.group

base.archivesName = mod.id

loom {
	serverOnlyMinecraftJar()

	mods.create(mod.id) {
		sourceSet(sourceSets.getByName("main"))
	}
}

repositories {
	mavenCentral()
	maven("https://maven.spiritstudios.dev/releases/")
	exclusiveContent {
		forRepository {
			maven {
				name = "Modrinth"
				url = uri("https://api.modrinth.com/maven")
			}
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
	maven("https://maven.nucleoid.xyz") {
		name = "Nucleoid"
	}
	maven("https://repo.sleeping.town/")
}

dependencies {
	minecraft(libs.minecraft)
	mappings(variantOf(libs.yarn) { classifier("v2") })
	modImplementation(libs.fabric.loader)

	modImplementation(libs.fabric.api)

	include(libs.kaleido)
	implementation(libs.kaleido)

	implementation(libs.jda) { exclude(module = "opus-java") }
	shadow(libs.jda) { exclude(module = "opus-java") }

	implementation(libs.discordwebhooks)
	shadow(libs.discordwebhooks)

	modImplementation(libs.styled.chat)
	modImplementation(libs.pb4.api.predicate)
	modImplementation(libs.pb4.api.placeholder)
	modImplementation(libs.pb4.api.playerdata)
	modImplementation(libs.fabric.permissions)
}

tasks.processResources {
	val map = mapOf(
		"mod_id" to mod.id,
		"mod_version" to mod.version,
		"fabric_loader_version" to libs.versions.fabric.loader.get(),
		"minecraft_version" to libs.versions.minecraft.get()
	)

	inputs.properties(map)
	filesMatching("fabric.mod.json") { expand(map) }
}

tasks.shadowJar {
	tasks.shadowJar.get().configurations.set(arrayListOf(project.configurations.shadow.get()))
	relocate("net.dv8tion.jda", "dev.spiritstudios.cantilever.jda.shade")
	relocate("com.eduardomcb.discord.webhook", "dev.spiritstudios.cantilever.discord.webhook.shade")
	minimize()
}

tasks.remapJar {
	dependsOn(tasks.shadowJar)
	mustRunAfter(tasks.shadowJar)

	inputFile.set(tasks.shadowJar.get().archiveFile)
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.release = 21
}

tasks.jar {
	from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
}

modrinth {
	token.set(System.getenv("MODRINTH_TOKEN"))
	projectId.set(mod.id)
	versionNumber.set(mod.version)
	uploadFile.set(tasks.remapJar)
	gameVersions.addAll(libs.versions.minecraft.get(), "1.21.4")
	loaders.addAll("fabric", "quilt")
	syncBodyFrom.set(rootProject.file("README.md").readText())
	dependencies {
		required.version("fabric-api", libs.versions.fabric.api.get())
	}
}
