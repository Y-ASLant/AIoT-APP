package compose.iot.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import compose.iot.data.preferences.PreferencesManager
import compose.iot.data.room.AppDatabase
import compose.iot.data.room.SensorHistoryDao
import compose.iot.data.room.SubscriptionCardDao
import compose.iot.data.video.VideoStreamRepository
import compose.iot.mqtt.MqttManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "aiot_app_database",
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSubscriptionCardDao(database: AppDatabase): SubscriptionCardDao = database.subscriptionCardDao()

    @Provides
    fun provideSensorHistoryDao(database: AppDatabase): SensorHistoryDao = database.sensorHistoryDao()

    @Provides
    @Singleton
    fun provideMqttManager(
        preferencesManager: PreferencesManager,
    ): MqttManager =
        MqttManager().apply {
            if (preferencesManager.mqttAutoConnect) {
                setMqttVersion(preferencesManager.mqttVersion)
                setServerUri("tcp://${preferencesManager.mqttServerIp}:${preferencesManager.mqttServerPort}")
                setClientId(preferencesManager.mqttClientId)
                setUsername(preferencesManager.mqttUsername)
                setPassword(preferencesManager.mqttPassword)
            }
        }

    @Provides
    @Named("video_stream_prefs")
    fun provideVideoStreamPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("video_stream_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideVideoStreamRepository(
        @Named("video_stream_prefs") prefs: SharedPreferences,
    ): VideoStreamRepository = VideoStreamRepository(prefs)
}
