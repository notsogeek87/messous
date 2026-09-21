package com.budgetflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetflow.engine.recognition.RecognizableService
import com.budgetflow.engine.recognition.ServiceMatch

/**
 * The discreet, live list of service suggestions shown right under a name/description field while
 * the user types (spec sections 3 and 15): no "search" button - it simply appears as [matches]
 * goes from empty to non-empty, with a light animation, and disappears the same way. Never shown
 * for text nothing was recognized from (spec section 10): that's controlled by the caller passing
 * an empty [matches] list, this composable has no opinion on when a match is "good enough".
 */
@Composable
fun ServiceSuggestionsPanel(
    matches: List<ServiceMatch>,
    onSelect: (RecognizableService) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = matches.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                matches.forEachIndexed { index, match ->
                    ServiceSuggestionRow(service = match.service, onClick = { onSelect(match.service) })
                    if (index != matches.lastIndex) Divider()
                }
            }
        }
    }
}

@Composable
private fun ServiceSuggestionRow(service: RecognizableService, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ServiceLogo(service = service, size = 32.dp)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = service.name, style = MaterialTheme.typography.bodyLarge)
            val subtitle = listOfNotNull(service.category, service.subCategory).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
