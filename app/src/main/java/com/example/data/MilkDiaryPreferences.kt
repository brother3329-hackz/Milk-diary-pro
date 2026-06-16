package com.example.data

import android.content.Context
import android.content.SharedPreferences

class MilkDiaryPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("milk_diary_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEFAULT_RATE = "default_rate"
        private const val KEY_DAILY_FEED_COST = "daily_feed_cost"
        private const val KEY_OTHER_DAILY_COST = "other_daily_cost"
    }

    var defaultRate: Float
        get() = prefs.getFloat(KEY_DEFAULT_RATE, 45.0f) // default 45 Rs/L
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_RATE, value).apply()

    var dailyFeedCost: Float
        get() = prefs.getFloat(KEY_DAILY_FEED_COST, 150.0f) // default 150 Rs
        set(value) = prefs.edit().putFloat(KEY_DAILY_FEED_COST, value).apply()

    var otherDailyCost: Float
        get() = prefs.getFloat(KEY_OTHER_DAILY_COST, 50.0f) // default 50 Rs
        set(value) = prefs.edit().putFloat(KEY_OTHER_DAILY_COST, value).apply()
}
