package com.example.nammapustaka.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nammapustaka.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LibraryDatabase.getDatabase(application).dao()
    
    private val firestore: FirebaseFirestore? by lazy {
        try {
            // Check if Firebase is initialized first
            val app = com.google.firebase.FirebaseApp.getInstance()
            FirebaseFirestore.getInstance(app)
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Firebase/Firestore not ready: ${e.message}")
            null
        }
    }

    private val _scannedBookState = MutableStateFlow<ScannedBookState?>(null)
    val scannedBookState = _scannedBookState.asStateFlow()

    private val _newBookFoundOnline = MutableStateFlow<BookEntity?>(null)
    val newBookFoundOnline = _newBookFoundOnline.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState = _errorState.asStateFlow()

    data class ScannedBookState(
        val book: BookEntity,
        val activeTransaction: TransactionEntity? = null
    )

    val allBooks: StateFlow<List<BookEntity>> = dao.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val allStudents: StateFlow<List<StudentEntity>> = dao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val allTransactions: StateFlow<List<TransactionEntity>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resetScannedBook() {
        Log.d("LibraryViewModel", "Resetting scanned book state")
        _scannedBookState.value = null
        _newBookFoundOnline.value = null
        _errorState.value = null
    }

    fun findBookByCode(code: String) {
        val trimmedCode = code.trim()
        Log.d("LibraryViewModel", "Searching for book with EXACT code: '$trimmedCode'")
        viewModelScope.launch {
            val book = dao.getBookByCode(trimmedCode)
            if (book != null) {
                Log.d("LibraryViewModel", "SUCCESS: Book found: ${book.title}")
                val activeTx = if (book.isIssued) dao.getActiveTransactionForBook(book.id) else null
                _scannedBookState.value = ScannedBookState(book, activeTx)
                _errorState.value = null
            } else {
                Log.w("LibraryViewModel", "FAILURE: No book matches code: '$trimmedCode' locally. Searching online...")
                searchBookOnline(trimmedCode)
            }
        }
    }

    private fun searchBookOnline(code: String) {
        viewModelScope.launch {
            _errorState.value = "Book not found locally. Using AI to fetch details..."
            try {
                // In a real app, you would use Google Books API or Gemini AI here.
                // For this implementation, we'll simulate an AI fetch from a book service.
                // If it looks like an ISBN or valid code, we "find" it.
                
                // Simulated AI Result (this would normally be a network call)
                if (code.length >= 3) {
                    val simulatedBook = BookEntity(
                        title = "AI Discovered: ${if(code.startsWith("97")) "Modern Literature" else "Academic Guide"}",
                        author = "Automated AI Fetch",
                        category = "Story",
                        bookCode = code,
                        pageCount = (100..500).random(),
                        coverUrl = "https://images.unsplash.com/photo-1543005187-9ef91307bb5e?q=80&w=200"
                    )
                    _newBookFoundOnline.value = simulatedBook
                    _errorState.value = null
                } else {
                    _errorState.value = "Book not found: $code"
                }
            } catch (e: Exception) {
                _errorState.value = "AI Fetch failed: ${e.message}"
            }
        }
    }

    fun confirmAddOnlineBook(book: BookEntity) {
        viewModelScope.launch {
            dao.insertBook(book)
            syncBookToFirebase(book)
            _newBookFoundOnline.value = null
            findBookByCode(book.bookCode)
        }
    }

    fun borrowBook(bookId: Long, student: StudentEntity) {
        Log.d("LibraryViewModel", "Borrowing book ID $bookId for student ${student.name}")
        viewModelScope.launch {
            try {
                val book = dao.getBookById(bookId) ?: return@launch
                if (!book.isIssued) {
                    val transaction = TransactionEntity(
                        bookId = book.id,
                        studentId = student.id,
                        studentName = student.name,
                        bookTitle = book.title,
                        borrowDate = System.currentTimeMillis(),
                        pageCount = book.pageCount
                    )
                    dao.insertTransaction(transaction)
                    syncTransactionToFirebase(transaction)
                    
                    dao.updateBook(book.copy(isIssued = true))
                    Log.d("LibraryViewModel", "Borrow successful for book: ${book.title}")
                    resetScannedBook()
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error during borrow: ${e.message}")
            }
        }
    }

    private fun syncTransactionToFirebase(tx: TransactionEntity) {
        firestore?.collection("transactions")
            ?.document(tx.id.toString())
            ?.set(tx)
    }

    fun returnBook(bookId: Long) {
        Log.d("LibraryViewModel", "Returning book ID $bookId")
        viewModelScope.launch {
            try {
                val book = dao.getBookById(bookId) ?: return@launch
                val transaction = dao.getActiveTransactionForBook(book.id)
                if (transaction != null) {
                    val returnTime = System.currentTimeMillis()
                    val diff = returnTime - transaction.borrowDate
                    val days = diff / (1000 * 60 * 60 * 24)
                    
                    var fine = 0.0
                    if (days > 7) {
                        fine = (days - 7) * 2.0 // ₹2 per day fine after 7 days
                    }

                    // Update Transaction
                    dao.updateTransaction(transaction.copy(
                        returnDate = returnTime, 
                        returned = true,
                        fineAmount = fine
                    ))
                    
                    // Update Book status
                    dao.updateBook(book.copy(isIssued = false))
                    
                    // Update Student's total pages and books count
                    val student = dao.getStudentById(transaction.studentId)
                    if (student != null) {
                        dao.updateStudent(student.copy(
                            totalPagesRead = student.totalPagesRead + transaction.pageCount,
                            booksCompleted = student.booksCompleted + 1
                        ))
                    }
                    
                    Log.d("LibraryViewModel", "Return successful for book: ${book.title}. Fine: ₹$fine")
                    resetScannedBook()
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error during return: ${e.message}")
            }
        }
    }

    fun markFinePaid(transactionId: Long) {
        viewModelScope.launch {
            dao.markFinePaid(transactionId)
            // Optionally sync updated transaction to Firebase
            val updatedTx = dao.getAllTransactions().stateIn(viewModelScope).value.find { it.id == transactionId }
            updatedTx?.let { syncTransactionToFirebase(it) }
        }
    }

    fun addBook(title: String, author: String, category: String, code: String, pageCount: Int) {
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) return
        
        val newBook = BookEntity(
            title = title, 
            author = author, 
            category = category, 
            bookCode = trimmedCode,
            pageCount = pageCount
        )

        viewModelScope.launch {
            dao.insertBook(newBook)
            syncBookToFirebase(newBook)
            Log.d("LibraryViewModel", "Book Added: $title ($pageCount pages)")
        }
    }

    fun updateBook(book: BookEntity) {
        viewModelScope.launch {
            dao.updateBook(book)
            syncBookToFirebase(book)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            dao.deleteBook(book)
            deleteBookFromFirebase(book)
        }
    }

    private fun deleteBookFromFirebase(book: BookEntity) {
        firestore?.collection("books")
            ?.document(book.bookCode)
            ?.delete()
            ?.addOnSuccessListener { Log.d("Firebase", "Book deleted successfully") }
            ?.addOnFailureListener { e -> Log.e("Firebase", "Delete failed", e) }
    }

    private fun syncBookToFirebase(book: BookEntity) {
        firestore?.collection("books")
            ?.document(book.bookCode)
            ?.set(book)
            ?.addOnSuccessListener { Log.d("Firebase", "Book synced successfully") }
            ?.addOnFailureListener { e -> Log.e("Firebase", "Sync failed", e) }
    }

    fun addStudent(name: String, studentId: String, className: String) {
        val student = StudentEntity(name = name, studentId = studentId, className = className)
        viewModelScope.launch {
            dao.insertStudent(student)
            syncStudentToFirebase(student)
        }
    }

    private fun syncStudentToFirebase(student: StudentEntity) {
        firestore?.collection("students")
            ?.document(student.studentId)
            ?.set(student)
    }

    fun getReviews(bookId: Long) = dao.getReviewsForBook(bookId)

    fun addReview(bookId: Long, studentName: String, rating: Int, comment: String) {
        viewModelScope.launch {
            dao.insertReview(ReviewEntity(bookId = bookId, studentName = studentName, rating = rating, comment = comment))
        }
    }

    fun updateTransactionBorrower(transactionId: Long, newStudent: StudentEntity) {
        viewModelScope.launch {
            val transactions = allTransactions.value
            val tx = transactions.find { it.id == transactionId }
            if (tx != null) {
                dao.updateTransaction(tx.copy(
                    studentId = newStudent.id,
                    studentName = newStudent.name
                ))
            }
        }
    }
}