package com.luffarschack

import android.content.Context

object PreferencesHelper {
    private const val PREFS_NAME = "lifetime_stats"

    fun saveLifetimeStats(context: Context, stats: GameStats) {
        val sharedPreferences = context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("lifetime_player_wins", stats.playerWins)
        editor.putInt("lifetime_computer_wins", stats.computerWins)
        editor.putInt("lifetime_ties", stats.ties)
        editor.apply()
    }

    fun getLifetimeStats(context: Context): Triple<Int, Int, Int> {
        val sharedPreferences = context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE)
        val playerWins = sharedPreferences.getInt("lifetime_player_wins", 0)
        val computerWins = sharedPreferences.getInt("lifetime_computer_wins", 0)
        val ties = sharedPreferences.getInt("lifetime_ties", 0)
        return Triple(playerWins, computerWins, ties)
    }
}