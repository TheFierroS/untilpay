package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
import java.time.Clock
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomCardRepository @Inject constructor(
    private val creditCardDao: CreditCardDao,
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

    private suspend fun existing(id: CardId): CreditCardEntity =
        requireNotNull(creditCardDao.findById(id.value)) { "There is no credit card ${id.value}" }
}
