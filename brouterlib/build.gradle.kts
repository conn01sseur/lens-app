plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    val main by getting {
        java.setSrcDirs(listOf(
            "../third_party/brouter/brouter-core/src/main/java",
            "../third_party/brouter/brouter-mapaccess/src/main/java",
            "../third_party/brouter/brouter-util/src/main/java",
            "../third_party/brouter/brouter-expressions/src/main/java",
            "../third_party/brouter/brouter-codec/src/main/java"
        ))
        resources.setSrcDirs(emptyList<String>())
    }
    val test by getting {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
