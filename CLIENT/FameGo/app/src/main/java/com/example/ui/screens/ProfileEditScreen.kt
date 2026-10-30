package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.ui.components.FameGoButton
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

/**
 * Full self-service profile: name / phone / company / date of birth persist
 * to profiles, plus a guarded danger zone. Delete wipes the backend for real
 * (delete-account Edge Function) — bookings, chats, favorites, tokens, the
 * auth row itself — then signs this device out.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
  onBack: () -> Unit,
  onDeleted: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val scope = rememberCoroutineScope()

  var name by remember(currentUser.id) { mutableStateOf(currentUser.name) }
  var phone by remember(currentUser.id) { mutableStateOf(currentUser.phone) }
  var company by remember(currentUser.id) { mutableStateOf(currentUser.companyName) }
  var dobMillis by remember(currentUser.id) {
    mutableStateOf(currentUser.dob.takeIf { it.isNotBlank() }?.let { runCatching {
      java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it)?.time
    }.getOrNull() })
  }
  var showDobPicker by remember { mutableStateOf(false) }
  var saving by remember { mutableStateOf(false) }
  var saveMsg by remember { mutableStateOf<String?>(null) }
  var saveOk by remember { mutableStateOf(false) }

  var confirmDelete by remember { mutableStateOf(false) }
  var deleteText by remember { mutableStateOf("") }
  var deleting by remember { mutableStateOf(false) }
  var deleteError by remember { mutableStateOf<String?>(null) }

  fun save() {
    saveMsg = null
    saveOk = false
    saving = true
    scope.launch {
      FameGoRepository.updateProfile(
        name = name,
        phone = phone,
        companyName = company,
        dobIso = dobMillis?.let { profileDobIso(it) }.orEmpty()
      ).onSuccess {
        saveOk = true
        saveMsg = "Profile saved."
      }.onFailure {
        saveMsg = FameGoRepository.friendlyMessage(it)
      }
      saving = false
    }
  }

  fun destroy() {
    deleteError = null
    deleting = true
    scope.launch {
      FameGoRepository.deleteAccount()
        .onSuccess { onDeleted() }
        .onFailure { deleteError = FameGoRepository.friendlyMessage(it) }
      deleting = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(androidx.compose.ui.graphics.Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FameGoTextSecondary)
      }
      Spacer(modifier = Modifier.width(4.dp))
      Text("Edit profile", color = FameGoWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(20.dp))

    EditField(value = name, onValue = { name = it }, label = "Full name", type = KeyboardType.Text, tag = "edit_name")
    Spacer(modifier = Modifier.height(12.dp))
    EditField(value = phone, onValue = { phone = it }, label = "Phone", type = KeyboardType.Phone, tag = "edit_phone")
    Spacer(modifier = Modifier.height(12.dp))
    EditField(value = company, onValue = { company = it }, label = "Company (optional)", type = KeyboardType.Text, tag = "edit_company")
    Spacer(modifier = Modifier.height(12.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = FameGoCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { showDobPicker = true }
        .testTag("edit_dob_field")
    ) {
      Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, null, tint = FameGoTextMuted, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = dobMillis?.let { profileDobDisplay(it) } ?: "Date of birth",
          color = if (dobMillis != null) FameGoWhite else FameGoTextMuted,
          fontSize = 15.sp
        )
      }
    }

    saveMsg?.let {
      Text(
        it,
        color = if (saveOk) com.example.ui.theme.FameGoSuccessGreen else FameGoLiveRed,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 10.dp)
      )
    }
    Spacer(modifier = Modifier.height(18.dp))
    FameGoButton(
      text = if (saving) "Saving…" else "Save changes",
      onClick = { save() },
      enabled = !saving,
      modifier = Modifier.fillMaxWidth(),
      testTag = "edit_save_button"
    )

    Spacer(modifier = Modifier.height(28.dp))
    Text("Danger zone", color = FameGoLiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = FameGoCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          "Delete my account",
          color = FameGoWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold
        )
        Text(
          "Wipes bookings, chats, favorites, saved spots and your login from FameGo servers — forever. This cannot be undone.",
          color = FameGoTextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
          modifier = Modifier.padding(top = 6.dp)
        )
        if (!confirmDelete) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { confirmDelete = true }
              .testTag("delete_account_start"),
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              "Delete my account…",
              color = FameGoLiveRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        } else {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            "Type DELETE to confirm:",
            color = FameGoTextMuted, fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          EditField(
            value = deleteText, onValue = { deleteText = it },
            label = "DELETE", type = KeyboardType.Text, tag = "delete_confirm_field"
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clickable { confirmDelete = false; deleteText = "" }
                .padding(vertical = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("Keep it", color = FameGoTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            FameGoButton(
              text = if (deleting) "Deleting…" else "Delete forever",
              onClick = { destroy() },
              enabled = !deleting && deleteText.trim() == "DELETE",
              modifier = Modifier.weight(1f),
              testTag = "delete_account_confirm"
            )
          }
          deleteError?.let {
            Text(it, color = FameGoLiveRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(40.dp))
  }

  if (showDobPicker) {
    val pickerState = androidx.compose.material3.rememberDatePickerState(
      initialSelectedDateMillis = dobMillis ?: run {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.YEAR, -18)
        cal.timeInMillis
      }
    )
    androidx.compose.material3.DatePickerDialog(
      onDismissRequest = { showDobPicker = false },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = { dobMillis = pickerState.selectedDateMillis; showDobPicker = false },
          enabled = pickerState.selectedDateMillis != null
        ) { Text("Done", color = FameGoGold, fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { showDobPicker = false }) {
          Text("Cancel", color = FameGoTextSecondary)
        }
      }
    ) {
      androidx.compose.material3.DatePicker(state = pickerState)
    }
  }
}

@Composable
private fun EditField(
  value: String,
  onValue: (String) -> Unit,
  label: String,
  type: KeyboardType,
  tag: String
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValue,
    label = { Text(label, color = FameGoTextMuted) },
    singleLine = true,
    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = type),
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedTextColor = FameGoWhite,
      unfocusedTextColor = FameGoWhite,
      focusedContainerColor = FameGoCard,
      unfocusedContainerColor = FameGoCard,
      focusedBorderColor = FameGoGold,
      unfocusedBorderColor = FameGoBorder
    ),
    modifier = Modifier.fillMaxWidth().testTag(tag)
  )
}

private fun profileDobIso(millis: Long): String =
  java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(millis))

private fun profileDobDisplay(millis: Long): String =
  java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date(millis))
