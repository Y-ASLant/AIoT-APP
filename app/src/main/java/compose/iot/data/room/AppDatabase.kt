package compose.iot.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import compose.iot.mqtt.SubscriptionCard

@Database(
    entities = [SubscriptionCard::class, SensorHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionCardDao(): SubscriptionCardDao

    abstract fun sensorHistoryDao(): SensorHistoryDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE subscription_cards ADD COLUMN cardSize TEXT NOT NULL DEFAULT 'S1x1'",
                    )
                }
            }

        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val dbInstance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "aiot_app_database",
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()
                instance = dbInstance
                dbInstance
            }
        }
    }
}
