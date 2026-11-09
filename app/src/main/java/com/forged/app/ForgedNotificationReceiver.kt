package com.forged.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForgedNotificationReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val pendingResult =
            goAsync()

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val exercise =
                    intent.getStringExtra(
                        "exercise"
                    ) ?: return@launch

                val amount =
                    intent.getIntExtra(
                        "amount",
                        0
                    )

                val unit =
                    intent.getStringExtra(
                        "unit"
                    ) ?: "REPS"

                val notificationId =
                    intent.getIntExtra(
                        "notification_id",
                        1000
                    )

                val action =
                    intent.action


                /*
                 * COMPLETE FROM NOTIFICATION
                 */

                if (
                    action ==
                    NotificationHelper.ACTION_COMPLETE
                ) {

                    completeWorkoutFromNotification(
                        context = context,
                        exercise = exercise,
                        amount = amount,
                        unit = unit
                    )
                }


                /*
                 * PENDING FROM NOTIFICATION
                 */

                else if (
                    action ==
                    NotificationHelper.ACTION_PENDING
                ) {

                    pendingWorkoutFromNotification(
                        context = context,
                        exercise = exercise,
                        amount = amount,
                        unit = unit
                    )
                }


                /*
                 * NORMAL CHALLENGE NOTIFICATION
                 *
                 * This is the important part.
                 *
                 * The workout becomes ACTIVE only
                 * when its notification actually fires.
                 */

                else {

                    activateWorkoutFromNotification(
                        context = context,
                        exercise = exercise,
                        amount = amount,
                        unit = unit
                    )

                    NotificationHelper.showChallengeNotification(
                        context = context,
                        exercise = exercise,
                        amount = amount,
                        unit = unit,
                        notificationId = notificationId
                    )
                }


                /*
                 * Remove notification after
                 * COMPLETE / PENDING action.
                 */

                if (
                    action ==
                    NotificationHelper.ACTION_COMPLETE ||
                    action ==
                    NotificationHelper.ACTION_PENDING
                ) {

                    val manager =
                        context.getSystemService(
                            Context.NOTIFICATION_SERVICE
                        ) as android.app.NotificationManager

                    manager.cancel(
                        notificationId
                    )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }


    /*
     * ACTIVATE WORKOUT
     *
     * Called when the random notification fires.
     */

    private suspend fun activateWorkoutFromNotification(
        context: Context,
        exercise: String,
        amount: Int,
        unit: String
    ) {

        val saved =
            ForgedDataStore
                .state(context)
                .first()


        /*
         * Don't issue more than the
         * configured daily limit.
         */

        if (
            saved.challengesIssuedToday >=
            saved.challengesPerDay
        ) {
            return
        }


        val newIssuedCount =
            saved.challengesIssuedToday + 1


        ForgedDataStore.saveState(

            context = context,

            state =
                saved.copy(

                    currentAmount =
                        amount,

                    currentExercise =
                        exercise,

                    currentUnit =
                        unit,

                    challengesIssuedToday =
                        newIssuedCount
                )
        )
    }


    /*
     * COMPLETE FROM NOTIFICATION
     */

    private suspend fun completeWorkoutFromNotification(
        context: Context,
        exercise: String,
        amount: Int,
        unit: String
    ) {

        val saved =
            ForgedDataStore
                .state(context)
                .first()
        // Prevent completing the same active workout twice.
        if (
            saved.currentExercise != exercise ||
            saved.currentAmount != amount ||
            saved.currentUnit != unit
        ) {
            return
        }


        /*
         * Daily limit reached.
         */

        if (
            saved.completedToday >=
            saved.challengesPerDay
        ) {
            return
        }


        val workout =
            Workout(
                exercise =
                    Exercise(
                        name = exercise,
                        minAmount = amount,
                        maxAmount = amount,
                        unit = unit
                    ),
                amount = amount
            )


        val earnedXp =
            WorkoutEngine.xpForWorkout(
                workout
            )


        val newXp =
            saved.xp + earnedXp


        val newCompleted =
            saved.completedToday + 1


        /*
         * Remove this workout from pending
         * if it was already pending.
         */

        val newPending =
            saved.pendingWorkouts.filterNot {

                it.exercise == exercise &&
                        it.amount == amount &&
                        it.unit == unit
            }


        ForgedDataStore.saveState(

            context = context,

            state =
                saved.copy(

                    xp = newXp,

                    completedToday =
                        newCompleted,

                    currentAmount = 0,

                    currentExercise = null,

                    currentUnit = "REPS",

                    pendingWorkouts =
                        newPending
                )
        )
    }


    /*
     * PENDING FROM NOTIFICATION
     */

    private suspend fun pendingWorkoutFromNotification(
        context: Context,
        exercise: String,
        amount: Int,
        unit: String
    ) {

        val saved =
            ForgedDataStore
                .state(context)
                .first()


        /*
         * Don't add duplicates.
         */

        val alreadyPending =
            saved.pendingWorkouts.any {

                it.exercise == exercise &&
                        it.amount == amount &&
                        it.unit == unit
            }


        val pendingList =
            if (alreadyPending) {

                saved.pendingWorkouts

            } else {

                saved.pendingWorkouts +
                        ForgedDataStore.StoredWorkout(

                            exercise =
                                exercise,

                            amount =
                                amount,

                            unit =
                                unit
                        )
            }


        ForgedDataStore.saveState(

            context = context,

            state =
                saved.copy(

                    currentAmount = 0,

                    currentExercise = null,

                    currentUnit = "REPS",

                    pendingWorkouts =
                        pendingList
                )
        )
    }
}