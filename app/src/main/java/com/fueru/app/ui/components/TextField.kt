package com.fueru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.fueru.app.ui.theme.FueruColors
import com.fueru.app.ui.theme.FueruType
import com.fueru.app.ui.theme.Radius

/** Direct port of components/core/Input.jsx. */
@Composable
fun FueruTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Radius.md)
    val borderColor = when {
        focused -> FueruColors.Fire4
        error != null -> FueruColors.SignalDanger
        else -> FueruColors.BorderSubtle
    }

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                color = FueruColors.TextSecondary,
                style = FueruType.caption,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(FueruColors.SurfaceSunken)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                textStyle = FueruType.body.copy(color = FueruColors.TextPrimary),
                cursorBrush = SolidColor(FueruColors.Fire4),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, color = FueruColors.TextMuted, style = FueruType.body)
                    }
                    innerTextField()
                },
            )
        }
        if (error != null) {
            Text(
                text = error,
                color = FueruColors.SignalDanger,
                style = FueruType.caption,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
