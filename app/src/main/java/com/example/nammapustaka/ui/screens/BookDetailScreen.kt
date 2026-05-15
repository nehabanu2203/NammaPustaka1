package com.example.nammapustaka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import com.example.nammapustaka.data.BookEntity
import com.example.nammapustaka.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(bookId: Long, viewModel: LibraryViewModel, navController: NavController) {
    val books by viewModel.allBooks.collectAsState()
    val book = books.find { it.id == bookId }
    val reviews by viewModel.getReviews(bookId).collectAsState(initial = emptyList())
    val students by viewModel.allStudents.collectAsState()

    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var selectedStudentName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Borrow/Return student selection
    var borrowExpanded by remember { mutableStateOf(false) }
    var selectedBorrowStudent by remember { mutableStateOf<com.example.nammapustaka.data.StudentEntity?>(null) }

    // Edit/Delete states
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Book not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Book")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Book", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Book") },
                text = { Text("Are you sure you want to delete '${book.title}'? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBook(book)
                            showDeleteDialog = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditDialog) {
            var editTitle by remember { mutableStateOf(book.title) }
            var editAuthor by remember { mutableStateOf(book.author) }
            var editCategory by remember { mutableStateOf(book.category) }
            var editPages by remember { mutableStateOf(book.pageCount.toString()) }
            var categoryExpanded by remember { mutableStateOf(false) }
            val categories = listOf("Story", "Science", "History", "Literature", "English", "Biology", "Coding")

            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Book Details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editAuthor,
                            onValueChange = { editAuthor = it },
                            label = { Text("Author") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            editCategory = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = editPages,
                            onValueChange = { editPages = it },
                            label = { Text("Pages") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updatedBook = book.copy(
                                title = editTitle,
                                author = editAuthor,
                                category = editCategory,
                                pageCount = editPages.toIntOrNull() ?: book.pageCount
                            )
                            viewModel.updateBook(updatedBook)
                            showEditDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Book Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    if (book.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = "Book Cover",
                            modifier = Modifier
                                .size(80.dp, 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("by ${book.author}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                color = if (book.isIssued) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (book.isIssued) "Issued" else "Available",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SuggestionChip(onClick = {}, label = { Text(book.category) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lending Section
            Text("Lending Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (book.isIssued) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("This book is currently borrowed.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Button(
                            onClick = { viewModel.returnBook(book.id) },
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Mark as Returned")
                        }
                    } else {
                        Text("Borrow this book", style = MaterialTheme.typography.titleSmall)
                        
                        ExposedDropdownMenuBox(
                            expanded = borrowExpanded,
                            onExpandedChange = { borrowExpanded = !borrowExpanded },
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedBorrowStudent?.name ?: "Select Student",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Student Name") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = borrowExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = borrowExpanded,
                                onDismissRequest = { borrowExpanded = false }
                            ) {
                                students.forEach { student ->
                                    DropdownMenuItem(
                                        text = { Text(student.name) },
                                        onClick = {
                                            selectedBorrowStudent = student
                                            borrowExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { 
                                selectedBorrowStudent?.let { viewModel.borrowBook(book.id, it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedBorrowStudent != null
                        ) {
                            Text("Confirm Borrowing")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Review Corner
            Text("Review Corner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Share your thoughts after reading", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Leave a Review", style = MaterialTheme.typography.titleSmall)
                    
                    // Student Selector
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedStudentName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Reviewer Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            students.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text(student.name) },
                                    onClick = {
                                        selectedStudentName = student.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Star Rating
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(5) { index ->
                            IconButton(onClick = { rating = index + 1 }) {
                                Icon(
                                    imageVector = if (index < rating) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = if (index < rating) Color(0xFFFFD700) else Color.Gray
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 100) comment = it },
                        label = { Text("Your one-sentence review") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("Example: This book was amazing!") }
                    )

                    Button(
                        onClick = {
                            if (selectedStudentName.isNotBlank() && comment.isNotBlank()) {
                                viewModel.addReview(book.id, selectedStudentName, rating, comment)
                                comment = ""
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        enabled = selectedStudentName.isNotBlank() && comment.isNotBlank()
                    ) {
                        Text("Submit Review")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reviews List
            Text("Community Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (reviews.isEmpty()) {
                Text("No reviews yet. Be the first to review!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                reviews.forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(review.studentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Row {
                                    repeat(review.rating) {
                                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Text("\"${review.comment}\"", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
