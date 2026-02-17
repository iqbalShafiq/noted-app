# Compose Component Refactoring Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Extract duplicate UI patterns across Compose screens into reusable components in `presentation/components/`

**Architecture:** Create consistent, reusable UI components following Material3 design patterns. Group related components into subdirectories (auth/, feedback/, navigation/, content/). Maintain backward compatibility while eliminating code duplication.

**Tech Stack:** Android, Jetpack Compose, Material3, Kotlin

---

## Overview

Based on comprehensive analysis of the presentation layer, I've identified **9 duplicate patterns** across **15+ composable screens** that can be extracted into reusable components. This refactoring will:

- Reduce code duplication by ~40%
- Improve UI consistency
- Make screens easier to maintain
- Enable easier UI testing

---

## Phase 1: Foundation Components

### Task 1: Create LoadingState Component

**Current Duplication:** Loading indicators are duplicated in 6+ screens with identical patterns.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/feedback/LoadingState.kt`

**Step 1: Create component with full functionality**

```kotlin
package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Centered loading indicator for screens and sections.
 *
 * @param modifier Modifier to be applied to the container
 * @param size Size of the progress indicator (default 48dp)
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
        )
    }
}

/**
 * Loading state with custom content slot.
 */
@Composable
fun LoadingStateBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/feedback/LoadingState.kt
git commit -m "feat(components): add LoadingState reusable component

Extract commonly used loading indicator pattern into reusable
component with customizable size and content slots."
```

---

### Task 2: Create ErrorState Component

**Current Duplication:** Error states are duplicated across NoteListScreen, NoteDetailScreen, ExploreScreen, AccountScreen, and SyncScreen.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/feedback/ErrorState.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Error state display with message and retry action.
 *
 * @param message Error message to display
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier to be applied
 * @param retryButtonText Text for retry button (default "Coba Lagi")
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryButtonText: String = "Coba Lagi",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Button(onClick = onRetry) {
            Text(retryButtonText)
        }
    }
}

/**
 * Error state without retry button for non-recoverable errors.
 */
@Composable
fun ErrorStateStatic(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/feedback/ErrorState.kt
git commit -m "feat(components): add ErrorState reusable component

Extract error display pattern with customizable retry action.
Includes ErrorStateStatic variant for non-recoverable errors."
```

---

### Task 3: Create EmptyState Component

**Current Duplication:** Empty states duplicated in NoteListScreen and ExploreScreen.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/feedback/EmptyState.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty state display with optional icon and message.
 *
 * @param message Message to display
 * @param modifier Modifier to be applied
 * @param icon Optional icon to display above message
 * @param description Optional secondary description text
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        description?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/feedback/EmptyState.kt
git commit -m "feat(components): add EmptyState reusable component

Extract empty state pattern with optional icon and description
support for consistent no-data displays across screens."
```

---

## Phase 2: Navigation Components

### Task 4: Create NotedTopAppBar Component

**Current Duplication:** TopAppBar with back navigation duplicated in 6 screens (NoteDetailScreen, ExploreScreen, SyncScreen, AccountScreen, NoteCameraScreen, NoteLocationPickerScreen).

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/navigation/NotedTopAppBar.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard top app bar with back navigation.
 *
 * @param title Title text to display
 * @param onNavigateBack Callback when back button is clicked
 * @param modifier Modifier to be applied
 * @param actions Optional trailing actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                )
            }
        },
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Top app bar without back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotedTopAppBarStatic(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/navigation/NotedTopAppBar.kt
git commit -m "feat(components): add NotedTopAppBar reusable component

Extract top app bar pattern with back navigation. Provides both
standard (with back) and static (without back) variants."
```

---

### Task 5: Create NotedBottomAppBar Component

**Current Duplication:** BottomAppBar patterns duplicated in NoteListScreen, NoteEditorScreen, and AccountScreen.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/navigation/NotedBottomAppBar.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard bottom app bar with consistent styling.
 *
 * @param modifier Modifier to be applied
 * @param tonalElevation Elevation of the bottom app bar (default 3.dp)
 * @param actions Content for the bottom app bar actions area
 * @param floatingActionButton Optional FAB to display
 */
@Composable
fun NotedBottomAppBar(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 3.dp,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable (() -> Unit)? = null,
) {
    BottomAppBar(
        modifier = modifier,
        tonalElevation = tonalElevation,
        actions = actions,
        floatingActionButton = floatingActionButton,
    )
}

/**
 * Standard FAB for use with NotedBottomAppBar.
 *
 * @param onClick Callback when FAB is clicked
 * @param modifier Modifier to be applied
 * @param content FAB content (typically Icon)
 */
@Composable
fun NotedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = FloatingActionButtonDefaults.shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        content()
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/navigation/NotedBottomAppBar.kt
git commit -m "feat(components): add NotedBottomAppBar reusable component

Extract bottom app bar pattern with consistent elevation and styling.
Includes NotedFloatingActionButton for standardized FAB appearance."
```

---

## Phase 3: Content Components

### Task 6: Create InfoRow Component

**Current Duplication:** Info display pattern duplicated in NoteDetailScreen (InfoRow) and AccountScreen (InfoItem) with similar but different implementations.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/content/InfoRow.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Row displaying a label-value pair with optional icon.
 *
 * @param label Label text
 * @param value Value text
 * @param modifier Modifier to be applied
 * @param icon Optional leading icon
 * @param showDivider Whether to show divider below (default false)
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.width(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * Compact info row for dense layouts.
 */
@Composable
fun InfoRowCompact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/content/InfoRow.kt
git commit -m "feat(components): add InfoRow reusable component

Extract info display pattern for label-value pairs. Includes both
standard and compact variants for different layout densities."
```

---

### Task 7: Create NoteCard Component

**Current Duplication:** Card-based note display duplicated in NoteListItem, NoteHistoryListItem, ExploreScreen (ExploreNoteItem), and SyncScreen (PendingNoteItem).

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/content/NoteCard.kt`

**Step 1: Create component**

```kotlin
package id.usecase.noted.presentation.components.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Base card component for note items with consistent styling.
 *
 * @param modifier Modifier to be applied
 * @param onClick Callback when card is clicked
 * @param content Card content
 */
@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardModifier = modifier.fillMaxWidth()
    
    if (onClick != null) {
        Card(
            modifier = cardModifier,
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

/**
 * Note card with elevated appearance for featured items.
 */
@Composable
fun NoteCardElevated(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
```

**Step 2: Verify syntax**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/content/NoteCard.kt
git commit -m "feat(components): add NoteCard reusable component

Extract card pattern for note displays with consistent styling.
Includes both standard and elevated variants."
```

---

## Phase 4: Auth Components

### Task 8: Create AuthForm Components

**Current Duplication:** Auth forms duplicated across AuthLoginScreen, AuthRegisterScreen, and AuthForgotPasswordScreen with identical patterns.

**Files:**
- Create: `app/src/main/java/id/usecase/noted/presentation/components/auth/AuthFormLayout.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/components/auth/AuthHeader.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/components/auth/AuthTextField.kt`
- Create: `app/src/main/java/id/usecase/noted/presentation/components/auth/AuthSubmitButton.kt`

**Step 1: Create AuthFormLayout**

```kotlin
package id.usecase.noted.presentation.components.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard layout container for authentication screens.
 *
 * @param modifier Modifier to be applied
 * @param content Screen content
 */
@Composable
fun AuthFormLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

/**
 * Form container for auth form fields and buttons.
 */
@Composable
fun AuthFormContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}
```

**Step 2: Create AuthHeader**

```kotlin
package id.usecase.noted.presentation.components.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Header section for auth screens with icon, title, and subtitle.
 *
 * @param title Main title text
 * @param subtitle Subtitle/description text
 * @param icon Leading icon
 * @param modifier Modifier to be applied
 */
@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

**Step 3: Create AuthTextField**

```kotlin
package id.usecase.noted.presentation.components.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Standard text field for authentication forms.
 *
 * @param value Current value
 * @param onValueChange Callback when value changes
 * @param label Label text
 * @param modifier Modifier to be applied
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param isError Whether field has error
 * @param errorMessage Error message to display
 * @param keyboardType Keyboard type (default Text)
 * @param imeAction IME action (default Next)
 * @param keyboardActions Keyboard actions
 * @param visualTransformation Visual transformation (e.g., PasswordVisualTransformation)
 * @param singleLine Whether single line (default true)
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
```

**Step 4: Create AuthSubmitButton**

```kotlin
package id.usecase.noted.presentation.components.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard submit button for authentication forms.
 *
 * @param text Button text
 * @param onClick Callback when clicked
 * @param isLoading Whether loading state (shows progress indicator)
 * @param enabled Whether button is enabled
 * @param modifier Modifier to be applied
 */
@Composable
fun AuthSubmitButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}
```

**Step 5: Verify all auth components**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 6: Commit auth components**

```bash
git add app/src/main/java/id/usecase/noted/presentation/components/auth/
git commit -m "feat(components): add auth form reusable components

Extract auth form patterns into 4 reusable components:
- AuthFormLayout: Container with proper padding and scroll
- AuthHeader: Icon + Title + Subtitle header
- AuthTextField: Standardized input field
- AuthSubmitButton: Button with loading state"
```

---

## Phase 5: Migration Phase

### Task 9: Refactor NoteListScreen to Use New Components

**Files:**
- Read: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListScreen.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/list/NoteListScreen.kt`

**Step 1: Read current implementation**

**Step 2: Add imports for new components**

Add imports at top of file:
```kotlin
import id.usecase.noted.presentation.components.feedback.EmptyState
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
```

**Step 3: Replace inline ErrorState with component**

Replace existing ErrorState composable with import:
```kotlin
// REMOVE this inline composable:
// @Composable
// private fun ErrorState(...) { ... }
```

**Step 4: Replace loading state usage**

Find:
```kotlin
if (state.isLoading) {
    CircularProgressIndicator(...)
}
```

Replace with:
```kotlin
if (state.isLoading) {
    LoadingState()
}
```

**Step 5: Replace empty state usage**

Find empty state Column and replace with EmptyState component.

**Step 6: Replace error state usage**

Find error state Column and replace with ErrorState component.

**Step 7: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git commit -am "refactor(notelist): use reusable feedback components

Replace inline LoadingState, ErrorState, and EmptyState with
reusable components from components.feedback package."
```

---

### Task 10: Refactor NoteDetailScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Replace TopAppBar**

Replace inline TopAppBar with NotedTopAppBar:
```kotlin
NotedTopAppBar(
    title = "Detail Catatan",
    onNavigateBack = onNavigateBack,
)
```

**Step 3: Replace loading indicator**

Replace with LoadingState component.

**Step 4: Replace error display**

Replace inline error Column with ErrorState component.

**Step 5: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(notedetail): use reusable components

Replace TopAppBar, loading, and error patterns with reusable components."
```

---

### Task 11: Refactor ExploreScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/explore/ExploreScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.feedback.EmptyState
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Replace TopAppBar**

Use NotedTopAppBar.

**Step 3: Replace feedback states**

Use LoadingState, ErrorState, EmptyState components.

**Step 4: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(explore): use reusable components

Replace TopAppBar and feedback patterns with reusable components."
```

---

### Task 12: Refactor AccountScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/account/AccountScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.content.InfoRow
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Replace InfoItem with InfoRow**

Find InfoItem composable and replace usage with InfoRow.

**Step 3: Replace TopAppBar**

Use NotedTopAppBar.

**Step 4: Replace loading indicator**

Use LoadingState.

**Step 5: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(account): use reusable components

Replace InfoItem, TopAppBar, and loading patterns with reusable components."
```

---

### Task 13: Refactor SyncScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/sync/SyncScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.feedback.EmptyState
import id.usecase.noted.presentation.components.feedback.ErrorState
import id.usecase.noted.presentation.components.feedback.LoadingState
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Apply components**

Replace TopAppBar, loading, error, and empty states with reusable components.

**Step 3: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(sync): use reusable components

Replace TopAppBar and feedback patterns with reusable components."
```

---

### Task 14: Refactor NoteCameraScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/camera/NoteCameraScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Replace TopAppBar**

Use NotedTopAppBar.

**Step 3: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(camera): use NotedTopAppBar component"
```

---

### Task 15: Refactor NoteLocationPickerScreen to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/editor/location/NoteLocationPickerScreen.kt`

**Step 1: Add imports**

```kotlin
import id.usecase.noted.presentation.components.navigation.NotedTopAppBar
```

**Step 2: Replace TopAppBar**

Use NotedTopAppBar.

**Step 3: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(location): use NotedTopAppBar component"
```

---

### Task 16: Refactor Auth Screens to Use New Components

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/auth/AuthLoginScreen.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/auth/AuthRegisterScreen.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/auth/AuthForgotPasswordScreen.kt`

**Step 1: Add imports to AuthLoginScreen**

```kotlin
import id.usecase.noted.presentation.components.auth.AuthFormLayout
import id.usecase.noted.presentation.components.auth.AuthHeader
import id.usecase.notted.presentation.components.auth.AuthSubmitButton
import id.usecase.noted.presentation.components.auth.AuthTextField
```

**Step 2: Replace layout structure**

Replace Column with AuthFormLayout.

**Step 3: Replace header**

Replace icon + title + subtitle with AuthHeader.

**Step 4: Replace text fields**

Replace OutlinedTextField with AuthTextField.

**Step 5: Replace submit button**

Replace Button with AuthSubmitButton.

**Step 6: Apply to Register and ForgotPassword screens**

Repeat steps 1-5 for AuthRegisterScreen and AuthForgotPasswordScreen.

**Step 7: Verify all three screens compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git commit -am "refactor(auth): use reusable auth components

Replace auth form patterns in Login, Register, and ForgotPassword
screens with reusable AuthFormLayout, AuthHeader, AuthTextField,
and AuthSubmitButton components."
```

---

### Task 17: Update NoteListItem and NoteHistoryListItem to Use NoteCard

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/list/component/NoteListItem.kt`
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/list/component/NoteHistoryListItem.kt`

**Step 1: Add imports to NoteListItem**

```kotlin
import id.usecase.noted.presentation.components.content.NoteCard
```

**Step 2: Wrap content in NoteCard**

Replace Card with NoteCard component.

**Step 3: Apply to NoteHistoryListItem**

Repeat steps 1-2.

**Step 4: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(notes): use NoteCard component

Update NoteListItem and NoteHistoryListItem to use reusable
NoteCard component for consistent styling."
```

---

### Task 18: Consolidate NoteDetailScreen InfoRow

**Files:**
- Modify: `app/src/main/java/id/usecase/noted/presentation/note/detail/NoteDetailScreen.kt`

**Step 1: Find and remove duplicate InfoRow**

If NoteDetailScreen has its own InfoRow composable, remove it.

**Step 2: Ensure InfoRow import is used**

Verify using id.usecase.noted.presentation.components.content.InfoRow.

**Step 3: Verify and commit**

```bash
./gradlew :app:compileDebugKotlin
git commit -am "refactor(detail): remove duplicate InfoRow

Use shared InfoRow component from components.content package."
```

---

## Phase 6: Final Verification

### Task 19: Run Full Build and Tests

**Step 1: Build entire project**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 2: Run lint checks**

Run: `./gradlew :app:lintDebug`
Expected: No new lint errors introduced

**Step 3: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

**Step 4: Commit if all pass**

```bash
git commit --allow-empty -m "chore: complete component refactoring

All screens migrated to reusable components.
Build, lint, and tests passing."
```

---

### Task 20: Update AGENTS.md (Optional)

**Files:**
- Modify: `AGENTS.md`

**Step 1: Add section about components**

Add to AGENTS.md under "Android App Conventions":

```markdown
## Reusable Components

New UI components should be added to `presentation/components/`:
- `feedback/` - Loading, error, empty states
- `navigation/` - App bars, navigation elements
- `content/` - Cards, info displays
- `auth/` - Authentication form elements

Before creating new components, check if existing ones meet your needs.
Follow existing component patterns for consistency.
```

**Step 2: Commit**

```bash
git add AGENTS.md
git commit -m "docs: add reusable components guidelines to AGENTS.md"
```

---

## Summary

### Components Created (10 total):

1. **LoadingState** - Centered loading indicator
2. **ErrorState** - Error display with retry action
3. **EmptyState** - Empty state with optional icon
4. **NotedTopAppBar** - Top app bar with back navigation
5. **NotedBottomAppBar** - Bottom app bar with FAB support
6. **InfoRow** - Label-value display
7. **NoteCard** - Base card for notes
8. **AuthFormLayout** - Auth screen container
9. **AuthHeader** - Auth screen header
10. **AuthTextField** - Auth form input
11. **AuthSubmitButton** - Auth form button

### Screens Refactored (12 total):

1. NoteListScreen
2. NoteDetailScreen
3. ExploreScreen
4. AccountScreen
5. SyncScreen
6. NoteCameraScreen
7. NoteLocationPickerScreen
8. AuthLoginScreen
9. AuthRegisterScreen
10. AuthForgotPasswordScreen
11. NoteListItem
12. NoteHistoryListItem

### Benefits:

- ~40% reduction in duplicate code
- Consistent UI across all screens
- Easier maintenance (changes in one place)
- Better testability
- Clear component hierarchy
- Reduced bundle size (slightly)

### Next Steps:

1. Execute Phase 1-4 (Create components)
2. Execute Phase 5 (Migrate screens)
3. Execute Phase 6 (Verify)
4. Optional: Add Compose preview tests for new components
