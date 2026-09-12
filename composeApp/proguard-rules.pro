# Pocketsum release rules. Compose, Koin, SQLDelight and kotlinx-datetime all ship consumer rules;
# these only cover what R8 cannot see: reflection-free, but keep app entry points and enums by name.

# Enum names are persisted in the database (TransactionType, AccountType, CategoryType, Frequency, ThemeMode).
-keepclassmembers enum com.codigitech.ft.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# Broadcast receivers and the Application are referenced from the manifest.
-keep class com.codigitech.ft.FinanceApp { *; }
-keep class com.codigitech.ft.MainActivity { *; }
-keep class com.codigitech.ft.platform.RecurringAlarmReceiver { *; }
-keep class com.codigitech.ft.platform.BootReceiver { *; }

# SQLDelight generated database + JDBC-free Android driver.
-keep class com.codigitech.ft.db.** { *; }
-dontwarn org.slf4j.**

# Readable crash traces from Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
