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
    if (SettingsStore.current.firstRunAt == 0L) {
        SettingsStore.update { it.copy(firstRunAt = korlibs.time.DateTime.now().unixMillisLong) }
    }
    RecordsStore.load()
    MusicPlayer.init(views.coroutineContext)

    // Live source of native width — surface size becomes known only after
    // first frame on Android, поэтому читаем лениво при каждом kinText().
    Display.nativeWidthProvider = { views.nativeWidth }

    val container = sceneContainer()
    Nav.container = container

    container.changeTo { SplashScene() }
}
