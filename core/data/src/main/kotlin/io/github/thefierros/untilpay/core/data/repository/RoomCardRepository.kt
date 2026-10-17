package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asEntity
import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.CardStatementDao
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CardStatement
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.StatementId
import java.time.Clock
import java.time.LocalDate
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class RoomCardRepository @Inject constructor(
    private val creditCardDao: CreditCardDao,
    private val cardStatementDao: CardStatementDao,
    private val clock: Clock,
) : CardRepository {

    override fun observeCards(): Flow<List<CreditCard>> =
        creditCardDao.observeActive().map { rows -> rows.map { it.asModel() } }

    override suspend fun createCard(
        name: String,
        currency: Currency,
        statementDay: Int,
        dueDay: Int,
    ): CardId {
        // Building the model first applies its rules, such as days within a month.
        val draft = CreditCard(CardId(0), name, currency, statementDay, dueDay)
        return CardId(
            creditCardDao.insert(
                CreditCardEntity(
                    name = draft.name,
                    currencyCode = draft.currency.currencyCode,
                    statementDay = draft.statementDay,
                    dueDay = draft.dueDay,
                    createdAt = clock.millis(),
                ),
            ),
        )
    }

    override suspend fun updateCard(card: CreditCard) {
        val row = existing(card.id)
        require(card.currency.currencyCode == row.currencyCode) {
            "The currency of credit card ${row.id} cannot change"
        }
        creditCardDao.update(
            row.copy(name = card.name, statementDay = card.statementDay, dueDay = card.dueDay),
        )
    }

    override suspend fun archiveCard(id: CardId) {
        creditCardDao.archive(existing(id).id, archivedAt = clock.millis())
    }

    override fun observeStatements(cardId: CardId): Flow<List<CardStatement>> =
        cardStatementDao.observeForCard(cardId.value).map { rows ->
            val currency = Currency.getInstance(existing(cardId).currencyCode)
            rows.map { it.asModel(currency) }
        }

    override suspend fun recordStatement(
        cardId: CardId,
        closingDate: LocalDate,
        dueDate: LocalDate,
        balance: Money,
        minimumPayment: Money,
        plannedPayment: Money,
    ): StatementId {
        // Building the model first applies its rules, such as minimum <= balance.
        val draft = CardStatement(
            id = StatementId(0),
            cardId = cardId,
            closingDate = closingDate,
            dueDate = dueDate,
            balance = balance,
            minimumPayment = minimumPayment,
            plannedPayment = plannedPayment,
        )
        requireFitsCard(draft)
        return StatementId(cardStatementDao.insert(draft.asEntity()))
    }

    override suspend fun updateStatement(statement: CardStatement) {
        val row = requireNotNull(cardStatementDao.findById(statement.id.value)) {
            "There is no card statement ${statement.id.value}"
        }
        require(row.cardId == statement.cardId.value) {
            "Card statement ${row.id} cannot move to another card"
        }
        requireFitsCard(statement)
        cardStatementDao.update(statement.asEntity())
    }

    /**
     * Checks what the statement alone cannot: that its amounts are in its
     * card's currency and that no other statement of the card closes that day.
     */
    private suspend fun requireFitsCard(statement: CardStatement) {
        val card = existing(statement.cardId)
        require(statement.balance.currency.currencyCode == card.currencyCode) {
            "Statements of credit card ${card.id} must be in ${card.currencyCode}, " +
                "got ${statement.balance.currency}"
        }
        val closingDay = statement.closingDate.toEpochDay()
        val clash = cardStatementDao.observeForCard(card.id).first()
            .any { it.closingDate == closingDay && it.id != statement.id.value }
        require(!clash) {
            "Credit card ${card.id} already has a statement closing on ${statement.closingDate}"
        }
    }

    private suspend fun existing(id: CardId): CreditCardEntity =
        requireNotNull(creditCardDao.findById(id.value)) { "There is no credit card ${id.value}" }
}
