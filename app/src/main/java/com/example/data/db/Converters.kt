package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.IntensityTechnique
import com.example.data.model.SetType

class Converters {
    @TypeConverter
    fun fromSetType(value: SetType): String = value.name

    @TypeConverter
    fun toSetType(value: String): SetType = runCatching { SetType.valueOf(value) }.getOrDefault(SetType.WORK)

    @TypeConverter
    fun fromIntensityTechnique(value: IntensityTechnique): String = value.name

    @TypeConverter
    fun toIntensityTechnique(value: String): IntensityTechnique =
        runCatching { IntensityTechnique.valueOf(value) }.getOrDefault(IntensityTechnique.NONE)
}
