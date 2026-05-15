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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nammapustaka.data.StudentEntity
import com.example.nammapustaka.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: LibraryViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    var showEditDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<com.example.nammapustaka.data.TransactionEntity?>(null) }

    if (showEditDialog && transactionToEdit != null) {
        var selectedStudent by remember { mutableStateOf<StudentEntity?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Borrower") },
            text = {
                Column {
                    Text("Select new student for: ${transactionToEdit!!.bookTitle}")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "Select Student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Student") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            students.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text(student.name) },
                                    onClick = {
                                        selectedStudent = student
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStudent != null) {
                            viewModel.updateTransactionBorrower(transactionToEdit!!.id, selectedStudent!!)
                            showEditDialog = false
                        }
                    },
                    enabled = selectedStudent != null
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Lending History", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Transaction Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "A Smart Library App Tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions recorded yet.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionItem(
                            tx = tx, 
                            sdf = sdf,
                            onEditClick = {
                                transactionToEdit = tx
                                showEditDialog = true
                            },
                            onPayFine = { amount ->
                                val upiUri = Uri.parse("upi://pay?pa=nehabanu@upi&pn=Neha%20Banu&am=$amount&cu=INR&tn=Library%20Fine")
                                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                try {
                                    context.startActivity(intent)
                                    // Mark as paid after launching UPI app
                                    // In a real app, you'd wait for a callback, but for this simplified version
                                    // we can mark it paid or add a "Confirm Payment" step.
                                    viewModel.markFinePaid(tx.id)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    tx: com.example.nammapustaka.data.TransactionEntity, 
    sdf: SimpleDateFormat,
    onEditClick: () -> Unit,
    onPayFine: (Double) -> Unit
) {
    val isOverdue = !tx.returned && (System.currentTimeMillis() - tx.borrowDate > 7L * 24 * 60 * 60 * 1000)
    
    // Calculate current fine for overdue live items
    val liveFine = if (isOverdue) {
        val diff = System.currentTimeMillis() - tx.borrowDate
        val days = diff / (1000 * 60 * 60 * 24)
        (days - 7) * 2.0
    } else 0.0

    val fineToDisplay = if (tx.returned) tx.fineAmount else liveFine

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) Color(0xFFFFEBEE) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, 
            color = if (isOverdue) Color.Red 
                    else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Edit Borrower",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Student: ${tx.studentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverdue) Color.Red.copy(alpha = 0.8f) 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = if (isOverdue) Color.Red else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Borrowed: ${sdf.format(Date(tx.borrowDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (tx.returned) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Returned: ${sdf.format(Date(tx.returnDate ?: 0))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (isOverdue) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = Color.Red
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "OVERDUE (Limit: 7 Days)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fine Section
                if (fineToDisplay > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Fine: ₹${"%.2f".format(fineToDisplay)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (tx.finePaid) Color(0xFF4CAF50) else Color.Red,
                            fontWeight = FontWeight.Black
                        )
                        if (!tx.finePaid) {
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { onPayFine(fineToDisplay) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Payments, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pay Now (UPI)", fontSize = 10.sp)
                            }
                        } else {
                            Spacer(Modifier.width(8.dp))
                            Text("Paid ✅", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tx.returned) Color(0xFFE8F5E9) 
                        else if (isOverdue) Color(0xFFFFEBEE)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.returned) Icons.Default.CheckCircle 
                                 else if (isOverdue) Icons.Default.Warning 
                                 else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (tx.returned) Color(0xFF4CAF50) 
                           else if (isOverdue) Color.Red
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
