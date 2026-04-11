package compose.iot.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import compose.iot.mqtt.SubscriptionCard
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionCardDao {
    @Query("SELECT * FROM subscription_cards")
    fun getAllCardsStream(): Flow<List<SubscriptionCard>>

    @Query("SELECT * FROM subscription_cards")
    suspend fun getAllCards(): List<SubscriptionCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<SubscriptionCard>)

    @Delete
    suspend fun deleteCard(card: SubscriptionCard)

    @Query("DELETE FROM subscription_cards WHERE topic = :topic AND jsonParam = :jsonParam")
    suspend fun deleteCardById(
        topic: String,
        jsonParam: String,
    )
}
