package compose.iot.ui.theme.page

import android.content.Context
import androidx.core.content.edit
import compose.iot.mqtt.DeviceType
import compose.iot.mqtt.ServerType
import compose.iot.mqtt.SubscriptionCard
import timber.log.Timber

private const val SUBSCRIPTION_CARDS_PREFS = "subscription_cards"
private const val SWITCH_STATES_PREFS = "switch_states"
private const val SLIDER_STATES_PREFS = "slider_states"

internal object SubscriptionCardStorage {
    fun loadActuatorCardValues(
        context: Context,
        cards: List<SubscriptionCard>,
    ): Map<String, String> {
        val switchStatesPrefs = context.getSharedPreferences(SWITCH_STATES_PREFS, Context.MODE_PRIVATE)
        val sliderStatesPrefs = context.getSharedPreferences(SLIDER_STATES_PREFS, Context.MODE_PRIVATE)
        val values = mutableMapOf<String, String>()

        cards.filter { it.deviceType == DeviceType.ACTUATOR }.forEach { card ->
            val cardId = "${card.topic}:${card.jsonParam}"

            if (card.isButtonStyle) {
                val isOn = switchStatesPrefs.getBoolean(cardId, false)
                values[cardId] =
                    if (isOn) {
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

    fun persistActuatorValue(
        context: Context,
        card: SubscriptionCard,
        cardId: String,
        value: String,
    ) {
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
                    Timber.e("无法解析滑块值: $value")
                }
            }

            card.isButtonStyle -> {
                val isOn =
                    if (card.serverType == ServerType.HomeAssistant) {
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
