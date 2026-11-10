package com.stealzalut.mobile.ui.screens.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.RoundedSzlIconSet
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.theme.szlColors

/**
 * Modular Feature Guide and Help Screen.
 * Provides interactive discovery and professional documentation for all 12 tools.
 */
@Composable
fun FeatureGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<FeatureCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val icons = MaterialTheme.icons

    // Intercept system Back when inside a tool detail sub-menu
    BackHandler(enabled = selectedItemId != null) {
        selectedItemId = null
    }

    AnimatedContent(
        targetState = selectedItemId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "FeatureGuideTransition",
        modifier = modifier
    ) { activeId ->
        if (activeId == null) {
            // --- 1. LIST VIEW ---
            val filteredItems = remember(selectedCategory, searchQuery) {
                FeatureGuideRegistry.filter(
                    category = selectedCategory,
                    query = searchQuery,
                    resolveString = { context.getString(it) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = icons.actionBack,
                            contentDescription = stringResource(R.string.guide_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.guide_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.guide_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icons.actionSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall)
                )

                // Category Chips Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    // "All" chip
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(stringResource(R.string.guide_cat_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // Individual category chips
                    FeatureCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(stringResource(cat.labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

                // Tools List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        FeatureGuideCard(
                            item = item,
                            onClick = { selectedItemId = item.id }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))
                    }
                }
            }
        } else {
            // --- 2. DETAIL VIEW ---
            val item = FeatureGuideRegistry.getById(activeId)
            if (item != null) {
                FeatureGuideDetailView(
                    item = item,
                    onBack = { selectedItemId = null }
                )
            }
        }
    }
}

@Composable
private fun FeatureGuideCard(
    item: FeatureGuideItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = MaterialTheme.icons

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(AppDimens.FeatureCategoryBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.iconProvider(RoundedSzlIconSet),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(item.category.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                Text(
                    text = stringResource(item.shortDescRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = icons.actionChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }
    }
}

@Composable
private fun FeatureGuideDetailView(
    item: FeatureGuideItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = MaterialTheme.icons

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Detail Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = icons.actionBack,
                    contentDescription = stringResource(R.string.guide_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.SpacingMedium)
        ) {
            // Reference Photo Showcase (for Style Transfer)
            if (item.demoType == FeatureDemoType.STYLE_TRANSFER) {
                Text(
                    text = stringResource(R.string.guide_reference_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = AppDimens.SpacingSmall, bottom = AppDimens.SpacingSmall)
                )

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                    color = MaterialTheme.szlColors.photoDarkroom,
                    border = BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.FeatureDemoReferenceHeight)
                                .clip(RoundedCornerShape(topStart = AppDimens.CornerRadiusLarge, topEnd = AppDimens.CornerRadiusLarge))
                        ) {
                            Image(
                                painter = painterResource(R.drawable.guide_sample_reference),
                                contentDescription = stringResource(R.string.guide_sample_reference_label),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(AppDimens.SpacingSmall)
                            ) {
                                Text(
                                    text = stringResource(R.string.guide_sample_reference_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.guide_reference_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(AppDimens.SpacingMedium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))
            }

            // Interactive Demo Card
            Text(
                text = stringResource(R.string.guide_section_interactive_demo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = AppDimens.SpacingSmall, bottom = AppDimens.SpacingSmall)
            )
            InteractiveBeforeAfterSlider(demoType = item.demoType)

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            // Explanation & Best Practices
            Text(
                text = stringResource(R.string.guide_section_explanation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = AppDimens.SpacingSmall)
            )
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(item.detailedExplanationRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(AppDimens.SpacingMedium)
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            // Key Parameters
            Text(
                text = stringResource(R.string.guide_section_parameters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = AppDimens.SpacingSmall)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                item.keyParameters.forEach { param ->
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppDimens.SpacingMedium)) {
                            Text(
                                text = stringResource(param.nameRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                            Text(
                                text = stringResource(param.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingHuge))
        }
    }
}
