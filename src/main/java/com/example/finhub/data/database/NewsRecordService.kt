package com.example.finhub.data.database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class NewsRecord(
    val id: String = "",
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val duration: Long? = null, // in seconds
    val status: String = "processing",
    val date: Date = Date()
)

class NewsRecordService {
    private val db = FirebaseFirestore.getInstance()
    private val recordsCollection = db.collection("news_record")

    // Create a new record when fetching starts
    suspend fun createRecord(): String {
        val now = Date()
        val record = mapOf(
            "startTime" to Timestamp(now),
            "date" to Timestamp(now),
            "status" to "processing",
            "progress" to 0
        )
        val docRef = recordsCollection.add(record).await()
        return docRef.id
    }

    // Update record status
    suspend fun updateStatus(recordId: String, status: String, progress: Int? = null) {
        val updates = mutableMapOf<String, Any>(
            "status" to status
        )
        if (progress != null) {
            updates["progress"] = progress
        }
        recordsCollection.document(recordId)
            .update(updates)
            .await()
    }

    // Update record when fetching ends
    suspend fun updateRecordEnd(recordId: String, status: String) {
        val endTime = Date()
        val docSnapshot = recordsCollection.document(recordId).get().await()
        val startTime = docSnapshot.getTimestamp("startTime")?.toDate() ?: return

        val duration = (endTime.time - startTime.time) / 1000 // Convert to seconds

        val updates = mapOf(
            "endTime" to Timestamp(endTime),
            "duration" to duration,
            "status" to status,
            "progress" to if (status == "completed") 100 else null
        )

        recordsCollection.document(recordId)
            .update(updates)
            .await()
    }

    // Get real-time updates for all records
    fun getRecordsFlow(): Flow<List<NewsRecord>> = callbackFlow {
        val subscription = recordsCollection
            .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50) // Limit to last 50 records for better performance
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val records = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        NewsRecord(
                            id = doc.id,
                            startTime = doc.getTimestamp("startTime")?.toDate() ?: Date(),
                            endTime = doc.getTimestamp("endTime")?.toDate(),
                            duration = doc.getLong("duration"),
                            status = doc.getString("status") ?: "unknown",
                            date = doc.getTimestamp("date")?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(records)
            }

        awaitClose { subscription.remove() }
    }
} 