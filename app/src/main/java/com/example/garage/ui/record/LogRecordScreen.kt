package com.example.garage.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Receipt
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.domain.model.ServiceCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.core.graphics.createBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogRecordScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    startScan: Boolean = false,
    viewModel: LogRecordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.uploadReceipt(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.uploadReceipt(bitmap)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openFileDescriptor(it, "r")?.use { pfd ->
                    val pdfRenderer = PdfRenderer(pfd)
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        val scale = 2f
                        val bitmap = createBitmap(
                            (page.width * scale).toInt(),
                            (page.height * scale).toInt()
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pdfRenderer.close()
                        viewModel.uploadReceipt(bitmap)
                    } else {
                        pdfRenderer.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(Unit) {
        if (startScan && !state.isEdit) {
            galleryLauncher.launch("image/*")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = navigationBarPadding + 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp, 4.dp, 16.dp, 4.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
            Text(
                text = if (state.isEdit) "Edit service record" else "Log a service",
                style = MaterialTheme.typography.titleMedium
            )
            if (!state.isEdit) {
                Spacer(modifier = Modifier.weight(1f))
                Box {
                    var showTopMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = "Scan Receipt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Take Photo") },
                            leadingIcon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload Image") },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload PDF") },
                            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                pdfLauncher.launch("application/pdf")
                            }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            var showMenu by remember { mutableStateOf(false) }

            Text(
                text = "Title",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Service type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ServiceCategory.entries.toList()) { category ->
                    CategoryChip(
                        label = category.label,
                        selected = category == state.category,
                        onClick = { viewModel.onCategorySelect(category) }
                    )
                }
            }

            Text(
                text = "Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateFormatter.format(
                        Instant.ofEpochMilli(state.date)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                    ),
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Select date",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = state.date,
                    initialDisplayMode = DisplayMode.Picker
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(modifier = Modifier.padding(top = 14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Odometer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = state.odometer,
                        onValueChange = viewModel::onOdometerChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        text = "Cost (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = state.cost,
                        onValueChange = viewModel::onCostChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = "Notes (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.receiptPhotoUrl != null || state.isEdit) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.receiptPhotoUrl != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Replace Receipt")
                            }
                            IconButton(
                                onClick = { viewModel.deleteReceipt() }
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete Receipt",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Attach Receipt")
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Take Photo") },
                            leadingIcon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload Image") },
                            leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Upload PDF") },
                            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                pdfLauncher.launch("application/pdf")
                            }
                        )
                    }
                }
            }

            if (state.isScanning) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                    Text("Scanning & uploading receipt...", color = MaterialTheme.colorScheme.primary)
                }
            }

            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.isEdit) "Save changes" else "Save record")
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = label,
        color = contentColor,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .then(
                if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}