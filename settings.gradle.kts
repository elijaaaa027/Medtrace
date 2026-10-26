pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app")

// Ensure MedTrace full-stack server is running on port 3000
try {
  val serverFile = file("dev-server.cjs")
  if (serverFile.exists()) {
    val isPortListening = try {
      java.net.Socket("127.0.0.1", 3000).use { true }
    } catch (_: Exception) {
      false
    }
    if (!isPortListening) {
      val logFile = file("/tmp/dev-server.log")
      ProcessBuilder("node", serverFile.absolutePath)
        .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        .redirectError(ProcessBuilder.Redirect.appendTo(logFile))
        .start()
    }
  }
} catch (_: Exception) {}

