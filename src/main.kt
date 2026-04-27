import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.math.geom.*
import logic.*
import scenes.*
import ui.*

suspend fun main() = Korge(
    windowSize = Size(Viewport.W, Viewport.H),
    backgroundColor = Colors["#f5f0e6"], // KIN_LIGHT.paper — реальный фон ставит сцена
    title = "Gomoku · Kintsugi",
) {
    Fonts.loadOnce()
    SettingsStore.load()

    val container = sceneContainer()
    Nav.container = container

    if (SettingsStore.current.firstRun) {
        SettingsStore.update { it.copy(firstRun = false) }
        container.changeTo { HelpScene() }
    } else {
        container.changeTo { MenuScene() }
    }
}
