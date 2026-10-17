package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
import java.util.Currency
import kotlinx.coroutines.flow.Flow

/** Creates and changes the user's credit cards. */
interface CardRepository {

    /** Active cards in the order they were created. */
    fun observeCards(): Flow<List<CreditCard>>

    /** Creates a card and returns its id. */
    suspend fun createCard(name: String, currency: Currency, statementDay: Int, dueDay: Int): CardId

    /** Saves a changed name, statement day or due day. A card's currency never changes. */
    suspend fun updateCard(card: CreditCard)

    /** Hides a card. Its statements and purchases are kept. */
    suspend fun archiveCard(id: CardId)
}
