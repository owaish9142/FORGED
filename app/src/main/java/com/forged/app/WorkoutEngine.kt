
package com.forged.app

import kotlin.random.Random

enum class Rank(
    val title: String,
    val minimumXp: Int
) {
    BEGINNER("BEGINNER", 0),
    ROOKIE("ROOKIE", 100),
    FIGHTER("FIGHTER", 250),
    WARRIOR("WARRIOR", 500),
    ELITE("ELITE", 1000),
    FORGED("FORGED", 2000);

    companion object {
        fun fromXp(xp: Int): Rank {
            return entries
                .reversed()
                .first { xp >= it.minimumXp }
        }
    }
}

data class Exercise(
    val name: String,
    val minAmount: Int,
    val maxAmount: Int,
    val unit: String = "REPS"
)

data class Workout(
    val exercise: Exercise,
    val amount: Int
) {
    val displayText: String
        get() = "$amount ${exercise.unit} ${exercise.name}"
}


object WorkoutEngine {

    val exercises = listOf(
        // UPPER BODY — 7
        Exercise("PUSH-UPS", 5, 8),
        Exercise("WIDE PUSH-UPS", 5, 8),
        Exercise("DIAMOND PUSH-UPS", 3, 6),
        Exercise("PIKE PUSH-UPS", 3, 6),
        Exercise("PULL-UPS", 2, 4),
        Exercise("CHIN-UPS", 2, 4),
        Exercise("WALL PUSH-UPS", 8, 12),

        // CORE — 5
        Exercise("SIT-UPS", 5, 10),
        Exercise("CRUNCHES", 8, 12),
        Exercise("LEG RAISES", 5, 8),
        Exercise("BICYCLE CRUNCHES", 8, 12),
        Exercise("PLANK", 15, 25, "SEC"),

        // LOWER BODY — 5
        Exercise("SQUATS", 8, 12),
        Exercise("REVERSE LUNGES", 6, 10),
        Exercise("CALF RAISES", 10, 15),
        Exercise("GLUTE BRIDGES", 8, 12),
        Exercise("WALL SITS", 15, 25, "SEC")
    )

    fun generateWorkout(
        xp: Int,
        previousExercise: String? = null
    ): Workout {
        val availableExercises = exercises.filter {
            it.name != previousExercise
        }.ifEmpty { exercises }

        val baseExercise = availableExercises.random()
        val multiplier = rankMultiplier(xp)

        val min = maxOf(1, (baseExercise.minAmount * multiplier).toInt())
        val max = maxOf(min, (baseExercise.maxAmount * multiplier).toInt())

        return Workout(
            exercise = baseExercise,
            amount = Random.nextInt(min, max + 1)
        )
    }

    fun generateDailyWorkouts(
        xp: Int,
        count: Int,
        savageMode: Boolean = false,
        selectedExerciseNames: Set<String> = exercises.map { it.name }.toSet()
    ): List<Workout> {
        val availableExercises = exercises.filter {
            it.name in selectedExerciseNames
        }

        if (availableExercises.isEmpty()) return emptyList()

        val rank = Rank.fromXp(xp)
        val multiplier = rankMultiplier(xp) * if (savageMode) 1.5 else 1.0

        return availableExercises.shuffled()
            .take(count.coerceIn(1, availableExercises.size))
            .map { exercise ->
                val min = maxOf(1, (exercise.minAmount * multiplier).toInt())
                val max = maxOf(min, (exercise.maxAmount * multiplier).toInt())

                Workout(
                    exercise = exercise,
                    amount = Random.nextInt(min, max + 1)
                )
            }
    }

    private fun rankMultiplier(xp: Int): Double {
        return when (Rank.fromXp(xp)) {
            Rank.BEGINNER -> 1.0
            Rank.ROOKIE -> 1.15
            Rank.FIGHTER -> 1.30
            Rank.WARRIOR -> 1.50
            Rank.ELITE -> 1.75
            Rank.FORGED -> 2.0
        }
    }

    fun getRank(xp: Int): Rank = Rank.fromXp(xp)

    fun xpForWorkout(workout: Workout): Int {
        return when (workout.exercise.name) {
            "PULL-UPS", "CHIN-UPS" -> workout.amount * 2
            "PLANK", "WALL SITS" -> workout.amount / 2
            else -> workout.amount
        }
    }

    fun xpUntilNextRank(xp: Int): Int? {
        val currentRank = Rank.fromXp(xp)
        val nextRank = Rank.entries.firstOrNull {
            it.minimumXp > currentRank.minimumXp
        }

        return nextRank?.minimumXp?.minus(xp)
    }
}
