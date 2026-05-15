package com.example.nammapustaka.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nammapustaka.data.TransactionEntity
import com.example.nammapustaka.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: LibraryViewModel) {
    val context = LocalContext.current
    val books by viewModel.allBooks.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    
    val overdueTransactions = transactions.filter { tx ->
        !tx.returned && (System.currentTimeMillis() - tx.borrowDate > 7L * 24 * 60 * 60 * 1000)
    }
    
    val pendingFines = transactions.filter { !it.finePaid }.sumOf { it.fineAmount }

    // State for Fine Collection Dialog
    var showFineCollectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Admin Dashboard 📊", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { showFineCollectionDialog = true }) {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = "Collect Fines")
                    }
                }
            )
        }
    ) { padding ->
        if (showFineCollectionDialog) {
            // Include both overdue live fines and already returned unpaid fines
            val allPendingFines = transactions.filter { tx ->
                val isReturnedButUnpaid = tx.returned && !tx.finePaid && tx.fineAmount > 0
                val isNotReturnedOverdue = !tx.returned && (System.currentTimeMillis() - tx.borrowDate > 7L * 24 * 60 * 60 * 1000)
                isReturnedButUnpaid || isNotReturnedOverdue
            }
            
            AlertDialog(
                onDismissRequest = { showFineCollectionDialog = false },
                title = { Text("Collect Fines") },
                text = {
                    if (allPendingFines.isEmpty()) {
                        Text("No pending fines at the moment.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(allPendingFines) { tx ->
                                // Calculate live fine if not returned yet
                                val currentFine = if (tx.returned) tx.fineAmount else {
                                    val days = (System.currentTimeMillis() - tx.borrowDate) / (1000 * 60 * 60 * 24)
                                    if (days > 7) (days - 7) * 2.0 else 0.0
                                }

                                ListItem(
                                    headlineContent = { Text(tx.studentName) },
                                    supportingContent = { 
                                        Text("${tx.bookTitle} • ₹${"%.2f".format(currentFine)} ${if(tx.returned) "(Returned)" else "(Overdue)"}") 
                                    },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = { 
                                                val upiUri = Uri.parse("upi://pay?pa=nehabanu@upi&pn=Neha%20Banu&am=$currentFine&cu=INR&tn=Library%20Fine")
                                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                                try {
                                                    context.startActivity(intent)
                                                    viewModel.markFinePaid(tx.id)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Payments, "UPI Pay", tint = Color(0xFF4CAF50))
                                            }
                                            IconButton(onClick = { viewModel.markFinePaid(tx.id) }) {
                                                Icon(Icons.Default.CheckCircle, "Mark Cash Paid", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFineCollectionDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text("Library Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            // Stats Grid
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Books",
                        value = books.size.toString(),
                        icon = Icons.Default.MenuBook,
                        color = Color(0xFF2196F3)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Students",
                        value = students.size.toString(),
                        icon = Icons.Default.Group,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Overdue",
                        value = overdueTransactions.size.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFF44336)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Fines",
                        value = "₹${"%.0f".format(pendingFines)}",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Overdue Tracking
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Overdue Tracking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            if (overdueTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(12.dp))
                            Text("No overdue books at the moment!", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(overdueTransactions) { tx ->
                    OverdueItem(
                        tx = tx,
                        onMarkReturned = { viewModel.returnBook(tx.bookId) },
                        onMarkPaid = { viewModel.markFinePaid(tx.id) }
                    )
                }
            }

            // Student Performance tracking could be added here
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { /* Export PDF Report */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Assignment, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Report")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
            Text(title, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun OverdueItem(tx: TransactionEntity, onMarkReturned: () -> Unit, onMarkPaid: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val diff = System.currentTimeMillis() - tx.borrowDate
    val days = diff / (1000 * 60 * 60 * 24)
    val fine = (days - 7) * 2.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Text("${days}d", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.bookTitle, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${tx.studentName} • ₹$fine Fine", style = MaterialTheme.typography.bodySmall, color = Color.Red)
            }

            if (fine > 0 && !tx.finePaid) {
                IconButton(onClick = onMarkPaid) {
                    Icon(Icons.Default.Payments, "Mark Paid", tint = Color(0xFFFF9800))
                }
            }
            
            IconButton(onClick = onMarkReturned) {
                Icon(Icons.Default.CheckCircle, "Return", tint = Color(0xFF4CAF50))
            }
        }
    }
}
