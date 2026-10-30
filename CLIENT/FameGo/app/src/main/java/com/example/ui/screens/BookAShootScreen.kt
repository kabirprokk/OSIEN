package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.data.MapPlace
import com.example.data.MapTilerGeocoding
import com.example.model.Booking
import com.example.model.BookingStatus
import com.example.model.CrewRequirement
import com.example.model.CrewRoleType
import com.example.model.ShootCategory
import com.example.model.ShootPlan
import com.example.model.ShootLocation
import com.example.ui.components.FlowPill
import com.example.ui.components.FlowPillState
import com.example.ui.components.FameGoOutlinedButton
import com.example.ui.components.AddressSuggestList
import com.example.ui.components.LocalityChips
import com.example.ui.components.SavedLocationChips
import com.example.ui.components.MapPreviewCard
import com.example.ui.components.FameGoRadialTimePickerDialog
import com.example.ui.components.RoleCounter
import com.example.ui.components.SoftCard
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import java.util.Calendar
import java.util.UUID

/** Minutes since midnight for a 12-hour time — used for same-day gating. */
private fun callTimeToMinutes(h12: Int, minute: Int, amPm: String): Int {
  val h24 = if (amPm == "PM") h12 % 12 + 12 else h12 % 12
  return h24 * 60 + minute
}

/** True when the picked slot is at/after the minimum (now + 1h buffer). */
private fun isCallTimeValid(
  h12: Int, minute: Int, amPm: String,
  minH12: Int, minMinute: Int, minAmPm: String
): Boolean = callTimeToMinutes(h12, minute, amPm) >= callTimeToMinutes(minH12, minMinute, minAmPm)

@Composable
fun BookAShootScreen(
  plan: ShootPlan,
  preselectedCategory: ShootCategory? = null,
  onBookingReadyForSearch: (Booking) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  // Reels-only 4-step flow: When → Where → About → Summary.
  // Category is always VIDEO (reels) and crew is always 1 videographer.
  var currentStep by remember { mutableStateOf(1) }
  val category = ShootCategory.VIDEO

  // Step 1: Date & custom call time
  var selectedDate by remember { mutableStateOf("") }
  var timeHour by remember { mutableStateOf("") }
  var timeMinute by remember { mutableStateOf("") }
  var timeAmPm by remember { mutableStateOf("AM") }
  // The plan arrives preselected (launchpad / plan screen) but stays editable
  // inside the booking via the Change option in Step 1.
  var activePlan by remember(plan) { mutableStateOf(plan) }
  val durationHours = activePlan.durationHours
  val callTime: String = run {
    val h = timeHour.trim().toIntOrNull()
    val m = timeMinute.trim().toIntOrNull()
    if (h == null || m == null || h !in 1..12 || m !in 0..59) ""
    else "%02d:%02d %s".format(h, m, timeAmPm)
  }

  // Same-day rule: analyse the current time and only allow slots at least
  // 1 hour out (booking at 7 → 8 and later, never before).
  val todayMin: Triple<Int, Int, String>? =
    if (selectedDate.equals("Today", ignoreCase = true)) {
      val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 60) }
      val h24 = cal.get(Calendar.HOUR_OF_DAY)
      val m = cal.get(Calendar.MINUTE)
      var h12 = h24 % 12
      if (h12 == 0) h12 = 12
      Triple(h12, m, if (h24 >= 12) "PM" else "AM")
    } else null
  // A previously picked time may have slipped into the past — block continue.
  // (Empty fields are not "blocked": the missing-time hint covers those.)
  val timeBlocked = todayMin?.let { (mh, mm, map) ->
    val h = timeHour.trim().toIntOrNull()
    val m = timeMinute.trim().toIntOrNull()
    h != null && m != null && !isCallTimeValid(h, m, timeAmPm, mh, mm, map)
  } == true

  // Step 2: Location
  var venueName by remember { mutableStateOf("") }
  var venueAddress by remember { mutableStateOf("") }
  var locationNotes by remember { mutableStateOf("") }

  // Crew is fixed: one videographer shoots the reel.
  val crewCounts: Map<CrewRoleType, Int> =
    mapOf(CrewRoleType.VIDEOGRAPHER to 1)

  // Step 3: Brief & Optional details
  var shootTitle by remember { mutableStateOf("") }
  var shootBrief by remember {
    mutableStateOf("")
  }
  var showReference by remember { mutableStateOf(false) }
  var referenceLink by remember { mutableStateOf("") }
  var showInstagramLink by remember { mutableStateOf(false) }
  var instagramRef by remember { mutableStateOf("") }
  var showDriveLink by remember { mutableStateOf(false) }
  var driveLink by remember { mutableStateOf("") }
  var showInstructions by remember { mutableStateOf(false) }
  var specialInstructions by remember { mutableStateOf("") }

  // Cost calculation — follows the editable plan.
  val estimatedCost = activePlan.priceRupees

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Top Navigation: Back / Close & Progress indicator (Tiny dots)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            if (currentStep > 1) currentStep-- else onCancel()
          },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (currentStep > 1) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
            contentDescription = "Back",
            tint = FameGoTextSecondary
          )
        }

        // 4 Minimal progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          (1..4).forEach { stepIndex ->
            Box(
              modifier = Modifier
                .height(3.dp)
                .width(if (stepIndex == currentStep) 20.dp else 8.dp)
                .clip(CircleShape)
                .background(
                  if (stepIndex == currentStep) FameGoGold
                  else if (stepIndex < currentStep) FameGoTextMuted
                  else FameGoBorderSubtle
                )
            )
          }
        }

        // Step counter
        Text(
          text = "$currentStep of 4",
          color = FameGoTextMuted,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Conversational Content Body
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        AnimatedContent(
          targetState = currentStep,
          transitionSpec = {
            if (targetState > initialState) {
              slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
              slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
          },
          label = "wizardSteps"
        ) { step ->
          when (step) {
            // STEP 1: When should the crew arrive? (custom time)
            1 -> StepWhen(
              selectedDate = selectedDate,
              onDateSelect = { selectedDate = it },
              timeHour = timeHour,
              onHourChange = { timeHour = it.filter(Char::isDigit).take(2) },
              timeMinute = timeMinute,
              onMinuteChange = { timeMinute = it.filter(Char::isDigit).take(2) },
              timeAmPm = timeAmPm,
              onAmPmChange = { timeAmPm = it },
              plan = activePlan,
              onPlanChange = { activePlan = it },
              todayMin = todayMin,
              timeBlocked = timeBlocked
            )

            // STEP 2: Where are we shooting?
            2 -> StepWhere(
              venueName = venueName,
              onVenueNameChange = { venueName = it },
              address = venueAddress,
              onAddressChange = { venueAddress = it },
              locationNotes = locationNotes,
              onLocationNotesChange = { locationNotes = it }
            )

            // STEP 3: Tell us about the reel
            3 -> StepTellUsAboutShoot(
              shootTitle = shootTitle,
              onShootTitleChange = { shootTitle = it },
              brief = shootBrief,
              onBriefChange = { shootBrief = it },
              showReference = showReference,
              onToggleReference = { showReference = !showReference },
              reference = referenceLink,
              onReferenceChange = { referenceLink = it },
              showInstagram = showInstagramLink,
              onToggleInstagram = { showInstagramLink = !showInstagramLink },
              instagram = instagramRef,
              onInstagramChange = { instagramRef = it },
              showDrive = showDriveLink,
              onToggleDrive = { showDriveLink = !showDriveLink },
              drive = driveLink,
              onDriveChange = { driveLink = it },
              showSpecial = showInstructions,
              onToggleSpecial = { showInstructions = !showInstructions },
              special = specialInstructions,
              onSpecialChange = { specialInstructions = it }
            )

            // STEP 4: Looks Good? (Summary)
            4 -> StepLooksGood(
              shootTitle = shootTitle,
              category = category,
              date = selectedDate,
              time = callTime,
              duration = durationHours,
              venue = venueName,
              address = venueAddress,
              crewCounts = crewCounts,
              estimatedCost = estimatedCost,
              onEdit = { currentStep = 1 }
            )
          }
        }
      }

      // Bottom Actions
      val canContinue = when (currentStep) {
        1 -> selectedDate.isNotBlank() && callTime.isNotBlank() && !timeBlocked
        2 -> venueName.isNotBlank() && venueAddress.isNotBlank()
        3 -> shootBrief.isNotBlank()
        else -> true
      }
      val continueHint = when {
        currentStep == 1 && selectedDate.isBlank() -> "Select date & time"
        currentStep == 1 && timeBlocked -> "Pick a later time"
        currentStep == 1 && !canContinue -> "Select date & time"
        currentStep == 2 && !canContinue -> "Add venue & address"
        currentStep == 3 && !canContinue -> "Add a shoot brief"
        else -> "Continue"
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
      ) {
        if (currentStep < 4) {
          FlowPill(
            state = FlowPillState.CONTINUE,
            customText = continueHint,
            enabled = canContinue,
            onClick = {
              if (currentStep < 4) currentStep++
            },
            testTag = "wizard_continue_button"
          )
        } else {
          // Final Step: Find My Crew & Edit
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            FameGoOutlinedButton(
              text = "Edit",
              onClick = { currentStep = 1 },
              modifier = Modifier.weight(1f),
              testTag = "wizard_edit_button"
            )
            FlowPill(
              state = FlowPillState.CUSTOM,
              customText = "Find My Crew",
              enabled = selectedDate.isNotBlank() && callTime.isNotBlank() &&
                venueName.isNotBlank() && venueAddress.isNotBlank() &&
                shootBrief.isNotBlank(),
              onClick = {
                val newBooking = Booking(
                  id = UUID.randomUUID().toString(),
                  bookingCode = "FG-" + (1000..9999).random(),
                  shootTitle = shootTitle.ifBlank { "Reel Shoot - $venueName" },
                  clientName = currentUser.name,
                  clientCompany = currentUser.companyName,
                  category = category,
                  // Preserve the date selected by the user. The old value always
                  // submitted Friday 12, even when another chip was selected.
                  dateText = selectedDate,
                  timeText = callTime,
                  durationHours = durationHours,
                  venueName = venueName,
                  fullAddress = venueAddress,
                  locationInstructions = locationNotes,
                  crewRequirements = listOf(
                    CrewRequirement(role = CrewRoleType.VIDEOGRAPHER, quantity = 1)
                  ),
                  shootDescription = shootBrief,
                  specialInstructions = specialInstructions,
                  brandName = currentUser.companyName,
                  referenceLink = referenceLink.ifBlank { instagramRef.ifBlank { driveLink } },
                  plan = activePlan,
                  priceRupees = activePlan.priceRupees,
                  status = BookingStatus.SEARCHING_CREW
                )
                onBookingReadyForSearch(newBooking)
              },
              modifier = Modifier.weight(1.6f),
              testTag = "wizard_find_crew_button"
            )
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// STEP 1: When should the crew arrive? (custom time)
// -----------------------------------------------------------------------------
@Composable
private fun StepWhen(
  selectedDate: String,
  onDateSelect: (String) -> Unit,
  timeHour: String,
  onHourChange: (String) -> Unit,
  timeMinute: String,
  onMinuteChange: (String) -> Unit,
  timeAmPm: String,
  onAmPmChange: (String) -> Unit,
  plan: ShootPlan,
  onPlanChange: (ShootPlan) -> Unit,
  todayMin: Triple<Int, Int, String>? = null,
  timeBlocked: Boolean = false
) {
  val scrollState = rememberScrollState()
  var showPlanPicker by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    Text(
      text = "When should the crew arrive?",
      color = FameGoWhite,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Choose your date, call time and duration",
      color = FameGoTextMuted,
      fontSize = 14.sp,
      modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    // Date
    Text(
      text = "Date",
      color = FameGoTextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val dates = listOf("Today", "Tomorrow", "Fri 12", "Sat 13", "Sun 14", "Mon 15")
      dates.forEach { date ->
        val isSelected = selectedDate.equals(date, ignoreCase = true)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = if (isSelected) FameGoGoldContainer else FameGoCard,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) FameGoGold else FameGoBorderSubtle
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onDateSelect(date) }
            .testTag("date_chip_$date")
        ) {
          Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = date,
              color = if (isSelected) FameGoGold else FameGoTextSecondary,
              fontSize = 14.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Call Time — radial clock dial: tap the field, slide a finger on the dial.
    Text(
      text = "Call Time",
      color = FameGoTextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(10.dp))

    var showClock by remember { mutableStateOf(false) }
    val hasTime = timeHour.trim().toIntOrNull() in 1..12 &&
      timeMinute.trim().toIntOrNull() in 0..59
    val timeLabel = if (hasTime) {
      "%02d:%02d %s".format(timeHour.trim().toInt(), timeMinute.trim().toInt(), timeAmPm)
    } else "Set call time"

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = if (hasTime) FameGoGoldContainer else FameGoCard,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (hasTime) FameGoGold else FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable { showClock = true }
        .testTag("call_time_field")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = if (hasTime) FameGoGold else FameGoTextMuted,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = timeLabel,
            color = if (hasTime) FameGoWhite else FameGoTextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Text(
            text = if (hasTime) "Tap to change" else "Tap to pick on the clock dial",
            color = FameGoTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = "Pick time",
          tint = FameGoGold,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    if (showClock) {
      FameGoRadialTimePickerDialog(
        initialHour12 = timeHour.trim().toIntOrNull()?.takeIf { it in 1..12 } ?: 9,
        initialMinute = timeMinute.trim().toIntOrNull()?.takeIf { it in 0..59 } ?: 30,
        initialAmPm = timeAmPm,
        minHour12 = todayMin?.first,
        minMinute = todayMin?.second ?: 0,
        minAmPm = todayMin?.third,
        onDismiss = { showClock = false },
        onConfirm = { h, m, ampm ->
          onHourChange("%02d".format(h))
          onMinuteChange("%02d".format(m))
          onAmPmChange(ampm)
          showClock = false
        }
      )
    }
    if (timeBlocked && todayMin != null) {
      Text(
        text = "That time has passed — same-day shoots need 1-hour notice (from %02d:%02d %s).".format(
          todayMin.first, todayMin.second, todayMin.third
        ),
        color = Color(0xFFFF7B84),
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
    Text(
      text = "Tap the time and slide your finger around the clock dial.",
      color = FameGoTextMuted,
      fontSize = 11.sp,
      modifier = Modifier.padding(top = 8.dp)
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Duration (simple and direct)
    Text(
      text = "Duration",
      color = FameGoTextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(14.dp), color = FameGoGoldContainer,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold),
      modifier = Modifier.fillMaxWidth().testTag("selected_plan_duration")
    ) {
      Column(Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(plan.durationLabel, color = FameGoGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "Change",
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { showPlanPicker = true }
              .border(1.dp, FameGoGold, RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("change_plan_button")
          )
        }
        Text("Included with ${plan.title}", color = FameGoTextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
      }
    }

    if (showPlanPicker) {
      PlanPickerDialog(
        current = plan,
        onDismiss = { showPlanPicker = false },
        onPick = { picked ->
          onPlanChange(picked)
          showPlanPicker = false
        }
      )
    }
  }
}

@Composable
private fun PlanPickerDialog(
  current: ShootPlan,
  onDismiss: () -> Unit,
  onPick: (ShootPlan) -> Unit
) {
  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = FameGoCardElevated,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold.copy(alpha = 0.35f)),
      shadowElevation = 16.dp,
      modifier = Modifier.fillMaxWidth().testTag("plan_picker_dialog")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Change plan",
          color = FameGoWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Price changes with the plan you pick",
          color = FameGoTextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )
        ShootPlan.entries.forEach { option ->
          val active = option == current
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (active) FameGoGoldContainer else FameGoCard,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (active) FameGoGold else FameGoBorderSubtle
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable { onPick(option) }
              .testTag("plan_option_${option.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = option.title,
                  color = if (active) FameGoGold else FameGoWhite,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = option.durationLabel,
                  color = FameGoTextMuted,
                  fontSize = 11.sp
                )
              }
              Text(
                text = "₹${"%,d".format(option.priceRupees)}",
                color = FameGoGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
        Text(
          text = "Cancel",
          color = FameGoTextSecondary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .clickable(onClick = onDismiss)
            .padding(top = 12.dp, bottom = 4.dp)
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// STEP 3: Where are we shooting?
// -----------------------------------------------------------------------------
@Composable
private fun StepWhere(
  venueName: String,
  onVenueNameChange: (String) -> Unit,
  address: String,
  onAddressChange: (String) -> Unit,
  locationNotes: String,
  onLocationNotesChange: (String) -> Unit
) {
  val scrollState = rememberScrollState()
  // Map preview state — Mumbai by default, follows geocode hits and picks.
  var mapLng by remember { mutableStateOf(MapTilerGeocoding.MUMBAI_LNG) }
  var mapLat by remember { mutableStateOf(MapTilerGeocoding.MUMBAI_LAT) }
  var mapZoom by remember { mutableIntStateOf(11) }
  var mapLabel by remember { mutableStateOf("Mumbai") }

  fun applyPlace(place: MapPlace) {
    onAddressChange(place.full)
    if (venueName.isBlank() && place.short.isNotBlank()) {
      onVenueNameChange(place.short)
    }
    place.longitude?.let { mapLng = it }
    place.latitude?.let { mapLat = it }
    mapZoom = 14
    mapLabel = place.short.ifBlank { "Selected spot" }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    Text(
      text = "Where are we shooting?",
      color = FameGoWhite,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Crew will navigate directly to this address",
      color = FameGoTextMuted,
      fontSize = 14.sp,
      modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    // Venue Name field
    OutlinedTextField(
      value = venueName,
      onValueChange = onVenueNameChange,
      label = { Text("Venue Name", color = FameGoTextMuted) },
      placeholder = { Text("e.g. Veranda Studio", color = FameGoTextMuted) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FameGoWhite,
        unfocusedTextColor = FameGoWhite,
        focusedContainerColor = FameGoCard,
        unfocusedContainerColor = FameGoCard,
        focusedBorderColor = FameGoGold,
        unfocusedBorderColor = FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("venue_name_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Address field
    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Address", color = FameGoTextMuted) },
      placeholder = { Text("Street address", color = FameGoTextMuted) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FameGoWhite,
        unfocusedTextColor = FameGoWhite,
        focusedContainerColor = FameGoCard,
        unfocusedContainerColor = FameGoCard,
        focusedBorderColor = FameGoGold,
        unfocusedBorderColor = FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("address_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Location Notes field
    OutlinedTextField(
      value = locationNotes,
      onValueChange = onLocationNotesChange,
      label = { Text("Location Notes", color = FameGoTextMuted) },
      placeholder = { Text("Building, floor, landmark, parking details...", color = FameGoTextMuted) },
      minLines = 2,
      maxLines = 3,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FameGoWhite,
        unfocusedTextColor = FameGoWhite,
        focusedContainerColor = FameGoCard,
        unfocusedContainerColor = FameGoCard,
        focusedBorderColor = FameGoGold,
        unfocusedBorderColor = FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("location_notes_input")
    )

    Spacer(modifier = Modifier.height(24.dp))

    // MapTiler address options — one tap fills the full address below notes.
    // NOTE: no auto-recenter while typing (the old onResults jump yanked the
    // map on every keystroke and fought user pans). The map moves only on an
    // explicit pick: suggestion tap, locality chip, or map "Use this spot".
    AddressSuggestList(
      query = address,
      onPick = ::applyPlace,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Movable map: pan/zoom, tap to drop a pin, "Use this spot" fills the address.
    MapPreviewCard(
      centerLng = mapLng,
      centerLat = mapLat,
      zoom = mapZoom,
      pinLabel = mapLabel,
      onPick = ::applyPlace,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(14.dp))

    LocalityChips(
      onPick = ::applyPlace,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(14.dp))

    SavedLocationChips(
      onPick = { saved ->
        onVenueNameChange(saved.venueName)
        onAddressChange(saved.address)
      },
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Type the street above and pick the exact spot — or enter the venue and full address manually. Past shoot spots are remembered above for one-tap refill.",
      color = FameGoTextMuted,
      fontSize = 12.sp,
      lineHeight = 17.sp
    )
  }
}

// -----------------------------------------------------------------------------
// STEP 3: Tell us about the reel
// -----------------------------------------------------------------------------
@Composable
private fun StepTellUsAboutShoot(
  shootTitle: String,
  onShootTitleChange: (String) -> Unit,
  brief: String,
  onBriefChange: (String) -> Unit,
  showReference: Boolean,
  onToggleReference: () -> Unit,
  reference: String,
  onReferenceChange: (String) -> Unit,
  showInstagram: Boolean,
  onToggleInstagram: () -> Unit,
  instagram: String,
  onInstagramChange: (String) -> Unit,
  showDrive: Boolean,
  onToggleDrive: () -> Unit,
  drive: String,
  onDriveChange: (String) -> Unit,
  showSpecial: Boolean,
  onToggleSpecial: () -> Unit,
  special: String,
  onSpecialChange: (String) -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    Text(
      text = "Tell us about the reel",
      color = FameGoWhite,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Give your videographer the key details",
      color = FameGoTextMuted,
      fontSize = 14.sp,
      modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
    )

    // Shoot Title
    OutlinedTextField(
      value = shootTitle,
      onValueChange = onShootTitleChange,
      label = { Text("Shoot Title", color = FameGoTextMuted) },
      placeholder = { Text("Restaurant launch content", color = FameGoTextMuted) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FameGoWhite,
        unfocusedTextColor = FameGoWhite,
        focusedContainerColor = FameGoCard,
        unfocusedContainerColor = FameGoCard,
        focusedBorderColor = FameGoGold,
        unfocusedBorderColor = FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("shoot_title_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Brief field
    OutlinedTextField(
      value = brief,
      onValueChange = onBriefChange,
      label = { Text("Brief", color = FameGoTextMuted) },
      placeholder = {
        Text(
          "Tell the crew what you need...",
          color = FameGoTextMuted,
          fontSize = 14.sp
        )
      },
      minLines = 4,
      maxLines = 6,
      shape = RoundedCornerShape(18.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = FameGoWhite,
        unfocusedTextColor = FameGoWhite,
        focusedContainerColor = FameGoCard,
        unfocusedContainerColor = FameGoCard,
        focusedBorderColor = FameGoGold,
        unfocusedBorderColor = FameGoBorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("shoot_brief_input")
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Optional",
      color = FameGoTextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(10.dp))

    // Expandable: Add reference
    ExpandableOptionalField(
      label = "Add reference",
      isExpanded = showReference,
      onToggle = onToggleReference,
      value = reference,
      onValueChange = onReferenceChange,
      placeholder = "Paste moodboard or website link"
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Expandable: Add Instagram link
    ExpandableOptionalField(
      label = "Add Instagram link",
      isExpanded = showInstagram,
      onToggle = onToggleInstagram,
      value = instagram,
      onValueChange = onInstagramChange,
      placeholder = "@username or reel link"
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Expandable: Add Drive link
    ExpandableOptionalField(
      label = "Add Drive link",
      isExpanded = showDrive,
      onToggle = onToggleDrive,
      value = drive,
      onValueChange = onDriveChange,
      placeholder = "drive.google.com/..."
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Expandable: Add instructions
    ExpandableOptionalField(
      label = "Add instructions",
      isExpanded = showSpecial,
      onToggle = onToggleSpecial,
      value = special,
      onValueChange = onSpecialChange,
      placeholder = "Special notes for the crew on set"
    )
  }
}

@Composable
private fun ExpandableOptionalField(
  label: String,
  isExpanded: Boolean,
  onToggle: () -> Unit,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String
) {
  SoftCard {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          color = FameGoTextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Icon(
          imageVector = Icons.Default.ExpandMore,
          contentDescription = null,
          tint = FameGoTextMuted,
          modifier = Modifier.size(18.dp)
        )
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = value,
          onValueChange = onValueChange,
          placeholder = { Text(placeholder, color = FameGoTextMuted, fontSize = 12.sp) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FameGoWhite,
            unfocusedTextColor = FameGoWhite,
            focusedContainerColor = FameGoCardElevated,
            unfocusedContainerColor = FameGoCardElevated,
            focusedBorderColor = FameGoGold,
            unfocusedBorderColor = FameGoBorderSubtle
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// STEP 6: Looks Good? (Clean Single Summary Card)
// -----------------------------------------------------------------------------
@Composable
private fun StepLooksGood(
  shootTitle: String,
  category: ShootCategory,
  date: String,
  time: String,
  duration: Int,
  venue: String,
  address: String,
  crewCounts: Map<CrewRoleType, Int>,
  estimatedCost: Int,
  onEdit: () -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
  ) {
    Text(
      text = "Looks good?",
      color = FameGoWhite,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Review your reel shoot before we lock your videographer",
      color = FameGoTextMuted,
      fontSize = 14.sp,
      modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
    )

    // The Single Summary Card
    SoftCard(
      shape = RoundedCornerShape(24.dp),
      isElevated = true,
      testTag = "booking_summary_card"
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        // Shoot type
        Text(
          text = shootTitle.ifBlank { "Reel Shoot" },
          color = FameGoGold,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Schedule
        Text(
          text = "$date • $time",
          color = FameGoWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "$duration hours",
          color = FameGoTextSecondary,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Venue
        Text(
          text = venue,
          color = FameGoWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = address,
          color = FameGoTextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Estimated Cost (plan price — crew is included, no per-head math)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Estimated Total",
            color = FameGoWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "₹$estimatedCost",
            color = FameGoGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
