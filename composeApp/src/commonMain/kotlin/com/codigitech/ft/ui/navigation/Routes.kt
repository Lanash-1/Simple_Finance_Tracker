package com.codigitech.ft.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions?accountId={accountId}"
    const val INSIGHTS = "insights"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"

    const val ACCOUNT_DETAIL = "account/{id}"
    const val EDIT_TRANSACTION = "transaction?id={id}&transferId={transferId}&accountId={accountId}"
    const val CATEGORIES = "categories"
    const val RECURRING = "recurring"

    /** Bottom-bar destinations, in display order. */
    val tabs = listOf(DASHBOARD, TRANSACTIONS, INSIGHTS, ACCOUNTS, SETTINGS)

    fun transactions(accountId: Long? = null) = "transactions?accountId=${accountId ?: -1}"
    fun accountDetail(id: Long) = "account/$id"
    fun newTransaction(accountId: Long? = null) = "transaction?id=-1&transferId=&accountId=${accountId ?: -1}"
    fun editTransaction(id: Long, transferId: String?) = "transaction?id=$id&transferId=${transferId ?: ""}&accountId=-1"

    fun isTab(route: String?) = route != null && route in tabs
    fun isEditor(route: String?) = route == EDIT_TRANSACTION
}
