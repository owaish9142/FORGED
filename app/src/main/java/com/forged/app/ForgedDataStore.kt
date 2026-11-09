package com.forged.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.forgedDataStore by preferencesDataStore(
    name = "forged_data"
)

object ForgedDataStore {

    // =========================
    // BASIC PROGRESS
    // =========================

    private val XP =
        intPreferencesKey("xp")

    private val COMPLETED_TODAY =
        intPreferencesKey("completed_today")

    private val CHALLENGES_ISSUED_TODAY =
        intPreferencesKey("challenges_issued_today")


    // =========================
    // CURRENT WORKOUT
    // =========================

    private val CURRENT_AMOUNT =
        intPreferencesKey("current_amount")

    private val CURRENT_EXERCISE =
        stringPreferencesKey("current_exercise")

    private val CURRENT_UNIT =
        stringPreferencesKey("current_unit")


    // =========================
    // PENDING WORKOUTS
    // =========================

    private val PENDING_WORKOUTS =
        stringPreferencesKey("pending_workouts")


    // =========================
    // DAILY DECK
    // =========================

    private val DAILY_DATE =
        stringPreferencesKey("daily_date")

    private val DAILY_WORKOUTS =
        stringPreferencesKey("daily_workouts")


    // =========================
    // SETTINGS
    // =========================

    private val CHALLENGES_PER_DAY =
        intPreferencesKey("challenges_per_day")

    private val SAVAGE_MODE =
        booleanPreferencesKey("savage_mode")
    private val SELECTED_EXERCISES =
        stringPreferencesKey("selected_exercises")


    // =========================
    // ACTIVE HOURS
    // =========================

    private val ACTIVE_START_HOUR =
        intPreferencesKey("active_start_hour")

    private val ACTIVE_START_MINUTE =
        intPreferencesKey("active_start_minute")

    private val ACTIVE_END_HOUR =
        intPreferencesKey("active_end_hour")

    private val ACTIVE_END_MINUTE =
        intPreferencesKey("active_end_minute")


    // =========================
    // DATA CLASSES
    // =========================

    data class StoredWorkout(
        val exercise: String,
        val amount: Int,
        val unit: String
    )

    data class State(

        // Progress

        val xp: Int = 0,

        val completedToday: Int = 0,

        val challengesIssuedToday: Int = 0,


        // Current workout

        val currentAmount: Int = 0,

        val currentExercise: String? = null,

        val currentUnit: String = "REPS",


        // Pending workouts

        val pendingWorkouts: List<StoredWorkout> =
            emptyList(),


        // Daily deck

        val dailyDate: String? = null,

        val dailyWorkouts: List<StoredWorkout> =
            emptyList(),


        // Settings

        val challengesPerDay: Int = 5,

        val savageMode: Boolean = false,

        val selectedExercises: Set<String> = emptySet(),


        // Active hours

        val activeStartHour: Int = 8,

        val activeStartMinute: Int = 0,

        val activeEndHour: Int = 22,

        val activeEndMinute: Int = 0
    )


    // =========================
    // READ STATE
    // =========================

    fun state(
        context: Context
    ): Flow<State> {

        return context.forgedDataStore.data.map { preferences ->

            State(

                // Progress

                xp =
                    preferences[XP] ?: 0,

                completedToday =
                    preferences[COMPLETED_TODAY] ?: 0,

                challengesIssuedToday =
                    preferences[
                        CHALLENGES_ISSUED_TODAY
                    ] ?: 0,


                // Current workout

                currentAmount =
                    preferences[CURRENT_AMOUNT] ?: 0,

                currentExercise =
                    preferences[CURRENT_EXERCISE],

                currentUnit =
                    preferences[CURRENT_UNIT]
                        ?: "REPS",


                // Pending workouts

                pendingWorkouts =
                    decodeWorkouts(
                        preferences[PENDING_WORKOUTS]
                    ),


                // Daily deck

                dailyDate =
                    preferences[DAILY_DATE],

                dailyWorkouts =
                    decodeWorkouts(
                        preferences[DAILY_WORKOUTS]
                    ),


                // Settings

                challengesPerDay =
                    preferences[
                        CHALLENGES_PER_DAY
                    ] ?: 5,

                savageMode =
                    preferences[SAVAGE_MODE]
                        ?: false,
                selectedExercises =
                    preferences[SELECTED_EXERCISES]
                        ?.split("|")
                        ?.filter { it.isNotBlank() }
                        ?.toSet()
                        ?: emptySet(),


                // Active hours

                activeStartHour =
                    preferences[
                        ACTIVE_START_HOUR
                    ] ?: 8,

                activeStartMinute =
                    preferences[
                        ACTIVE_START_MINUTE
                    ] ?: 0,

                activeEndHour =
                    preferences[
                        ACTIVE_END_HOUR
                    ] ?: 22,

                activeEndMinute =
                    preferences[
                        ACTIVE_END_MINUTE
                    ] ?: 0
            )
        }
    }


    // =========================
    // SAVE STATE
    // =========================

    suspend fun saveState(
        context: Context,
        state: State
    ) {

        context.forgedDataStore.edit { preferences ->

            // =========================
            // PROGRESS
            // =========================

            preferences[XP] =
                state.xp

            preferences[COMPLETED_TODAY] =
                state.completedToday

            preferences[CHALLENGES_ISSUED_TODAY] =
                state.challengesIssuedToday


            // =========================
            // SETTINGS
            // =========================

            preferences[CHALLENGES_PER_DAY] =
                state.challengesPerDay

            preferences[SAVAGE_MODE] =
                state.savageMode
            if (state.selectedExercises.isNotEmpty()) {
                preferences[SELECTED_EXERCISES] =
                    state.selectedExercises.joinToString("|")
            } else {
                preferences.remove(SELECTED_EXERCISES)
            }


            // =========================
            // ACTIVE HOURS
            // =========================

            preferences[ACTIVE_START_HOUR] =
                state.activeStartHour

            preferences[ACTIVE_START_MINUTE] =
                state.activeStartMinute

            preferences[ACTIVE_END_HOUR] =
                state.activeEndHour

            preferences[ACTIVE_END_MINUTE] =
                state.activeEndMinute


            // =========================
            // CURRENT WORKOUT
            // =========================

            if (
                state.currentExercise != null &&
                state.currentAmount > 0
            ) {

                preferences[CURRENT_EXERCISE] =
                    state.currentExercise

                preferences[CURRENT_AMOUNT] =
                    state.currentAmount

                preferences[CURRENT_UNIT] =
                    state.currentUnit

            } else {

                preferences.remove(
                    CURRENT_EXERCISE
                )

                preferences.remove(
                    CURRENT_AMOUNT
                )

                preferences.remove(
                    CURRENT_UNIT
                )
            }


            // =========================
            // PENDING WORKOUTS
            // =========================

            if (
                state.pendingWorkouts.isNotEmpty()
            ) {

                preferences[PENDING_WORKOUTS] =
                    encodeWorkouts(
                        state.pendingWorkouts
                    )

            } else {

                preferences.remove(
                    PENDING_WORKOUTS
                )
            }


            // =========================
            // DAILY DATE
            // =========================

            if (
                state.dailyDate != null
            ) {

                preferences[DAILY_DATE] =
                    state.dailyDate

            } else {

                preferences.remove(
                    DAILY_DATE
                )
            }


            // =========================
            // DAILY WORKOUT DECK
            // =========================

            if (
                state.dailyWorkouts.isNotEmpty()
            ) {

                preferences[DAILY_WORKOUTS] =
                    encodeWorkouts(
                        state.dailyWorkouts
                    )

            } else {

                preferences.remove(
                    DAILY_WORKOUTS
                )
            }
        }
    }


    // =========================
    // JSON ENCODING
    // =========================

    private fun encodeWorkouts(
        workouts: List<StoredWorkout>
    ): String {

        val array =
            JSONArray()

        workouts.forEach { workout ->

            val objectJson =
                JSONObject()

            objectJson.put(
                "exercise",
                workout.exercise
            )

            objectJson.put(
                "amount",
                workout.amount
            )

            objectJson.put(
                "unit",
                workout.unit
            )

            array.put(
                objectJson
            )
        }

        return array.toString()
    }


    // =========================
    // JSON DECODING
    // =========================

    private fun decodeWorkouts(
        json: String?
    ): List<StoredWorkout> {

        if (
            json.isNullOrBlank()
        ) {
            return emptyList()
        }

        return try {

            val array =
                JSONArray(json)

            val workouts =
                mutableListOf<StoredWorkout>()

            for (
            index in 0 until array.length()
            ) {

                val objectJson =
                    array.getJSONObject(index)

                workouts.add(

                    StoredWorkout(

                        exercise =
                            objectJson.getString(
                                "exercise"
                            ),

                        amount =
                            objectJson.getInt(
                                "amount"
                            ),

                        unit =
                            objectJson.optString(
                                "unit",
                                "REPS"
                            )
                    )
                )
            }

            workouts

        } catch (
            exception: Exception
        ) {

            emptyList()
        }
    }


    // =========================
    // RESET EVERYTHING
    // =========================

    suspend fun clearAll(
        context: Context
    ) {

        context.forgedDataStore.edit { preferences ->

            preferences.clear()
        }
    }
}