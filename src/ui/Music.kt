package ui

import korlibs.audio.sound.*
import korlibs.io.file.std.*
import logic.*
import kotlin.coroutines.*

// Фоновый эмбиент. Живёт поверх сцен: старт в main.kt, тоггл в настройках
// дёргает sync(). Громкость намеренно низкая — музыка тут фон, не событие.
object MusicPlayer {
    private var sound: Sound? = null
    private var channel: SoundChannel? = null
    private var ctx: CoroutineContext? = null

    suspend fun init(context: CoroutineContext) {
        ctx = context
        sound = runCatching { resourcesVfs["sounds/ambient.wav"].readMusic() }.getOrNull()
        sync()
    }

    fun sync() {
        val want = SettingsStore.current.music
        if (want && channel == null) {
            val s = sound ?: return
            val c = ctx ?: return
            channel = s.play(c, infinitePlaybackTimes).also { it.volume = 0.25 }
        } else if (!want) {
            channel?.stop()
            channel = null
        }
    }
}
