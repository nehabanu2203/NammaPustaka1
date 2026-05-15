package com.example.nammapustaka.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.nammapustaka.viewmodel.LibraryViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: LibraryViewModel, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scannedBookState by viewModel.scannedBookState.collectAsState()
    val newBookFoundOnline by viewModel.newBookFoundOnline.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val students by viewModel.allStudents.collectAsState()
    
    var isProcessingLocal by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<com.example.nammapustaka.data.StudentEntity?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Log.w("ScannerScreen", "Camera permission denied")
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                try {
                    val image = InputImage.fromFilePath(context, it)
                    BarcodeScanning.getClient().process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                val code = barcodes[0].rawValue
                                if (code != null) {
                                    Log.d("ScannerScreen", "QR Detected from gallery: $code")
                                    isProcessingLocal = true
                                    viewModel.findBookByCode(code)
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No QR code found in selected image")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ScannerScreen", "Gallery scan failed", e)
                            scope.launch {
                                snackbarHostState.showSnackbar("Failed to scan image")
                            }
                        }
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Error opening gallery image", e)
                }
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle Error State (Book not found)
    LaunchedEffect(errorState) {
        errorState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetScannedBook()
            isProcessingLocal = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { galleryLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan from Gallery")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasCameraPermission) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission is required to scan QR codes", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                // Camera always active to prevent black screen issues
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (!isProcessingLocal && scannedBookState == null) {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    BarcodeScanning.getClient().process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                val code = barcodes[0].rawValue
                                                if (code != null) {
                                                    Log.d("ScannerScreen", "QR Detected: $code")
                                                    isProcessingLocal = true
                                                    viewModel.findBookByCode(code)
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { 
                                            imageProxy.close() 
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                                Log.d("ScannerScreen", "Camera bound successfully")
                            } catch (e: Exception) {
                                Log.e("ScannerScreen", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Scanner Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Position QR Code in the frame",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Transparent)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.1f))
                        )
                    }
                }
            }

            // AI New Book Dialog
            newBookFoundOnline?.let { book ->
                AlertDialog(
                    onDismissRequest = { 
                        viewModel.resetScannedBook()
                        isProcessingLocal = false
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "AI: New Book Detected",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (book.coverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = book.coverUrl,
                                    contentDescription = "Book Cover",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Text(book.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Text("By ${book.author}", style = MaterialTheme.typography.bodyMedium)
                            Text("Category: ${book.category}", style = MaterialTheme.typography.labelSmall)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("This book is not in your library. AI has fetched its details. Would you like to add it automatically?", 
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmAddOnlineBook(book)
                                // isProcessingLocal stays true so it doesn't re-scan immediately
                            }
                        ) {
                            Text("Add to Library")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            viewModel.resetScannedBook()
                            isProcessingLocal = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Result Dialog
            scannedBookState?.let { state ->
                AlertDialog(
                    onDismissRequest = { 
                        viewModel.resetScannedBook()
                        isProcessingLocal = false
                    },
                    icon = {
                        Icon(
                            imageVector = if (state.book.isIssued) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.book.isIssued) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (state.book.isIssued) "Book Borrowed" else "Book Available",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.book.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("By ${state.book.author}", style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.book.isIssued) {
                                state.activeTransaction?.let { tx ->
                                    Text("Currently with: ${tx.studentName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                // Student Selection
                                if (students.isEmpty()) {
                                    Text("No students registered. Please add students first.", color = MaterialTheme.colorScheme.error)
                                } else {
                                    ExposedDropdownMenuBox(
                                        expanded = dropdownExpanded,
                                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedStudent?.name ?: "Select Student",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Borrower") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
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
                            }
                        }
                    },
                    confirmButton = {
                        if (state.book.isIssued) {
                            Button(
                                onClick = {
                                    viewModel.returnBook(state.book.id)
                                    isProcessingLocal = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Return Book")
                            }
                        } else {
                            Button(
                                onClick = {
                                    selectedStudent?.let {
                                        viewModel.borrowBook(state.book.id, it)
                                        isProcessingLocal = false
                                    }
                                },
                                enabled = selectedStudent != null
                            ) {
                                Text("Borrow")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            viewModel.resetScannedBook()
                            isProcessingLocal = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
