package com.forged.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forged.app.ui.theme.FORGEDTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.forged.app.ForgedDataStore.StoredWorkout
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Permission result handled.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            FORGEDTheme {
                ForgedHome()
            }
        }
    }
}

@Composable
fun ForgedHome() {

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var showSettings by remember {
        mutableStateOf(false)
    }

    var loaded by remember {
        mutableStateOf(false)
    }

    var xp by remember {
        mutableIntStateOf(0)
    }

    var completedToday by remember {
        mutableIntStateOf(0)
    }

    var challengesIssuedToday by remember {
        mutableIntStateOf(0)
    }

    var challengesPerDay by remember {
        mutableIntStateOf(5)
    }

    var activeStartHour by remember {
        mutableIntStateOf(8)
    }

    var activeStartMinute by remember {
        mutableIntStateOf(0)
    }

    var activeEndHour by remember {
        mutableIntStateOf(22)
    }

    var activeEndMinute by remember {
        mutableIntStateOf(0)
    }

    var savageMode by remember {
        mutableStateOf(false)
    }
    var selectedExercises by remember {
        mutableStateOf(
            WorkoutEngine.exercises.map { it.name }.toSet()
        )
    }

    var pending by remember {
        mutableStateOf(false)
    }

    var pendingWorkouts by remember {
        mutableStateOf<List<ForgedDataStore.StoredWorkout>>(emptyList())
    }

    var currentWorkout by remember {
        mutableStateOf(
            WorkoutEngine.generateWorkout(0)
        )
    }

    var todayWorkouts by remember {
        mutableStateOf<List<Workout>>(emptyList())
    }

    /*
     * TODAY'S DATE
     */
    fun todayDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())
    }

    /*
     * LOAD DATA
     */
    LaunchedEffect(Unit) {

        val saved =
            ForgedDataStore
                .state(context)
                .first()

        xp = saved.xp

        completedToday =
            saved.completedToday

        challengesIssuedToday =
            saved.challengesIssuedToday

        challengesPerDay =
            saved.challengesPerDay

        savageMode =
            saved.savageMode

        activeStartHour =
            saved.activeStartHour

        activeStartMinute =
            saved.activeStartMinute

        activeEndHour =
            saved.activeEndHour

        activeEndMinute =
            saved.activeEndMinute

        pendingWorkouts =
            saved.pendingWorkouts

        val today =
            todayDate()

        val isNewDay =
            saved.dailyDate != today
        val needsSetup =
            saved.dailyDate == null &&
                    saved.dailyWorkouts.isEmpty()

        if (needsSetup) {
            showSettings = true
            loaded = true
            return@LaunchedEffect
        }

        /*
         * Check whether saved daily deck
         * belongs to today.
         */

        if (
            saved.dailyDate == today &&
            saved.dailyWorkouts.isNotEmpty()
        ) {

            todayWorkouts =
                saved.dailyWorkouts.map {

                    Workout(
                        exercise =
                            Exercise(
                                name = it.exercise,
                                minAmount = it.amount,
                                maxAmount = it.amount,
                                unit = it.unit
                            ),
                        amount = it.amount
                    )
                }


        } else {

            /*
             * NEW DAY
             */

            todayWorkouts =
                WorkoutEngine
                    .generateDailyWorkouts(
                        xp = xp,
                        count = challengesPerDay,
                        savageMode = savageMode
                    )

            completedToday = 0
            challengesIssuedToday = 0

            /*
             * First challenge of the day.
             */

            currentWorkout =
                Workout(
                    exercise =
                        Exercise(
                            name = "",
                            minAmount = 0,
                            maxAmount = 0,
                            unit = "REPS"
                        ),
                    amount = 0
                )

            challengesIssuedToday = 0
        }
        /*
 * SCHEDULE TODAY'S NOTIFICATIONS
 */

        if (isNewDay) {
            cancelScheduledNotifications(context)


            scheduleDailyNotifications(
                context = context,
                workouts = todayWorkouts,
                startHour = activeStartHour,
                startMinute = activeStartMinute,
                endHour = activeEndHour,
                endMinute = activeEndMinute
            )
        }

        /*
         * Restore current workout.
         */

        if (
            !isNewDay &&
            saved.currentExercise != null &&
            saved.currentAmount > 0
        ) {

            currentWorkout =
                Workout(
                    exercise =
                        Exercise(
                            name =
                                saved.currentExercise,
                            minAmount =
                                saved.currentAmount,
                            maxAmount =
                                saved.currentAmount,
                            unit =
                                saved.currentUnit
                        ),
                    amount =
                        saved.currentAmount
                )
        } else {
            currentWorkout =
                Workout(
                    exercise =
                        Exercise(
                            name = "",
                            minAmount = 0,
                            maxAmount = 0,
                            unit = "REPS"
                        ),
                    amount = 0
                )
        }

        /*
         * Restore pending state.
         */

        pending =
            saved.pendingWorkouts.any {
                it.exercise ==
                        currentWorkout.exercise.name &&
                        it.amount ==
                        currentWorkout.amount
            }

        /*
         * Save today's deck immediately.
         */

        saveStateNow(
            context = context,
            xp = xp,
            completedToday = completedToday,
            challengesIssuedToday =
                challengesIssuedToday,
            challengesPerDay =
                challengesPerDay,
            savageMode =
                savageMode,
            selectedExercises = selectedExercises,
            activeStartHour = activeStartHour,
            activeStartMinute = activeStartMinute,
            activeEndHour = activeEndHour,
            activeEndMinute = activeEndMinute,
            currentWorkout =
                currentWorkout,
            pendingWorkouts =
                pendingWorkouts,
            todayWorkouts =
                todayWorkouts
        )

        loaded = true
    }

    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect

        ForgedDataStore.state(context).collect { saved ->
            // Ignore old or uninitialized daily data.
            if (
                saved.dailyDate != todayDate() ||
                saved.dailyWorkouts.isEmpty()
            ) {
                return@collect
            }

            xp = saved.xp
            selectedExercises =
                if (saved.selectedExercises.isEmpty()) {
                    WorkoutEngine.exercises.map { it.name }.toSet()
                } else {
                    saved.selectedExercises
                }
            completedToday = saved.completedToday
            challengesIssuedToday = saved.challengesIssuedToday
            challengesPerDay = saved.challengesPerDay
            savageMode = saved.savageMode

            activeStartHour = saved.activeStartHour
            activeStartMinute = saved.activeStartMinute
            activeEndHour = saved.activeEndHour
            activeEndMinute = saved.activeEndMinute

            pendingWorkouts = saved.pendingWorkouts

            todayWorkouts = saved.dailyWorkouts.map {
                Workout(
                    exercise = Exercise(
                        name = it.exercise,
                        minAmount = it.amount,
                        maxAmount = it.amount,
                        unit = it.unit
                    ),
                    amount = it.amount
                )
            }

            currentWorkout =
                if (
                    saved.currentExercise != null &&
                    saved.currentAmount > 0
                ) {
                    Workout(
                        exercise = Exercise(
                            name = saved.currentExercise,
                            minAmount = saved.currentAmount,
                            maxAmount = saved.currentAmount,
                            unit = saved.currentUnit
                        ),
                        amount = saved.currentAmount
                    )
                } else {
                    Workout(
                        exercise = Exercise(
                            name = "",
                            minAmount = 0,
                            maxAmount = 0,
                            unit = "REPS"
                        ),
                        amount = 0
                    )
                }

            pending = saved.pendingWorkouts.any {
                it.exercise == currentWorkout.exercise.name &&
                        it.amount == currentWorkout.amount &&
                        it.unit == currentWorkout.exercise.unit
            }
        }
    }


    /*
     * SAVE FUNCTION
     */
    fun saveCurrentState() {

        scope.launch {

            saveStateNow(
                context = context,
                xp = xp,
                completedToday =
                    completedToday,
                challengesIssuedToday =
                    challengesIssuedToday,
                challengesPerDay =
                    challengesPerDay,
                savageMode =
                    savageMode,
                selectedExercises = selectedExercises,
                activeStartHour = activeStartHour,
                activeStartMinute = activeStartMinute,
                activeEndHour = activeEndHour,
                activeEndMinute = activeEndMinute,
                currentWorkout =
                    currentWorkout,
                pendingWorkouts =
                    pendingWorkouts,
                todayWorkouts =
                    todayWorkouts
            )
        }
    }

    /*
     * COMPLETE WORKOUT
     */
    fun completeWorkout() {
        if (
            currentWorkout.amount <= 0 ||
            currentWorkout.exercise.name.isBlank()
        ) {
            return
        }

        if (completedToday >= challengesPerDay) {
            return
        }

        val workoutToComplete = currentWorkout

        val earnedXp =
            WorkoutEngine.xpForWorkout(workoutToComplete)

        pendingWorkouts = pendingWorkouts.filterNot {
            it.exercise == workoutToComplete.exercise.name &&
                    it.amount == workoutToComplete.amount &&
                    it.unit == workoutToComplete.exercise.unit
        }

        pending = false

        xp += earnedXp
        completedToday++

        currentWorkout = Workout(
            exercise = Exercise(
                name = "",
                minAmount = 0,
                maxAmount = 0,
                unit = "REPS"
            ),
            amount = 0
        )


        saveCurrentState()
    }

    fun completePendingWorkout(workout: StoredWorkout) {
        if (workout.amount <= 0 || workout.exercise.isBlank()) return
        if (completedToday >= challengesPerDay) return

        val index = pendingWorkouts.indexOfFirst {
            it.exercise == workout.exercise &&
                    it.amount == workout.amount &&
                    it.unit == workout.unit
        }

        if (index == -1) return

        val workoutToComplete = Workout(
            exercise = Exercise(
                name = workout.exercise,
                minAmount = workout.amount,
                maxAmount = workout.amount,
                unit = workout.unit
            ),
            amount = workout.amount
        )

        val earnedXp = WorkoutEngine.xpForWorkout(workoutToComplete)

        pendingWorkouts = pendingWorkouts.toMutableList().also {
            it.removeAt(index)
        }

        xp += earnedXp
        completedToday++

        saveCurrentState()
    }


    /*
     * MARK PENDING
     */
    fun markPending() {
        if (
            currentWorkout.amount <= 0 ||
            currentWorkout.exercise.name.isBlank()
        ) {
            return
        }

        val alreadyPending = pendingWorkouts.any {
            it.exercise == currentWorkout.exercise.name &&
                    it.amount == currentWorkout.amount &&
                    it.unit == currentWorkout.exercise.unit
        }

        if (!alreadyPending) {
            pendingWorkouts = pendingWorkouts +
                    ForgedDataStore.StoredWorkout(
                        exercise = currentWorkout.exercise.name,
                        amount = currentWorkout.amount,
                        unit = currentWorkout.exercise.unit
                    )
        }

        pending = true
        saveCurrentState()
    }

    /*
     * SETTINGS SCREEN
     */
    if (showSettings) {

        ForgedSettingsScreen(
            challengesPerDay = challengesPerDay,
            savageMode = savageMode,

            activeStartHour = activeStartHour,
            activeStartMinute = activeStartMinute,

            activeEndHour = activeEndHour,
            activeEndMinute = activeEndMinute,

            onStartTimeChanged = { hour, minute ->

                activeStartHour = hour
                activeStartMinute = minute

                scope.launch {
                    saveCurrentState()
                }
            },

            onEndTimeChanged = { hour, minute ->

                activeEndHour = hour
                activeEndMinute = minute

                scope.launch {
                    saveCurrentState()
                }
            },

            onChallengesChanged = {

                challengesPerDay = it

                scope.launch {
                    saveCurrentState()
                }
            },

            onSavageModeChanged = {

                savageMode = it

                scope.launch {
                    saveCurrentState()
                }
            },

            selectedExercises = selectedExercises,

            onExerciseSelectionChanged = { newSelection ->
                selectedExercises = newSelection
                saveCurrentState()
            },

            onForgeDay = {

                todayWorkouts = WorkoutEngine.generateDailyWorkouts(
                    xp = xp,
                    count = challengesPerDay,
                    savageMode = savageMode,
                    selectedExerciseNames = selectedExercises
                )

                completedToday = 0
                challengesIssuedToday = 0

                currentWorkout =
                    Workout(
                        exercise =
                            Exercise(
                                name = "",
                                minAmount = 0,
                                maxAmount = 0,
                                unit = "REPS"
                            ),
                        amount = 0
                    )

                pendingWorkouts = emptyList()
                pending = false

                scope.launch {

                    cancelScheduledNotifications(context)
                    saveCurrentState()

                    scheduleDailyNotifications(
                        context = context,
                        workouts = todayWorkouts,
                        startHour = activeStartHour,
                        startMinute = activeStartMinute,
                        endHour = activeEndHour,
                        endMinute = activeEndMinute
                    )

                    showSettings = false
                }
            },

            onBack = {
                showSettings = false
            }
        )

        return
    }

    /*
     * LOADING
     */
    if (!loaded) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color(0xFF0D0D0D)
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "FORGED",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        return
    }

    /*
     * COLORS
     */

    val background =
        Color(0xFF0D0D0D)

    val card =
        Color(0xFF171717)

    val white =
        Color.White

    val muted =
        Color(0xFF858585)

    val completeSelected =
        !pending

    val pendingSelected =
        pending

    /*
     * RANK
     */

    val rank =
        WorkoutEngine.getRank(xp)

    val xpToNextRank =
        WorkoutEngine.xpUntilNextRank(xp)

    /*
     * MAIN UI
     */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(
                horizontal = 24.dp,
                vertical = 24.dp
            )
    ) {

        /*
         * HEADER
         */

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            Column {

                Text(
                    text = "FORGED",
                    color = white,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Earn your strength.",
                    color = muted,
                    fontSize = 14.sp
                )
            }

            /*
             * SETTINGS
             */

            Text(
                text = "⚙",
                color = muted,
                fontSize = 20.sp,
                modifier = Modifier.clickable {
                    showSettings = true
                }
            )
        }


        /*
         * SPACE BELOW HEADER
         */

        Spacer(
            modifier = Modifier.height(40.dp)
        )


        /*
         * RANK DISPLAY
         */

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /*
             * CURRENT RANK
             */

            Text(
                text = rank.title,
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )


            /*
             * NEXT RANK
             */

            val nextRank =
                Rank.entries.firstOrNull {
                    it.minimumXp > rank.minimumXp
                }


            if (nextRank != null) {

                /*
                 * XP TEXT
                 */

                Text(
                    text = "$xp / ${nextRank.minimumXp} XP",
                    color = muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                /*
                 * XP PROGRESS
                 */

                val rankStart =
                    rank.minimumXp

                val rankEnd =
                    nextRank.minimumXp

                val rankProgress =
                    (
                            (xp - rankStart).toFloat() /
                                    (rankEnd - rankStart).toFloat()
                            ).coerceIn(0f, 1f)


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 70.dp)
                        .height(5.dp)
                        .background(
                            Color(0xFF333333),
                            RoundedCornerShape(10.dp)
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(rankProgress)
                            .height(5.dp)
                            .background(
                                white,
                                RoundedCornerShape(10.dp)
                            )
                    )
                }

            } else {

                Text(
                    text = "MAX RANK",
                    color = muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }


        /*
         * SPACE BEFORE TODAY
         */

        Spacer(
            modifier = Modifier.height(40.dp)
        )


        /*
         * TODAY
         */

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text =
                    "TODAY'S CHALLENGES",

                color = muted,

                fontSize = 12.sp,

                letterSpacing = 1.5.sp
            )

            Text(
                text =
                    "$completedToday / $challengesPerDay",

                color = white,

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        /*
         * CHALLENGE CARD
         */

        /*
  * CHALLENGE CARD
  */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = card,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                if (completedToday >= challengesPerDay) {

                    Column {

                        Text(
                            text = "DAY COMPLETE",
                            color = white,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "You've completed all challenges.",
                            color = white,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                    }

                } else if (currentWorkout.amount > 0) {

                    Column {

                        Text(
                            text = currentWorkout.amount.toString(),
                            color = white,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${currentWorkout.exercise.unit} ${currentWorkout.exercise.name}",
                            color = Color(0xFFB5B5B5),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        Text(
                            text = if (pending) "PENDING" else "DO IT NOW",
                            color = white,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                } else {

                    Column {

                        Text(
                            text = "CHALLENGE COMPLETE",
                            color = white,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "You've earned your XP.",
                            color = white,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "We'll notify you when your next challenge is ready.",
                            color = Color(0xFF777777),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
         * COMPLETE / PENDING
         */


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val completeInteractionSource = remember { MutableInteractionSource() }
                    val completePressed by completeInteractionSource.collectIsPressedAsState()

                    Button(
                        onClick = {
                            completeWorkout()
                        },
                        enabled = completedToday < challengesPerDay,
                        interactionSource = completeInteractionSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (completePressed) white else card,
                            contentColor = if (completePressed) Color.Black else Color(0xFF777777),
                            disabledContainerColor = card,
                            disabledContentColor = Color(0xFF777777)
                        )
                    ) {
                        Text(
                            text = if (completedToday >= challengesPerDay) {
                                "DONE FOR TODAY"
                            } else {
                                "COMPLETE"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    val pendingInteractionSource = remember { MutableInteractionSource() }
                    val pendingPressed by pendingInteractionSource.collectIsPressedAsState()

                    Button(
                        onClick = {
                            markPending()
                        },
                        enabled = completedToday < challengesPerDay,
                        interactionSource = pendingInteractionSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pendingPressed) white else card,
                            contentColor = if (pendingPressed) Color.Black else Color(0xFF777777),
                            disabledContainerColor = card,
                            disabledContentColor = Color(0xFF777777)
                        )
                    ) {
                        Text(
                            text = if (completedToday >= challengesPerDay) {
                                "NO CHALLENGES"
                            } else {
                                "PENDING"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                /*
         * DAILY PROGRESS
         */

                Text(
                    text =
                        "TODAY'S PROGRESS",

                    color = muted,

                    fontSize = 12.sp,

                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                val dailyProgress =
                    (completedToday.toFloat() /
                            challengesPerDay.toFloat())
                        .coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Color(0xFF333333),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(dailyProgress)
                            .height(4.dp)
                            .background(
                                white,
                                RoundedCornerShape(10.dp)
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                /*
         * RANK PROGRESS
         */

                Text(
                    text =
                        "RANK PROGRESS",

                    color = muted,

                    fontSize = 12.sp,

                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        if (xpToNextRank != null)
                            "$xpToNextRank XP until next rank"
                        else
                            "Maximum rank reached.",

                    color = white,

                    fontSize = 15.sp,

                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                /*
         * PENDING
         */


                Text(
                    text = "PENDING",
                    color = muted,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (pendingWorkouts.isNotEmpty()) {
                    pendingWorkouts.forEach { workout ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${workout.amount} ${workout.unit} ${workout.exercise}",
                                color = white,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    completePendingWorkout(workout)
                                }
                            ) {
                                Text("COMPLETE")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = if (savageMode) "You still owe these."
                        else "Complete them when you're ready.",
                        color = Color(0xFF777777),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Nothing pending.",
                        color = Color(0xFF777777),
                        fontSize = 15.sp
                    )
                }


            }
        }
    }
}

/*
 * SAVE EVERYTHING
 */
private fun scheduleDailyNotifications(
    context: android.content.Context,
    workouts: List<Workout>,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int
) {

    if (workouts.isEmpty()) {
        return
    }

    val now =
        System.currentTimeMillis()

    val calendar =
        java.util.Calendar.getInstance()

    calendar.timeInMillis = now

    /*
     * Active hours:
     *
     * 08:00 → 22:00
     */

    val start =
        java.util.Calendar.getInstance().apply {
            set(
                java.util.Calendar.HOUR_OF_DAY,
                startHour
            )
            set(
                java.util.Calendar.MINUTE,
                startMinute
            )
            set(
                java.util.Calendar.SECOND,
                0
            )
            set(
                java.util.Calendar.MILLISECOND,
                0
            )
        }

    val end =
        java.util.Calendar.getInstance().apply {
            set(
                java.util.Calendar.HOUR_OF_DAY,
                endHour
            )
            set(
                java.util.Calendar.MINUTE,
                endMinute
            )
            set(
                java.util.Calendar.SECOND,
                0
            )
            set(
                java.util.Calendar.MILLISECOND,
                0
            )
        }

    /*
     * Don't schedule a notification
     * in the past.
     */

    val startTime =
        maxOf(
            start.timeInMillis,
            now + 60_000
        )

    val endTime =
        end.timeInMillis

    if (startTime >= endTime) {
        return
    }

    val random =
        kotlin.random.Random

    /*
     * Generate unique random times.
     */

    val availableTimes =
        mutableSetOf<Long>()

    val numberOfNotifications =
        workouts.size

    while (
        availableTimes.size <
        numberOfNotifications
    ) {

        val randomTime =
            random.nextLong(
                startTime,
                endTime
            )

        availableTimes.add(
            randomTime
        )
    }

    val sortedTimes =
        availableTimes.sorted()

    workouts.forEachIndexed {
            index,
            workout ->

        NotificationHelper.scheduleNotification(

            context =
                context,

            exercise =
                workout.exercise.name,

            amount =
                workout.amount,

            unit =
                workout.exercise.unit,

            triggerTime =
                sortedTimes[index],

            notificationId =
                5000 + index
        )
    }
}
private suspend fun saveStateNow(
    context: android.content.Context,
    xp: Int,
    completedToday: Int,
    challengesIssuedToday: Int,
    challengesPerDay: Int,
    savageMode: Boolean,
    selectedExercises: Set<String>,
    activeStartHour: Int,
    activeStartMinute: Int,
    activeEndHour: Int,
    activeEndMinute: Int,
    currentWorkout: Workout,
    pendingWorkouts: List<ForgedDataStore.StoredWorkout>,
    todayWorkouts: List<Workout>
) {

    val dailyStoredWorkouts =
        todayWorkouts.map {

            ForgedDataStore.StoredWorkout(

                exercise =
                    it.exercise.name,

                amount =
                    it.amount,

                unit =
                    it.exercise.unit
            )
        }

    val state =
        ForgedDataStore.State(

            xp = xp,
            selectedExercises = selectedExercises,

            completedToday =
                completedToday,

            challengesIssuedToday =
                challengesIssuedToday,

            currentAmount =
                currentWorkout.amount,

            currentExercise =
                currentWorkout
                    .exercise
                    .name,

            currentUnit =
                currentWorkout
                    .exercise
                    .unit,
            activeStartHour = activeStartHour,
            activeStartMinute = activeStartMinute,
            activeEndHour = activeEndHour,
            activeEndMinute = activeEndMinute,

            pendingWorkouts =
                pendingWorkouts,

            dailyDate =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).format(Date()),

            dailyWorkouts =
                dailyStoredWorkouts,

            challengesPerDay =
                challengesPerDay,

            savageMode =
                savageMode
        )

    ForgedDataStore.saveState(
        context = context,
        state = state
    )
}
