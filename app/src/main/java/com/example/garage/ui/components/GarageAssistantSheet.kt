package com.example.garage.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.garage.ui.assistant.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageAssistantSheet(
    onDismiss: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState()
    val state by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Garage Assistant",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::onPromptChange,
                label = { Text("Ask about your vehicles...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.askAssistant() })
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = viewModel::askAssistant,
                enabled = !state.isLoading && state.prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Ask")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (state.response.isNotBlank()) {
                Text(text = state.response, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
