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
    const val MENU_JOURNAL = "Журнал"
    const val MENU_SEALS = "Печати"
    // Ссылка на Чайную рендерится двумя шрифтами (茶 есть только в Noto
    // Serif JP), поэтому строка разбита: слово локализуемое, глиф — нет.
    const val MENU_TEA_WORD = "Чайная "
    const val MENU_TEA_GLYPH = "茶 →"
    const val BRAND_VERTICAL = "五目並べ" // татэгаки на правом поле меню
    const val HANKO = "五" // киноварная печать в футере

    // Splash
    const val SPLASH_TITLE = "Гомоку"
    const val SPLASH_LOADING = "ЗАГРУЗКА…"

    // Чайная — поддержка проекта, разовые покупки без подписок
    const val TEA_TITLE = "Чайная"
    const val TEA_SECTION = "ПОДДЕРЖКА ПРОЕКТА"
    const val TEA_INTRO = "Гомоку — проект для души: без подписок и слежки. Если игра дарит тишину — поддержите."
    const val TEA_FOOTER = "Всё — разовые покупки. Подписок нет."
    const val TEA_SOON = "СКОРО"
    const val TEA_STUB = "Покупки появятся в мобильной сборке"

    // Цены — мок для международного рынка; реальные локализованные цены
    // придут из биллинга стора (Google Play Billing / StoreKit).
    val teaItems = listOf(
        TeaItem("茶", TeaTone.VERMILLION, "Чашка чая", "Разовая поддержка автора", "$0.99"),
        TeaItem("壺", TeaTone.VERMILLION, "Чайник", "Большая поддержка — от души", "$4.99"),
        TeaItem("銀", TeaTone.SILVER, "Серебряная жила", "Тема металла для кинцуги", "$0.99"),
        TeaItem("銅", TeaTone.COPPER, "Медная жила", "Тема металла для кинцуги", "$0.99"),
        TeaItem("貝", TeaTone.SOON, "Камни из ракушки", "Хамагури — набор камней", null),
        TeaItem("静", TeaTone.GOLD, "Тишина", "Убрать рекламу навсегда", "$1.99"),
    )

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
    const val GAME_HINT = "Подсказка"
    const val GAME_15X15 = "15 × 15"
    const val GAME_VS = "vs"
    const val CHIP_BLACK = "Чёрные"
    const val CHIP_WHITE = "Белые"
    const val CHIP_YOU = "ВЫ"
    const val CHIP_P1 = "ИГРОК 1"
    const val CHIP_P2 = "ИГРОК 2"
    const val CHIP_AI = "AI"
    const val THINKING_VERTICAL = "思考中" // татэгаки у доски, пока AI думает
    const val CONFIRM_PLACE = "Поставить камень"
    const val CONFIRM_CANCEL = "×"
    const val FORBIDDEN_OVERLINE = "Запрещено: ряд длиннее пяти"
    const val FORBIDDEN_DOUBLE_FOUR = "Запрещено: двойная четвёрка"
    const val FORBIDDEN_DOUBLE_THREE = "Запрещено: двойная тройка"

    // Настройки
    const val BACK = "← Назад"
    const val SETTINGS_TITLE = "Настройки"
    const val SETTINGS_SECTION = "ПАРАМЕТРЫ ПАРТИИ"
    const val SETTINGS_THEME = "Тема"
    const val THEME_LIGHT = "☀  Свет"
    const val THEME_DARK = "☾  Тьма"
    const val SETTINGS_RULES = "Правила"
    const val RULES_CLASSIC = "Классика"
    const val RULES_RENJU = "Рэндзю"
    const val SETTINGS_DIFFICULTY = "Сложность AI"
    const val SETTINGS_PLAYER_COLOR = "Цвет игрока (vs AI)"
    const val COLOR_ALTERNATE = "Чередовать"
    const val COLOR_WHITE = "Белые"
    const val COLOR_BLACK = "Чёрные"
    const val SETTINGS_HELPERS = "Помощники"
    const val SETTINGS_HINTS = "Подсказки ходов"
    const val SETTINGS_CONFIRM = "Подтверждение хода"
    const val SETTINGS_SOUND = "Звуки и вибрация"
    const val SETTINGS_MUSIC = "Музыка"
    const val SETTINGS_VERSION_PREFIX = "ВЕРСИЯ "
    const val SETTINGS_ONLINE = "ОНЛАЙН"

    // Помощь
    const val HELP_TITLE = "Помощь"
    const val HELP_SECTION = "ПРАВИЛА И СОВЕТЫ"

    // Победа / Поражение
    const val VICTORY_META = "ПАРТИЯ ЗАВЕРШЕНА"
    const val VICTORY_AGAIN = "Ещё партию"
    const val VICTORY_TO_MENU = "В меню"
    const val VICTORY_VIEW_BOARD = "Посмотреть доску"
    const val VICTORY_NEW_SEAL = "НОВАЯ ПЕЧАТЬ"
    const val VICTORY_KIFU = "Кифу партии →"
    const val DEFEAT_TITLE = "Поражение"
    const val DEFEAT_SUB = "Трещину заполняет золото"
    const val DEFEAT_REMATCH = "Реванш"

    // Кифу
    const val KIFU_TITLE = "Кифу"
    const val KIFU_UNFINISHED = "Партия не окончена"
    const val KIFU_MOVES_PREFIX = "ходов: "
    const val KIFU_SHARE = "Поделиться"
    const val KIFU_SAVE = "Сохранить"
    const val KIFU_COPIED = "Нотация скопирована в буфер"
    const val KIFU_SAVED_PREFIX = "Сохранено: "
    const val KIFU_EXPORT_FAILED = "Не получилось — попробуйте ещё раз"

    // Журнал
    const val JOURNAL_TITLE = "Журнал"
    const val JOURNAL_SECTION = "КИФУ СЫГРАННЫХ ПАРТИЙ"
    const val JOURNAL_EMPTY = "Партий пока нет — сыграйте первую."
    const val JOURNAL_UNFINISHED_PREFIX = "Не окончена · ход "
    const val JOURNAL_CONTINUE = "Продолжить"
    const val JOURNAL_WIN_BLACK = "Победа чёрных"
    const val JOURNAL_WIN_WHITE = "Победа белых"
    const val JOURNAL_DRAW = "Ничья"
    const val MODE_AI_PREFIX = "vs AI · "
    const val MODE_PVP = "vs Игрок"
    const val TODAY = "Сегодня"
    const val YESTERDAY = "Вчера"
    val MONTHS_GEN = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )

    // Печати
    const val SEALS_TITLE = "Печати"
    const val SEALS_COLLECTED_PREFIX = "СОБРАНО "
    const val SEALS_OF = " ИЗ "
    const val SEALS_LEGEND_LABEL = "ЛЕГЕНДА"
    const val SEALS_LEGEND_LABEL_OUT = "ЛЕГЕНДА · ВНЕ КОЛЛЕКЦИИ"
    const val SEALS_LEGEND_LOCKED_TITLE = "Золотая печать"
    const val SEALS_LEGEND_LOCKED_BODY = "Откроется, когда соберёте все девять печатей."
    const val SEALS_LEGEND_OPEN_TITLE = "和 · Ничья"
    const val SEALS_LEGEND_OPEN_BODY = "225 камней и ни одной пятёрки. Почти никто её не видел."
    const val SEALS_MASTER_LABEL = "МАСТЕРСКАЯ ПЕЧАТЬ"
    const val SEALS_MASTER_TITLE = "極 · Вершина"
    const val SEALS_MASTER_BODY = "Все печати собраны — узор завершён золотом."
    const val SEALS_HINT = "Печати ставятся сами — просто играйте."
    const val SEALS_HINT_COMPLETE = "Узор завершён. Осталась легенда."

    fun sealName(key: String): String = when (key) {
        "初" -> "Первая победа"
        "連" -> "Три подряд"
        "速" -> "До 20 ходов"
        "心" -> "Без отмен"
        "師" -> "Сложный AI"
        "白" -> "Победа белыми"
        "石" -> "1000 камней"
        "月" -> "Партия за полночь"
        "友" -> "10 партий вдвоём"
        "和" -> "Ничья"
        "極" -> "Вершина"
        else -> key
    }

    // «1 ход / 2 хода / 5 ходов»
    fun movesWord(n: Int): String = plural(n, "ход", "хода", "ходов")
    fun gamesWord(n: Int): String = plural(n, "партия", "партии", "партий")
    fun winsWord(n: Int): String = plural(n, "победа", "победы", "побед")
    const val JOURNAL_BEST_STREAK = "лучшая серия "

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val d10 = n % 10
        val d100 = n % 100
        return when {
            d100 in 11..14 -> many
            d10 == 1 -> one
            d10 in 2..4 -> few
            else -> many
        }
    }

    // Сложность AI
    const val DIFFICULTY_EASY = "Легко"
    const val DIFFICULTY_MID = "Средне"
    const val DIFFICULTY_HARD = "Сложно"

    val helpEntries = listOf(
        HelpEntry("一", "Цель игры", "Выстройте пять камней в ряд по горизонтали, вертикали или диагонали. Чёрные ходят первыми."),
        HelpEntry("二", "Управление", "Касание — поставить камень. Отменить последний ход можно из нижней панели."),
        HelpEntry("三", "Стратегия", "Блокируйте открытые тройки соперника и стройте «вилки» — двойные угрозы, которые нельзя закрыть одним ходом."),
        HelpEntry("四", "Центр", "Первый ход — традиционно тэнгэн, центр доски. Это даёт максимум направлений атаки."),
        HelpEntry("五", "Кифу", "После партии откройте кифу — традиционную запись игры. Каждый камень пронумерован в порядке ходов: видно, как строилась атака и где партия была решена."),
        HelpEntry("六", "Рэндзю", "Профессиональные правила (включаются в настройках). Чёрным запрещены «двойная тройка», «двойная четвёрка» и ряд длиннее пяти. Запрещённая точка подсвечивается киноварью, доска отвечает дрожанием. Белые без ограничений."),
    )
}

data class HelpEntry(val num: String, val title: String, val body: String)

// Позиция «Чайной»: тон плитки. SOON — пунктирная заглушка без цены.
enum class TeaTone { VERMILLION, GOLD, SILVER, COPPER, SOON }
data class TeaItem(val kanji: String, val tone: TeaTone, val name: String, val sub: String, val price: String?)

// UI-подпись уровня сложности. Живёт здесь, а не в enum-е, чтобы
// logic-слой не тянул тексты интерфейса.
val Difficulty.label: String
    get() = when (this) {
        Difficulty.EASY -> Str.DIFFICULTY_EASY
        Difficulty.MID -> Str.DIFFICULTY_MID
        Difficulty.HARD -> Str.DIFFICULTY_HARD
    }
