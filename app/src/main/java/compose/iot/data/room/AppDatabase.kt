package compose.iot.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import compose.iot.mqtt.SubscriptionCard

@Database(
    entities = [SubscriptionCard::class, SensorHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionCardDao(): SubscriptionCardDao

    abstract fun sensorHistoryDao(): SensorHistoryDao

    companion object {
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
                        // Use fallbackToDestructiveMigration if you want to wipe data on schema change during dev
                        .fallbackToDestructiveMigration()
                        .build()
                instance = dbInstance
                dbInstance
            }
        }
    }
}
