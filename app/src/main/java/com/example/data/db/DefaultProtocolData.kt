package com.example.data.db

import com.example.data.model.*

object DefaultProtocolData {

    val defaultGyms = listOf(
        Gym(id = 1, name = "Gym A — Panatta Performance Lab", notes = "Primary bodybuilding facility, cam resistance matched"),
        Gym(id = 2, name = "Gym B — Metroflex Heavy Iron", notes = "Hammer Strength plate-loaded equipment"),
        Gym(id = 3, name = "Hotel Gym / Commercial", notes = "Cable stacks and pin-loaded selectorized machines")
    )

    val defaultExercises = listOf(
        // Chest
        Exercise(
            id = 1,
            name = "Incline Machine Press — Panatta",
            baseName = "Incline Chest Press",
            manufacturer = "Panatta",
            muscleGroup = "Chest",
            secondaryMuscles = "Front Delts, Triceps",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 2.5f,
            executionCues = "• Controlled 3-sec eccentric\n• Chest stays elevated throughout\n• Do not bounce at bottom turnaround\n• Drive elbows inward and lock into pec contraction\n• Terminate set when front delts take over tension",
            seatPosition = "4",
            handlePosition = "Wide neutral",
            notes = "Converging motion path"
        ),
        Exercise(
            id = 2,
            name = "Horizontal Chest Press — Hammer Strength",
            baseName = "Flat Chest Press",
            manufacturer = "Hammer Strength",
            muscleGroup = "Chest",
            secondaryMuscles = "Triceps",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 2.5f,
            executionCues = "• Scapulae retracted and depressed into pad\n• Full stretch without shoulder anterior glide\n• Explosive intent on concentric",
            seatPosition = "3",
            handlePosition = "Mid-grip",
            notes = "Plate-loaded isobaric"
        ),
        Exercise(
            id = 3,
            name = "Cable Fly — Dual Low-to-High",
            baseName = "Cable Fly",
            manufacturer = "Life Fitness",
            muscleGroup = "Chest",
            secondaryMuscles = "Front Delts",
            defaultRepMin = 12,
            defaultRepMax = 15,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 120,
            defaultIncrementKg = 2.5f,
            executionCues = "• Slight forward torso tilt\n• Hug an iron barrel at peak squeeze\n• 1-sec peak isometric hold",
            seatPosition = "Standing",
            handlePosition = "Low pulley with D-handles"
        ),
        // Biceps
        Exercise(
            id = 4,
            name = "Preacher Curl — Prime Plate Loaded",
            baseName = "Preacher Curl",
            manufacturer = "Prime",
            muscleGroup = "Biceps",
            secondaryMuscles = "Forearms",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 120,
            defaultIncrementKg = 2.5f,
            executionCues = "• Armpits flush on preacher pad\n• Cam setting #3 for overloaded peak stretch\n• Smooth turnaround, do not hyperextend elbows",
            seatPosition = "2",
            handlePosition = "Semi-supinated"
        ),
        Exercise(
            id = 5,
            name = "Cable Bicep Curl — Dual Stack",
            baseName = "Cable Curl",
            manufacturer = "Technogym",
            muscleGroup = "Biceps",
            secondaryMuscles = "Brachialis",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 120,
            defaultIncrementKg = 2.5f,
            executionCues = "• Elbows pinned slightly in front of ribcage\n• Supinate hard at peak contraction",
            seatPosition = "Standing",
            handlePosition = "Straight revolving bar"
        ),
        // Back
        Exercise(
            id = 6,
            name = "Neutral Grip Lat Pulldown — Nautilus",
            baseName = "Lat Pulldown",
            manufacturer = "Nautilus",
            muscleGroup = "Back",
            secondaryMuscles = "Biceps, Rear Delts",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 2.5f,
            executionCues = "• Drive elbows downward into back pockets\n• Minimal torso swing, lats under continuous tension",
            seatPosition = "Pad 3 locked",
            handlePosition = "MAG close grip"
        ),
        Exercise(
            id = 7,
            name = "Chest-Supported T-Bar Row — Arsenal",
            baseName = "T-Bar Row",
            manufacturer = "Arsenal Strength",
            muscleGroup = "Back",
            secondaryMuscles = "Rhomboids, Mid Traps",
            defaultRepMin = 8,
            defaultRepMax = 10,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 5f,
            executionCues = "• Sternum glues to the pad\n• Protraction at bottom, deep scapular squeeze at top",
            seatPosition = "Chest pad 2",
            handlePosition = "Pronated wide"
        ),
        Exercise(
            id = 8,
            name = "Single Arm High Cable Lat Row",
            baseName = "Single Arm Row",
            manufacturer = "Cable Machine",
            muscleGroup = "Back",
            secondaryMuscles = "Lats",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 1,
            defaultWorkSets = 2,
            defaultRestSeconds = 120,
            defaultIncrementKg = 2.5f,
            executionCues = "• Full lat stretch with torso twist\n• Pull strictly to hip crevice",
            seatPosition = "Half-kneeling",
            handlePosition = "Single D-ring"
        ),
        // Shoulders & Triceps
        Exercise(
            id = 9,
            name = "Seated Overhead Shoulder Press — Panatta",
            baseName = "Shoulder Press",
            manufacturer = "Panatta",
            muscleGroup = "Delts",
            secondaryMuscles = "Triceps, Upper Chest",
            defaultRepMin = 8,
            defaultRepMax = 10,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 2.5f,
            executionCues = "• Keep elbows 30 degrees in front of clavicle\n• Stop 1 inch above ears on eccentric",
            seatPosition = "Seat 3",
            handlePosition = "Neutral handles"
        ),
        Exercise(
            id = 10,
            name = "Lateral Raise Machine — Prime",
            baseName = "Lateral Raise",
            manufacturer = "Prime",
            muscleGroup = "Delts",
            secondaryMuscles = "Traps",
            defaultRepMin = 12,
            defaultRepMax = 15,
            defaultRir = 0,
            defaultWorkSets = 3,
            defaultRestSeconds = 120,
            defaultIncrementKg = 2.5f,
            executionCues = "• Lead with elbows, thumbs angled slightly downward\n• Prime cam on #2 for mid-range overload\n• Ideal for Rest-Pause set execution",
            seatPosition = "Seat 4",
            handlePosition = "Padded arm lever"
        ),
        Exercise(
            id = 11,
            name = "Cross Body Cable Tricep Extension",
            baseName = "Tricep Extension",
            manufacturer = "Dual Cable Stack",
            muscleGroup = "Triceps",
            secondaryMuscles = "",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 120,
            defaultIncrementKg = 1.25f,
            executionCues = "• Cables crossed at collarbone height\n• Extend straight out matching rear delt angle",
            seatPosition = "Standing",
            handlePosition = "Cable balls directly"
        ),
        // Legs
        Exercise(
            id = 12,
            name = "Seated Leg Curl — Panatta",
            baseName = "Hamstring Curl",
            manufacturer = "Panatta",
            muscleGroup = "Hamstrings",
            secondaryMuscles = "Calves",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 3,
            defaultRestSeconds = 150,
            defaultIncrementKg = 2.5f,
            executionCues = "• Thigh pad locked firmly against quads\n• Toes pointed dorsiflexed\n• 2-sec eccentric with hamstring stretch",
            seatPosition = "Backrest 3, Ankle pad 2",
            handlePosition = "Firm grip on handles"
        ),
        Exercise(
            id = 13,
            name = "Linear Hack Squat — Cybex / Panatta",
            baseName = "Hack Squat",
            manufacturer = "Cybex",
            muscleGroup = "Quads",
            secondaryMuscles = "Glutes",
            defaultRepMin = 10,
            defaultRepMax = 12,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 240,
            defaultIncrementKg = 5f,
            executionCues = "• Feet low and shoulder-width on platform\n• Deep knee flexion tracking over toes\n• Smooth reversal out of the hole without jerking spine",
            seatPosition = "Back pad neutral",
            handlePosition = "Safety handles locked",
            notes = "Plate-loaded 45-degree angle"
        ),
        Exercise(
            id = 14,
            name = "Leg Extension — Prime",
            baseName = "Leg Extension",
            manufacturer = "Prime",
            muscleGroup = "Quads",
            secondaryMuscles = "",
            defaultRepMin = 12,
            defaultRepMax = 15,
            defaultRir = 0,
            defaultWorkSets = 2,
            defaultRestSeconds = 150,
            defaultIncrementKg = 2.5f,
            executionCues = "• Hips jammed down into seat\n• 1-sec hard isometric at lockout\n• Rest-pause protocol on final work set",
            seatPosition = "Back 2, Shin pad 3",
            handlePosition = "Side handles pulled up"
        ),
        Exercise(
            id = 15,
            name = "Romanian Deadlift (RDL) — Dumbbells",
            baseName = "RDL",
            manufacturer = "Free Weights",
            muscleGroup = "Hamstrings",
            secondaryMuscles = "Glutes, Lower Back",
            defaultRepMin = 8,
            defaultRepMax = 10,
            defaultRir = 1,
            defaultWorkSets = 2,
            defaultRestSeconds = 180,
            defaultIncrementKg = 5f,
            executionCues = "• Hips push backwards toward the wall\n• Knees soft but static angle\n• Stop when hamstrings reach maximum stretch limit",
            seatPosition = "Standing",
            handlePosition = "Strapped neutral grip"
        )
    )

    val defaultProgramDays = listOf(
        ProgramDay(id = 1, dayIndex = 0, dayCode = "SEG", title = "Chest + Biceps", isRestDay = false, description = "Upper push hypertrophy and bicep peaks"),
        ProgramDay(id = 2, dayIndex = 1, dayCode = "TER", title = "Back", isRestDay = false, description = "Lat width, mid-back density and rear girdle"),
        ProgramDay(id = 3, dayIndex = 2, dayCode = "QUA", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery"),
        ProgramDay(id = 4, dayIndex = 3, dayCode = "QUI", title = "Shoulders + Triceps + Touch-ups", isRestDay = false, description = "Delts cap, triceps lateral head & forearms"),
        ProgramDay(id = 5, dayIndex = 4, dayCode = "SEX", title = "Legs", isRestDay = false, description = "Quad dominance, hamstring overload & hack squat progression"),
        ProgramDay(id = 6, dayIndex = 5, dayCode = "SÁB", title = "Rest", isRestDay = true, description = "Rest day · Active flush & recovery"),
        ProgramDay(id = 7, dayIndex = 6, dayCode = "DOM", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery prior to Monday")
    )

    val defaultProgramExercises = listOf(
        // Day 1: Chest + Biceps
        ProgramExercise(id = 1, programDayId = 1, exerciseId = 1, orderIndex = 1, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
        ProgramExercise(id = 2, programDayId = 1, exerciseId = 2, orderIndex = 2, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
        ProgramExercise(id = 3, programDayId = 1, exerciseId = 3, orderIndex = 3, targetWorkSets = 2, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 120),
        ProgramExercise(id = 4, programDayId = 1, exerciseId = 4, orderIndex = 4, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120),
        ProgramExercise(id = 5, programDayId = 1, exerciseId = 5, orderIndex = 5, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120),

        // Day 2: Back
        ProgramExercise(id = 6, programDayId = 2, exerciseId = 6, orderIndex = 1, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
        ProgramExercise(id = 7, programDayId = 2, exerciseId = 7, orderIndex = 2, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 0, restSeconds = 180),
        ProgramExercise(id = 8, programDayId = 2, exerciseId = 8, orderIndex = 3, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 1, restSeconds = 120),

        // Day 4: Shoulders + Triceps + Touch-ups
        ProgramExercise(id = 9, programDayId = 4, exerciseId = 9, orderIndex = 1, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 0, restSeconds = 180),
        ProgramExercise(id = 10, programDayId = 4, exerciseId = 10, orderIndex = 2, targetWorkSets = 3, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 120),
        ProgramExercise(id = 11, programDayId = 4, exerciseId = 11, orderIndex = 3, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120),

        // Day 5: Legs
        ProgramExercise(id = 12, programDayId = 5, exerciseId = 12, orderIndex = 1, targetWorkSets = 3, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 150),
        ProgramExercise(id = 13, programDayId = 5, exerciseId = 13, orderIndex = 2, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 240),
        ProgramExercise(id = 14, programDayId = 5, exerciseId = 15, orderIndex = 3, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 1, restSeconds = 180),
        ProgramExercise(id = 15, programDayId = 5, exerciseId = 14, orderIndex = 4, targetWorkSets = 2, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 150)
    )
}
