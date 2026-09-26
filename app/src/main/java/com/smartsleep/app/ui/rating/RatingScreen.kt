package com.smartsleep.app.ui.rating

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsleep.app.R
import com.smartsleep.app.SmartSleepApplication
import com.smartsleep.app.domain.model.WakeRating
import com.smartsleep.app.ui.common.GenericViewModelFactory

@Composable
fun RatingRoute(sessionId: Long, onDone: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SmartSleepApplication
    val viewModel: RatingViewModel = viewModel(
        factory = GenericViewModelFactory(app.repository) { RatingViewModel(it) }
    )
    RatingScreen(
        onSubmit = { rating, wokeBefore -> viewModel.submit(sessionId, rating, wokeBefore, onDone) },
        onSkip = { viewModel.skip(sessionId, onDone) }
    )
}

@Composable
fun RatingScreen(onSubmit: (WakeRating, Boolean) -> Unit, onSkip: () -> Unit) {
    var selectedRating by remember { mutableStateOf<WakeRating?>(null) }
    var wokeBeforeAlarm by remember { mutableStateOf<Boolean?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.rating_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(20.dp))

        RatingOption(WakeRating.VERY_GOOD, stringResource(R.string.rating_very_good), selectedRating) { selectedRating = it }
        RatingOption(WakeRating.GOOD, stringResource(R.string.rating_good), selectedRating) { selectedRating = it }
        RatingOption(WakeRating.OK, stringResource(R.string.rating_ok), selectedRating) { selectedRating = it }
        RatingOption(WakeRating.BAD, stringResource(R.string.rating_bad), selectedRating) { selectedRating = it }
        RatingOption(WakeRating.VERY_BAD, stringResource(R.string.rating_very_bad), selectedRating) { selectedRating = it }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.woke_before_alarm_question), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { wokeBeforeAlarm = true }, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(R.string.yes) + if (wokeBeforeAlarm == true) " ✓" else "")
            }
            OutlinedButton(onClick = { wokeBeforeAlarm = false }) {
                Text(stringResource(R.string.no) + if (wokeBeforeAlarm == false) " ✓" else "")
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { selectedRating?.let { onSubmit(it, wokeBeforeAlarm ?: false) } },
            enabled = selectedRating != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.save))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.skip))
        }
    }
}

@Composable
private fun RatingOption(rating: WakeRating, label: String, selected: WakeRating?, onSelect: (WakeRating) -> Unit) {
    val isSelected = rating == selected
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(selected = isSelected, onClick = { onSelect(rating) })
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
