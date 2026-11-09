package com.forged.app

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

@Composable
fun ForgedSettingsScreen(
    challengesPerDay: Int,
    savageMode: Boolean,

    activeStartHour: Int,
    activeStartMinute: Int,

    activeEndHour: Int,
    activeEndMinute: Int,

    onStartTimeChanged: (Int, Int) -> Unit,
    onEndTimeChanged: (Int, Int) -> Unit,

    onChallengesChanged: (Int) -> Unit,
    onSavageModeChanged: (Boolean) -> Unit,
    onForgeDay: () -> Unit,
    selectedExercises: Set<String>,
    onExerciseSelectionChanged: (Set<String>) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val exerciseCategories = listOf(
        "UPPER BODY" to listOf(
            "PUSH-UPS",
            "WIDE PUSH-UPS",
            "DIAMOND PUSH-UPS",
            "PIKE PUSH-UPS",
            "PULL-UPS",
            "CHIN-UPS",
            "WALL PUSH-UPS"
        ),
        "CORE" to listOf(
            "SIT-UPS",
            "CRUNCHES",
            "LEG RAISES",
            "BICYCLE CRUNCHES",
            "PLANK"
        ),
        "LOWER BODY" to listOf(
            "SQUATS",
            "REVERSE LUNGES",
            "CALF RAISES",
            "GLUTE BRIDGES",
            "WALL SITS"
        )
    )

    val background = Color(0xFF0D0D0D)
    val white = Color.White
    val muted = Color(0xFF858585)

    fun formatTime(
        hour: Int,
        minute: Int
    ): String {

        val suffix =
            if (hour >= 12) "PM" else "AM"

        val displayHour =
            when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

        return String.format(
            Locale.getDefault(),
            "%02d:%02d %s",
            displayHour,
            minute,
            suffix
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 72.dp,
                bottom = 24.dp
            )
    ) {

        /*
         * HEADER
         */

        Text(
            text = "FORGED",
            color = white,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.clickable {
                onBack()
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "SETTINGS",
            color = muted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        /*
         * ACTIVE HOURS
         */

        Text(
            text = "ACTIVE HOURS",
            color = muted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onStartTimeChanged(
                                hour,
                                minute
                            )
                        },
                        activeStartHour,
                        activeStartMinute,
                        false
                    ).show()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = formatTime(
                    activeStartHour,
                    activeStartMinute
                ),
                color = white,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "START",
                color = muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onEndTimeChanged(
                                hour,
                                minute
                            )
                        },
                        activeEndHour,
                        activeEndMinute,
                        false
                    ).show()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = formatTime(
                    activeEndHour,
                    activeEndMinute
                ),
                color = white,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "END",
                color = muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        /*
         * CHALLENGES PER DAY
         */

        Text(
            text = "CHALLENGES PER DAY",
            color = muted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "$challengesPerDay",
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Text(
                    text = "−",
                    color = white,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        if (challengesPerDay > 1) {
                            onChallengesChanged(
                                challengesPerDay - 1
                            )
                        }
                    }
                )

                Text(
                    text = "+",
                    color = white,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        if (challengesPerDay < 6) {
                            onChallengesChanged(
                                challengesPerDay + 1
                            )
                        }
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        /*
         * Ascension MODE
         */

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "Ascension",
                    color = white,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Strict pending reminders.",
                    color = muted,
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = savageMode,
                onCheckedChange = onSavageModeChanged
            )
        }

        Spacer(
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "EXERCISES",
            color = muted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        exerciseCategories.forEach { (category, exercises) ->

            Text(
                text = category,
                color = white,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(
                    top = 12.dp,
                    bottom = 8.dp
                )
            )

            exercises.forEach { exercise ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (exercise in selectedExercises) {
                                if (selectedExercises.size > 1) {
                                    onExerciseSelectionChanged(
                                        selectedExercises - exercise
                                    )
                                }
                            } else {
                                onExerciseSelectionChanged(
                                    selectedExercises + exercise
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = exercise in selectedExercises,
                        onCheckedChange = { checked ->
                            if (checked) {
                                onExerciseSelectionChanged(
                                    selectedExercises + exercise
                                )
                            } else if (selectedExercises.size > 1) {
                                onExerciseSelectionChanged(
                                    selectedExercises - exercise
                                )
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            checkmarkColor = Color.Black,
                            uncheckedColor = muted
                        )
                    )

                    Text(
                        text = exercise,
                        color = white,
                        fontSize = 14.sp
                    )
                }
            }
        }
        /*
         * FORGE MY DAY
         */

        Text(
            text = "FORGE MY DAY",
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    onForgeDay()
                }
                .padding(
                    vertical = 16.dp
                )
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )
    }
}