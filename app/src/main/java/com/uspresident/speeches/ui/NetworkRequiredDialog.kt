package com.uspresident.speeches.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uspresident.speeches.R

@Composable
fun NetworkRequiredDialog(onConfirmExit: () -> Unit) {
    BackHandler(onBack = onConfirmExit)

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.network_required_title)) },
        text = { Text(stringResource(R.string.network_required_message)) },
        confirmButton = {
            TextButton(onClick = onConfirmExit) {
                Text(stringResource(R.string.confirm))
            }
        },
    )
}
