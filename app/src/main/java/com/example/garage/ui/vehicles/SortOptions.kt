package com.example.garage.ui.vehicles

enum class VehicleSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    YEAR_DESC("Year (Newest)"),
    YEAR_ASC("Year (Oldest)"),
    RECENTLY_ADDED("Recently added")
}

enum class RecordSortOption(val label: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    ODOMETER_DESC("Odometer (Highest)"),
    ODOMETER_ASC("Odometer (Lowest)"),
    COST_DESC("Cost (Highest)"),
    COST_ASC("Cost (Lowest)")
}
