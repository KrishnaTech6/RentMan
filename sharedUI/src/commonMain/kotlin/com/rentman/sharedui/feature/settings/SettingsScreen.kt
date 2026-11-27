package com.rentman.sharedui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentman.domain.DomainError
import com.rentman.domain.messages.MessageKind
import com.rentman.domain.settings.AppSettings
import com.rentman.domain.settings.SettingsField
import com.rentman.domain.tenancy.FieldProblem
import com.rentman.sharedui.components.ActionButton
import com.rentman.sharedui.components.ActionRow
import com.rentman.sharedui.components.CenteredMessage
import com.rentman.sharedui.components.kindLabel
import com.rentman.sharedui.components.Divider
import com.rentman.sharedui.components.ErrorMessage
import com.rentman.sharedui.components.ScreenTopBar
import com.rentman.sharedui.components.Section
import com.rentman.sharedui.components.Segmented
import com.rentman.sharedui.components.StepButton
import com.rentman.sharedui.components.TextRow
import com.rentman.sharedui.components.label
import com.rentman.sharedui.components.message
import com.rentman.sharedui.components.rememberBackupFiles
import com.rentman.sharedui.components.rememberNotificationPermission
import com.rentman.sharedui.components.timeLabel
import com.rentman.sharedui.feature.settings.SettingsIntent.Back
import com.rentman.sharedui.feature.settings.SettingsIntent.BackUpNow
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShareFailed
import com.rentman.sharedui.feature.settings.SettingsIntent.OpenDrive
import com.rentman.sharedui.feature.settings.SettingsIntent.OpenMessage
import com.rentman.sharedui.feature.settings.SettingsIntent.BackupShared
import com.rentman.sharedui.feature.settings.SettingsIntent.RateChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.RepeatChanged
import com.rentman.sharedui.feature.settings.SettingsIntent.RestoreFrom
import com.rentman.sharedui.feature.settings.SettingsIntent.Retry
import com.rentman.sharedui.feature.settings.SettingsIntent.Save
import com.rentman.sharedui.feature.settings.SettingsIntent.TimeStepped
import com.rentman.sharedui.feature.settings.SettingsIntent.UndoRestore
import com.rentman.sharedui.feature.settings.SettingsUiState.Content
import com.rentman.sharedui.feature.settings.SettingsUiState.Error
import com.rentman.sharedui.feature.settings.SettingsUiState.Loading
import com.rentman.sharedui.mvi.NavigationEffect
import com.rentman.sharedui.resources.Res
import com.rentman.sharedui.resources.messages_title
import com.rentman.sharedui.resources.drive_hint
import com.rentman.sharedui.resources.drive_title
import com.rentman.sharedui.resources.backup_last
import com.rentman.sharedui.resources.backup_never
import com.rentman.sharedui.resources.backup_now
import com.rentman.sharedui.resources.backup_privacy
import com.rentman.sharedui.resources.backup_restore
import com.rentman.sharedui.resources.backup_restored
import com.rentman.sharedui.resources.backup_share_failed
import com.rentman.sharedui.resources.backup_title
import com.rentman.sharedui.resources.backup_undo
import com.rentman.sharedui.resources.backup_undo_hint
import com.rentman.sharedui.resources.backup_undone
import com.rentman.sharedui.resources.earlier_time
import com.rentman.sharedui.resources.field_global_rate
import com.rentman.sharedui.resources.field_reminder_time
import com.rentman.sharedui.resources.later_time
import com.rentman.sharedui.resources.loading
import com.rentman.sharedui.resources.notifications_allow
import com.rentman.sharedui.resources.notifications_hint
import com.rentman.sharedui.resources.problem_rate
import com.rentman.sharedui.resources.problem_required
import com.rentman.sharedui.resources.rate_hint
import com.rentman.sharedui.resources.repeat_every_2
import com.rentman.sharedui.resources.repeat_every_3
import com.rentman.sharedui.resources.repeat_once
import com.rentman.sharedui.resources.save_settings
import com.rentman.sharedui.resources.section_rate
import com.rentman.sharedui.resources.section_reminders
import com.rentman.sharedui.resources.settings_title
import com.rentman.sharedui.theme.RentManColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val TIME_STEP_MINUTES = 30

/** The only composable that knows the ViewModel (ARCHITECTURE.md §5). */
@Composable
internal fun SettingsRoute(
    onOpenMessage: (MessageKind) -> Unit,
    onOpenDrive: () -> Unit,
    onDone: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val files = rememberBackupFiles(onPicked = { viewModel.onIntent(RestoreFrom(it)) })
    NavigationEffect(viewModel.navigation) { event ->
        when (event) {
            SettingsNavEvent.Close -> onDone()
            is SettingsNavEvent.OpenMessage -> onOpenMessage(event.kind)
            SettingsNavEvent.OpenDrive -> onOpenDrive()
            is SettingsNavEvent.ShareBackup ->
                viewModel.onIntent(if (files.share(event.fileName, event.json)) BackupShared else BackupShareFailed)
        }
    }
    SettingsScreen(state, viewModel::onIntent, onPickBackup = files.pick)
}

@Composable
internal fun SettingsScreen(state: SettingsUiState, onIntent: (SettingsIntent) -> Unit, onPickBackup: () -> Unit) {
    Column(Modifier.fillMaxSize().background(RentManColors.Background)) {
        ScreenTopBar(title = stringResource(Res.string.settings_title), onBack = { onIntent(Back) })
        when (state) {
            Loading -> CenteredMessage(stringResource(Res.string.loading))
            is Error -> ErrorMessage(state.error, onRetry = { onIntent(Retry) })
            is Content -> Form(state, onIntent, onPickBackup)
        }
    }
}

@Composable
private fun Form(state: Content, onIntent: (SettingsIntent) -> Unit, onPickBackup: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Section(stringResource(Res.string.section_rate)) {
                TextRow(
                    label = stringResource(Res.string.field_global_rate),
                    value = state.rate,
                    onValueChange = { onIntent(RateChanged(it)) },
                    problem = rateProblem(state),
                    keyboardType = KeyboardType.Decimal,
                    hint = stringResource(Res.string.rate_hint),
                )
            }
            Section(stringResource(Res.string.section_reminders)) {
                val notifications = rememberNotificationPermission()
                // Only once the phone has said "not allowed": while it is still checking, the row would flash up and vanish.
                if (notifications.allowed == false) {
                    ActionRow(
                        stringResource(Res.string.notifications_allow),
                        onClick = notifications.request,
                        hint = stringResource(Res.string.notifications_hint),
                    )
                    Divider()
                }
                TimeStepper(state.reminderMinuteOfDay, onStep = { onIntent(TimeStepped(it)) })
                Segmented(
                    options = AppSettings.REPEAT_CHOICES.map { repeatLabel(it) },
                    selectedIndex = AppSettings.REPEAT_CHOICES.indexOf(state.reminderRepeatDays),
                    onSelect = { onIntent(RepeatChanged(AppSettings.REPEAT_CHOICES[it])) },
                )
            }
            Section(stringResource(Res.string.messages_title)) {
                MessageKind.entries.forEachIndexed { index, kind ->
                    if (index > 0) Divider()
                    ActionRow(kind.kindLabel(), onClick = { onIntent(OpenMessage(kind)) })
                }
            }
            Section(stringResource(Res.string.drive_title)) {
                ActionRow(stringResource(Res.string.drive_title), onClick = { onIntent(OpenDrive) }, hint = stringResource(Res.string.drive_hint))
            }
            Section(stringResource(Res.string.backup_title)) {
                BasicText(
                    text = state.lastBackupOn?.let { stringResource(Res.string.backup_last, it.label()) } ?: stringResource(Res.string.backup_never),
                    style = TextStyle(fontSize = 15.sp, color = RentManColors.Text),
                    modifier = Modifier.padding(top = 12.dp),
                )
                ActionRow(stringResource(Res.string.backup_now), onClick = { onIntent(BackUpNow) }, enabled = !state.isWorking)
                Divider()
                ActionRow(
                    stringResource(Res.string.backup_restore),
                    onClick = onPickBackup,
                    hint = stringResource(Res.string.backup_privacy),
                    enabled = !state.isWorking,
                )
                state.undoRestoreOn?.let { day ->
                    Divider()
                    ActionRow(
                        stringResource(Res.string.backup_undo),
                        onClick = { onIntent(UndoRestore) },
                        hint = stringResource(Res.string.backup_undo_hint, day.label()),
                        enabled = !state.isWorking,
                    )
                }
                state.backupMessage?.let {
                    val failed = it == BackupMessage.ShareFailed
                    BasicText(
                        stringResource(
                            when (it) {
                                BackupMessage.Restored -> Res.string.backup_restored
                                BackupMessage.RestoreUndone -> Res.string.backup_undone
                                BackupMessage.ShareFailed -> Res.string.backup_share_failed
                            },
                        ),
                        style = TextStyle(fontSize = 14.sp, color = if (failed) RentManColors.Danger else RentManColors.Success),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                state.backupError?.let {
                    BasicText(it.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(bottom = 12.dp))
                }
            }
            state.saveError?.let {
                BasicText(it.message(), style = TextStyle(fontSize = 14.sp, color = RentManColors.Danger), modifier = Modifier.padding(4.dp))
            }
        }
        ActionButton(
            text = stringResource(Res.string.save_settings),
            enabled = !state.isSaving,
            onClick = { onIntent(Save(state.toForm())) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun rateProblem(state: Content): String? = state.problems[SettingsField.Rate]?.let { problem ->
    stringResource(if (problem == FieldProblem.Required) Res.string.problem_required else Res.string.problem_rate)
}

@Composable
private fun repeatLabel(days: Int): String = stringResource(
    when (days) {
        2 -> Res.string.repeat_every_2
        3 -> Res.string.repeat_every_3
        else -> Res.string.repeat_once
    },
)

@Composable
private fun TimeStepper(minuteOfDay: Int, onStep: (Int) -> Unit) {
    val earlier = stringResource(Res.string.earlier_time)
    val later = stringResource(Res.string.later_time)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(Res.string.field_reminder_time), style = TextStyle(fontSize = 13.sp, color = RentManColors.TextMuted))
            BasicText(
                text = timeLabel(minuteOfDay),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RentManColors.Text),
            )
        }
        StepButton("‹", earlier) { onStep(-TIME_STEP_MINUTES) }
        StepButton("›", later) { onStep(TIME_STEP_MINUTES) }
    }
}

private val previewContent = Content(rate = "8", reminderMinuteOfDay = 540, reminderRepeatDays = 3)

@Preview
@Composable
private fun LoadingPreview() = SettingsScreen(Loading, {}, {})

@Preview
@Composable
private fun ErrorPreview() = SettingsScreen(Error(DomainError.Storage("disk")), {}, {})

@Preview
@Composable
private fun ContentPreview() = SettingsScreen(previewContent, {}, {})

@Preview
@Composable
private fun WithProblemPreview() = SettingsScreen(
    previewContent.copy(rate = "abc", problems = mapOf(SettingsField.Rate to FieldProblem.Invalid)),
    {},
    {},
)

@Preview
@Composable
private fun BackedUpPreview() = SettingsScreen(
    previewContent.copy(lastBackupOn = LocalDate(2026, 11, 24), undoRestoreOn = LocalDate(2026, 12, 1), backupMessage = BackupMessage.Restored),
    {},
    {},
)
