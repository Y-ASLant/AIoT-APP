package compose.iot.ui.theme.page

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import compose.iot.mqtt.CardStyle
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import org.json.JSONArray
import org.json.JSONObject

private const val SUBSCRIPTION_CARDS_PREFS = "subscription_cards"
private const val SWITCH_STATES_PREFS = "switch_states"
private const val SLIDER_STATES_PREFS = "slider_states"

internal object SubscriptionCardStorage {
    fun saveCards(context: Context, cards: List<SubscriptionCard>) {
        val prefs = context.getSharedPreferences(SUBSCRIPTION_CARDS_PREFS, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        cards.forEach { card ->
            val cardJson = JSONObject().apply {
                put("topic", card.topic)
                put("displayName", card.displayName)
                put("jsonParam", card.jsonParam)
                put("unitSuffix", card.unitSuffix)
                put("cardStyle", card.cardStyle.name)
                put("deviceType", card.deviceType.name)
                put("serverType", card.serverType.name)
                put("isButtonStyle", card.isButtonStyle)
                put("isSliderStyle", card.isSliderStyle)
                put("isPushButtonStyle", card.isPushButtonStyle)
                put("switchOnValue", card.switchOnValue)
                put("switchOffValue", card.switchOffValue)
                put("buttonValue", card.buttonValue)
                put("sliderMin", card.sliderMin)
                put("sliderMax", card.sliderMax)
                put("sliderStep", card.sliderStep)
            }
            jsonArray.put(cardJson)
        }

        prefs.edit { putString("cards", jsonArray.toString()) }
    }

    fun loadCards(context: Context): List<SubscriptionCard> {
        val prefs = context.getSharedPreferences(SUBSCRIPTION_CARDS_PREFS, Context.MODE_PRIVATE)
        val cardsJson = prefs.getString("cards", "[]") ?: "[]"

        return try {
            val jsonArray = JSONArray(cardsJson)
            List(jsonArray.length()) { index ->
                val cardJson = jsonArray.getJSONObject(index)
                SubscriptionCard(
                    topic = cardJson.getString("topic"),
                    displayName = cardJson.getString("displayName"),
                    jsonParam = cardJson.getString("jsonParam"),
                    unitSuffix = cardJson.getString("unitSuffix"),
                    cardStyle = try {
                        CardStyle.valueOf(cardJson.getString("cardStyle"))
                    } catch (_: Exception) {
                        CardStyle.MINIMAL
                    },
                    deviceType = try {
                        DeviceType.valueOf(cardJson.getString("deviceType"))
                    } catch (_: Exception) {
                        DeviceType.SENSOR
                    },
                    serverType = try {
                        ServerType.valueOf(cardJson.getString("serverType"))
                    } catch (_: Exception) {
                        ServerType.EMQX
                    },
                    isButtonStyle = cardJson.optBoolean("isButtonStyle", false),
                    isSliderStyle = cardJson.optBoolean("isSliderStyle", false),
                    isPushButtonStyle = cardJson.optBoolean("isPushButtonStyle", false),
                    switchOnValue = cardJson.optString("switchOnValue", "1"),
                    switchOffValue = cardJson.optString("switchOffValue", "0"),
                    buttonValue = cardJson.optString("buttonValue", "1"),
                    sliderMin = cardJson.optDouble("sliderMin", 0.0).toFloat(),
                    sliderMax = cardJson.optDouble("sliderMax", 100.0).toFloat(),
                    sliderStep = cardJson.optDouble("sliderStep", 1.0).toFloat()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadActuatorCardValues(context: Context, cards: List<SubscriptionCard>): Map<String, String> {
        val switchStatesPrefs = context.getSharedPreferences(SWITCH_STATES_PREFS, Context.MODE_PRIVATE)
        val sliderStatesPrefs = context.getSharedPreferences(SLIDER_STATES_PREFS, Context.MODE_PRIVATE)
        val values = mutableMapOf<String, String>()

        cards.filter { it.deviceType == DeviceType.ACTUATOR }.forEach { card ->
            val cardId = "${card.topic}:${card.jsonParam}"

            if (card.isButtonStyle) {
                val isOn = switchStatesPrefs.getBoolean(cardId, false)
                values[cardId] = if (isOn) {
                    if (card.serverType == ServerType.HomeAssistant) "on" else card.switchOnValue
                } else {
                    if (card.serverType == ServerType.HomeAssistant) "off" else card.switchOffValue
                }
            } else if (card.isSliderStyle) {
                values[cardId] = sliderStatesPrefs.getFloat(cardId, card.sliderMin).toString()
            }
        }

        return values
    }

    fun persistActuatorValue(context: Context, card: SubscriptionCard, cardId: String, value: String) {
        if (card.deviceType != DeviceType.ACTUATOR || value == "NULL") {
            return
        }

        when {
            card.isSliderStyle -> {
                val floatValue = value.toFloatOrNull()
                if (floatValue != null) {
                    context.getSharedPreferences(SLIDER_STATES_PREFS, Context.MODE_PRIVATE)
                        .edit { putFloat(cardId, floatValue) }
                } else {
                    Log.e("CardStorage", "无法解析滑块值: $value")
                }
            }

            card.isButtonStyle -> {
                val isOn = if (card.serverType == ServerType.HomeAssistant) {
                    value == "on"
                } else {
                    value == card.switchOnValue
                }
                context.getSharedPreferences(SWITCH_STATES_PREFS, Context.MODE_PRIVATE)
                    .edit { putBoolean(cardId, isOn) }
            }
        }
    }
}
