package com.forged.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "forged_challenges"

    const val ACTION_COMPLETE =
        "com.forged.app.ACTION_COMPLETE"

    const val ACTION_PENDING =
        "com.forged.app.ACTION_PENDING"

    private const val CHANNEL_NAME =
        "FORGED Challenges"

    private const val CHANNEL_DESCRIPTION =
        "Random FORGED workout challenges"


    /*
     * CREATE NOTIFICATION CHANNEL
     */

    fun createChannel(
        context: Context
    ) {

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

        channel.description =
            CHANNEL_DESCRIPTION

        manager.createNotificationChannel(
            channel
        )
    }


    /*
     * SHOW CHALLENGE NOTIFICATION
     */

    fun showChallengeNotification(
        context: Context,
        exercise: String,
        amount: Int,
        unit: String,
        notificationId: Int
    ) {

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        /*
         * OPEN APP
         */

        val openIntent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val openPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId + 10000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        /*
         * COMPLETE ACTION
         */

        val completeIntent =
            Intent(
                context,
                ForgedNotificationReceiver::class.java
            ).apply {

                action =
                    ACTION_COMPLETE

                putExtra(
                    "exercise",
                    exercise
                )

                putExtra(
                    "amount",
                    amount
                )

                putExtra(
                    "unit",
                    unit
                )

                putExtra(
                    "notification_id",
                    notificationId
                )
            }

        val completePendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId * 2,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        /*
         * PENDING ACTION
         */

        val pendingIntent =
            Intent(
                context,
                ForgedNotificationReceiver::class.java
            ).apply {

                action =
                    ACTION_PENDING

                putExtra(
                    "exercise",
                    exercise
                )

                putExtra(
                    "amount",
                    amount
                )

                putExtra(
                    "unit",
                    unit
                )

                putExtra(
                    "notification_id",
                    notificationId
                )
            }

        val pendingPendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId * 2 + 1,
                pendingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        /*
         * NOTIFICATION
         */

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(
                    "FORGED // CHALLENGE"
                )

                .setContentText(
                    "$amount $exercise"
                )

                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "$amount $exercise\n\n" +
                                    "Your challenge is waiting."
                        )
                )

                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                .setAutoCancel(true)

                .setContentIntent(
                    openPendingIntent
                )

                .addAction(
                    android.R.drawable.ic_menu_send,
                    "COMPLETE",
                    completePendingIntent
                )

                .addAction(
                    android.R.drawable.ic_menu_recent_history,
                    "PENDING",
                    pendingPendingIntent
                )

                .build()


        NotificationManagerCompat
            .from(context)
            .notify(
                notificationId,
                notification
            )
    }


    /*
     * SCHEDULE NOTIFICATION
     */

    fun scheduleNotification(
        context: Context,
        exercise: String,
        amount: Int,
        unit: String,
        triggerTime: Long,
        notificationId: Int
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager


        val intent =
            Intent(
                context,
                ForgedNotificationReceiver::class.java
            ).apply {

                putExtra(
                    "exercise",
                    exercise
                )

                putExtra(
                    "amount",
                    amount
                )

                putExtra(
                    "unit",
                    unit
                )

                putExtra(
                    "notification_id",
                    notificationId
                )
            }



        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId + 50000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}
fun cancelScheduledNotifications(context: Context) {
    val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

    for (notificationId in 5000..5005) {
        val intent = Intent(
            context,
            ForgedNotificationReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 50000,
            intent,
            PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}