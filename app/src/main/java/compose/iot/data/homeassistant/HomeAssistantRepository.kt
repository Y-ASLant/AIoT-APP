package compose.iot.data.homeassistant

import compose.iot.data.room.SubscriptionCardDao
import compose.iot.R
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import compose.iot.ui.theme.page.SubscriptionCardStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class HADevice(
    val entityId: String,
    val friendlyName: String,
    val state: String,
    val deviceClass: String?,
)

class HomeAssistantRepository @Inject constructor(
    private val dao: SubscriptionCardDao,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) {
    suspend fun fetchDevices(
        serverUrl: String,
        accessToken: String,
    ): Result<List<HADevice>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection =
                    (URL("$serverUrl/api/states").openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer $accessToken")
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 5000
                        readTimeout = 5000
                        instanceFollowRedirects = true
                    }

                try {
                    check(connection.responseCode == 200) {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    buildList {
                        JSONArray(response).let { array ->
                            for (index in 0 until array.length()) {
                                val item = array.getJSONObject(index)
                                val entityId = item.getString("entity_id")
                                val attributes = item.getJSONObject("attributes")
                                add(
                                    HADevice(
                                        entityId = entityId,
                                        friendlyName = attributes.optString("friendly_name", entityId),
                                        state = item.getString("state"),
                                        deviceClass = attributes.optString("device_class").takeIf { it.isNotEmpty() },
                                    ),
                                )
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }.onFailure { Timber.e(it, "Failed to fetch Home Assistant devices") }
        }

    suspend fun addDevice(device: HADevice): Result<SubscriptionCard> =
        runCatching {
            val card = buildCard(device)
            check(!dao.exists(card.topic, card.jsonParam)) { "Device already added" }
            dao.insertCards(listOf(card))
            card
        }

    fun buildCard(device: HADevice): SubscriptionCard =
        SubscriptionCard(
            topic = "homeassistant/${device.entityId}/state",
            displayName = device.friendlyName,
            jsonParam = "state",
            cardStyle = CardStyle.MINIMAL,
            serverType = ServerType.HomeAssistant,
            deviceType =
                when {
                    device.entityId.startsWith("number.") -> DeviceType.ACTUATOR
                    device.entityId.startsWith("switch.") -> DeviceType.ACTUATOR
                    device.entityId.startsWith("button.") -> DeviceType.ACTUATOR
                    device.entityId.startsWith("input_button.") -> DeviceType.ACTUATOR
                    device.entityId.startsWith("light.") -> DeviceType.ACTUATOR
                    device.deviceClass in listOf("light", "fan", "cover", "button") -> DeviceType.ACTUATOR
                    else -> DeviceType.SENSOR
                },
            unitSuffix =
                when (device.deviceClass) {
                    "temperature" -> appContext.getString(R.string.temperature_unit_celsius)
                    "humidity" -> "%"
                    else -> ""
                },
            isButtonStyle = device.entityId.startsWith("switch.") || device.entityId.startsWith("light."),
            isSliderStyle = device.entityId.startsWith("number."),
            isPushButtonStyle = device.entityId.startsWith("button."),
            buttonValue = "1",
        )

    fun persistInitialActuatorState(
        appContext: android.content.Context,
        card: SubscriptionCard,
        state: String,
    ) {
        if (card.deviceType != DeviceType.ACTUATOR) return

        val cardId = "${card.topic}:${card.jsonParam}"
        val persistedValue =
            when {
                card.isSliderStyle -> state.toFloatOrNull()?.toString() ?: "0"
                else -> state
            }
        SubscriptionCardStorage.persistActuatorValue(appContext, card, cardId, persistedValue)
    }

    fun normalizeServerUrl(serverUrl: String): String {
        var finalUrl = serverUrl.trim()
        if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
            finalUrl = "http://$finalUrl"
        }
        if (finalUrl.endsWith("/")) {
            finalUrl = finalUrl.dropLast(1)
        }
        return finalUrl
    }
}
