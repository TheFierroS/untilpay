package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CardStatement
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.StatementId
import java.time.LocalDate
import java.util.Currency
import kotlinx.coroutines.flow.Flow

/** Creates and changes the user's credit cards and their statements. */
interface CardRepository {

    /** Active cards in the order they were created. */
    fun observeCards(): Flow<List<CreditCard>>

    /** Creates a card and returns its id. */
    suspend fun createCard(name: String, currency: Currency, statementDay: Int, dueDay: Int): CardId

    /** Saves a changed name, statement day or due day. A card's currency never changes. */
    suspend fun updateCard(card: CreditCard)

    /** Hides a card. Its statements and purchases are kept. */
    suspend fun archiveCard(id: CardId)

    /** Every statement of a card, oldest first. */
    fun observeStatements(cardId: CardId): Flow<List<CardStatement>>

    /**
     * Records a statement as printed by the bank and returns its id. Amounts
     * must be in the card's currency, and a card has one statement per closing
     * date.
     */
    suspend fun recordStatement(
        cardId: CardId,
        closingDate: LocalDate,
        dueDate: LocalDate,
        balance: Money,
        minimumPayment: Money,
        plannedPayment: Money,
    ): StatementId

    /** Saves a corrected statement or a changed planned payment. A statement stays on its card. */
    suspend fun updateStatement(statement: CardStatement)
}
