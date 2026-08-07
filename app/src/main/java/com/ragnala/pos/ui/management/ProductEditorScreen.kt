package com.ragnala.pos.ui.management

import com.ragnala.pos.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ragnala.pos.data.db.CategoryEntity
import com.ragnala.pos.data.db.ModifierGroupEntity

@Composable
fun ProductEditorScreen(
    state: ProductEditorState,
    categories: List<CategoryEntity>,
    groups: List<ModifierGroupEntity> = emptyList(),
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAvailableChange: (Boolean) -> Unit,
    onImagePathChange: (String?) -> Unit,
    onToggleGroup: (String) -> Unit = {},
    onSave: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.savedProductId) {
        if (state.savedProductId != null) onSaved()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.mgmt_back))
                }
                Text(
                    text = if (state.editingId != null) stringResource(R.string.mgmt_edit_product) else stringResource(R.string.mgmt_new_product),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.mgmt_add_menu_item),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.mgmt_add_menu_item_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.mgmt_product_name)) },
                    singleLine = true,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.mgmt_description)) },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.priceInput,
                    onValueChange = { input ->
                        if (input.length <= 18 && input.all { it.isDigit() || it == '.' } &&
                            parseRupiahInput(input) != null
                        ) {
                            onPriceChange(input)
                        }
                    },
                    label = { Text(stringResource(R.string.mgmt_price_rupiah)) },
                    supportingText = { Text(stringResource(R.string.mgmt_price_example)) },
                    singleLine = true,
                    enabled = !state.saving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.mgmt_category_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (categories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.mgmt_no_categories),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    categories.forEach { category ->
                        Surface(
                            onClick = { if (!state.saving) onCategoryChange(category.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (state.categoryId == category.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = state.categoryId == category.id,
                                    onClick = { if (!state.saving) onCategoryChange(category.id) },
                                    enabled = !state.saving,
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.mgmt_available_for_ordering), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.available) stringResource(R.string.mgmt_available_desc)
                                else stringResource(R.string.mgmt_unavailable_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.available,
                            onCheckedChange = onAvailableChange,
                            enabled = !state.saving,
                        )
                    }
                }

                ProductImagePicker(
                    imagePath = state.imagePath,
                    onImagePathChange = onImagePathChange,
                    enabled = !state.saving,
                )

                ModifierGroupAssignment(
                    groups = groups,
                    selectedIds = state.selectedGroupIds,
                    onToggle = onToggleGroup,
                    enabled = !state.saving,
                )

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !state.saving && categories.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.mgmt_save_product))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ModifierGroupAssignment(
    groups: List<ModifierGroupEntity>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    enabled: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mgmt_modifier_groups),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.mgmt_no_modifier_groups),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        groups.forEach { group ->
            val selected = group.id in selectedIds
            Surface(
                onClick = { if (enabled) onToggle(group.id) },
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { if (enabled) onToggle(group.id) },
                        enabled = enabled,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.bodyLarge)
                        val constraint = buildString {
                            if (group.required) append(stringResource(R.string.mgmt_required))
                            val range = if (group.minSelections == group.maxSelections)
                                group.minSelections.toString()
                            else "${group.minSelections}-${group.maxSelections}"
                            append(" Â· ${stringResource(R.string.mgmt_pick_label)} $range")
                        }
                        Text(
                            constraint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImagePicker(
    imagePath: String?,
    onImagePathChange: (String?) -> Unit,
    enabled: Boolean,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                onImagePathChange(uri.toString())
            }
        },
    )

    Text(
        text = stringResource(R.string.mgmt_product_photo),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp)),
            )
            IconButton(
                onClick = { if (enabled) onImagePathChange(null) },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.mgmt_remove_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                                MaterialTheme.colorScheme.primaryContainer.copy(0.1f),
                            ),
                        ),
                    )
                    .clickable { if (enabled) imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = stringResource(R.string.mgmt_add_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
