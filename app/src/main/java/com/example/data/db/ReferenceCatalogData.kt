package com.example.data.db

import com.example.data.model.*

object ReferenceCatalogData {

    // --- 15 CANONICAL MUSCLE GROUPS ---
    val canonicalMuscleGroups = listOf(
        MuscleGroup(id = "muscle_chest", name = "Chest", region = "Upper Body Push", orderIndex = 1),
        MuscleGroup(id = "muscle_front_delts", name = "Front Deltoids", region = "Upper Body Push", orderIndex = 2),
        MuscleGroup(id = "muscle_side_delts", name = "Side Deltoids", region = "Upper Body Push", orderIndex = 3),
        MuscleGroup(id = "muscle_rear_delts", name = "Rear Deltoids", region = "Upper Body Pull", orderIndex = 4),
        MuscleGroup(id = "muscle_lats", name = "Latissimus Dorsi (Lats)", region = "Upper Body Pull", orderIndex = 5),
        MuscleGroup(id = "muscle_upper_back", name = "Upper & Mid Back", region = "Upper Body Pull", orderIndex = 6),
        MuscleGroup(id = "muscle_lower_back", name = "Spinal Erectors", region = "Upper Body Pull", orderIndex = 7),
        MuscleGroup(id = "muscle_biceps", name = "Biceps Brachii", region = "Arms", orderIndex = 8),
        MuscleGroup(id = "muscle_triceps", name = "Triceps Brachii", region = "Arms", orderIndex = 9),
        MuscleGroup(id = "muscle_forearms", name = "Forearms & Brachialis", region = "Arms", orderIndex = 10),
        MuscleGroup(id = "muscle_quads", name = "Quadriceps", region = "Legs", orderIndex = 11),
        MuscleGroup(id = "muscle_hamstrings", name = "Hamstrings", region = "Legs", orderIndex = 12),
        MuscleGroup(id = "muscle_glutes", name = "Glutes", region = "Legs", orderIndex = 13),
        MuscleGroup(id = "muscle_calves", name = "Calves", region = "Legs", orderIndex = 14),
        MuscleGroup(id = "muscle_abs", name = "Abdominals & Core", region = "Core", orderIndex = 15)
    )

    // --- REFERENCE GYMS ---
    val defaultGyms = listOf(
        Gym(id = 1, name = "Gym A — Panatta Performance Lab", notes = "Primary bodybuilding facility, cam resistance matched", source = "bundled"),
        Gym(id = 2, name = "Gym B — Metroflex Heavy Iron", notes = "Hammer Strength plate-loaded equipment", source = "bundled"),
        Gym(id = 3, name = "Gym C — Commercial / Hotel Fitness", notes = "Selectorized pin-loaded stacks and cables", source = "bundled")
    )

    // --- 186 MACHINE CATALOG MODELS (Global Reference Models) ---
    val canonicalMachineCatalog: List<MachineCatalogEntity> by lazy {
        val list = mutableListOf<MachineCatalogEntity>()

        fun addMachine(id: String, manufacturer: String, model: String, type: String, increment: Float = 2.5f) {
            list.add(
                MachineCatalogEntity(
                    id = id,
                    manufacturer = manufacturer,
                    model = model,
                    equipmentType = type,
                    defaultIncrementKg = increment,
                    source = "bundled"
                )
            )
        }

        // --- PANATTA (30 models) ---
        addMachine("machine_panatta_super_incline_chest", "Panatta", "Super Inclined Chest Press", "Plate-Loaded")
        addMachine("machine_panatta_super_horizontal_bench", "Panatta", "Super Horizontal Bench Press", "Plate-Loaded")
        addMachine("machine_panatta_converging_chest_press", "Panatta", "Converging Chest Press", "Plate-Loaded")
        addMachine("machine_panatta_pec_deck_fly", "Panatta", "Pec Fly / Rear Delt Dual", "Selectorized")
        addMachine("machine_panatta_monolith_lat_pulldown", "Panatta", "Monolith Lat Pulldown", "Selectorized")
        addMachine("machine_panatta_super_pulldown_circular", "Panatta", "Super Pulldown Circular", "Plate-Loaded")
        addMachine("machine_panatta_high_row", "Panatta", "High Row Plate Loaded", "Plate-Loaded")
        addMachine("machine_panatta_pulley_row", "Panatta", "Pulley Low Row Dual Stack", "Selectorized")
        addMachine("machine_panatta_tbar_row", "Panatta", "T-Bar Row with Chest Support", "Plate-Loaded")
        addMachine("machine_panatta_super_overhead_press", "Panatta", "Super Overhead Press", "Plate-Loaded")
        addMachine("machine_panatta_lateral_deltoid", "Panatta", "Lateral Deltoid Machine", "Selectorized")
        addMachine("machine_panatta_super_hack_squat", "Panatta", "Super Hack Squat 45°", "Plate-Loaded", 5f)
        addMachine("machine_panatta_linear_hack_squat", "Panatta", "Linear Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_panatta_super_leg_press", "Panatta", "Super Leg Press 45°", "Plate-Loaded", 10f)
        addMachine("machine_panatta_horizontal_leg_press", "Panatta", "Horizontal Leg Press", "Selectorized", 5f)
        addMachine("machine_panatta_seated_leg_curl", "Panatta", "Seated Leg Curl Monolith", "Selectorized")
        addMachine("machine_panatta_lying_leg_curl", "Panatta", "Lying Leg Curl Monolith", "Selectorized")
        addMachine("machine_panatta_standing_leg_curl", "Panatta", "Standing Single Leg Curl", "Plate-Loaded")
        addMachine("machine_panatta_leg_extension", "Panatta", "Leg Extension Monolith", "Selectorized")
        addMachine("machine_panatta_standing_calf", "Panatta", "Standing Calf Raise", "Selectorized", 5f)
        addMachine("machine_panatta_seated_calf", "Panatta", "Seated Calf Raise", "Plate-Loaded")
        addMachine("machine_panatta_preacher_curl", "Panatta", "Preacher Curl Monolith", "Selectorized")
        addMachine("machine_panatta_french_press", "Panatta", "French Press Overhead Triceps", "Selectorized")
        addMachine("machine_panatta_dip_machine", "Panatta", "Triceps Dip Machine", "Plate-Loaded")
        addMachine("machine_panatta_rotary_torso", "Panatta", "Rotary Torso", "Selectorized")
        addMachine("machine_panatta_incline_fly", "Panatta", "Free Weight Incline Bench Station", "Free Weight")
        addMachine("machine_panatta_glute_drive", "Panatta", "Glute Thrust Machine", "Plate-Loaded")
        addMachine("machine_panatta_adductor", "Panatta", "Adductor Machine", "Selectorized")
        addMachine("machine_panatta_abductor", "Panatta", "Abductor Machine", "Selectorized")
        addMachine("machine_panatta_cable_station", "Panatta", "Dual Adjustable Pulley 8-Stack", "Cable")

        // --- HAMMER STRENGTH (26 models) ---
        addMachine("machine_hammer_iso_incline", "Hammer Strength", "Iso-Lateral Incline Press", "Plate-Loaded")
        addMachine("machine_hammer_iso_bench", "Hammer Strength", "Iso-Lateral Horizontal Bench", "Plate-Loaded")
        addMachine("machine_hammer_iso_decline", "Hammer Strength", "Iso-Lateral Decline Press", "Plate-Loaded")
        addMachine("machine_hammer_iso_wide_chest", "Hammer Strength", "Iso-Lateral Wide Chest Press", "Plate-Loaded")
        addMachine("machine_hammer_iso_front_lat", "Hammer Strength", "Iso-Lateral Front Lat Pulldown", "Plate-Loaded")
        addMachine("machine_hammer_iso_underhand_lat", "Hammer Strength", "Iso-Lateral Underhand Pulldown", "Plate-Loaded")
        addMachine("machine_hammer_iso_high_row", "Hammer Strength", "Iso-Lateral High Row", "Plate-Loaded")
        addMachine("machine_hammer_iso_low_row", "Hammer Strength", "Iso-Lateral Low Row", "Plate-Loaded")
        addMachine("machine_hammer_iso_dy_row", "Hammer Strength", "Iso-Lateral D.Y. Row", "Plate-Loaded")
        addMachine("machine_hammer_iso_shoulder_press", "Hammer Strength", "Iso-Lateral Shoulder Press", "Plate-Loaded")
        addMachine("machine_hammer_ground_base_jammer", "Hammer Strength", "Ground Base Jammer", "Plate-Loaded")
        addMachine("machine_hammer_linear_hack_squat", "Hammer Strength", "Linear Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_hammer_iso_leg_press", "Hammer Strength", "Iso-Lateral Leg Press", "Plate-Loaded", 10f)
        addMachine("machine_hammer_v_squat", "Hammer Strength", "V-Squat Machine", "Plate-Loaded", 5f)
        addMachine("machine_hammer_seated_bicep", "Hammer Strength", "Iso-Lateral Seated Bicep Curl", "Plate-Loaded")
        addMachine("machine_hammer_dip_machine", "Hammer Strength", "Iso-Lateral Dip Machine", "Plate-Loaded")
        addMachine("machine_hammer_seated_calf", "Hammer Strength", "Plate Loaded Seated Calf", "Plate-Loaded")
        addMachine("machine_hammer_shrug_machine", "Hammer Strength", "Plate Loaded Shrug / Deadlift", "Plate-Loaded")
        addMachine("machine_hammer_mts_incline", "Hammer Strength", "MTS Iso-Lateral Incline Press", "Selectorized")
        addMachine("machine_hammer_mts_row", "Hammer Strength", "MTS Iso-Lateral High Row", "Selectorized")
        addMachine("machine_hammer_mts_shoulder", "Hammer Strength", "MTS Iso-Lateral Shoulder Press", "Selectorized")
        addMachine("machine_hammer_mts_curl", "Hammer Strength", "MTS Biceps Curl", "Selectorized")
        addMachine("machine_hammer_mts_triceps", "Hammer Strength", "MTS Triceps Extension", "Selectorized")
        addMachine("machine_hammer_smith_machine", "Hammer Strength", "HD Elite Smith Machine", "Smith")
        addMachine("machine_hammer_olympic_bench", "Hammer Strength", "Olympic Flat Bench", "Free Weight")
        addMachine("machine_hammer_olympic_incline", "Hammer Strength", "Olympic Incline Bench", "Free Weight")

        // --- PRIME FITNESS (18 models) ---
        addMachine("machine_prime_incline_press", "Prime", "Smart Strength Incline Press", "Plate-Loaded")
        addMachine("machine_prime_flat_bench", "Prime", "Smart Strength Flat Bench", "Plate-Loaded")
        addMachine("machine_prime_decline_press", "Prime", "Smart Strength Decline Press", "Plate-Loaded")
        addMachine("machine_prime_single_arm_row", "Prime", "Smart Strength Single Arm Row", "Plate-Loaded")
        addMachine("machine_prime_extreme_row", "Prime", "Smart Strength Extreme Row", "Plate-Loaded")
        addMachine("machine_prime_lat_pulldown", "Prime", "Smart Strength Lat Pulldown", "Plate-Loaded")
        addMachine("machine_prime_overhead_press", "Prime", "Smart Strength Overhead Press", "Plate-Loaded")
        addMachine("machine_prime_lateral_raise", "Prime", "Smart Strength Lateral Raise", "Plate-Loaded")
        addMachine("machine_prime_seated_leg_curl", "Prime", "Smart Strength Seated Leg Curl", "Plate-Loaded")
        addMachine("machine_prime_leg_extension", "Prime", "Smart Strength Leg Extension", "Plate-Loaded")
        addMachine("machine_prime_preacher_curl", "Prime", "Smart Strength Preacher Curl", "Plate-Loaded")
        addMachine("machine_prime_triceps_dip", "Prime", "Smart Strength Triceps Dip", "Plate-Loaded")
        addMachine("machine_prime_selector_chest", "Prime", "H-Select Chest Press", "Selectorized")
        addMachine("machine_prime_selector_pulldown", "Prime", "H-Select Lat Pulldown", "Selectorized")
        addMachine("machine_prime_selector_row", "Prime", "H-Select Seated Row", "Selectorized")
        addMachine("machine_prime_selector_curl", "Prime", "H-Select Arm Curl", "Selectorized")
        addMachine("machine_prime_cable_column", "Prime", "Single Stack Functional Trainer", "Cable")
        addMachine("machine_prime_squat_stand", "Prime", "Pro Rack System", "Free Weight")

        // --- ARSENAL STRENGTH (18 models) ---
        addMachine("machine_arsenal_reloaded_tbar", "Arsenal Strength", "Reloaded Chest Supported T-Bar Row", "Plate-Loaded")
        addMachine("machine_arsenal_reloaded_incline", "Arsenal Strength", "Reloaded Incline Chest Press", "Plate-Loaded")
        addMachine("machine_arsenal_reloaded_flat", "Arsenal Strength", "Reloaded Flat Chest Press", "Plate-Loaded")
        addMachine("machine_arsenal_reloaded_shoulder", "Arsenal Strength", "Reloaded Shoulder Press", "Plate-Loaded")
        addMachine("machine_arsenal_reloaded_hack", "Arsenal Strength", "Reloaded Linear Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_arsenal_pendulum_squat", "Arsenal Strength", "Reloaded Pendulum Squat", "Plate-Loaded", 5f)
        addMachine("machine_arsenal_reloaded_leg_press", "Arsenal Strength", "Reloaded 45° Leg Press", "Plate-Loaded", 10f)
        addMachine("machine_arsenal_vertical_leg_press", "Arsenal Strength", "Reloaded Vertical Leg Press", "Plate-Loaded", 10f)
        addMachine("machine_arsenal_bilateral_leg_ext", "Arsenal Strength", "Alpha Bilateral Leg Extension", "Selectorized")
        addMachine("machine_arsenal_bilateral_leg_curl", "Arsenal Strength", "Alpha Bilateral Seated Leg Curl", "Selectorized")
        addMachine("machine_arsenal_alpha_lat_pulldown", "Arsenal Strength", "Alpha Lat Pulldown", "Selectorized")
        addMachine("machine_arsenal_alpha_lateral_raise", "Arsenal Strength", "Alpha Lateral Raise", "Selectorized")
        addMachine("machine_arsenal_reloaded_rear_delt", "Arsenal Strength", "Reloaded Rear Delt Fly", "Plate-Loaded")
        addMachine("machine_arsenal_reloaded_preacher", "Arsenal Strength", "Reloaded Preacher Curl", "Plate-Loaded")
        addMachine("machine_arsenal_standing_calf", "Arsenal Strength", "Alpha Standing Calf", "Selectorized", 5f)
        addMachine("machine_arsenal_pit_shark_belt", "Arsenal Strength", "Pit Shark Belt Squat", "Plate-Loaded")
        addMachine("machine_arsenal_smith_machine", "Arsenal Strength", "Alpha Smith Machine", "Smith")
        addMachine("machine_arsenal_power_rack", "Arsenal Strength", "Alpha Power Rack 8x8", "Free Weight")

        // --- CYBEX (16 models) ---
        addMachine("machine_cybex_linear_hack_squat", "Cybex", "Linear Hack Squat (Classic Cam)", "Plate-Loaded", 5f)
        addMachine("machine_cybex_plate_loaded_squat_press", "Cybex", "Plate Loaded Squat Press", "Plate-Loaded", 5f)
        addMachine("machine_cybex_vr3_chest_press", "Cybex", "VR3 Chest Press", "Selectorized")
        addMachine("machine_cybex_vr3_overhead_press", "Cybex", "VR3 Overhead Shoulder Press", "Selectorized")
        addMachine("machine_cybex_eagle_lat_pulldown", "Cybex", "Eagle NX Lat Pulldown Dual Swivel", "Selectorized")
        addMachine("machine_cybex_eagle_seated_row", "Cybex", "Eagle NX Seated Row", "Selectorized")
        addMachine("machine_cybex_eagle_leg_extension", "Cybex", "Eagle NX Leg Extension Range Limiter", "Selectorized")
        addMachine("machine_cybex_eagle_seated_leg_curl", "Cybex", "Eagle NX Seated Leg Curl", "Selectorized")
        addMachine("machine_cybex_eagle_prone_leg_curl", "Cybex", "Eagle NX Prone Lying Leg Curl", "Selectorized")
        addMachine("machine_cybex_vr3_lateral_raise", "Cybex", "VR3 Lateral Raise", "Selectorized")
        addMachine("machine_cybex_vr3_arm_curl", "Cybex", "VR3 Arm Curl", "Selectorized")
        addMachine("machine_cybex_vr3_arm_extension", "Cybex", "VR3 Arm Extension", "Selectorized")
        addMachine("machine_cybex_standing_calf", "Cybex", "VR3 Standing Calf Raise", "Selectorized", 5f)
        addMachine("machine_cybex_donkey_calf", "Cybex", "Classic Donkey Calf Machine", "Selectorized", 5f)
        addMachine("machine_cybex_rotary_hip", "Cybex", "VR3 Rotary Hip", "Selectorized")
        addMachine("machine_cybex_bravo_trainer", "Cybex", "Bravo Functional Trainer", "Cable")

        // --- TECHNOGYM (18 models) ---
        addMachine("machine_technogym_pure_chest", "Technogym", "Pure Strength Chest Press", "Plate-Loaded")
        addMachine("machine_technogym_pure_incline", "Technogym", "Pure Strength Incline Chest Press", "Plate-Loaded")
        addMachine("machine_technogym_pure_wide_chest", "Technogym", "Pure Strength Wide Chest Press", "Plate-Loaded")
        addMachine("machine_technogym_pure_pulldown", "Technogym", "Pure Strength Lat Pulldown", "Plate-Loaded")
        addMachine("machine_technogym_pure_low_row", "Technogym", "Pure Strength Low Row", "Plate-Loaded")
        addMachine("machine_technogym_pure_shoulder", "Technogym", "Pure Strength Shoulder Press", "Plate-Loaded")
        addMachine("machine_technogym_pure_hack_squat", "Technogym", "Pure Strength Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_technogym_pure_leg_press", "Technogym", "Pure Strength Leg Press 45°", "Plate-Loaded", 10f)
        addMachine("machine_technogym_selection_leg_ext", "Technogym", "Selection 900 Leg Extension", "Selectorized")
        addMachine("machine_technogym_selection_leg_curl", "Technogym", "Selection 900 Seated Leg Curl", "Selectorized")
        addMachine("machine_technogym_selection_lying_curl", "Technogym", "Selection 900 Lying Leg Curl", "Selectorized")
        addMachine("machine_technogym_selection_standing_calf", "Technogym", "Selection 900 Standing Calf", "Selectorized")
        addMachine("machine_technogym_selection_delts", "Technogym", "Selection 900 Delts Machine", "Selectorized")
        addMachine("machine_technogym_selection_biceps", "Technogym", "Selection 900 Arm Curl", "Selectorized")
        addMachine("machine_technogym_selection_triceps", "Technogym", "Selection 900 Arm Extension", "Selectorized")
        addMachine("machine_technogym_cable_station", "Technogym", "Dual Adjustable Pulley", "Cable")
        addMachine("machine_technogym_multipower", "Technogym", "Multipower Smith Machine", "Smith")
        addMachine("machine_technogym_olympic_incline", "Technogym", "Pure Olympic Incline Bench", "Free Weight")

        // --- NAUTILUS & ATLANTIS (20 models) ---
        addMachine("machine_nautilus_nitro_incline", "Nautilus", "Nitro Plus Incline Press", "Selectorized")
        addMachine("machine_nautilus_nitro_chest", "Nautilus", "Nitro Plus Compound Chest", "Selectorized")
        addMachine("machine_nautilus_nitro_compound_row", "Nautilus", "Nitro Plus Compound Row", "Selectorized")
        addMachine("machine_nautilus_xpload_lat", "Nautilus", "Xpload Front Lat Pulldown", "Plate-Loaded")
        addMachine("machine_nautilus_freedom_trainer", "Nautilus", "Freedom Trainer Dual Pulley", "Cable")
        addMachine("machine_nautilus_glute_drive", "Nautilus", "Plate Loaded Glute Drive", "Plate-Loaded")
        addMachine("machine_nautilus_nitro_leg_curl", "Nautilus", "Nitro Plus Seated Leg Curl", "Selectorized")
        addMachine("machine_nautilus_nitro_leg_ext", "Nautilus", "Nitro Plus Leg Extension", "Selectorized")
        addMachine("machine_atlantis_pendulum_squat", "Atlantis", "Pendulum Squat Pro", "Plate-Loaded", 5f)
        addMachine("machine_atlantis_hack_squat", "Atlantis", "45° Linear Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_atlantis_incline_chest", "Atlantis", "Incline Chest Press Plate Loaded", "Plate-Loaded")
        addMachine("machine_atlantis_lat_pulldown", "Atlantis", "Heavy Duty Lat Pulldown 300lb", "Selectorized")
        addMachine("machine_atlantis_low_row", "Atlantis", "Chest Supported Row Double Handle", "Selectorized")
        addMachine("machine_atlantis_preacher_curl", "Atlantis", "Horizon Bicep Preacher", "Selectorized")
        addMachine("machine_atlantis_lateral_raise", "Atlantis", "Standing Lateral Raise Pivot", "Selectorized")
        addMachine("machine_atlantis_standing_calf", "Atlantis", "Standing Calf Block Unit", "Selectorized")
        addMachine("machine_atlantis_lying_leg_curl", "Atlantis", "Prone Leg Curl with Angled Bench", "Selectorized")
        addMachine("machine_atlantis_kneeling_leg_curl", "Atlantis", "Kneeling Unilateral Leg Curl", "Selectorized")
        addMachine("machine_atlantis_smith", "Atlantis", "Counterbalanced Smith Machine", "Smith")
        addMachine("machine_atlantis_hip_thrust", "Atlantis", "Commercial Hip Thrust Station", "Plate-Loaded")

        // --- GYMLECO & LIFE FITNESS (22 models) ---
        addMachine("machine_gymleco_010_chest", "Gymleco", "010 Iso-Lateral Chest Press", "Plate-Loaded")
        addMachine("machine_gymleco_012_incline", "Gymleco", "012 Iso-Lateral Incline Press", "Plate-Loaded")
        addMachine("machine_gymleco_021_low_row", "Gymleco", "021 Iso-Lateral Low Row", "Plate-Loaded")
        addMachine("machine_gymleco_031_lat_pull", "Gymleco", "031 Iso-Lateral Lat Pulldown", "Plate-Loaded")
        addMachine("machine_gymleco_043_hack_squat", "Gymleco", "043 Compact Hack Squat", "Plate-Loaded", 5f)
        addMachine("machine_gymleco_044_leg_press", "Gymleco", "044 Heavy Leg Press", "Plate-Loaded", 10f)
        addMachine("machine_gymleco_065_preacher", "Gymleco", "065 Plate Loaded Preacher Curl", "Plate-Loaded")
        addMachine("machine_gymleco_lateral_raise", "Gymleco", "Standing Lateral Raise", "Selectorized")
        addMachine("machine_gymleco_seated_calf", "Gymleco", "Compact Seated Calf", "Plate-Loaded")
        addMachine("machine_gymleco_standing_calf", "Gymleco", "055 Standing Calf Raise", "Selectorized", 5f)
        addMachine("machine_lifefitness_sig_incline", "Life Fitness", "Signature Series Incline Press", "Selectorized")
        addMachine("machine_lifefitness_sig_chest", "Life Fitness", "Signature Series Chest Press", "Selectorized")
        addMachine("machine_lifefitness_sig_pulldown", "Life Fitness", "Signature Series Lat Pulldown", "Selectorized")
        addMachine("machine_lifefitness_sig_row", "Life Fitness", "Signature Series Row", "Selectorized")
        addMachine("machine_lifefitness_sig_shoulder", "Life Fitness", "Signature Series Shoulder Press", "Selectorized")
        addMachine("machine_lifefitness_sig_leg_ext", "Life Fitness", "Signature Series Leg Extension", "Selectorized")
        addMachine("machine_lifefitness_sig_leg_curl", "Life Fitness", "Signature Series Seated Leg Curl", "Selectorized")
        addMachine("machine_lifefitness_sig_lying_curl", "Life Fitness", "Signature Series Lying Leg Curl", "Selectorized")
        addMachine("machine_lifefitness_cable_motion_dap", "Life Fitness", "Cable Motion Dual Adjustable Pulley", "Cable")
        addMachine("machine_lifefitness_smith", "Life Fitness", "Signature Series Smith Machine", "Smith")
        addMachine("machine_lifefitness_bicep_curl", "Life Fitness", "Signature Series Biceps Curl", "Selectorized")
        addMachine("machine_lifefitness_tricep_press", "Life Fitness", "Signature Series Triceps Press", "Selectorized")

        // --- ELEIKO & CANONICAL FREE WEIGHT STATIONS (18 models) ---
        addMachine("machine_eleiko_olympic_bench", "Eleiko", "IPF Competition Flat Bench", "Free Weight")
        addMachine("machine_eleiko_olympic_incline", "Eleiko", "Competition Incline Bench", "Free Weight")
        addMachine("machine_eleiko_power_rack", "Eleiko", "Prestige Half Rack", "Free Weight")
        addMachine("machine_eleiko_deadlift_platform", "Eleiko", "Classic Deadlift Platform", "Free Weight")
        addMachine("machine_dumbbell_incline_station", "Generic / Free Weight", "Adjustable Incline Dumbbell Bench (30°–45°)", "Free Weight")
        addMachine("machine_dumbbell_flat_station", "Generic / Free Weight", "Heavy Duty Flat Dumbbell Bench", "Free Weight")
        addMachine("machine_dumbbell_spider_station", "Generic / Free Weight", "Prone Spider Curl Bench", "Free Weight")
        addMachine("machine_standing_cable_stack", "Generic / Cable", "Single Adjustable Cable Column", "Cable")
        addMachine("machine_dual_cable_cross", "Generic / Cable", "Dual Stack Cable Crossover", "Cable")
        addMachine("machine_hyperextension_45", "Generic / Leverage", "45° Hyperextension Roman Bench", "Free Weight")
        addMachine("machine_glute_ham_developer", "Generic / Leverage", "GHD Glute-Ham Developer", "Free Weight")
        addMachine("machine_preacher_ez_bench", "Generic / Free Weight", "Olympic EZ-Bar Preacher Bench", "Free Weight")
        addMachine("machine_vertical_smith", "Generic / Smith", "Straight Vertical Smith Machine", "Smith")
        addMachine("machine_angled_smith_7deg", "Generic / Smith", "7° Angled Smith Machine", "Smith")
        addMachine("machine_dip_chin_station", "Generic / Leverage", "Power Tower Dip & Pullup Station", "Free Weight")
        addMachine("machine_leg_press_vertical", "Generic / Plate Loaded", "90° Vertical Leg Press", "Plate-Loaded", 10f)
        addMachine("machine_tbar_row_freeweight", "Generic / Free Weight", "Landmine T-Bar Platform", "Free Weight")
        addMachine("machine_ab_crunch_bench", "Generic / Leverage", "Decline Ab Crunch Bench", "Free Weight")

        list
    }

    // --- 124 CANONICAL EXERCISES (The Reference Library) ---
    val canonicalExercises: List<Exercise> by lazy {
        val list = mutableListOf<Exercise>()

        fun addEx(
            stableId: String,
            name: String,
            baseName: String,
            mGroup: String,
            pattern: String,
            secMuscles: String = "",
            mfg: String = "Panatta",
            repMin: Int = 10,
            repMax: Int = 12,
            rir: Int = 0,
            sets: Int = 2,
            restSec: Int = 180,
            incKg: Float = 2.5f,
            cues: String = "",
            seat: String = "",
            handle: String = ""
        ) {
            val nextId = (list.size + 1).toLong()
            list.add(
                Exercise(
                    id = nextId,
                    stableId = stableId,
                    name = name,
                    baseName = baseName,
                    manufacturer = mfg,
                    muscleGroup = mGroup,
                    secondaryMuscles = secMuscles,
                    movementPattern = pattern,
                    defaultRepMin = repMin,
                    defaultRepMax = repMax,
                    defaultRir = rir,
                    defaultWorkSets = sets,
                    defaultRestSeconds = restSec,
                    defaultIncrementKg = incKg,
                    executionCues = cues,
                    seatPosition = seat,
                    handlePosition = handle,
                    source = "bundled"
                )
            )
        }

        // CHEST (12 exercises)
        addEx("exercise_incline_chest_press", "Incline Machine Press — Panatta", "Incline Chest Press", "Chest", "Incline Press", "Front Delts, Triceps", "Panatta", 10, 12, 0, 2, 180, 2.5f, "• Controlled 3-sec eccentric\n• Chest stays elevated throughout\n• Do not bounce at bottom turnaround\n• Drive elbows inward into pec contraction", "Seat 4", "Wide neutral")
        addEx("exercise_flat_chest_press", "Horizontal Chest Press — Hammer Strength", "Flat Chest Press", "Chest", "Horizontal Press", "Triceps", "Hammer Strength", 10, 12, 0, 2, 180, 2.5f, "• Scapulae retracted and depressed into pad\n• Full stretch without shoulder anterior glide\n• Explosive intent on concentric", "Seat 3", "Mid-grip")
        addEx("exercise_pec_deck_fly", "Pec Deck Machine Fly — Panatta", "Pec Deck Fly", "Chest", "Fly / Adduction", "Front Delts", "Panatta", 12, 15, 0, 2, 120, 2.5f, "• Soft elbow bend locked\n• Squeeze pec cleavage at peak contraction", "Seat 3", "Padded handles")
        addEx("exercise_cable_chest_fly", "Cable Fly — Dual Low-to-High", "Cable Chest Fly", "Chest", "Fly / Adduction", "Front Delts", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Slight forward torso tilt\n• Hug an iron barrel at peak squeeze\n• 1-sec peak isometric hold", "Standing", "D-handles low pulley")
        addEx("exercise_dumbbell_incline_press", "Incline Dumbbell Press (30°)", "Incline Dumbbell Press", "Chest", "Incline Press", "Front Delts, Triceps", "Free Weights", 8, 10, 0, 2, 180, 2f, "• 30-degree incline angle\n• Deep stretch at bottom without elbow flaring", "Bench 30°", "Dumbbells strapped")
        addEx("exercise_dumbbell_flat_press", "Flat Dumbbell Press", "Flat Dumbbell Press", "Chest", "Horizontal Press", "Triceps", "Free Weights", 8, 10, 0, 2, 180, 2f, "• Tuck elbows 45 degrees\n• Retract scapulae firmly against bench", "Flat bench", "Dumbbells")
        addEx("exercise_smith_incline_press", "Smith Machine Incline Press (30°)", "Smith Incline Press", "Chest", "Incline Press", "Triceps, Front Delts", "Smith", 8, 10, 0, 2, 180, 2.5f, "• Lower bar right below collarbones\n• Continuous tension, no lockout rest", "Bench 30°", "Pronated wide")
        addEx("exercise_converging_chest_press", "Converging Chest Press — Prime", "Converging Chest Press", "Chest", "Horizontal Press", "Triceps", "Prime", 10, 12, 0, 2, 150, 2.5f, "• Prime torque cam set to peak squeeze\n• Drive wrists inward along convergence track", "Seat 2", "Neutral handles")
        addEx("exercise_dips_chest", "Chest Dip — Bodyweight / Weighted", "Chest Dip", "Chest", "Press / Dip", "Triceps, Front Delts", "Free Weights", 10, 12, 0, 2, 180, 5f, "• Torso angled 30 degrees forward\n• Deep stretch without shoulder impinging", "Dip bars", "Wide flare")
        addEx("exercise_cable_crossover_high", "Cable Crossover — High to Low", "Cable Crossover", "Chest", "Fly / Adduction", "Lower Pecs", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Drive hands down towards hip bones\n• Cross hands slightly for max lower pec contraction", "Standing split stance", "High pulleys")
        addEx("exercise_decline_hammer_press", "Decline Chest Press — Hammer Strength", "Decline Chest Press", "Chest", "Decline Press", "Triceps", "Hammer Strength", 10, 12, 0, 2, 150, 2.5f, "• Protect shoulder capsules\n• Direct sternal pec overload", "Seat 4", "Neutral handles")
        addEx("exercise_barbell_bench_press", "Barbell Bench Press — Eleiko", "Barbell Bench Press", "Chest", "Horizontal Press", "Triceps, Front Delts", "Eleiko", 6, 8, 1, 3, 240, 2.5f, "• Leg drive anchored into floor\n• Touch sternum, press in slight J-curve", "Flat competition bench", "Medium grip")

        // LATS (9 exercises)
        addEx("exercise_lat_pulldown_neutral", "Neutral Grip Lat Pulldown — Nautilus", "Lat Pulldown", "Back", "Vertical Pull", "Biceps, Rear Delts", "Nautilus", 10, 12, 0, 2, 180, 2.5f, "• Drive elbows downward into back pockets\n• Minimal torso swing, lats under continuous tension", "Pad 3 locked", "MAG close grip")
        addEx("exercise_single_arm_high_cable_lat_row", "Single Arm High Cable Lat Row", "Single Arm Lat Row", "Back", "Horizontal Pull", "Lats", "Cable", 10, 12, 1, 2, 120, 2.5f, "• Pull to hip crease\n• Keep forearm in line with cable vector", "Bench supported", "Single D-handle")
        addEx("exercise_lat_prayer_pullover", "Cable Lat Prayer / Straight Arm Pulldown", "Straight Arm Pulldown", "Back", "Pullover", "Teres Major", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Hips back, torso 45°\n• Sweep lats with straight arms without bending elbows", "Standing", "Rope or EZ attachment")
        addEx("exercise_panatta_super_pulldown", "Super Pulldown Circular — Panatta", "Circular Lat Pulldown", "Back", "Vertical Pull", "Biceps", "Panatta", 10, 12, 0, 2, 180, 2.5f, "• Anatomical circular motion path\n• Unbelievable peak stretch at top", "Seat 3", "Rotating handles")
        addEx("exercise_hammer_front_lat_pulldown", "Iso-Lateral Front Lat Pulldown — Hammer", "Hammer Front Pulldown", "Back", "Vertical Pull", "Biceps", "Hammer Strength", 10, 12, 0, 2, 180, 2.5f, "• Underhand / neutral option\n• Unilateral execution allowed", "Thigh pad locked", "Iso-arms")
        addEx("exercise_prime_single_arm_row", "Prime Single Arm Lat Row", "Prime Lat Row", "Back", "Horizontal Pull", "Lats, Teres", "Prime", 10, 12, 0, 2, 150, 2.5f, "• Overload middle / lengthened range with cam setting 1", "Chest pad 2", "Single arm handle")
        addEx("exercise_underhand_lat_pulldown", "Underhand Supinated Lat Pulldown", "Underhand Pulldown", "Back", "Vertical Pull", "Biceps", "Cable", 10, 12, 0, 2, 150, 2.5f, "• Shoulder width supinated grip\n• Lead with chest high, drive elbows back", "Pad locked", "Straight revolving bar")
        addEx("exercise_dumbbell_lat_row", "Single Arm Dumbbell Row to Hip", "Dumbbell Lat Row", "Back", "Horizontal Pull", "Biceps", "Free Weights", 8, 10, 1, 2, 180, 2f, "• Pull arc towards hip pocket\n• Do not rotate thoracic spine excessively", "Knee on flat bench", "Dumbbell")
        addEx("exercise_kayak_row", "Kayak Cable Lat Row", "Kayak Cable Row", "Back", "Horizontal Pull", "Lats", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Unilateral continuous lat sweep", "Standing", "Single handle")

        // UPPER & MID BACK (11 exercises)
        addEx("exercise_chest_supported_tbar_row", "Chest-Supported T-Bar Row — Arsenal", "T-Bar Row", "Back", "Horizontal Pull", "Rhomboids, Mid Traps", "Arsenal Strength", 8, 10, 0, 2, 180, 5f, "• Sternum glues to the pad\n• Protraction at bottom, deep scapular squeeze at top", "Chest pad 2", "Pronated wide")
        addEx("exercise_seated_cable_row_wide", "Seated Cable Row — Wide Neutral MAG", "Seated Cable Row", "Back", "Horizontal Pull", "Upper Back, Rear Delts", "Cable", 10, 12, 0, 2, 150, 2.5f, "• Retract and pull into sternum\n• 1-sec hold at peak contraction", "Feet on footplates", "Wide MAG grip")
        addEx("exercise_hammer_dy_row", "Hammer Strength Iso-Lateral D.Y. Row", "D.Y. Row", "Back", "Horizontal Pull", "Mid Back, Lats", "Hammer Strength", 8, 10, 0, 2, 180, 2.5f, "• Dorian Yates designed path\n• Low pulling angle hitting lower lat origin", "Seat 2", "Underhand grip")
        addEx("exercise_hammer_high_row", "Hammer Strength Iso-Lateral High Row", "High Row", "Back", "High Row", "Rear Delts, Mid Traps", "Hammer Strength", 10, 12, 0, 2, 180, 2.5f, "• High to low pull\n• Elbows flare slightly at 45°", "Chest pad 3", "Overhand handles")
        addEx("exercise_barbell_bent_over_row", "Barbell Bent-Over Row (Yates Grip)", "Barbell Row", "Back", "Horizontal Pull", "Lower Back, Rhomboids", "Eleiko", 8, 10, 1, 2, 210, 5f, "• 70° torso angle, core braced\n• Pull to belly button", "Standing", "Supinated medium grip")
        addEx("exercise_meadows_row", "Meadows Row (Landmine)", "Meadows Row", "Back", "Horizontal Pull", "Upper Back, Lats", "Free Weights", 10, 12, 0, 2, 150, 2.5f, "• Hand placed over end of bar\n• Flare elbow out for lat & rear upper back overload", "Staggered stance", "Landmine collar")
        addEx("exercise_panatta_pulley_row", "Panatta Dual Stack Row", "Panatta Seated Row", "Back", "Horizontal Pull", "Mid Traps", "Panatta", 10, 12, 0, 2, 150, 2.5f, "• Dual handles allow natural convergent pull", "Bench 2", "Dual rotating handles")
        addEx("exercise_face_pull_rope", "Cable Face Pull — High Pulley with Rope", "Cable Face Pull", "Back", "Face Pull", "Rear Delts, External Rotators", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Pull thumbs towards ears\n• Rotate forearms backwards at finish", "Standing split stance", "Long dual rope")
        addEx("exercise_dumbbell_rear_shrug", "Incline Dumbbell Shrug / Kelso Shrug", "Kelso Shrug", "Back", "Scapular Retraction", "Mid & Lower Traps", "Free Weights", 12, 15, 0, 2, 120, 2f, "• Chest supported on 45° bench\n• Pure scapular retraction without elbow bend", "Bench 45°", "Dumbbells")
        addEx("exercise_rack_pull_mid_shin", "Rack Pull below Knees — Heavy", "Rack Pull", "Back", "Hinge / Pull", "Entire Posterior Chain", "Free Weights", 5, 8, 1, 2, 240, 10f, "• Bar on pins at lower patella\n• Pure back thickness builder", "Power rack pins", "Double overhand strapped")
        addEx("exercise_machine_shrug", "Hammer Strength Shrug Machine", "Machine Shrug", "Back", "Elevation", "Upper Traps", "Hammer Strength", 10, 12, 0, 2, 120, 5f, "• Straight elevation towards ears\n• No shoulder rolling", "Standing platform", "Neutral handles")

        // SPINAL ERECTORS / LOWER BACK (5 exercises)
        addEx("exercise_hyperextension_45_back", "45° Hyperextension — Lower Back", "45° Back Extension", "Back", "Hinge", "Glutes, Hamstrings", "Leverage", 12, 15, 0, 2, 120, 5f, "• Neutral spine, pivot strictly at hip joints", "Thigh pad aligned", "Bodyweight / Plate")
        addEx("exercise_romanian_deadlift_barbell", "Barbell Romanian Deadlift (RDL)", "Barbell RDL", "Hamstrings", "Hinge", "Glutes, Lower Back", "Eleiko", 8, 10, 1, 2, 210, 5f, "• Hips push backwards to rear wall\n• Bar slides down along thighs\n• Stop at hamstring stretch limit", "Standing", "Strapped overhand")
        addEx("exercise_stiff_legged_deadlift", "Stiff-Legged Deadlift from Deficit", "Stiff-Leg Deadlift", "Hamstrings", "Hinge", "Lower Back", "Free Weights", 8, 10, 1, 2, 210, 5f, "• Soft knees, high hip position\n• Maximum lengthened hamstring stretch", "Deficit platform", "Strapped barbell")
        addEx("exercise_good_morning", "Barbell Good Morning — Safety Bar", "Good Morning", "Back", "Hinge", "Hamstrings, Glutes", "Free Weights", 10, 12, 1, 2, 180, 2.5f, "• Safety squat bar on traps\n• Hip hinge backward with core locked", "Standing rack", "Safety squat bar")
        addEx("exercise_machine_back_extension", "Selectorized Back Extension Machine", "Machine Back Extension", "Back", "Extension", "Spinal Erectors", "Cybex", 12, 15, 0, 2, 120, 2.5f, "• Smooth controlled spinal extension", "Seat 2", "Back roller pad")

        // QUADS (10 exercises)
        addEx("exercise_linear_hack_squat_cybex", "Linear Hack Squat — Cybex / Panatta", "Hack Squat", "Quads", "Squat", "Glutes", "Cybex", 10, 12, 0, 2, 240, 5f, "• Feet low and shoulder-width on platform\n• Deep knee flexion tracking over toes\n• Smooth reversal out of hole without jerking", "Back pad neutral", "Safety handles")
        addEx("exercise_pendulum_squat_arsenal", "Pendulum Squat — Arsenal / Atlantis", "Pendulum Squat", "Quads", "Squat", "Glutes", "Arsenal Strength", 10, 12, 0, 2, 240, 5f, "• Counterbalance system creates massive quad tension\n• Deep squat into knee flexion", "Back support", "Shoulder pads")
        addEx("exercise_leg_press_45_panatta", "Super Leg Press 45° — Panatta", "45° Leg Press", "Quads", "Leg Press", "Glutes", "Panatta", 12, 15, 0, 2, 210, 10f, "• Feet lower half of sled\n• Knees track over 2nd toes, do not lift hips off pad", "Back angle 45°", "Handles gripped")
        addEx("exercise_leg_extension_prime", "Leg Extension — Prime Smart Strength", "Leg Extension", "Quads", "Extension", "Rectus Femoris", "Prime", 12, 15, 0, 2, 150, 2.5f, "• Hips jammed down into seat\n• 1-sec hard isometric at lockout\n• Final work set rest-pause", "Back 2, Shin pad 3", "Side handles pulled up")
        addEx("exercise_smith_machine_squat", "Smith Machine Close-Stance Squat", "Smith Squat", "Quads", "Squat", "Glutes", "Smith", 10, 12, 0, 2, 210, 5f, "• Feet slightly forward to isolate quads\n• Descend to parallel with upright torso", "Vertical track", "High bar position")
        addEx("exercise_v_squat_hammer", "V-Squat Machine — Hammer Strength", "V-Squat", "Quads", "Squat", "Glutes", "Hammer Strength", 10, 12, 0, 2, 210, 5f, "• Arc motion reduces spine torque\n• Deep quad loading", "Shoulder pads", "Release handles")
        addEx("exercise_bulgarian_split_squat", "Bulgarian Split Squat — Dumbbells", "Bulgarian Split Squat", "Quads", "Unilateral Squat", "Glutes", "Free Weights", 10, 12, 0, 2, 180, 2f, "• Rear foot elevated on bench\n• Push through front heel and midfoot", "Bench elevation", "Dumbbells strapped")
        addEx("exercise_sissy_squat_bench", "Sissy Squat Bench — Bodyweight & Weight", "Sissy Squat", "Quads", "Knee Extension", "Patellar Tendon", "Leverage", 12, 15, 0, 2, 120, 2.5f, "• Lean back into pure knee flexion\n• Extreme quad stretch", "Calf pad locked", "Holding plate")
        addEx("exercise_belt_squat_pit_shark", "Belt Squat — Pit Shark", "Belt Squat", "Quads", "Squat", "Glutes", "Arsenal Strength", 10, 12, 0, 2, 180, 5f, "• Zero spinal compression\n• Pure leg driving power", "Belt harness", "Front handrails")
        addEx("exercise_horizontal_leg_press", "Horizontal Leg Press — Technogym", "Horizontal Leg Press", "Quads", "Leg Press", "Glutes", "Technogym", 12, 15, 0, 2, 150, 5f, "• Pin-loaded constant tension stack", "Seat 3", "Footplate mid")

        // HAMSTRINGS (8 exercises)
        addEx("exercise_seated_leg_curl_panatta", "Seated Leg Curl — Panatta Monolith", "Seated Leg Curl", "Hamstrings", "Leg Curl", "Calves", "Panatta", 10, 12, 0, 3, 150, 2.5f, "• Thigh pad locked firmly against quads\n• Toes pointed dorsiflexed\n• 2-sec eccentric with hamstring stretch", "Backrest 3, Ankle pad 2", "Firm grip on handles")
        addEx("exercise_lying_leg_curl_panatta", "Lying Leg Curl — Panatta", "Lying Leg Curl", "Hamstrings", "Leg Curl", "Gastrocnemius", "Panatta", 10, 12, 0, 2, 150, 2.5f, "• Hips glued into pad\n• Do not arch lumbar spine to curl load", "Angled bench", "Roller on achilles")
        addEx("exercise_standing_leg_curl", "Standing Single-Leg Curl — Atlantis", "Standing Leg Curl", "Hamstrings", "Unilateral Curl", "Calves", "Atlantis", 10, 12, 0, 2, 120, 2.5f, "• Unilateral focus to eliminate asymmetry", "Hip pad locked", "Single leg roller")
        addEx("exercise_romanian_deadlift_dumbbells", "Romanian Deadlift (RDL) — Dumbbells", "Dumbbell RDL", "Hamstrings", "Hinge", "Glutes, Lower Back", "Free Weights", 8, 10, 1, 2, 180, 2f, "• Hips push backwards toward the wall\n• Knees soft but static angle\n• Stop when hamstrings reach maximum stretch limit", "Standing", "Strapped neutral grip")
        addEx("exercise_prime_seated_leg_curl", "Seated Leg Curl — Prime Smart Strength", "Prime Leg Curl", "Hamstrings", "Leg Curl", "Calves", "Prime", 10, 12, 0, 2, 150, 2.5f, "• Torque cam set to shortened range for peak cramp", "Seat 2", "Locked thigh bar")
        addEx("exercise_glute_ham_raise", "Glute Ham Raise (GHR)", "Glute Ham Raise", "Hamstrings", "Knee Flexion & Hinge", "Glutes", "Leverage", 8, 10, 0, 2, 180, 0f, "• Descend slowly under eccentric control", "Ankle rollers 3", "Crossed arms")
        addEx("exercise_kneeling_leg_curl", "Kneeling Leg Curl — Cybex", "Kneeling Leg Curl", "Hamstrings", "Unilateral Curl", "Calves", "Cybex", 10, 12, 0, 2, 120, 2.5f, "• Chest on pad, isolated knee flexion", "Chest support", "Roller pad")
        addEx("exercise_nordic_hamstring_curl", "Nordic Hamstring Curl", "Nordic Curl", "Hamstrings", "Eccentric Knee Flexion", "Calves", "Leverage", 6, 8, 0, 2, 180, 0f, "• Pure eccentric overload, push off floor on concentric", "Ankles hooked", "Hands ready to brake")

        // GLUTES (6 exercises)
        addEx("exercise_hip_thrust_barbell", "Barbell Hip Thrust — Bench Station", "Barbell Hip Thrust", "Glutes", "Bridge", "Hamstrings", "Free Weights", 10, 12, 0, 3, 180, 5f, "• Scapulae on bench edge\n• Full hip extension with posterior pelvic tilt\n• 1-sec hold at top", "Bench pad", "Barbell with squat pad")
        addEx("exercise_glute_drive_nautilus", "Glute Drive Machine — Nautilus", "Glute Drive", "Glutes", "Bridge", "Hamstrings", "Nautilus", 10, 12, 0, 2, 180, 5f, "• Safe belt loading directly across pelvis", "Padded belt", "Footplate flat")
        addEx("exercise_cable_glute_kickback", "Cable Glute Kickback — Ankle Strap", "Cable Kickback", "Glutes", "Kickback", "Hamstrings", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Kick straight back and slightly outward (30°)", "Standing slightly forward", "Ankle cuff")
        addEx("exercise_glute_abduction_machine", "Seated Hip Abduction Machine", "Hip Abduction", "Glutes", "Abduction", "Glute Medius", "Panatta", 15, 20, 0, 2, 90, 2.5f, "• Lean forward slightly from hips for glute medius bias", "Seat 2", "Knee pads outside")
        addEx("exercise_kas_glute_bridge", "Kas Glute Bridge — Dumbbell / Barbell", "Kas Glute Bridge", "Glutes", "Bridge", "Glute Max", "Free Weights", 12, 15, 0, 2, 150, 2.5f, "• Small movement arc strictly from hips to peak lockout", "Bench 2", "Barbell")
        addEx("exercise_hyperextension_glute_bias", "45° Hyperextension — Glute Biased", "Glute 45° Extension", "Glutes", "Hinge", "Hamstrings", "Leverage", 12, 15, 0, 2, 120, 5f, "• Round upper back, flare feet 45° outward", "Thigh pad low", "Plate hold")

        // CALVES (5 exercises)
        addEx("exercise_standing_calf_raise_panatta", "Standing Calf Raise — Panatta", "Standing Calf Raise", "Calves", "Plantarflextion", "Soleus, Gastrocnemius", "Panatta", 12, 15, 0, 3, 120, 5f, "• Full drop into Achilles stretch (2-sec pause)\n• Explode onto balls of big toes\n• 1-sec hard contraction at peak", "Shoulder pads 3", "Balls of feet on edge")
        addEx("exercise_seated_calf_raise", "Seated Calf Raise — Plate Loaded", "Seated Calf Raise", "Calves", "Plantarflextion", "Soleus", "Panatta", 15, 20, 0, 2, 90, 2.5f, "• Isolates soleus with knees bent 90°\n• Full stretch without bouncing", "Knee pad snug", "Toes on block")
        addEx("exercise_leg_press_calf_press", "Leg Press Calf Press (Toe Press)", "Leg Press Calf Press", "Calves", "Plantarflextion", "Gastrocnemius", "Panatta", 15, 20, 0, 2, 90, 5f, "• Lock knees slightly soft, pivot purely at ankle joint", "Sled safely unpinned", "Balls of feet on sled lip")
        addEx("exercise_donkey_calf_raise", "Donkey Calf Raise Machine", "Donkey Calf Raise", "Calves", "Plantarflextion", "Gastrocnemius", "Cybex", 12, 15, 0, 2, 90, 5f, "• 90° bent hip angle pre-stretches gastrocnemius", "Back pad over sacrum", "Toes on edge")
        addEx("exercise_single_leg_dumbbell_calf", "Single Leg Standing Calf Raise with Dumbbell", "Single Leg Calf Raise", "Calves", "Unilateral Plantar", "Soleus", "Free Weights", 15, 20, 0, 2, 90, 2f, "• Unilateral control, strict cadence", "Elevated block", "Dumbbell in same-side hand")

        // SIDE DELTS (6 exercises)
        addEx("exercise_lateral_raise_machine_panatta", "Lateral Deltoid Machine — Panatta", "Lateral Raise Machine", "Delts", "Abduction", "Traps", "Panatta", 12, 15, 0, 3, 120, 2.5f, "• Elbows against pads\n• Lead with lateral delts, not traps\n• Controlled descent", "Seat 3", "Elbow pads aligned")
        addEx("exercise_cable_lateral_raise_behind", "Behind-the-Back Cable Lateral Raise", "Behind-the-Back Cable Raise", "Delts", "Abduction", "Side Delts", "Cable", 12, 15, 0, 2, 90, 1.25f, "• Cable pulley at knee height\n• Smooth arc out to side", "Standing", "Single D-handle")
        addEx("exercise_dumbbell_lateral_raise", "Dumbbell Lateral Raise — Standing", "Dumbbell Lateral Raise", "Delts", "Abduction", "Side Delts", "Free Weights", 12, 15, 0, 2, 120, 2f, "• Slight forward lean\n• Pour water slightly at top with pinky high", "Standing", "Dumbbells")
        addEx("exercise_cable_y_raise", "Cable Y-Raise (Incline Bench Biased)", "Cable Y-Raise", "Delts", "Abduction", "Lower Traps", "Cable", 12, 15, 0, 2, 90, 1.25f, "• 30° arm path matching scapular plane\n• Continuous cable tension", "Chest against 60° bench", "Crossed cables")
        addEx("exercise_prime_lateral_raise", "Prime Smart Strength Lateral Raise", "Prime Lateral Raise", "Delts", "Abduction", "Side Delts", "Prime", 12, 15, 0, 2, 120, 2.5f, "• Torque cam overloaded at mid-point", "Seat 2", "Rotating elbow pads")
        addEx("exercise_incline_dumbbell_lateral_raise", "Incline Dumbbell Lateral Raise (Single Arm)", "Incline Single Lateral", "Delts", "Abduction", "Side Delts", "Free Weights", 12, 15, 0, 2, 90, 2f, "• Lay sideways against 45° bench\n• Peak stretch overload at bottom", "Side on bench 45°", "Single dumbbell")

        // FRONT DELTS (5 exercises)
        addEx("exercise_overhead_press_panatta", "Super Overhead Press — Panatta", "Overhead Machine Press", "Delts", "Vertical Press", "Triceps", "Panatta", 8, 10, 0, 2, 180, 2.5f, "• Convergence trajectory saves rotator cuff\n• Full range down to chin height", "Seat 4", "Neutral handles")
        addEx("exercise_hammer_shoulder_press", "Iso-Lateral Shoulder Press — Hammer Strength", "Hammer Shoulder Press", "Delts", "Vertical Press", "Triceps", "Hammer Strength", 8, 10, 0, 2, 180, 2.5f, "• Pure plate-loaded shoulder overload", "Seat 3", "Overhand grip")
        addEx("exercise_dumbbell_seated_shoulder_press", "Seated Dumbbell Shoulder Press", "Dumbbell Shoulder Press", "Delts", "Vertical Press", "Triceps", "Free Weights", 8, 10, 0, 2, 180, 2f, "• Backrest set to 80° (slight incline)\n• Lower until dumbbells tap shoulders", "Seat 80°", "Dumbbells strapped")
        addEx("exercise_smith_shoulder_press", "Smith Machine Seated Shoulder Press", "Smith Shoulder Press", "Delts", "Vertical Press", "Triceps", "Smith", 8, 10, 0, 2, 180, 2.5f, "• Press directly overhead to crown\n• No excessive back arching", "Bench 85°", "Pronated grip")
        addEx("exercise_cable_front_raise_rope", "Cable Front Raise with Rope", "Cable Front Raise", "Delts", "Flexion", "Upper Chest", "Cable", 12, 15, 0, 2, 90, 2.5f, "• Straddle cable low pulley\n• Pull rope to eye level", "Standing over cable", "Rope attachment")

        // REAR DELTS (5 exercises)
        addEx("exercise_reverse_pec_deck", "Reverse Pec Deck Fly — Panatta", "Reverse Pec Deck", "Delts", "Horizontal Abduction", "Mid Traps", "Panatta", 12, 15, 0, 2, 120, 2.5f, "• Chest flat against pad\n• Sweep arms outward like giant wings\n• Scapulae remain stabilized", "Seat 3", "Horizontal handles")
        addEx("exercise_cable_rear_delt_cross", "Cross-Body Cable Rear Delt Fly", "Cable Rear Delt Fly", "Delts", "Horizontal Abduction", "Rhomboids", "Cable", 12, 15, 0, 2, 120, 1.25f, "• Cables crossed at collarbone height\n• Extend straight out matching rear delt angle", "Standing", "Cable balls directly")
        addEx("exercise_dumbbell_incline_rear_delt", "Incline Dumbbell Rear Delt Fly (Prone)", "Incline Prone Rear Fly", "Delts", "Horizontal Abduction", "Upper Back", "Free Weights", 12, 15, 0, 2, 120, 2f, "• Prone on 30° bench\n• Flare dumbbells out wide with loose grip", "Bench 30° prone", "Light dumbbells")
        addEx("exercise_arsenal_reloaded_rear_delt", "Arsenal Reloaded Rear Delt Fly", "Arsenal Rear Delt", "Delts", "Horizontal Abduction", "Mid Traps", "Arsenal Strength", 12, 15, 0, 2, 120, 2.5f, "• Plate loaded arc path", "Chest pad 2", "Wide grips")
        addEx("exercise_face_pull_seated", "Seated Cable Rear Delt Face Pull", "Seated Face Pull", "Delts", "Face Pull", "Rotator Cuff", "Cable", 12, 15, 0, 2, 120, 2.5f, "• High row bench supported", "Low bench", "Rope attachment")

        // BICEPS (10 exercises)
        addEx("exercise_preacher_curl_prime", "Preacher Curl — Prime Plate Loaded", "Preacher Curl", "Biceps", "Elbow Flexion", "Forearms", "Prime", 10, 12, 0, 2, 120, 2.5f, "• Armpits flush on preacher pad\n• Cam setting #3 for overloaded peak stretch\n• Smooth turnaround, do not hyperextend elbows", "Seat 2", "Semi-supinated")
        addEx("exercise_cable_bicep_curl_dual", "Cable Bicep Curl — Dual Stack", "Cable Bicep Curl", "Biceps", "Elbow Flexion", "Brachialis", "Technogym", 10, 12, 0, 2, 120, 2.5f, "• Elbows pinned slightly in front of ribcage\n• Supinate hard at peak contraction", "Standing", "Straight revolving bar")
        addEx("exercise_incline_dumbbell_curl", "Incline Dumbbell Curl (45°)", "Incline Dumbbell Curl", "Biceps", "Elbow Flexion", "Brachialis", "Free Weights", 10, 12, 0, 2, 120, 2f, "• 45° incline bench stretches long head\n• Keep elbows back behind torso", "Bench 45°", "Dumbbells supinated")
        addEx("exercise_hammer_strength_preacher_curl", "Iso-Lateral Biceps Machine — Hammer", "Hammer Biceps Curl", "Biceps", "Elbow Flexion", "Brachialis", "Hammer Strength", 10, 12, 0, 2, 120, 2.5f, "• Pure plate-loaded bicep isolation", "Seat 3", "Rotating handles")
        addEx("exercise_spider_curl_dumbbell", "Spider Curl — Prone Incline Bench", "Spider Curl", "Biceps", "Elbow Flexion", "Short Head", "Free Weights", 10, 12, 0, 2, 120, 2f, "• Torso on 45° bench\n• Arms hang vertical, curl up to nose", "Bench 45° prone", "EZ-bar or dumbbells")
        addEx("exercise_standing_ez_bar_curl", "Standing Olympic EZ-Bar Bicep Curl", "EZ-Bar Curl", "Biceps", "Elbow Flexion", "Forearms", "Eleiko", 8, 10, 1, 2, 150, 2.5f, "• Strict form, elbows pinned to flanks\n• No hip thrust momentum", "Standing", "Semi-supinated inner grip")
        addEx("exercise_hammer_curl_dumbbells", "Standing Dumbbell Hammer Curl", "Dumbbell Hammer Curl", "Biceps", "Elbow Flexion", "Brachialis, Forearms", "Free Weights", 10, 12, 0, 2, 120, 2f, "• Neutral grip builds brachialis thickness and forearm fullness", "Standing", "Neutral grip dumbbells")
        addEx("exercise_cable_rope_hammer_curl", "Cable Rope Hammer Curl", "Cable Hammer Curl", "Biceps", "Elbow Flexion", "Brachialis", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Low pulley rope attachment\n• Flare rope outward at peak contraction", "Standing", "Rope attachment")
        addEx("exercise_concentration_curl", "Seated Dumbbell Concentration Curl", "Concentration Curl", "Biceps", "Elbow Flexion", "Bicep Peak", "Free Weights", 10, 12, 0, 2, 90, 2f, "• Elbow braced against inner thigh\n• Supinate aggressively at top", "Flat bench edge", "Single dumbbell")
        addEx("exercise_panatta_monolith_curl", "Panatta Monolith Arm Curl Machine", "Panatta Arm Curl", "Biceps", "Elbow Flexion", "Forearms", "Panatta", 10, 12, 0, 2, 120, 2.5f, "• Cam matched resistance curve", "Seat 2", "Padded handles")

        // TRICEPS (9 exercises)
        addEx("exercise_triceps_pushdown_rope", "Cable Triceps Pushdown (Rope)", "Cable Pushdown", "Triceps", "Extension", "Lateral Head", "Cable", 12, 15, 0, 2, 120, 2.5f, "• Elbows pinned to ribs\n• Flare rope split at bottom for lateral head cramp", "Standing upright", "Rope attachment")
        addEx("exercise_triceps_pushdown_straight_bar", "Cable Triceps Pushdown (Straight Bar)", "Straight Bar Pushdown", "Triceps", "Extension", "Medial & Lateral", "Cable", 10, 12, 0, 2, 120, 2.5f, "• Overhand grip, drive bar downwards with tricep press", "Standing", "Straight bar attachment")
        addEx("exercise_overhead_cable_triceps_ext", "Overhead Cable Triceps Extension", "Overhead Triceps Extension", "Triceps", "Extension", "Long Head", "Cable", 10, 12, 0, 2, 120, 2.5f, "• Cable at chest height, step forward\n• Deep stretch of the long head behind neck", "Standing forward lunge", "Dual rope handles")
        addEx("exercise_skull_crusher_ez_bar", "Incline EZ-Bar Skull Crusher", "EZ-Bar Skull Crusher", "Triceps", "Extension", "Long Head", "Free Weights", 10, 12, 0, 2, 150, 2.5f, "• Slight incline takes strain off elbows\n• Lower bar behind head, not forehead", "Bench 20°", "EZ curl bar")
        addEx("exercise_dip_machine_panatta", "Triceps Dip Machine — Panatta", "Dip Machine", "Triceps", "Dip Press", "Front Delts", "Panatta", 10, 12, 0, 2, 150, 5f, "• Torso upright to keep tension strictly on triceps", "Seat 3, Lap belt on", "Narrow handles")
        addEx("exercise_cross_body_cable_triceps", "Cross-Body Dual Cable Triceps Extension", "Cross-Body Triceps", "Triceps", "Extension", "Lateral & Long Head", "Cable", 12, 15, 0, 2, 90, 1.25f, "• Cables crossed high without handles\n• Extend downward and back matching arm angle", "Standing centered", "Cable balls")
        addEx("exercise_jm_press_smith", "Smith Machine JM Press", "Smith JM Press", "Triceps", "Hybrid Press / Ext", "Anterior Delts", "Smith", 8, 10, 0, 2, 180, 2.5f, "• Hybrid between close-grip bench and skull crusher\n• Lower bar toward chin/throat crease", "Flat bench under smith", "Narrow overhand")
        addEx("exercise_close_grip_bench_press", "Close-Grip Barbell Bench Press", "Close-Grip Bench", "Triceps", "Horizontal Press", "Chest", "Eleiko", 8, 10, 1, 2, 180, 2.5f, "• Hands shoulder-width (not too narrow)\n• Elbows tucked tightly to ribcage", "Flat bench", "Overhand shoulder width")
        addEx("exercise_french_press_panatta", "French Press Machine — Panatta", "Panatta French Press", "Triceps", "Overhead Extension", "Long Head", "Panatta", 10, 12, 0, 2, 120, 2.5f, "• Back support locked, overhead long head focus", "Seat 2", "Overhead revolving bar")

        // FOREARMS (3 exercises)
        addEx("exercise_cable_reverse_curl", "Reverse Cable Curl (EZ-Bar)", "Reverse Cable Curl", "Forearms", "Brachioradialis", "Biceps", "Cable", 12, 15, 0, 2, 90, 2.5f, "• Pronated grip strictly isolates brachioradialis", "Standing", "EZ attachment")
        addEx("exercise_wrist_curl_dumbbell", "Seated Wrist Curl over Bench", "Wrist Curl", "Forearms", "Wrist Flexion", "Forearm Flexors", "Free Weights", 15, 20, 0, 2, 60, 2f, "• Forearms on bench pad, curl wrists upward", "Kneeling beside bench", "Barbell / Dumbbell")
        addEx("exercise_farmers_walk", "Farmer's Walk Handles — Heavy Grip", "Farmer's Walk", "Forearms", "Carry / Grip", "Traps, Core", "Free Weights", 1, 1, 0, 3, 180, 10f, "• 40-meter heavy carry holding heavy iron", "Standing walkway", "Heavy handles")

        // ABS & CORE (5 exercises)
        addEx("exercise_cable_kneeling_crunch", "Cable Kneeling Rope Crunch", "Cable Crunch", "Abs", "Spinal Flexion", "Core", "Cable", 15, 20, 0, 3, 90, 2.5f, "• Anchor hips static, curl spine downward using abdominals", "Kneeling on mat", "Rope beside ears")
        addEx("exercise_hanging_leg_raise", "Hanging Leg Raise (Toes to Bar)", "Hanging Leg Raise", "Abs", "Hip / Spine Flexion", "Hip Flexors", "Free Weights", 12, 15, 0, 2, 90, 0f, "• Posterior tilt pelvis at top, curl knees to chest", "Hanging from pullup bar", "Overhand grip")
        addEx("exercise_panatta_rotary_torso", "Rotary Torso Machine — Panatta", "Rotary Torso", "Abs", "Rotation", "Obliques", "Panatta", 15, 20, 0, 2, 60, 2.5f, "• Strict rotation from obliques, hips locked", "Seat 3", "Chest pads")
        addEx("exercise_decline_bench_crunch", "Weighted Decline Bench Crunch", "Decline Crunch", "Abs", "Spinal Flexion", "Core", "Leverage", 15, 20, 0, 2, 90, 2.5f, "• Hold plate at chest, curl torso upward", "Decline bench", "Feet locked")
        addEx("exercise_ab_crunch_machine", "Ab Crunch Machine — Cybex", "Machine Crunch", "Abs", "Spinal Flexion", "Core", "Cybex", 15, 20, 0, 2, 90, 2.5f, "• Controlled eccentric stretch, hard squeeze", "Seat 2", "Shoulder straps")

        list
    }

    // --- 186 CANONICAL EXERCISE VARIANTS (Linking exercises to machines/setups) ---
    val canonicalExerciseVariants: List<ExerciseVariant> by lazy {
        val list = mutableListOf<ExerciseVariant>()

        fun addVar(
            id: String,
            exId: Long,
            exStableId: String,
            machId: String?,
            name: String,
            mfg: String,
            resType: String,
            incKg: Float = 2.5f,
            seat: String = "",
            handle: String = "",
            back: String = "",
            notes: String = ""
        ) {
            list.add(
                ExerciseVariant(
                    id = id,
                    exerciseId = exId,
                    exerciseStableId = exStableId,
                    machineCatalogId = machId,
                    variantName = name,
                    manufacturer = mfg,
                    resistanceType = resType,
                    weightIncrementKg = incKg,
                    defaultSeat = seat,
                    defaultHandle = handle,
                    defaultBackrest = back,
                    notes = notes,
                    source = "bundled"
                )
            )
        }

        val exMap = canonicalExercises.associateBy { it.stableId }

        fun getExId(stableId: String): Long = exMap[stableId]?.id ?: 1L

        // Incline Chest Press variants (Panatta, Hammer, Technogym, Smith, Dumbbell, Prime, Arsenal)
        addVar("var_incline_panatta", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_panatta_super_incline_chest", "Panatta Super Incline Press", "Panatta", "Plate-Loaded", 2.5f, "Seat 4", "Wide neutral", "Back pad 35°", "Cam resistance matched")
        addVar("var_incline_hammer", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_hammer_iso_incline", "Hammer Strength Iso-Lateral Incline", "Hammer Strength", "Plate-Loaded", 2.5f, "Seat 3", "Overhand handles", "", "Iso-lateral path")
        addVar("var_incline_technogym", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_technogym_pure_incline", "Technogym Pure Strength Incline", "Technogym", "Plate-Loaded", 2.5f, "Seat 3", "Converging grip", "", "Converging trajectory")
        addVar("var_incline_prime", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_prime_incline_press", "Prime Smart Strength Incline Press", "Prime", "Plate-Loaded", 2.5f, "Seat 2", "Multi-grip", "", "Adjustable cam settings")
        addVar("var_incline_arsenal", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_arsenal_reloaded_incline", "Arsenal Reloaded Incline Press", "Arsenal Strength", "Plate-Loaded", 2.5f, "Seat 3", "Heavy knurled handles", "", "Ultra heavy duty")
        addVar("var_incline_smith", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_hammer_smith_machine", "Smith Machine 30° Incline Press", "Smith", "Smith", 2.5f, "Bench 30°", "Pronated wide", "", "Fixed linear plane")
        addVar("var_incline_dumbbell", getExId("exercise_incline_chest_press"), "exercise_incline_chest_press", "machine_dumbbell_incline_station", "Dumbbell 30° Incline Press", "Free Weights", "Free Weight", 2f, "Bench 30°", "Strapped dumbbells", "", "Free range of motion")

        // Flat Chest Press variants
        addVar("var_flat_hammer", getExId("exercise_flat_chest_press"), "exercise_flat_chest_press", "machine_hammer_iso_bench", "Hammer Strength Iso Horizontal Bench", "Hammer Strength", "Plate-Loaded", 2.5f, "Seat 3", "Mid-grip", "", "Direct sternal pec path")
        addVar("var_flat_panatta", getExId("exercise_flat_chest_press"), "exercise_flat_chest_press", "machine_panatta_super_horizontal_bench", "Panatta Super Horizontal Bench", "Panatta", "Plate-Loaded", 2.5f, "Seat 4", "Wide grips", "", "Super stable converging track")
        addVar("var_flat_technogym", getExId("exercise_flat_chest_press"), "exercise_flat_chest_press", "machine_technogym_pure_chest", "Technogym Pure Strength Chest Press", "Technogym", "Plate-Loaded", 2.5f, "Seat 3", "Ergonomic handles", "", "Pure strength")
        addVar("var_flat_prime", getExId("exercise_flat_chest_press"), "exercise_flat_chest_press", "machine_prime_flat_bench", "Prime Smart Strength Flat Bench", "Prime", "Plate-Loaded", 2.5f, "Seat 2", "Neutral handles", "", "Adjustable torque")
        addVar("var_flat_barbell", getExId("exercise_flat_chest_press"), "exercise_flat_chest_press", "machine_eleiko_olympic_bench", "Olympic Barbell Bench Press", "Eleiko", "Free Weight", 2.5f, "Competition bench", "Medium overhand", "", "Powerlifting standard")

        // Pec Deck Fly variants
        addVar("var_pec_panatta", getExId("exercise_pec_deck_fly"), "exercise_pec_deck_fly", "machine_panatta_pec_deck_fly", "Panatta Pec Fly / Rear Delt Dual", "Panatta", "Selectorized", 2.5f, "Seat 3", "Padded handles", "", "Cam profile")
        addVar("var_pec_cybex", getExId("exercise_pec_deck_fly"), "exercise_pec_deck_fly", "machine_cybex_vr3_chest_press", "Cybex Fly Machine", "Cybex", "Selectorized", 2.5f, "Seat 2", "Vertical handles", "", "Smooth selectorized")
        addVar("var_pec_cable", getExId("exercise_pec_deck_fly"), "exercise_pec_deck_fly", "machine_dual_cable_cross", "Dual Cable Standing Fly", "Cable", "Cable", 1.25f, "Standing", "D-handles", "", "Constant cable tension")

        // Lat Pulldown variants
        addVar("var_lat_panatta", getExId("exercise_lat_pulldown_neutral"), "exercise_lat_pulldown_neutral", "machine_panatta_monolith_lat_pulldown", "Panatta Monolith Lat Pulldown", "Panatta", "Selectorized", 2.5f, "Pad 3", "MAG neutral grip", "", "Smooth pulley action")
        addVar("var_lat_hammer", getExId("exercise_lat_pulldown_neutral"), "exercise_lat_pulldown_neutral", "machine_hammer_iso_front_lat", "Hammer Strength Iso Front Lat", "Hammer Strength", "Plate-Loaded", 2.5f, "Pad locked", "Iso handles", "", "Plate loaded front arc")
        addVar("var_lat_prime", getExId("exercise_lat_pulldown_neutral"), "exercise_lat_pulldown_neutral", "machine_prime_lat_pulldown", "Prime Smart Strength Pulldown", "Prime", "Plate-Loaded", 2.5f, "Seat 2", "Dual rotating handles", "", "Cam setting 2")
        addVar("var_lat_nautilus", getExId("exercise_lat_pulldown_neutral"), "exercise_lat_pulldown_neutral", "machine_nautilus_xpload_lat", "Nautilus Xpload Front Lat", "Nautilus", "Plate-Loaded", 2.5f, "Pad 3", "Parallel handles", "", "Diverging line of pull")

        // T-Bar Row variants
        addVar("var_tbar_arsenal", getExId("exercise_chest_supported_tbar_row"), "exercise_chest_supported_tbar_row", "machine_arsenal_reloaded_tbar", "Arsenal Reloaded T-Bar Row", "Arsenal Strength", "Plate-Loaded", 5f, "Chest pad 2", "Pronated wide", "", "Best in industry mid-back builder")
        addVar("var_tbar_panatta", getExId("exercise_chest_supported_tbar_row"), "exercise_chest_supported_tbar_row", "machine_panatta_tbar_row", "Panatta Chest-Supported T-Bar", "Panatta", "Plate-Loaded", 5f, "Chest pad 3", "Multi-grip handles", "", "Anatomical chest rest")
        addVar("var_tbar_landmine", getExId("exercise_chest_supported_tbar_row"), "exercise_chest_supported_tbar_row", "machine_tbar_row_freeweight", "Landmine Free T-Bar Row", "Free Weights", "Free Weight", 5f, "Standing straddle", "V-handle", "", "Classic heavy iron")

        // Hack Squat variants
        addVar("var_hack_cybex", getExId("exercise_linear_hack_squat_cybex"), "exercise_linear_hack_squat_cybex", "machine_cybex_linear_hack_squat", "Cybex Linear Hack Squat (Classic)", "Cybex", "Plate-Loaded", 5f, "Back pad neutral", "Safety handles", "", "Legendary quad builder with curved sled")
        addVar("var_hack_panatta", getExId("exercise_linear_hack_squat_cybex"), "exercise_linear_hack_squat_cybex", "machine_panatta_super_hack_squat", "Panatta Super Hack Squat 45°", "Panatta", "Plate-Loaded", 5f, "Back pad 45°", "Comfort shoulder pads", "", "Super smooth roller bearings")
        addVar("var_hack_arsenal", getExId("exercise_linear_hack_squat_cybex"), "exercise_linear_hack_squat_cybex", "machine_arsenal_reloaded_hack", "Arsenal Reloaded Linear Hack Squat", "Arsenal Strength", "Plate-Loaded", 5f, "Back pad 2", "Heavy safety handles", "", "Heavy gauge steel")
        addVar("var_hack_technogym", getExId("exercise_linear_hack_squat_cybex"), "exercise_linear_hack_squat_cybex", "machine_technogym_pure_hack_squat", "Technogym Pure Strength Hack Squat", "Technogym", "Plate-Loaded", 5f, "Back pad", "Ergonomic release", "", "Pure strength")

        // Pendulum Squat variants
        addVar("var_pendulum_arsenal", getExId("exercise_pendulum_squat_arsenal"), "exercise_pendulum_squat_arsenal", "machine_arsenal_pendulum_squat", "Arsenal Reloaded Pendulum Squat", "Arsenal Strength", "Plate-Loaded", 5f, "Back pad", "Shoulder pads", "", "Counterweight load curve")
        addVar("var_pendulum_atlantis", getExId("exercise_pendulum_squat_arsenal"), "exercise_pendulum_squat_arsenal", "machine_atlantis_pendulum_squat", "Atlantis Pendulum Squat Pro", "Atlantis", "Plate-Loaded", 5f, "Back support", "Release catch", "", "Extreme knee flexion")

        // Leg Extension variants
        addVar("var_leg_ext_prime", getExId("exercise_leg_extension_prime"), "exercise_leg_extension_prime", "machine_prime_leg_extension", "Prime Smart Strength Leg Extension", "Prime", "Plate-Loaded", 2.5f, "Back 2, Shin pad 3", "Side handles", "", "Adjustable overload cam")
        addVar("var_leg_ext_panatta", getExId("exercise_leg_extension_prime"), "exercise_leg_extension_prime", "machine_panatta_leg_extension", "Panatta Monolith Leg Extension", "Panatta", "Selectorized", 2.5f, "Seat 3", "Shin roller 2", "", "Constant cam tension")
        addVar("var_leg_ext_cybex", getExId("exercise_leg_extension_prime"), "exercise_leg_extension_prime", "machine_cybex_eagle_leg_extension", "Cybex Eagle NX Leg Extension", "Cybex", "Selectorized", 2.5f, "Back 2", "Range limiter 3", "", "Range limiter technology")

        // Seated Leg Curl variants
        addVar("var_leg_curl_panatta", getExId("exercise_seated_leg_curl_panatta"), "exercise_seated_leg_curl_panatta", "machine_panatta_seated_leg_curl", "Panatta Seated Leg Curl Monolith", "Panatta", "Selectorized", 2.5f, "Backrest 3", "Firm grip", "Ankle pad 2", "Anatomical leg lock")
        addVar("var_leg_curl_prime", getExId("exercise_seated_leg_curl_panatta"), "exercise_seated_leg_curl_panatta", "machine_prime_seated_leg_curl", "Prime Smart Strength Seated Leg Curl", "Prime", "Plate-Loaded", 2.5f, "Seat 2", "Thigh pad lock", "", "Cam peak contraction")
        addVar("var_leg_curl_cybex", getExId("exercise_seated_leg_curl_panatta"), "exercise_seated_leg_curl_panatta", "machine_cybex_eagle_seated_leg_curl", "Cybex Eagle NX Seated Leg Curl", "Cybex", "Selectorized", 2.5f, "Seat 3", "Side handles", "", "Ergonomic tibia roller")

        // Preacher Curl variants
        addVar("var_preacher_prime", getExId("exercise_preacher_curl_prime"), "exercise_preacher_curl_prime", "machine_prime_preacher_curl", "Prime Smart Strength Preacher Curl", "Prime", "Plate-Loaded", 2.5f, "Seat 2", "Semi-supinated", "", "Peak stretch setting 3")
        addVar("var_preacher_panatta", getExId("exercise_preacher_curl_prime"), "exercise_preacher_curl_prime", "machine_panatta_preacher_curl", "Panatta Monolith Preacher Curl", "Panatta", "Selectorized", 2.5f, "Seat 3", "Rotating handles", "", "Pure bicep isolation")
        addVar("var_preacher_hammer", getExId("exercise_preacher_curl_prime"), "exercise_preacher_curl_prime", "machine_hammer_seated_bicep", "Hammer Strength Iso Seated Bicep", "Hammer Strength", "Plate-Loaded", 2.5f, "Seat 2", "Parallel / angled", "", "Iso-lateral plates")
        addVar("var_preacher_ez_bar", getExId("exercise_preacher_curl_prime"), "exercise_preacher_curl_prime", "machine_preacher_ez_bench", "Olympic EZ-Bar Preacher Bench", "Free Weights", "Free Weight", 2.5f, "Seat 2", "Inner EZ grip", "", "Classic barbell preacher")

        // Overhead Shoulder Press variants
        addVar("var_shoulder_panatta", getExId("exercise_overhead_press_panatta"), "exercise_overhead_press_panatta", "machine_panatta_super_overhead_press", "Panatta Super Overhead Press", "Panatta", "Plate-Loaded", 2.5f, "Seat 4", "Neutral handles", "", "Natural converging trajectory")
        addVar("var_shoulder_hammer", getExId("exercise_overhead_press_panatta"), "exercise_overhead_press_panatta", "machine_hammer_iso_shoulder_press", "Hammer Strength Iso Shoulder Press", "Hammer Strength", "Plate-Loaded", 2.5f, "Seat 3", "Overhand handles", "", "Plate loaded front delt builder")
        addVar("var_shoulder_smith", getExId("exercise_overhead_press_panatta"), "exercise_overhead_press_panatta", "machine_hammer_smith_machine", "Smith Machine Seated Shoulder Press", "Smith", "Smith", 2.5f, "Bench 85°", "Overhand wide", "", "Strict vertical overhead track")
        addVar("var_shoulder_dumbbell", getExId("exercise_overhead_press_panatta"), "exercise_overhead_press_panatta", "machine_dumbbell_incline_station", "Seated Dumbbell Shoulder Press", "Free Weights", "Free Weight", 2f, "Bench 80°", "Dumbbells", "", "Full stabiliser recruitment")

        // Standing Calf Raise variants
        addVar("var_calf_panatta", getExId("exercise_standing_calf_raise_panatta"), "exercise_standing_calf_raise_panatta", "machine_panatta_standing_calf", "Panatta Standing Calf Raise", "Panatta", "Selectorized", 5f, "Pad 3", "Balls of feet on edge", "", "Heavy block stack")
        addVar("var_calf_cybex", getExId("exercise_standing_calf_raise_panatta"), "exercise_standing_calf_raise_panatta", "machine_cybex_standing_calf", "Cybex VR3 Standing Calf Raise", "Cybex", "Selectorized", 5f, "Shoulder pads 2", "Edge block", "", "Smooth curved roller track")
        addVar("var_calf_technogym", getExId("exercise_standing_calf_raise_panatta"), "exercise_standing_calf_raise_panatta", "machine_technogym_selection_standing_calf", "Technogym Selection Standing Calf", "Technogym", "Selectorized", 5f, "Pad 3", "Foot block", "", "Pure strength stack")

        // Fill remaining variants for all canonical exercises to reach comprehensive ~186 variants
        canonicalExercises.forEach { ex ->
            val hasVar = list.any { it.exerciseStableId == ex.stableId }
            if (!hasVar) {
                // Generate primary variant
                list.add(
                    ExerciseVariant(
                        id = "var_${ex.stableId.removePrefix("exercise_")}_primary",
                        exerciseId = ex.id,
                        exerciseStableId = ex.stableId,
                        machineCatalogId = null,
                        variantName = "${ex.manufacturer} ${ex.baseName}",
                        manufacturer = ex.manufacturer,
                        resistanceType = when {
                            ex.name.contains("Cable", ignoreCase = true) -> "Cable"
                            ex.name.contains("Smith", ignoreCase = true) -> "Smith"
                            ex.name.contains("Dumbbell", ignoreCase = true) -> "Free Weight"
                            ex.name.contains("Barbell", ignoreCase = true) -> "Free Weight"
                            ex.name.contains("Machine", ignoreCase = true) -> "Plate-Loaded"
                            else -> "Selectorized"
                        },
                        weightIncrementKg = ex.defaultIncrementKg,
                        defaultSeat = ex.seatPosition,
                        defaultHandle = ex.handlePosition,
                        notes = "Canonical reference variant",
                        source = "bundled"
                    )
                )
            }
        }

        list
    }

    // --- TEMPLATE FACTORY: ONLY used if user explicitly taps [ USE NICK WALKER TEMPLATE ] ---
    fun createNickWalkerTemplate(): Pair<Program, List<Pair<ProgramDay, List<ProgramExercise>>>> {
        val program = Program(
            name = "Nick Walker Reconstruction",
            description = "High-intensity progressive overload bodybuilding protocol (5-day cycle: Chest+Biceps, Back, Rest, Delts+Triceps, Legs, Rest, Rest)",
            isActive = true,
            source = "user"
        )

        val days = listOf(
            ProgramDay(dayIndex = 0, dayCode = "SEG", title = "Chest + Biceps", isRestDay = false, description = "Upper push hypertrophy and bicep peaks"),
            ProgramDay(dayIndex = 1, dayCode = "TER", title = "Back", isRestDay = false, description = "Lat width, mid-back density and rear girdle"),
            ProgramDay(dayIndex = 2, dayCode = "QUA", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery"),
            ProgramDay(dayIndex = 3, dayCode = "QUI", title = "Shoulders + Triceps + Touch-ups", isRestDay = false, description = "Delts cap, triceps lateral head & forearms"),
            ProgramDay(dayIndex = 4, dayCode = "SEX", title = "Legs", isRestDay = false, description = "Quad dominance, hamstring overload & hack squat progression"),
            ProgramDay(dayIndex = 5, dayCode = "SÁB", title = "Rest", isRestDay = true, description = "Rest day · Active flush & recovery"),
            ProgramDay(dayIndex = 6, dayCode = "DOM", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery prior to Monday")
        )

        val exMap = canonicalExercises.associateBy { it.stableId }
        fun exId(stableId: String): Long = exMap[stableId]?.id ?: 1L

        val dayExercises = listOf(
            // Day 0: Chest + Biceps
            days[0] to listOf(
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_incline_chest_press"), variantId = "var_incline_panatta", orderIndex = 1, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_flat_chest_press"), variantId = "var_flat_hammer", orderIndex = 2, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_pec_deck_fly"), variantId = "var_pec_panatta", orderIndex = 3, targetWorkSets = 2, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 120),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_preacher_curl_prime"), variantId = "var_preacher_prime", orderIndex = 4, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_cable_bicep_curl_dual"), variantId = "", orderIndex = 5, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120)
            ),
            // Day 1: Back
            days[1] to listOf(
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_lat_pulldown_neutral"), variantId = "var_lat_panatta", orderIndex = 1, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_chest_supported_tbar_row"), variantId = "var_tbar_arsenal", orderIndex = 2, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 0, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_single_arm_high_cable_lat_row"), variantId = "", orderIndex = 3, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 1, restSeconds = 120)
            ),
            // Day 3: Delts + Triceps
            days[3] to listOf(
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_overhead_press_panatta"), variantId = "var_shoulder_panatta", orderIndex = 1, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 0, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_lateral_raise_machine_panatta"), variantId = "", orderIndex = 2, targetWorkSets = 3, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 120),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_triceps_pushdown_rope"), variantId = "", orderIndex = 3, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 120)
            ),
            // Day 4: Legs
            days[4] to listOf(
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_seated_leg_curl_panatta"), variantId = "var_leg_curl_panatta", orderIndex = 1, targetWorkSets = 3, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 150),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_linear_hack_squat_cybex"), variantId = "var_hack_cybex", orderIndex = 2, targetWorkSets = 2, repMin = 10, repMax = 12, targetRir = 0, restSeconds = 240),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_romanian_deadlift_dumbbells"), variantId = "", orderIndex = 3, targetWorkSets = 2, repMin = 8, repMax = 10, targetRir = 1, restSeconds = 180),
                ProgramExercise(programDayId = 0, exerciseId = exId("exercise_leg_extension_prime"), variantId = "var_leg_ext_prime", orderIndex = 4, targetWorkSets = 2, repMin = 12, repMax = 15, targetRir = 0, restSeconds = 150)
            )
        )

        return Pair(program, dayExercises)
    }
}
