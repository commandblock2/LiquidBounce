package net.ccbluex.liquidbounce.script

import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.impl.client.gametest.FabricClientGameTestRunner
import net.fabricmc.loader.api.ModContainer
import net.fabricmc.loader.api.entrypoint.EntrypointContainer
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.game.GameProvider
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.fabricmc.loader.impl.launch.knot.Knot
import net.minecraft.MinecraftVersion
import net.minecraft.client.MinecraftClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class ScriptTestsLoader {
}


class ClientTest {
    @Test
    fun testInGameWorld() {
        // Create a custom test runner
        val runner = TestWorldRunner()
        runner.runInTestWorld { context ->
            // Your test code using the context


            // Run assertions
            context.runOnClient<Throwable> { client ->
                assertNotNull(client.world)
                // More assertions...
            }
        }
    }
}

class TestWorldRunner {

    private fun determineAssetIndex(mcVersion: String, assetsDir: String): String {
        // Check the assets/indexes directory for available index files
        val assetsIndexDir = File(assetsDir, "indexes")
        if (assetsIndexDir.exists() && assetsIndexDir.isDirectory) {
            // Get all JSON files in the indexes directory
            val indexFiles = assetsIndexDir.listFiles { file -> file.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()

            if (indexFiles.isEmpty()) {
                println("Warning: No asset index files found in $assetsIndexDir")
                return mcVersion // Fallback to using the version itself
            }

            // Try exact version match first
            if (indexFiles.contains(mcVersion)) {
                return mcVersion
            }

            // Try major.minor version (e.g., "1.19" for "1.19.2")
            val majorMinorVersion = mcVersion.split(".").take(2).joinToString(".")
            if (indexFiles.contains(majorMinorVersion)) {
                return majorMinorVersion
            }

            // Some versions use just a number as the asset index
            // Try to find a numeric index that might correspond to our version
            val numericIndices = indexFiles.filter { it.all { char -> char.isDigit() } }

            // If we're on a recent version and there are numeric indices, use the highest one
            if (numericIndices.isNotEmpty()) {
                val highestIndex = numericIndices.maxByOrNull { it.toIntOrNull() ?: 0 }
                if (highestIndex != null) {
                    println("Using asset index $highestIndex for Minecraft $mcVersion")
                    return highestIndex
                }
            }

            // If we have any index files, use the most recent one based on alphabetical sorting
            // (which works for version-named indices like "1.19", "1.20", etc.)
            val mostRecentIndex = indexFiles.maxOrNull()
            if (mostRecentIndex != null) {
                println("Using most recent asset index $mostRecentIndex for Minecraft $mcVersion")
                return mostRecentIndex
            }
        }

        println("Warning: Could not determine asset index for version $mcVersion, using version as index")
        return mcVersion // Fallback to using the version itself
    }


    fun runInTestWorld(testLogic: (ClientGameTestContext) -> Unit) {

        // Set fabric development environment
//        System.setProperty("fabric.development", "true")
//        System.setProperty("lithium.disable", "true")


        val mcVersion = MinecraftVersion.CURRENT.name

        // Get the project root directory
        // this maybe improper and should be improved
        val projectDir = File(System.getProperty("user.dir"))
        val gameDir = File(projectDir, "run").absolutePath
        val gradleHome = System.getenv("GRADLE_HOME") ?: System.getProperty("GRADLE_HOME")
        val assetsDir = File(
            gradleHome ?: (System.getProperty("user.home") + "/.gradle"),
            "caches/fabric-loom/assets"
        ).absolutePath


        Thread {
            
            val launcher = FabricLauncherBase::class.java.getDeclaredField("launcher")
            launcher.isAccessible = true
            val knotLauncher = launcher.get(null) as Knot

            knotLauncher.init(
                arrayOf(
//                    "--username", "UnitTestPlayer",
//                    "--version", mcVersion,
//                    "--gameDir", gameDir,
                    "--assetsDir", assetsDir,
                    "--assetIndex", determineAssetIndex(mcVersion, assetsDir),
//                    "--versionType", "release",
//                    "--accessToken", "UnitTestAccessToken",
//                    "--uuid", Uuids.getOfflinePlayerUuid("UnitTestPlayer").toString()
                )
            )
        }.start()


        // Wait for client to initialize
        while (MinecraftClient.getInstance() == null) {
            Thread.sleep(1)
        }

        val testComplete = CompletableFuture<Boolean>()

        val test = { context: ClientGameTestContext ->
            try {
                testLogic(context)
                testComplete.complete(true)
            } catch (e: Exception) {
                testComplete.completeExceptionally(e)
            }
        }

        FabricClientGameTestRunner.currentlyRunningGameTest =
            object : EntrypointContainer<net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest> {
                override fun getEntrypoint(): net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest {
                    return net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest { context ->
                        test(context)
                        Unit
                    }
                }

                override fun getProvider(): ModContainer? {
                    return FabricLoaderImpl.INSTANCE.allMods.find {
                        it.metadata.id == "liquidbounce"
                    }
                }
            }

        try {
            FabricClientGameTestRunner.start()

            // Wait for test completion with timeout
            testComplete.get(600, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AssertionError("Test failed or timed out", e)
        } finally {
            // Cleanup
            MinecraftClient.getInstance()?.stop()
        }
    }
}
