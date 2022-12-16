package com.victorhvs.tfc.data.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.victorhvs.tfc.core.DispatcherProvider
import com.victorhvs.tfc.data.extensions.observeStatefulDoc
import com.victorhvs.tfc.domain.enums.FirestoreState
import com.victorhvs.tfc.domain.models.Order
import com.victorhvs.tfc.domain.models.Stock
import com.victorhvs.tfc.domain.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface FirebaseDataSource {
    suspend fun getStocks(): List<Stock>

    suspend fun getRanking(): List<User>

    suspend fun getUser(userId: String): Flow<FirestoreState<User?>>

    suspend fun postOrder(order: Order, userId: String)
}

class FirebaseDataSourceImp @Inject constructor(
    val client: FirebaseFirestore,
    val dispatcher: DispatcherProvider
) : FirebaseDataSource {

    companion object {
        const val STOCK_REF = "stocks"
        const val STOCK_ORDER_FIELD = "uuid"
        const val STOCKS_PER_PAGE = 20L

        const val USER_REF = "users"
        const val RANKING_SIZE = 50L
    }

    override suspend fun getStocks(): List<Stock> {
        return withContext(dispatcher.io()) {
            val snapshot = client.collection(STOCK_REF)
                .whereEqualTo("enabled", true)
                .orderBy(STOCK_ORDER_FIELD, Query.Direction.ASCENDING)
                .limit(STOCKS_PER_PAGE)
                .get()
                .await()

            snapshot.toObjects(Stock::class.java)
        }
    }

    override suspend fun getRanking(): List<User> {
        return withContext(dispatcher.io()) {
            val mLotteryCollection = client.collection(USER_REF)

            val snapshot = mLotteryCollection.limit(RANKING_SIZE)
                .orderBy("proftable", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(User::class.java)
        }
    }

    override suspend fun getUser(userId: String): Flow<FirestoreState<User?>> {
        val path = "${USER_REF}/$userId"
        return observeStatefulDoc(client.document(path))
    }

    override suspend fun postOrder(order: Order, userId: String) {
        return withContext(dispatcher.io()) {
            val orderPath = "users/$userId/orders"
            val orderRef = client.collection(orderPath).document()

            orderRef.set(order.copy(uuid = orderRef.id, userId = userId)).await()
        }
    }
}
