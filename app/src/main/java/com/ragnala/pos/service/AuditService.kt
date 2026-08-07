package com.ragnala.pos.service

import com.ragnala.pos.data.db.AuditDao
import com.ragnala.pos.data.db.AuditEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AuditService(private val auditDao: AuditDao) {

    fun recent(limit: Int = 200): Flow<List<AuditEntity>> = auditDao.observeRecent(limit)

    suspend fun record(
        action: String,
        entityType: String,
        entityId: String,
        delta: String,
        userLabel: String,
        reason: String? = null,
        now: Long = System.currentTimeMillis(),
    ) {
        auditDao.insert(
            AuditEntity(
                id = UUID.randomUUID().toString(),
                timestamp = now,
                action = action,
                entityType = entityType,
                entityId = entityId,
                delta = delta,
                reason = reason,
                userLabel = userLabel,
            ),
        )
    }
}
