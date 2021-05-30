dependencies {
    implementation(project(":petblocks-api"))
    implementation(project(":petblocks-core"))
    implementation(project(":petblocks-bukkit-api"))

    compileOnly("org.spigotmc:spigot116R3:1.16.4-R3.0")
    compileOnly("com.google.inject:guice:5.0.1")

    testCompile("org.spigotmc:spigot116R3:1.16.4-R3.0")
}
