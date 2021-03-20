package it.antoniocasto.covidtracker

enum class Metric {
    NEGATIVE,
    POSITIVE,
    DEATH
}

enum class TimeScale(val numDays: Int) {
    THREEMONTH(90),
    MONTH(30),
    MAX(-1)
}