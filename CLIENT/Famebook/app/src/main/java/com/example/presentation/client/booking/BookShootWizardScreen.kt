package com.example.presentation.client.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.CrewRequirement
import com.example.domain.model.CrewRole
import com.example.domain.model.LocationInfo
import com.example.domain.model.ShootType
import com.example.domain.model.User
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SecondaryDarkButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookShootWizardScreen(
  currentUser: User,
  templateBooking: Booking? = null,
  onBackClick: () -> Unit,
  onRequestCrewSubmit: (Booking) -> Unit
) {
  var currentStep by remember { mutableIntStateOf(1) }

  // State
  var selectedShootType by remember { mutableStateOf(templateBooking?.shootType ?: ShootType.VIDEOGRAPHY) }
  var selectedDate by remember { mutableStateOf("Tomorrow, 10:00 AM") }
  var selectedDuration by remember { mutableIntStateOf(templateBooking?.durationHours ?: 4) }
  var locationName by remember { mutableStateOf(templateBooking?.location?.name ?: "Bandra Production Studio") }
  var locationAddress by remember { mutableStateOf(templateBooking?.location?.address ?: "Bandra West, Mumbai") }
  var locationInstructions by remember { mutableStateOf("") }

  val crewQuantities = remember {
    mutableStateMapOf<CrewRole, Int>().apply {
      val templateRoles = templateBooking?.requirements?.associate { it.role to it.count }
      if (templateRoles.isNullOrEmpty()) {
        put(CrewRole.CINEMATOGRAPHER, 1)
        put(CrewRole.ASSISTANT, 1)
      } else {
        putAll(templateRoles)
      }
    }
  }

  var shootTitle by remember { mutableStateOf(templateBooking?.title ?: "") }
  var shootDescription by remember { mutableStateOf(templateBooking?.description ?: "") }
  var specialInstructions by remember { mutableStateOf(templateBooking?.specialInstructions ?: "") }

  Scaffold(
    containerColor = ObsidianBlack,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "STEP 0$currentStep OF 06",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = AmberGold
            )
            Text(
              text = when (currentStep) {
                1 -> "WHAT ARE YOU SHOOTING?"
                2 -> "WHEN?"
                3 -> "WHERE?"
                4 -> "WHO DO YOU NEED?"
                5 -> "ADDITIONAL DETAILS"
                6 -> "REVIEW"
                else -> "BOOK A SHOOT"
              },
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              if (currentStep > 1) {
                currentStep--
              } else {
                onBackClick()
              }
            }
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = PureWhite
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
      )
    },
    bottomBar = {
      Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (currentStep > 1) {
            SecondaryDarkButton(
              text = "BACK",
              onClick = { currentStep-- },
              modifier = Modifier.weight(0.35f)
            )
          }

          PrimaryGoldButton(
            text = if (currentStep == 6) "REQUEST CREW" else "CONTINUE",
            onClick = {
              if (currentStep < 6) {
                currentStep++
              } else {
                val requirements = crewQuantities.filter { it.value > 0 }.map {
                  CrewRequirement(it.key, it.value)
                }.ifEmpty {
                  listOf(CrewRequirement(CrewRole.CINEMATOGRAPHER, 1))
                }

                val finalTitle = shootTitle.ifBlank {
                  "${currentUser.companyName ?: "Production"} • ${selectedShootType.title}"
                }

                val newBooking = Booking(
                  id = "BK-${UUID.randomUUID().toString().take(6).uppercase()}",
                  clientId = currentUser.id,
                  clientName = currentUser.name,
                  clientCompany = currentUser.companyName,
                  clientPhone = currentUser.phone,
                  clientEmail = currentUser.email,
                  shootType = selectedShootType,
                  title = finalTitle,
                  description = shootDescription,
                  date = selectedDate.split(",").firstOrNull() ?: "Tomorrow",
                  startTime = selectedDate.split(",").getOrNull(1)?.trim() ?: "10:00 AM",
                  durationHours = selectedDuration,
                  location = LocationInfo(
                    name = locationName.ifBlank { "Client Venue" },
                    address = locationAddress.ifBlank { "Mumbai" }
                  ),
                  requirements = requirements,
                  specialInstructions = "$locationInstructions\n$specialInstructions".trim(),
                  status = BookingStatus.SEARCHING_CREW
                )
                onRequestCrewSubmit(newBooking)
              }
            },
            modifier = Modifier.weight(if (currentStep > 1) 0.65f else 1f)
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp)
    ) {
      AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
          if (targetState > initialState) {
            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
              slideOutHorizontally { width -> -width } + fadeOut()
            )
          } else {
            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
              slideOutHorizontally { width -> width } + fadeOut()
            )
          }
        },
        label = "wizard_step_transition"
      ) { step ->
        when (step) {
          1 -> Step1ShootType(selected = selectedShootType, onSelect = { selectedShootType = it })
          2 -> Step2When(
            selectedDate = selectedDate,
            onSelectDate = { selectedDate = it },
            duration = selectedDuration,
            onDurationChange = { selectedDuration = it }
          )
          3 -> Step3Where(
            name = locationName,
            address = locationAddress,
            onNameChange = { locationName = it },
            onAddressChange = { locationAddress = it },
            instructions = locationInstructions,
            onInstructionsChange = { locationInstructions = it }
          )
          4 -> Step4Who(
            quantities = crewQuantities,
            onIncrement = { role ->
              crewQuantities[role] = (crewQuantities[role] ?: 0) + 1
            },
            onDecrement = { role ->
              val count = crewQuantities[role] ?: 0
              if (count > 0) crewQuantities[role] = count - 1
            }
          )
          5 -> Step5Brief(
            title = shootTitle,
            onTitleChange = { shootTitle = it },
            description = shootDescription,
            onDescriptionChange = { shootDescription = it },
            specialInstructions = specialInstructions,
            onSpecialInstructionsChange = { specialInstructions = it },
            shootType = selectedShootType
          )
          6 -> Step6Review(
            shootType = selectedShootType,
            date = selectedDate,
            duration = selectedDuration,
            location = locationName,
            address = locationAddress,
            requirements = crewQuantities.filter { it.value > 0 },
            title = shootTitle,
            description = shootDescription,
            client = currentUser
          )
        }
      }
    }
  }
}

/**
 * 01 WHAT ARE YOU SHOOTING?
 * Clean option cards with expressive iconography and high contrast (Zero Images)
 */
@Composable
private fun Step1ShootType(
  selected: ShootType,
  onSelect: (ShootType) -> Unit
) {
  val options = listOf(
    Triple(ShootType.VIDEOGRAPHY, "Cinema & Video", Icons.Default.Videocam),
    Triple(ShootType.FASHION_SHOOT, "Fashion Editorial", Icons.Default.CameraAlt),
    Triple(ShootType.EVENT_COVERAGE, "Live Events & Galas", Icons.Default.Celebration),
    Triple(ShootType.PRODUCT_SHOOT, "Product Commercial", Icons.Default.CenterFocusStrong),
    Triple(ShootType.PHOTOGRAPHY, "Still Photography", Icons.Default.PhotoCamera),
    Triple(ShootType.CORPORATE_SHOOT, "Corporate & Keynote", Icons.Default.BusinessCenter)
  )

  LazyColumn(
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(options) { (type, title, icon) ->
      val isSelected = selected == type
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) DarkElevated else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
          width = if (isSelected) 1.5.dp else 1.dp,
          color = if (isSelected) AmberGold else DarkBorder
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelect(type) }
          .testTag("wizard_shoot_type_${type.name.lowercase()}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) AmberGoldContainer else DarkSurface)
                .border(
                  1.dp,
                  if (isSelected) AmberGold.copy(alpha = 0.5f) else DarkBorder,
                  RoundedCornerShape(12.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AmberGold else TextSecondary,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = type.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) AmberGold else TextSecondary
              )
            }
          }

          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(if (isSelected) AmberGold else Color.Transparent)
              .border(
                1.5.dp,
                if (isSelected) AmberGold else TextMuted,
                CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ObsidianBlack,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * 02 WHEN?
 * Large date & time selector
 */
@Composable
private fun Step2When(
  selectedDate: String,
  onSelectDate: (String) -> Unit,
  duration: Int,
  onDurationChange: (Int) -> Unit
) {
  val dateSlots = listOf(
    "Today, 06:00 PM",
    "Tomorrow, 09:00 AM",
    "Tomorrow, 02:00 PM",
    "This Weekend, 10:00 AM",
    "Next Monday, 09:00 AM"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 20.dp)
  ) {
    Text(
      text = "SELECT CALL TIME",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = AmberGold
    )

    Spacer(modifier = Modifier.height(14.dp))

    dateSlots.forEach { slot ->
      val isSelected = selectedDate == slot
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) AmberGoldContainer else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
          width = 1.dp,
          color = if (isSelected) AmberGold else DarkBorder
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 5.dp)
          .clickable { onSelectDate(slot) }
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = if (isSelected) AmberGold else TextSecondary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = slot,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              ),
              color = PureWhite
            )
          }

          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = AmberGold,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "ESTIMATED DURATION ON SET",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = AmberGold
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      listOf(2, 4, 6, 8, 12).forEach { hours ->
        val isSelected = duration == hours
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) AmberGold else DarkCard,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AmberGold else DarkBorder
          ),
          modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .clickable { onDurationChange(hours) }
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${hours}h",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isSelected) ObsidianBlack else PureWhite
            )
          }
        }
      }
    }
  }
}

/**
 * 03 WHERE?
 * Clean location input
 */
@Composable
private fun Step3Where(
  name: String,
  address: String,
  onNameChange: (String) -> Unit,
  onAddressChange: (String) -> Unit,
  instructions: String,
  onInstructionsChange: (String) -> Unit
) {
  val presets = listOf(
    "FameBros Studio • Andheri West",
    "Bandra Production Loft • Bandra West",
    "Juhu Beach Coastal Set • Juhu",
    "BKC Corporate Center • Bandra Kurla Complex"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 20.dp)
  ) {
    Text(
      text = "SHOOT LOCATION / STUDIO",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = AmberGold
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
      value = name,
      onValueChange = onNameChange,
      label = { Text("Venue / Studio Name", color = TextSecondary) },
      placeholder = { Text("e.g. Bandra Studio 4", color = TextMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("wizard_location_name"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text("Address & City", color = TextSecondary) },
      placeholder = { Text("e.g. Bandra West, Mumbai", color = TextMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("wizard_location_address"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(
      value = instructions,
      onValueChange = onInstructionsChange,
      label = { Text("Location instructions", color = TextSecondary) },
      placeholder = { Text("Access notes, parking, entry details", color = TextMuted) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 2,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "FREQUENT PRODUCTION HUBS",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = TextMuted
    )

    Spacer(modifier = Modifier.height(10.dp))

    presets.forEach { preset ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clickable {
            val parts = preset.split("•")
            onNameChange(parts[0].trim())
            onAddressChange(parts.getOrNull(1)?.trim() ?: "Mumbai")
          }
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = AmberGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = preset,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite
          )
        }
      }
    }
  }
}

/**
 * 04 WHO DO YOU NEED?
 * Role selectors
 */
@Composable
private fun Step4Who(
  quantities: Map<CrewRole, Int>,
  onIncrement: (CrewRole) -> Unit,
  onDecrement: (CrewRole) -> Unit
) {
  val roles = listOf(
    CrewRole.CINEMATOGRAPHER,
    CrewRole.PHOTOGRAPHER,
    CrewRole.VIDEOGRAPHER,
    CrewRole.DRONE_OPERATOR,
    CrewRole.EDITOR,
    CrewRole.ASSISTANT
  )

  LazyColumn(
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(roles) { role ->
      val count = quantities[role] ?: 0
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (count > 0) AmberGoldContainer else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (count > 0) AmberGold else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = role.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = PureWhite
            )
            Text(
              text = role.defaultEquipment,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = { onDecrement(role) },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkElevated)
            ) {
              Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Remove",
                tint = PureWhite,
                modifier = Modifier.size(16.dp)
              )
            }

            Text(
              text = "$count",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = if (count > 0) AmberGold else PureWhite,
              modifier = Modifier.width(24.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            IconButton(
              onClick = { onIncrement(role) },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AmberGold)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = ObsidianBlack,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * 05 TELL US MORE
 * Creative Brief
 */
@Composable
private fun Step5Brief(
  title: String,
  onTitleChange: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  specialInstructions: String,
  onSpecialInstructionsChange: (String) -> Unit,
  shootType: ShootType
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 20.dp)
  ) {
    Text(
      text = "PROJECT TITLE",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = AmberGold
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = title,
      onValueChange = onTitleChange,
      placeholder = { Text("e.g. Autumn Urban Lookbook", color = TextMuted) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("wizard_brief_title"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "CREATIVE VISION & INSTRUCTIONS",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      ),
      color = AmberGold
    )

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedTextField(
      value = description,
      onValueChange = onDescriptionChange,
      placeholder = {
        Text(
          "Mood, key shots, lighting preference, or specific camera glass requirements...",
          color = TextMuted
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .testTag("wizard_brief_description"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp)
    )

    Spacer(modifier = Modifier.height(18.dp))

    OutlinedTextField(
      value = specialInstructions,
      onValueChange = onSpecialInstructionsChange,
      label = { Text("Special instructions", color = TextSecondary) },
      placeholder = { Text("Anything the crew should know before the shoot", color = TextMuted) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        focusedBorderColor = AmberGold,
        unfocusedBorderColor = DarkBorder
      ),
      shape = RoundedCornerShape(14.dp)
    )
  }
}

/**
 * 06 REVIEW
 * Minimal, confident summary
 */
@Composable
private fun Step6Review(
  shootType: ShootType,
  date: String,
  duration: Int,
  location: String,
  address: String,
  requirements: Map<CrewRole, Int>,
  title: String,
  description: String,
  client: User
) {
  LazyColumn(
    contentPadding = PaddingValues(vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = title.ifBlank { "Production Shoot" },
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
        color = PureWhite
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${shootType.title} • FameBros Studio Production",
        style = MaterialTheme.typography.bodyMedium,
        color = AmberGold
      )
    }

    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          ReviewRow(label = "CALL TIME", value = "$date ($duration hours on set)")
          Spacer(modifier = Modifier.height(12.dp))
          ReviewRow(label = "VENUE", value = "$location, $address")
          Spacer(modifier = Modifier.height(12.dp))
          val crewSummary = requirements.entries.joinToString(", ") { "${it.value}x ${it.key.title}" }
          ReviewRow(label = "CREW REQUIRED", value = crewSummary.ifBlank { "1x Lead Cinematographer" })
          if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            ReviewRow(label = "BRIEF", value = description)
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "CLIENT CONTACT",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "${client.name} (${client.companyName ?: "Client"})",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = PureWhite
          )
          Text(
            text = "${client.phone} • ${client.email}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }
      }
    }
  }
}

@Composable
private fun ReviewRow(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
      color = AmberGold
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      color = PureWhite
    )
  }
}
