package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

@Composable
fun SymbolTextEditDialog(initialText: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Edit symbol") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Symbol or text") }) },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
