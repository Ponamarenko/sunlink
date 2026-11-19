package com.sunlink.svitgrid.data

/**
 * Единый источник значений по умолчанию для адреса backend.
 *
 * Используется и SettingsRepository (значения по умолчанию), и RetrofitClient
 * (fallback до загрузки настроек), и UI (плейсхолдеры). Раньше адрес был
 * продублирован в трёх независимых местах.
 */
object BackendDefaults {
    const val HOST = "ha4a.pp.ua"
    const val PORT = 8800

    val BASE_URL: String = "http://$HOST:$PORT/"

    /** Адрес изолированного DEMO backend (этап 12) для подсказки в UI. */
    const val DEMO_HOST = "127.0.0.1"
    const val DEMO_PORT = 8801
}
