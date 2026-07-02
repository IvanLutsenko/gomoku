package ui

import logic.ai.Difficulty

// Все пользовательские строки в одном месте — задел под локализацию.
// ponytail: плоский object вместо i18n-фреймворка; при добавлении второго
// языка заменить на выбор таблицы строк по настройке locale.
object Str {
    // Бренд
    const val BRAND_IDEOGRAM = "五目"
    const val BRAND_TRACKED = "G  O  M  O  K  U"
    const val BRAND_TAGLINE = "Пять камней в ряд"
    const val FOOTER_EDITION = "2026 · KORGE EDITION"

    // Меню
    const val MENU_PLAY_AI = "Игра с AI"
    const val MENU_PLAY_PVP = "Игра вдвоём"
    const val MENU_SETTINGS = "Настройки"
    const val MENU_HELP = "Помощь"

    // Игра
    const val GAME_VS_AI_PREFIX = "VS  AI · "
    const val GAME_VS_HUMAN = "VS  ИГРОК"
    const val GAME_MENU = "МЕНЮ"
    const val GAME_MOVE_PREFIX = "ХОД "
    const val GAME_AI_THINKING = "AI думает…"
    const val TURN_BLACK = "Ход чёрных"
    const val TURN_WHITE = "Ход белых"
    const val BLACK_WINS = "Чёрные победили"
    const val WHITE_WINS = "Белые победили"
    const val DRAW = "Ничья"
    const val GAME_UNDO = "← Отменить"
    const val GAME_NEW = "Новая партия"

    // Настройки
    const val BACK = "← Назад"
    const val SETTINGS_TITLE = "Настройки"
    const val SETTINGS_SECTION = "ПАРАМЕТРЫ ПАРТИИ"
    const val SETTINGS_THEME = "Тема"
    const val THEME_LIGHT = "☀  Свет"
    const val THEME_DARK = "☾  Тьма"
    const val SETTINGS_DIFFICULTY = "Сложность AI"
    const val SETTINGS_HELPERS = "Помощники"
    const val SETTINGS_HINTS = "Подсказки ходов"
    const val SETTINGS_SOUND = "Звуки и вибрация"
    const val SETTINGS_VERSION_PREFIX = "ВЕРСИЯ "
    const val SETTINGS_ONLINE = "ОНЛАЙН"

    // Помощь
    const val HELP_TITLE = "Помощь"
    const val HELP_SECTION = "ПРАВИЛА И СОВЕТЫ"

    // Победа
    const val VICTORY_META = "ПАРТИЯ ЗАВЕРШЕНА"
    const val VICTORY_AGAIN = "Ещё партию"
    const val VICTORY_TO_MENU = "В меню"

    // Сложность AI
    const val DIFFICULTY_EASY = "Легко"
    const val DIFFICULTY_MID = "Средне"
    const val DIFFICULTY_HARD = "Сложно"

    val helpEntries = listOf(
        HelpEntry("一", "Цель игры", "Выстройте пять камней в ряд по горизонтали, вертикали или диагонали. Чёрные ходят первыми."),
        HelpEntry("二", "Управление", "Касание — поставить камень. Отменить последний ход можно из нижней панели."),
        HelpEntry("三", "Стратегия", "Блокируйте открытые тройки соперника и стройте «вилки» — двойные угрозы, которые нельзя закрыть одним ходом."),
        HelpEntry("四", "Центр", "Первый ход — традиционно тэнгэн, центр доски. Это даёт максимум направлений атаки."),
        HelpEntry("五", "Кинцуги", "Победная линия выкладывается золотом — как трещины в керамике, которые становятся украшением."),
    )
}

data class HelpEntry(val num: String, val title: String, val body: String)

// UI-подпись уровня сложности. Живёт здесь, а не в enum-е, чтобы
// logic-слой не тянул тексты интерфейса.
val Difficulty.label: String
    get() = when (this) {
        Difficulty.EASY -> Str.DIFFICULTY_EASY
        Difficulty.MID -> Str.DIFFICULTY_MID
        Difficulty.HARD -> Str.DIFFICULTY_HARD
    }
