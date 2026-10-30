package com.example.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.CrewRepository
import com.example.domain.repository.UserRepository
import com.example.presentation.components.PrimaryGoldButton
import com.example.presentation.components.SecondaryDarkButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LensCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RecRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  currentUser: User,
  userRepository: UserRepository,
  crewRepository: CrewRepository,
  onLogout: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val crewProfile by crewRepository.getCrewProfileByUserId(currentUser.id).collectAsState(initial = null)

  val openEmail: (String) -> Unit = { email ->
    runCatching {
      context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
    }
  }
  val openLink: (String) -> Unit = { url ->
    runCatching {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
  }
  val copyText: (String, String) -> Unit = { text, message ->
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FameBook", text))
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  var showEditDialog by remember { mutableStateOf(false) }
  var showLogoutConfirm by remember { mutableStateOf(false) }
  var showHelpDialog by remember { mutableStateOf(false) }
  var notificationsEnabled by remember { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
  ) {
    // Header Avatar & Identity
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(DarkElevated)
            .border(2.dp, AmberGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (currentUser.role) {
              UserRole.CLIENT -> Icons.Default.Business
              UserRole.CREW -> Icons.Default.CameraAlt
              UserRole.ADMIN -> Icons.Default.Videocam
            },
            contentDescription = null,
            tint = AmberGold,
            modifier = Modifier.size(42.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = currentUser.name,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              when (currentUser.role) {
                UserRole.CLIENT -> LensCyan.copy(alpha = 0.2f)
                UserRole.CREW -> EmeraldSuccess.copy(alpha = 0.2f)
                UserRole.ADMIN -> AmberGoldContainer
              }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${currentUser.role.name} VERIFIED",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = when (currentUser.role) {
              UserRole.CLIENT -> LensCyan
              UserRole.CREW -> EmeraldSuccess
              UserRole.ADMIN -> AmberGold
            }
          )
        }

        if (currentUser.companyName != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = currentUser.companyName!!,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // Profile Details Card
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "ACCOUNT DETAILS",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = TextMuted
            )

            TextButton(
              onClick = { showEditDialog = true },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "EDIT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AmberGold
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          ProfileInfoRow(icon = Icons.Default.Email, label = "Email", value = currentUser.email)
          Spacer(modifier = Modifier.height(10.dp))
          ProfileInfoRow(icon = Icons.Default.Phone, label = "Phone", value = currentUser.phone)

          if (currentUser.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow(icon = Icons.Default.Info, label = "Bio", value = currentUser.bio)
          }

          if (currentUser.role == UserRole.CREW && crewProfile != null) {
            val cp = crewProfile!!
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = DarkBorder)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "PRODUCTION SPECIFICATIONS",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = AmberGold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Primary Role: ${cp.primaryRole.title}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Production Gear:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = TextMuted
            )
            Text(
              text = cp.gearSummary,
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Rating: ${cp.rating} ★ (${cp.totalCompletedShoots} verified productions)",
              style = MaterialTheme.typography.bodySmall,
              color = AmberGold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }

    // Settings & Preferences
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "PREFERENCES & SECURITY",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Push Notifications",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = PureWhite
                )
                Text(
                  text = "Shoot requests & dispatch alerts",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted
                )
              }
            }

            Switch(
              checked = notificationsEnabled,
              onCheckedChange = { notificationsEnabled = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = AmberGold,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkElevated
              )
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = DarkBorder)
          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "FameBook Core",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = PureWhite
                )
                Text(
                  text = "OSIEN v1A • Verified Studio",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = DarkBorder)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showHelpDialog = true }
              .padding(vertical = 8.dp)
              .testTag("profile_help_center"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AmberGold,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Help Center",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = PureWhite
                )
                Text(
                  text = "FAQs, guides & support",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted
                )
              }
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "Open help center",
              tint = TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // Sign Out Button
    item {
      SecondaryDarkButton(
        text = "SIGN OUT",
        onClick = { showLogoutConfirm = true },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_logout_button")
      )
    }
  }

  // Edit Profile Dialog
  if (showEditDialog) {
    var editName by remember { mutableStateOf(currentUser.name) }
    var editPhone by remember { mutableStateOf(currentUser.phone) }
    var editCompany by remember { mutableStateOf(currentUser.companyName ?: "") }
    var editBio by remember { mutableStateOf(currentUser.bio) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Edit Profile", color = PureWhite, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Full Name", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editPhone,
            onValueChange = { editPhone = it },
            label = { Text("Phone", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editCompany,
            onValueChange = { editCompany = it },
            label = { Text("Company / Organization", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = AmberGold,
              unfocusedBorderColor = DarkBorder
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              userRepository.updateProfile(
                name = editName,
                email = currentUser.email,
                phone = editPhone,
                bio = editBio,
                company = editCompany.ifBlank { null }
              )
              showEditDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBlack)
        ) {
          Text("Save Changes", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Help Center Dialog
  if (showHelpDialog) {
    AlertDialog(
      onDismissRequest = { showHelpDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Help Center", color = PureWhite, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          helpFaqs.forEach { faq ->
            HelpFaqItem(question = faq.first, answer = faq.second)
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "CONTACT",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = AmberGold
          )
          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Owner Contact",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
          )
          HelpContactRow(
            label = "Gmail",
            value = "famebrosstudio.franchise1@gmail.com",
            onClick = { openEmail("famebrosstudio.franchise1@gmail.com") }
          )
          HelpContactRow(
            label = "Instagram",
            value = "Famebrosstudio",
            onClick = { openLink("https://instagram.com/famebrosstudio") }
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Developer Contact",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = PureWhite
          )
          HelpContactRow(
            label = "Gmail",
            value = "kabirsayed.k@gmail.com",
            onClick = { openEmail("kabirsayed.k@gmail.com") }
          )
          HelpContactRow(
            label = "Instagram",
            value = "kabir.found.nebula",
            onClick = { openLink("https://instagram.com/kabir.found.nebula") }
          )
          HelpContactRow(
            label = "Discord",
            value = "Kabirprokk",
            onClick = { copyText("Kabirprokk", "Discord username copied") }
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showHelpDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = ObsidianBlack)
        ) {
          Text("Close", fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // Logout Confirmation Dialog
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      containerColor = DarkSurface,
      title = {
        Text("Sign Out", color = PureWhite, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "Are you sure you want to sign out of your FameBook account?",
          color = TextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            scope.launch {
              userRepository.logout()
              onLogout()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = RecRed, contentColor = PureWhite)
        ) {
          Text("Sign Out", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

private val helpFaqs = listOf(
  "How do I book a shoot?" to
    "Go to Home, tap Book a Shoot, then follow the six steps: shoot type, date and time, location, crew needed, details, and review. Tap Request Crew to send it to available crew.",
  "When is my booking confirmed?" to
    "Your booking is confirmed the moment a crew member accepts your request. You will see the assigned crew and can open chat from the booking.",
  "How do I cancel a request?" to
    "Open the booking from Home or Bookings and cancel it there. Cancellation is available while the request is still open.",
  "How do crew receive my request?" to
    "Available crew instantly get a request popup with your shoot details. The first crew member to accept is assigned to your shoot.",
  "How do I contact my crew?" to
    "Once a shoot is confirmed, open the booking and use Chat or Call to coordinate directly with your assigned crew.",
  "How do account roles work?" to
    "Every new account starts as a client. Crew and admin access is assigned by the studio and cannot be changed from inside the app."
)

@Composable
private fun HelpContactRow(label: String, value: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = TextMuted
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = AmberGold,
      textAlign = androidx.compose.ui.text.style.TextAlign.End,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun HelpFaqItem(question: String, answer: String) {
  var expanded by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .padding(vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = question,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = PureWhite,
        modifier = Modifier.weight(1f)
      )
      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = if (expanded) "Collapse" else "Expand",
        tint = AmberGold,
        modifier = Modifier.size(16.dp)
      )
    }
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Text(
        text = answer,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(top = 6.dp)
      )
    }
  }
  HorizontalDivider(color = DarkBorder)
}

@Composable
private fun ProfileInfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = TextMuted,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = PureWhite
      )
    }
  }
}
