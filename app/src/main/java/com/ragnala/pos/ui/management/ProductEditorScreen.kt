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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ragnala.pos.data.db.IngredientEntity
import com.ragnala.pos.data.db.ModifierGroupEntity
import com.ragnala.pos.ui.components.RagnalaPrimaryButton
import com.ragnala.pos.ui.components.RagnalaSectionHeader
import com.ragnala.pos.ui.components.RagnalaCard
import com.ragnala.pos.ui.theme.RagnalaSpacing
import com.ragnala.pos.ui.theme.RagnalaRadius

@Composable
fun ProductEditorScreen(
    state: ProductEditorState,
    categories: List<CategoryEntity>,
    groups: List<ModifierGroupEntity> = emptyList(),
    ingredients: List<IngredientEntity> = emptyList(),
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAvailableChange: (Boolean) -> Unit,
    onImagePathChange: (String?) -> Unit,
    onToggleGroup: (String) -> Unit = {},
    onAddCategory: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onAddModifierGroup: (
        String, Boolean, Int, Int, List<Pair<String, Long>>, (Boolean) -> Unit,
    ) -> Unit = { _, _, _, _, _, _ -> },
    onAddRecipeRow: () -> Unit = {},
    onRemoveRecipeRow: (Int) -> Unit = {},
    onRecipeIngredientChange: (Int, String?) -> Unit = { _, _ -> },
    onRecipeQuantityChange: (Int, String) -> Unit = { _, _ -> },
    onSave: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.savedProductId) {
        if (state.savedProductId != null) onSaved()
    }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }

    if (showCategoryDialog) {
        AddCategoryDialog(
            onConfirm = { name ->
                onAddCategory(name) { success ->
                    if (success) showCategoryDialog = false
                }
            },
            onDismiss = { showCategoryDialog = false },
        )
    }

    if (showGroupDialog) {
        AddModifierGroupDialog(
            onConfirm = { name, required, min, max, options ->
                onAddModifierGroup(name, required, min, max, options) { success ->
                    if (success) showGroupDialog = false
                }
            },
            onDismiss = { showGroupDialog = false },
        )
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

                RagnalaSectionHeader("Basic information")
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
                OutlinedButton(
                    onClick = { showCategoryDialog = true },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.mgmt_add_category)) }


                RagnalaSectionHeader("Menu appearance")
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

                RagnalaSectionHeader("Customization")
                ModifierGroupAssignment(
                    groups = groups,
                    selectedIds = state.selectedGroupIds,
                    onToggle = onToggleGroup,
                    enabled = !state.saving,
                    onAddGroup = { showGroupDialog = true },
                )

                RagnalaSectionHeader("Recipe & Stock")
                RecipeIngredientsSection(
                    drafts = state.recipeItems,
                    ingredients = ingredients,
                    enabled = !state.saving,
                    onAdd = onAddRecipeRow,
                    onRemove = onRemoveRecipeRow,
                    onIngredientChange = onRecipeIngredientChange,
                    onQuantityChange = onRecipeQuantityChange,
                )

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                RagnalaPrimaryButton(
                    text = if (state.saving) "Saving…" else stringResource(R.string.mgmt_save_product),
                    onClick = onSave,
                    enabled = !state.saving && categories.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
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
    onAddGroup: () -> Unit,
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
                            append(" \u00b7 ${stringResource(R.string.mgmt_pick_label)} $range")
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
        if (enabled) {
            OutlinedButton(
                onClick = onAddGroup,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text(stringResource(R.string.mgmt_add_modifier_group)) }
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
@Composable
private fun RecipeIngredientsSection(
    drafts: List<RecipeDraft>,
    ingredients: List<IngredientEntity>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onIngredientChange: (Int, String?) -> Unit,
    onQuantityChange: (Int, String) -> Unit,
    enabled: Boolean,
) {
    var pickerFor by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mgmt_ingredients_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.mgmt_ingredients_optional_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        drafts.forEachIndexed { index, draft ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (enabled) pickerFor = index },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = ingredients
                                .firstOrNull { it.id == draft.ingredientId }
                                ?.let { "${it.name} (${it.unit})" }
                                ?: stringResource(R.string.mgmt_ingredient_picker_hint),
                            maxLines = 1,
                        )
                    }
                    OutlinedTextField(
                        value = draft.quantity,
                        onValueChange = { text ->
                            if (text.length <= 12 && text.all { it.isDigit() || it == '.' }) {
                                onQuantityChange(index, text)
                            }
                        },
                        label = { Text(stringResource(R.string.mgmt_quantity)) },
                        singleLine = true,
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(min = 96.dp),
                    )
                    IconButton(onClick = { if (enabled) onRemove(index) }, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.mgmt_remove),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.mgmt_recipe_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (ingredients.isNotEmpty() && enabled) {
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.mgmt_add_ingredient)) }
        }
    }

    pickerFor?.let { rowIndex ->
        AlertDialog(
            onDismissRequest = { pickerFor = null },
            title = { Text(stringResource(R.string.mgmt_ingredients_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ingredients.isEmpty()) {
                        Text(stringResource(R.string.mgmt_no_ingredients))
                    }
                    ingredients.forEach { ingredient ->
                        Surface(
                            onClick = { onIngredientChange(rowIndex, ingredient.id); pickerFor = null },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${ingredient.name} (${ingredient.unit})",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerFor = null }) {
                    Text(stringResource(R.string.mgmt_cancel))
                }
            },
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mgmt_add_category)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.mgmt_category_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.mgmt_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mgmt_cancel)) }
        },
    )
}

@Composable
private fun AddModifierGroupDialog(
    onConfirm: (String, Boolean, Int, Int, List<Pair<String, Long>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(false) }
    var minInput by remember { mutableStateOf("0") }
    var maxInput by remember { mutableStateOf("1") }
    var options by remember { mutableStateOf(listOf("" to "")) }

    fun parsedMin(): Int? = minInput.toIntOrNull()
    fun parsedMax(): Int? = maxInput.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mgmt_add_modifier_group)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.mgmt_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mgmt_group_required),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = required, onCheckedChange = { required = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = { text -> if (text.length <= 3 && text.all(Char::isDigit)) minInput = text },
                        label = { Text(stringResource(R.string.mgmt_group_min)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = maxInput,
                        onValueChange = { text -> if (text.length <= 3 && text.all(Char::isDigit)) maxInput = text },
                        label = { Text(stringResource(R.string.mgmt_group_max)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.mgmt_group_options),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                options.forEachIndexed { index, (optionName, optionPrice) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = optionName,
                            onValueChange = { value ->
                                options = options.toMutableList().also { it[index] = value to optionPrice }
                            },
                            label = { Text(stringResource(R.string.mgmt_option_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = optionPrice,
                            onValueChange = { value ->
                                if (value.length <= 10 && value.all { it.isDigit() || it == '-' }) {
                                    options = options.toMutableList().also { it[index] = optionName to value }
                                }
                            },
                            label = { Text(stringResource(R.string.mgmt_option_price)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (options.size > 1) {
                                    options = options.filterIndexed { i, _ -> i != index }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.mgmt_remove),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { options = options + ("" to "") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.mgmt_add_option)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val min = parsedMin() ?: 0
                    val max = parsedMax() ?: 1
                    val parsedOptions = options.mapNotNull { (optionName, optionPrice) ->
                        val trimmedName = optionName.trim()
                        if (trimmedName.isEmpty()) return@mapNotNull null
                        val price = if (optionPrice.isBlank()) 0L
                        else optionPrice.trim().removePrefix("-").toLongOrNull()?.let { if (optionPrice.trim().startsWith("-")) -it else it }
                        if (price == null) null else trimmedName to price
                    }
                    onConfirm(name, required, min, max, parsedOptions)
                },
                enabled = name.isNotBlank() &&
                    (minInput.toIntOrNull() ?: 0) >= 0 &&
                    (minInput.toIntOrNull() ?: 0) <= (maxInput.toIntOrNull() ?: 0) &&
                    options.any { it.first.isNotBlank() && it.second.all { c -> c.isDigit() || c == '-' } },
            ) { Text(stringResource(R.string.mgmt_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.mgmt_cancel)) }
        },
    )
}
