package com.kkfittracking.model

import java.time.LocalDate

/** One logged set with names, ready for export. */
data class ExportSet(
    val date: LocalDate,
    val exercise: String,
    val category: String,
    val type: ExerciseType,
    val values: SetValues,
    /** The exercise's own weight unit (lb, kg or machine levels); null follows the app's. */
    val weightUnits: UnitSystem? = null,
)

/**
 * Spreadsheet-friendly CSV: one row per set, numbers with a "." decimal point, weights in each
 * exercise's own unit (named in the Unit column: kg, lb or lvl for machine levels), distances in
 * the user's units (named in the header), the side of one-sided sets, and time in seconds.
 */
object CsvExport {
    fun workouts(sets: List<ExportSet>, units: UnitSystem): String {
        val header = listOf(
            "Date", "Exercise", "Category", "Set", "Side",
            "Weight", "Unit", "Reps",
            "Distance (${units.distanceUnit})", "Height (${units.lengthUnit})",
            "Time (s)", "RPE", "Note", "Drop set",
        )
        val rows = sets.groupBy { it.date to it.exercise }.values.flatMap { exerciseSets ->
            // Drop sets share the number of the set they continue.
            var number = 0
            exerciseSets.map { set ->
                val v = set.values
                val weightUnits = set.weightUnits ?: units
                val weight = v.weightKg?.takeIf { set.type.usesWeight }
                if (!v.isDropSet || number == 0) number++
                listOf(
                    set.date.toString(),
                    set.exercise,
                    set.category,
                    number.toString(),
                    v.side?.label.orEmpty(),
                    weight?.let { formatNumber(weightUnits.weightFromKg(it)) }.orEmpty(),
                    weightUnits.weightUnit.takeIf { weight != null }.orEmpty(),
                    v.reps?.toString().orEmpty(),
                    v.distanceMeters?.takeIf { set.type.usesDistance }?.let { formatNumber(units.distanceFromMeters(it)) }.orEmpty(),
                    v.distanceMeters?.takeIf { set.type.usesHeight }?.let { formatNumber(units.heightFromMeters(it)) }.orEmpty(),
                    v.durationSeconds?.toString().orEmpty(),
                    v.rpe?.toString().orEmpty(),
                    v.note,
                    if (v.isDropSet) "yes" else "",
                )
            }
        }
        return toCsv(listOf(header) + rows)
    }

    fun bodyMeasurements(measurements: List<BodyMeasurement>, units: UnitSystem): String {
        val rows = measurements.sortedWith(compareBy({ it.date }, { it.metric.ordinal })).map {
            listOf(
                it.date.toString(),
                it.metric.label,
                formatNumber(it.metric.toDisplay(it.value, units)),
                it.metric.unit(units),
            )
        }
        return toCsv(listOf(listOf("Date", "Measurement", "Value", "Unit")) + rows)
    }

    fun toCsv(rows: List<List<String>>): String = rows.joinToString("\r\n", postfix = "\r\n") { row ->
        row.joinToString(",") { escape(it) }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
