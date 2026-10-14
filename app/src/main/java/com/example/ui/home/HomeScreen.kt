package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnswerDocument
import com.example.model.NeetSubject
import com.example.model.PaperStyle
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenCanvas: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val allAnswers by viewModel.allAnswers.collectAsState()
    val currentDoc by viewModel.currentDocument.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf<NeetSubject?>(null) }
    var showNewAnswerDialog by remember { mutableStateOf(false) }

    // Dialog for renaming
    var renameTargetDoc by remember { mutableStateOf<AnswerDocument?>(null) }

    val filteredAnswers = allAnswers.filter { doc ->
        val matchesSearch = doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.questionNumber.contains(searchQuery, ignoreCase = true)
        val matchesSubject = selectedSubjectFilter == null || doc.subject == selectedSubjectFilter
        matchesSearch && matchesSubject
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("home_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E3A8A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "InkKey",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NEET Handwriting • ChatGPT Checker",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewAnswerDialog = true },
                containerColor = Color(0xFF10A37F),
                contentColor = Color.White,
                modifier = Modifier.testTag("home_fab_new_answer")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Answer")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Hero Card: Resume Last / Unfinished Answer
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_answer_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(currentDoc.subject.colorHex)
                            ) {
                                Text(
                                    text = currentDoc.subject.displayName,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${currentDoc.pages.size} page(s)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${currentDoc.questionNumber}: ${currentDoc.title}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Last active handwriting workspace",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.resumeLastAnswer()
                                onOpenCanvas()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_writing_button")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Writing", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Create Row for NEET Subjects
            item {
                Text(
                    text = "New NEET Solution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectQuickCard(
                        title = "Physics",
                        subtitle = "Numericals",
                        color = Color(0xFF2563EB),
                        icon = Icons.Default.ElectricBolt,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.startNewAnswer(
                                subject = NeetSubject.PHYSICS,
                                paperStyle = PaperStyle.RULED,
                                questionNum = "Q.${allAnswers.size + 1}",
                                title = "Physics Numerical Solution"
                            )
                            onOpenCanvas()
                        }
                    )

                    SubjectQuickCard(
                        title = "Chemistry",
                        subtitle = "Reactions",
                        color = Color(0xFFD97706),
                        icon = Icons.Default.Science,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.startNewAnswer(
                                subject = NeetSubject.CHEMISTRY,
                                paperStyle = PaperStyle.RULED,
                                questionNum = "Q.${allAnswers.size + 1}",
                                title = "Reaction & Mechanism"
                            )
                            onOpenCanvas()
                        }
                    )

                    SubjectQuickCard(
                        title = "Biology",
                        subtitle = "Diagrams",
                        color = Color(0xFF059669),
                        icon = Icons.Default.Biotech,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.startNewAnswer(
                                subject = NeetSubject.BIOLOGY,
                                paperStyle = PaperStyle.PLAIN_WHITE,
                                questionNum = "Q.${allAnswers.size + 1}",
                                title = "Diagram & Labels"
                            )
                            onOpenCanvas()
                        }
                    )

                    SubjectQuickCard(
                        title = "AMOLED",
                        subtitle = "Dark Mode",
                        color = Color(0xFF09090B),
                        icon = Icons.Default.DarkMode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.startNewAnswer(
                                subject = NeetSubject.PHYSICS,
                                paperStyle = PaperStyle.AMOLED_BLACK,
                                questionNum = "Q.${allAnswers.size + 1}",
                                title = "AMOLED Dark Solution"
                            )
                            onOpenCanvas()
                        }
                    )
                }
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search answers by title or Q#...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_answers_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChipCustom(
                        label = "All",
                        selected = selectedSubjectFilter == null,
                        onClick = { selectedSubjectFilter = null }
                    )
                    NeetSubject.values().forEach { subj ->
                        FilterChipCustom(
                            label = subj.displayName,
                            selected = selectedSubjectFilter == subj,
                            color = Color(subj.colorHex),
                            onClick = { selectedSubjectFilter = subj }
                        )
                    }
                }
            }

            // Recent Answers Section
            item {
                Text(
                    text = "Saved Answers (${filteredAnswers.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredAnswers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No answers matching \"$searchQuery\"" else "No saved answers yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap the + button to write your first handwritten answer!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredAnswers, key = { it.id }) { answer ->
                    AnswerCard(
                        document = answer,
                        onClick = {
                            viewModel.openAnswer(answer.id)
                            onOpenCanvas()
                        },
                        onDuplicate = { viewModel.duplicateAnswer(answer) },
                        onRename = { renameTargetDoc = answer },
                        onDelete = { viewModel.deleteAnswer(answer.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Modal: New Answer Custom Setup
    if (showNewAnswerDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newQuestionNum by remember { mutableStateOf("Q.${allAnswers.size + 1}") }
        var newSubject by remember { mutableStateOf(NeetSubject.PHYSICS) }
        var newPaperStyle by remember { mutableStateOf(PaperStyle.RULED) }

        AlertDialog(
            onDismissRequest = { showNewAnswerDialog = false },
            title = { Text("Start New NEET Answer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newQuestionNum,
                        onValueChange = { newQuestionNum = it },
                        label = { Text("Question Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Topic / Title (e.g., Optics Ray Diagram)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeetSubject.values().forEach { subj ->
                            val isSel = newSubject == subj
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(subj.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newSubject = subj }
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

                    Text("Paper Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(PaperStyle.RULED, PaperStyle.GRAPH, PaperStyle.PLAIN_WHITE, PaperStyle.AMOLED_BLACK).forEach { style ->
                            val isSel = newPaperStyle == style
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { newPaperStyle = style }
                            ) {
                                Text(
                                    text = if (style == PaperStyle.AMOLED_BLACK) "AMOLED" else style.displayName,
                                    fontSize = 10.sp,
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
                Button(
                    onClick = {
                        val title = if (newTitle.isBlank()) "${newSubject.displayName} Solution" else newTitle
                        viewModel.startNewAnswer(
                            subject = newSubject,
                            paperStyle = newPaperStyle,
                            questionNum = newQuestionNum,
                            title = title
                        )
                        showNewAnswerDialog = false
                        onOpenCanvas()
                    }
                ) {
                    Text("Open Canvas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAnswerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Rename Answer
    renameTargetDoc?.let { target ->
        var editTitle by remember { mutableStateOf(target.title) }
        var editQNum by remember { mutableStateOf(target.questionNumber) }

        AlertDialog(
            onDismissRequest = { renameTargetDoc = null },
            title = { Text("Rename Answer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editQNum,
                        onValueChange = { editQNum = it },
                        label = { Text("Question #") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameAnswer(target.id, editTitle, editQNum)
                        renameTargetDoc = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetDoc = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SubjectQuickCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilterChipCustom(
    label: String,
    selected: Boolean,
    color: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (color != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnswerCard(
    document: AnswerDocument,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(document.updatedAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("answer_card_${document.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(document.subject.colorHex))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(document.subject.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = document.subject.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(document.subject.colorHex),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${document.pages.size} page(s)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${document.questionNumber}: ${document.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Answer Actions")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Canvas") },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
