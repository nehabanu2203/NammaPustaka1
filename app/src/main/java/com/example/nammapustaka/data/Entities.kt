package com.example.nammapustaka.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val category: String,
    val coverUrl: String = "",
    val isIssued: Boolean = false,
    val bookCode: String,
    val pageCount: Int = 0 // New field for leaderboard tracking
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val studentId: String,
    val className: String,
    val totalPagesRead: Int = 0, // Track total pages
    val booksCompleted: Int = 0  // Track count of books
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val studentId: Long,
    val studentName: String,
    val bookTitle: String,
    val borrowDate: Long,
    val returnDate: Long? = null,
    val returned: Boolean = false,
    val pageCount: Int = 0, // Store pages at time of transaction
    val fineAmount: Double = 0.0,
    val finePaid: Boolean = false
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val studentName: String,
    val rating: Int,
    val comment: String
)