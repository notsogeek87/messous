package com.budgetflow.app.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetflow.app.R
import com.budgetflow.app.di.ServiceLocator
import com.budgetflow.app.di.simpleViewModelFactory
import com.budgetflow.app.domain.model.Category
import com.budgetflow.app.domain.model.CategoryGroups
import com.budgetflow.app.ui.components.CategoryIcons
import com.budgetflow.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = simpleViewModelFactory { CategoriesViewModel(ServiceLocator.categoryRepository) }
    )
    val categories by viewModel.categories.collectAsState()
    var editing by remember { mutableStateOf<Category?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add))
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(icon = Icons.Filled.Add, title = stringResource(R.string.categories_add), modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                categories.groupBy { it.group }.forEach { (group, categoriesInGroup) ->
                    item(key = "header_$group") {
                        Text(
                            group,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(categoriesInGroup, key = { it.id }) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editing = category; showDialog = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(CategoryIcons.of(category.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(category.name)
                            }
                            IconButton(onClick = { viewModel.delete(category) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { viewModel.save(it); showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDialog(initial: Category?, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var group by remember { mutableStateOf(initial?.group ?: CategoryGroups.OTHER) }
    var icon by remember { mutableStateOf(initial?.icon ?: CategoryIcons.availableKeys.first()) }
    var groupExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.categories_add else R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = it }) {
                    OutlinedTextField(
                        value = group,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_group)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                        CategoryGroups.all.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { group = option; groupExpanded = false }
                            )
                        }
                    }
                }

                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelMedium)
                LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    items(CategoryIcons.availableKeys) { key ->
                        val selected = key == icon
                        Surface(
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp).clickable { icon = key }
                        ) {
                            Icon(
                                CategoryIcons.of(key),
                                contentDescription = key,
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                onSave(Category(id = initial?.id ?: 0, name = name, group = group, icon = icon, isDefault = initial?.isDefault ?: false))
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
