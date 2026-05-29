package com.g2b.bidapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDatePicker(
    initialDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "날짜 선택",
    selectableDates: SelectableDates = object : SelectableDates {},
    confirmText: String = "확인",
    dismissText: String = "취소",
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = selectableDates,
    )

    val isConfirmEnabled by remember {
        derivedStateOf { state.selectedDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onDateSelected)
                    onDismiss()
                },
                enabled = isConfirmEnabled,
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
    ) {
        DatePicker(
            state = state,
            showModeToggle = true,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = 24.dp, end = 12.dp, top = 16.dp
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDateRangePicker(
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null,
    onRangeSelected: (startMillis: Long?, endMillis: Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "날짜 범위 선택",
    confirmText: String = "확인",
    dismissText: String = "취소",
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis,
        initialSelectedEndDateMillis = initialEndDateMillis,
    )
    val isConfirmEnabled by remember {
        derivedStateOf { state.selectedEndDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onRangeSelected(state.selectedStartDateMillis, state.selectedEndDateMillis)
                    onDismiss()
                },
                enabled = isConfirmEnabled,
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
    ) {
        DateRangePicker(
            state = state,
            headline = {
                val fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                val startText = state.selectedStartDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().format(fmt) }
                    ?: "시작일"
                val endText = state.selectedEndDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().format(fmt) }
                    ?: "종료일"

                Text(
                    text = "$startText ~ $endText",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            },
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}