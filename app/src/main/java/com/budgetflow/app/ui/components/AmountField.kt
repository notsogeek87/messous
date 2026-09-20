package com.budgetflow.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.budgetflow.app.R

/** A text field restricted to a valid decimal amount, e.g. "17.99" or "17,99". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.transaction_amount)
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val normalized = text.replace(',', '.')
            if (normalized.isEmpty() || normalized.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                onValueChange(text)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        suffix = { Text("€") },
        modifier = modifier
    )
}

fun String.toAmountOrNull(): Double? = replace(',', '.').toDoubleOrNull()
