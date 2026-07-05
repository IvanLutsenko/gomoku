package scenes

import korlibs.image.color.*
import korlibs.image.text.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import logic.*
import model.*
import ui.*

// Журнал партий — кифу как летопись. Незавершённая партия (живёт в
// GameSession) — первой строкой с кнопкой «Продолжить»; завершённые — из
// RecordsStore, тап открывает кифу.
// ponytail: показываем последние 4 записи без скролла; полный список — когда
// понадобится (records хранит до 50).
class JournalScene : Scene() {

    override suspend fun SContainer.sceneMain() {
        val theme = Theme.colors
        val w = Viewport.W.toDouble()
        val h = Viewport.H.toDouble()

        kinPaperBackground(theme)
        onBackOrEscape { Nav.goMenu() }

        kinTextButton(Str.BACK, color = theme.muted) { Nav.goMenu() }.apply {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, 22.0)
        }

        var y = 56.0
        kinText(Str.JOURNAL_TITLE, Type.title, theme.ink) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 44.0
        container {
            kinSeam(
                x1 = 2.0, y1 = 6.0, x2 = 94.0, y2 = 6.0,
                jitter = 2.5, seed = 71, width = 1.3,
                color = theme.gold, colorSoft = theme.goldSoft,
            )
        }.position(24.0, y)
        y += 18.0
        kinText(Str.JOURNAL_SECTION, Type.labelCaps.size, theme.muted, Fonts.uiMedium) {
            alignment = TextAlignment.TOP_LEFT
            position(24.0, y)
        }
        y += 32.0

        val unfinished = GameSession.game.gameState == GameState.PLAYING &&
            GameSession.game.getMoveCount() > 0
        val records = RecordsStore.current.games.take(if (unfinished) 3 else 4)

        if (!unfinished && records.isEmpty()) {
            kinText(Str.JOURNAL_EMPTY, Type.captionItalic, theme.muted) {
                alignment = TextAlignment.TOP_CENTER
                position(w / 2.0, y + 80.0)
            }
        }

        if (unfinished) {
            val g = GameSession.game
            y = renderRow(
                this, y, w, theme,
                moves = g.getMoveHistory(), winLine = null,
                dateLine = modeLine(
                    if (GameSession.mode == GameMode.AI) "ai" else "pvp",
                    SettingsStore.current.aiDifficulty,
                ),
                label = Str.JOURNAL_UNFINISHED_PREFIX + g.getMoveCount(),
                dot = DotKind.UNFINISHED,
                continueBtn = true,
                divider = records.isNotEmpty(),
                onTap = { Nav.goGameKeepState() },
            )
        }

        records.forEachIndexed { i, rec ->
            y = renderRow(
                this, y, w, theme,
                moves = rec.moves, winLine = rec.winLine,
                dateLine = "${dateLabel(rec.endedAt)} · ${modeLine(rec.mode, rec.difficulty)}",
                label = resultLabel(rec),
                dot = dotKind(rec),
                continueBtn = false,
                divider = i < records.size - 1,
                onTap = { Nav.goKifu(rec) { Nav.goJournal() } },
            )
        }

        // Итоговая строка статистики
        val s = RecordsStore.current.stats
        if (s.games > 0) {
            val footerY = h - 40.0
            solidRect(w - 48.0, 1.0, theme.line) { position(24.0, footerY - 16.0) }
            val statsLine = "${s.games} ${Str.gamesWord(s.games)} · ${s.wins} ${Str.winsWord(s.wins)}" +
                " · ${Str.JOURNAL_BEST_STREAK}${s.bestStreak}"
            kinText(capsTracked(statsLine, 0), 11.0, theme.muted, Fonts.uiMedium) {
                alignment = TextAlignment.TOP_CENTER
                position(w / 2.0, footerY)
            }
        }
    }

    private enum class DotKind { WIN, LOSS, UNFINISHED, NEUTRAL }

    private fun dotKind(rec: GameRecord): DotKind = when {
        rec.result == GameState.DRAW -> DotKind.NEUTRAL
        rec.mode == "ai" && rec.humanColor != null -> {
            val humanWon = (rec.result == GameState.BLACK_WINS && rec.humanColor == StoneColor.BLACK) ||
                (rec.result == GameState.WHITE_WINS && rec.humanColor == StoneColor.WHITE)
            if (humanWon) DotKind.WIN else DotKind.LOSS
        }
        else -> DotKind.WIN
    }

    private fun resultLabel(rec: GameRecord): String {
        val base = when (rec.result) {
            GameState.BLACK_WINS -> Str.JOURNAL_WIN_BLACK
            GameState.WHITE_WINS -> Str.JOURNAL_WIN_WHITE
            else -> Str.JOURNAL_DRAW
        }
        val n = rec.moves.size
        return "$base · $n ${Str.movesWord(n)}"
    }

    private fun modeLine(mode: String, difficulty: logic.ai.Difficulty?): String =
        if (mode == "ai") Str.MODE_AI_PREFIX + (difficulty?.label?.lowercase() ?: "")
        else Str.MODE_PVP

    private fun dateLabel(endedAt: Long): String {
        val dt = DateTime.fromUnixMillis(endedAt).local
        val now = DateTime.now().local
        fun sameDay(a: DateTimeTz, b: DateTimeTz) =
            a.year == b.year && a.month1 == b.month1 && a.dayOfMonth == b.dayOfMonth
        val yesterday = (DateTime.now() - 1.days).local
        return when {
            sameDay(dt, now) -> {
                val mm = dt.minutes.toString().padStart(2, '0')
                "${Str.TODAY} · ${dt.hours}:$mm"
            }
            sameDay(dt, yesterday) -> Str.YESTERDAY
            else -> "${dt.dayOfMonth} ${Str.MONTHS_GEN[dt.month1 - 1]}"
        }
    }

    private fun renderRow(
        host: Container, y: Double, w: Double, theme: KinPalette,
        moves: List<Move>, winLine: List<Position>?,
        dateLine: String, label: String, dot: DotKind,
        continueBtn: Boolean, divider: Boolean,
        onTap: () -> Unit,
    ): Double {
        val rowH = 82.0
        val row = host.container { }.position(24.0, y)
        val rowW = w - 48.0

        row.kinMiniBoard(moves, winLine, 56.0, theme).position(0.0, (rowH - 56.0) / 2.0 - 4.0)

        val textX = 56.0 + 14.0
        row.kinText(dateLine, 11.0, theme.muted, Fonts.ui) {
            alignment = TextAlignment.TOP_LEFT
            position(textX, 16.0)
        }
        // точка результата + подпись
        val dotR = 3.5
        val dotY = 40.0
        when (dot) {
            DotKind.WIN -> row.circle(dotR, theme.gold) { position(textX, dotY) }
            DotKind.LOSS -> row.circle(dotR, theme.vermillion) { position(textX, dotY) }
            else -> row.circle(dotR, Colors.TRANSPARENT, stroke = theme.lineFirm, strokeThickness = 1.0) {
                position(textX, dotY)
            }
        }
        row.kinText(label, 13.5, theme.ink, Fonts.uiSemiBold) {
            alignment = TextAlignment.MIDDLE_LEFT
            position(textX + dotR * 2 + 7.0, dotY + dotR)
        }

        if (continueBtn) {
            val btnW = 96.0
            val btnH = 32.0
            val btn = row.container { }.position(rowW - btnW, (rowH - btnH) / 2.0 - 4.0)
            btn.roundRect(Size(btnW, btnH), RectCorners(BtnSpec.RADIUS - 2.0), fill = BtnSpec.primaryBg(theme))
            btn.kinText(Str.JOURNAL_CONTINUE, 12.0, BtnSpec.primaryFg, Fonts.uiMedium) {
                alignment = TextAlignment.MIDDLE_CENTER
                position(btnW / 2.0, btnH / 2.0)
            }
        } else {
            row.kinText("→", 14.0, theme.muted, Fonts.ui) {
                alignment = TextAlignment.MIDDLE_RIGHT
                position(rowW, rowH / 2.0 - 4.0)
            }
        }

        row.solidRect(rowW, rowH - 8.0, Colors.TRANSPARENT).onClick { onTap() }

        if (divider) {
            host.solidRect(rowW, 1.0, theme.line) { position(24.0, y + rowH - 4.0) }
        }
        return y + rowH
    }
}
