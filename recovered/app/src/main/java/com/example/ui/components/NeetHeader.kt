package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnswerDocument
import com.example.model.CanvasScrollMode
import com.example.model.NeetSubject
import com.example.model.PaperStyle
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeetHeader(
    viewModel: MainViewModel,
    document: AnswerDocument,
    currentPageIndex: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onBackToHome: () -> Unit,
    onSendToChatGpt: () -> Unit,
    onCheckAnswerPrompt: () -> Unit,
    onExportPdf: () -> Unit,
    onSaveImage: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onToggleFullScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDeletePageDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showEditInfoDialog = true }
                            .testTag("document_title_button")
                    ) {
                        // Subject badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(document.subject.colorHex))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = document.subject.displayName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${document.questionNumber}: ${document.title}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit title",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${document.paperStyle.displayName} • ${document.scrollMode.displayName}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        IconButton(
                            onClick = onBackToHome,
                            modifier = Modifier.size(48.dp).testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Undo & Redo with distinct non-overlapping touch targets
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.size(44.dp).testTag("header_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.size(44.dp).testTag("header_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Full Screen Button
                    IconButton(
                        onClick = onToggleFullScreen,
                        modifier = Modifier.size(44.dp).testTag("header_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen Mode",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Theme selector
                    Box {
                        IconButton(
                            onClick = { showThemeMenu = true },
                            modifier = Modifier.size(44.dp).testTag("header_theme_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Change Paper Theme",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }



                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            Text(
                                text = "Paper Style",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            PaperStyle.values().forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style.displayName) },
                                    onClick = {
                                        viewModel.setPaperStyle(style)
                                        showThemeMenu = false
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Canvas Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            CanvasScrollMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        viewModel.setScrollMode(mode)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Check Answer Prompt helper
                    IconButton(
                        onClick = onCheckAnswerPrompt,
                        modifier = Modifier.testTag("check_answer_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Check My Answer Prompt",
                            tint = Color(0xFF059669)
                        )
                    }

                    // Primary Action: Prominent SEND TO CHATGPT button
                    Button(
                        onClick = onSendToChatGpt,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10A37F), // ChatGPT signature green / emerald
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("send_to_chatgpt_primary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send to ChatGPT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.testTag("header_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save as Image (PNG)") },
                                onClick = {
                                    showMoreMenu = false
                                    onSaveImage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showMoreMenu = false
                                    onExportPdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Current Page") },
                                onClick = {
                                    showMoreMenu = false
                                    showClearConfirmDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Current Page") },
                                onClick = {
                                    showMoreMenu = false
                                    showDeletePageDialog = true
                                }
                            )
                        }
                    }

                    // Settings & Preferences
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("canvas_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Preferences",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(

                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Sub-row: Page navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Page controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.goToPage(currentPageIndex - 1) },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(28.dp).testTag("prev_page_button")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                    }

                    Text(
                        text = "Page ${currentPageIndex + 1} of ${document.pages.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { viewModel.goToPage(currentPageIndex + 1) },
                        enabled = currentPageIndex < document.pages.size - 1,
                        modifier = Modifier.size(28.dp).testTag("next_page_button")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                    }

                    // Add Page button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clickable { viewModel.addPage() }
                            .testTag("add_page_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("New Page", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Page Clear & Delete Actions
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.size(28.dp).testTag("quick_clear_page_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear Page",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (document.pages.size > 1) {
                        IconButton(
                            onClick = { showDeletePageDialog = true },
                            modifier = Modifier.size(28.dp).testTag("quick_delete_page_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Page",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

            }
        }
    }

    // Dialog: Edit Document Info
    if (showEditInfoDialog) {
        var editTitle by remember { mutableStateOf(document.title) }
        var editQuestion by remember { mutableStateOf(document.questionNumber) }
        var selectedSubject by remember { mutableStateOf(document.subject) }

        AlertDialog(
            onDismissRequest = { showEditInfoDialog = false },
            title = { Text("Edit NEET Question Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editQuestion,
                        onValueChange = { editQuestion = it },
                        label = { Text("Question #") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Topic / Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeetSubject.values().forEach { subj ->
                            val isSel = selectedSubject == subj
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(subj.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSubject = subj }
                            ) {
                                Text(
                                    text = subj.displayName,
                                    fontSize = 10.sp,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDocumentInfo(editTitle, editQuestion)
                        viewModel.setSubject(selectedSubject)
                        showEditInfoDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInfoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Clear Page Confirmation
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Current Page?") },
            text = { Text("All handwriting and drawings on Page ${currentPageIndex + 1} will be erased. You can still undo this action.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCurrentPage()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Page Confirmation
    if (showDeletePageDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePageDialog = false },
            title = { Text("Delete Page ${currentPageIndex + 1}?") },
            text = { Text("This page will be removed from your answer document.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCurrentPage()
                        showDeletePageDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
