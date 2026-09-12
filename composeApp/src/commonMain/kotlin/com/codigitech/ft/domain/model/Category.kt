package com.codigitech.ft.domain.model

enum class CategoryType(val label: String) {
    INCOME("Income"), EXPENSE("Expense");

    companion object {
        fun fromName(name: String): CategoryType = entries.firstOrNull { it.name == name } ?: EXPENSE
    }
}

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: CategoryType,
    val isCustom: Boolean,
    val isDefault: Boolean,
)

/** Predefined categories seeded on first launch. Icons are emoji so they render on both platforms without icon packs. */
object DefaultCategories {
    val expense = listOf(
        Triple("Food", "🍔", "#EF6C00"),
        Triple("Transport", "🚗", "#1976D2"),
        Triple("Shopping", "🛍️", "#8E24AA"),
        Triple("Bills", "🧾", "#00897B"),
        Triple("Rent", "🏠", "#5D4037"),
        Triple("Health", "❤️", "#D32F2F"),
        Triple("Entertainment", "🎬", "#F9A825"),
        Triple("Other", "📦", "#607D8B"),
    )
    val income = listOf(
        Triple("Salary", "💰", "#2E7D32"),
        Triple("Business", "💼", "#00838F"),
        Triple("Gift", "🎁", "#C2185B"),
        Triple("Interest", "🏦", "#558B2F"),
        Triple("Other", "📦", "#607D8B"),
    )
    const val UNCATEGORIZED_NAME = "Uncategorized"
    const val UNCATEGORIZED_ICON = "❓"
    const val UNCATEGORIZED_COLOR = "#9E9E9E"
}
