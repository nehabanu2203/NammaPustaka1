package com.example.nammapustaka.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE bookCode = :code LIMIT 1")
    suspend fun getBookByCode(code: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM students ORDER BY totalPagesRead DESC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert
    suspend fun insertStudent(student: StudentEntity)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Query("SELECT * FROM transactions ORDER BY borrowDate DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET finePaid = 1 WHERE id = :transactionId")
    suspend fun markFinePaid(transactionId: Long)

    @Query("SELECT * FROM transactions WHERE bookId = :bookId AND returned = 0 LIMIT 1")
    suspend fun getActiveTransactionForBook(bookId: Long): TransactionEntity?

    @Query("SELECT * FROM reviews WHERE bookId = :bookId")
    fun getReviewsForBook(bookId: Long): Flow<List<ReviewEntity>>

    @Insert
    suspend fun insertReview(review: ReviewEntity)
}