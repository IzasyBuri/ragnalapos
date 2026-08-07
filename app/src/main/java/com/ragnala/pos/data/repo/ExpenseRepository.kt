package com.ragnala.pos.data.repo

import com.ragnala.pos.data.db.ExpenseDao
import com.ragnala.pos.data.db.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun observeBetween(start: Long, end: Long): Flow<List<ExpenseEntity>> =
        expenseDao.observeBetween(start, end)

    suspend fun between(start: Long, end: Long): List<ExpenseEntity> =
        expenseDao.between(start, end)

    suspend fun save(expense: ExpenseEntity) = expenseDao.upsert(expense)
    suspend fun delete(expense: ExpenseEntity) = expenseDao.delete(expense)
}
