package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.AssignedCrewMember
import com.example.model.BookingStatus
import com.example.model.CrewApplicationStatus
import com.example.model.CrewRoleType
import com.example.model.Role
import com.example.ui.components.LiveOrb
import com.example.ui.components.FameGoPressable
import com.example.ui.components.SoftCard
import com.example.ui.theme.fameGoRise
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoCardElevated
import com.example.ui.theme.FameGoGold
import com.example.ui.theme.FameGoGoldContainer
import com.example.ui.theme.FameGoLiveRed
import com.example.ui.theme.FameGoSuccessGreen
import com.example.ui.theme.FameGoSurface
import com.example.ui.theme.FameGoTextMuted
import com.example.ui.theme.FameGoTextPrimary
import com.example.ui.theme.FameGoTextSecondary
import com.example.ui.theme.FameGoWhite
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
  onOpenBooking: (String) -> Unit,
  onOpenSupport: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bookings by FameGoRepository.bookings.collectAsState()
  val crewProfiles by FameGoRepository.crewProfiles.collectAsState()

  val activeSearches = bookings.filter { it.status == BookingStatus.SEARCHING_CREW }
  val activeShoots = bookings.filter { it.status == BookingStatus.IN_PROGRESS || it.status == BookingStatus.CONFIRMED }
  val availableCrewCount = crewProfiles.count { it.isAvailable }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Live shoots",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Text(
        text = "Overview",
        color = FameGoTextMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
      )

      // 2 High-Signal Status Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Shoots Today
        SoftCard(
          modifier = Modifier.weight(1f),
          isElevated = activeShoots.isNotEmpty(),
          shape = RoundedCornerShape(18.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Shoots today",
              color = FameGoTextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${activeShoots.size + activeSearches.size}",
              color = FameGoWhite,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 4.dp)
            )
            Text(
              text = if (activeSearches.isNotEmpty()) "${activeSearches.size} finding crew" else "All crew assigned",
              color = FameGoTextSecondary,
              fontSize = 11.sp
            )
          }
        }

        // Available Crew
        SoftCard(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(18.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Available crew",
              color = FameGoTextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "$availableCrewCount",
              color = FameGoSuccessGreen,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 4.dp)
            )
            Text(
              text = "Ready for shoots",
              color = FameGoTextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Section 1: Active Searches needing assignment
      if (activeSearches.isNotEmpty()) {
        Text(
          text = "Finding crew",
          color = FameGoWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          activeSearches.forEach { search ->
            SoftCard(
              isElevated = true,
              onClick = { onOpenBooking(search.id) },
              testTag = "admin_search_${search.id}"
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveOrb(color = FameGoGold, size = 7.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Finding crew",
                      color = FameGoGold,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                  Text(text = "₹${search.estimatedBudget}", color = FameGoWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = search.title, color = FameGoWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                  text = "${search.location.venueName} • ${search.date} • ${search.time}",
                  color = FameGoTextMuted,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fast Manual Assign Button
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = FameGoGold,
                  modifier = Modifier
                    .clickable {
                      FameGoRepository.assignCrewToBooking(
                        bookingId = search.id,
                        crew = AssignedCrewMember(
                          crewId = "crew_1",
                          name = "Aarav Mehta",
                          role = CrewRoleType.CINEMATOGRAPHER,
                          phone = "+91 98200 11223",
                          gear = "Sony FX6 Cinema Line & Rig",
                          rating = 4.95,
                          isVerified = true
                        )
                      )
                    }
                    .testTag("admin_assign_button_${search.id}")
                ) {
                  Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Assign Aarav Mehta", color = FameGoBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }

      // Section 2: Recent bookings
      Text(
        text = "Recent bookings",
        color = FameGoWhite,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(10.dp))

      if (activeShoots.isEmpty()) {
        SoftCard {
          Text(
            text = "No shoots in progress right now.",
            color = FameGoTextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          activeShoots.forEach { shoot ->
            SoftCard(
              onClick = { onOpenBooking(shoot.id) },
              testTag = "admin_active_shoot_${shoot.id}"
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveOrb(
                      color = if (shoot.status == BookingStatus.IN_PROGRESS) FameGoGold else FameGoSuccessGreen,
                      size = 7.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = when (shoot.status) {
                        BookingStatus.IN_PROGRESS -> "Shoot in progress"
                        BookingStatus.CONFIRMED -> "Crew confirmed"
                        BookingStatus.COMPLETED -> "Wrap completed"
                        else -> "Confirmed"
                      },
                      color = if (shoot.status == BookingStatus.IN_PROGRESS) FameGoGold else FameGoSuccessGreen,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  Text(
                    text = shoot.date,
                    color = FameGoTextMuted,
                    fontSize = 11.sp
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = shoot.title, color = FameGoWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                  text = "${shoot.time} • ${shoot.location.venueName} • Crew: ${shoot.assignedCrew.firstOrNull()?.crewName ?: "Assigned"}",
                  color = FameGoTextMuted,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Spacer(modifier = Modifier.height(110.dp))
    }
  }
}

// =============================================================================
// PEOPLE TAB — crew applications to review + every user with role switching.
// =============================================================================

@Composable
private fun AdminApplicationCard(
  app: com.example.model.CrewApplication,
  modifier: Modifier = Modifier
) {
  SoftCard(
    isElevated = true,
    testTag = "admin_application_${app.id}",
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          LiveOrb(color = FameGoGold, size = 7.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Under review",
            color = FameGoGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
        Text(
          text = "${app.experienceYears} yrs",
          color = FameGoTextMuted,
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(text = app.fullName.ifBlank { "Unnamed" }, color = FameGoWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Text(
        text = "${app.city} • ${app.iphoneModel}",
        color = FameGoTextSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp)
      )
      Text(
        text = "${app.phone} • ${app.email}",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp)
      )
      if (app.portfolioUrl.isNotBlank()) {
        Text(
          text = "Work: ${app.portfolioUrl}",
          color = FameGoGold,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      if (app.instagramHandle.isNotBlank()) {
        Text(
          text = "IG: ${app.instagramHandle}",
          color = FameGoTextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      if (app.bestShoot.isNotBlank()) {
        Text(
          text = "Best: ${app.bestShoot}",
          color = FameGoTextSecondary,
          fontSize = 12.sp,
          lineHeight = 17.sp,
          modifier = Modifier.padding(top = 6.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        com.example.ui.components.FameGoPressable(
          onClick = { FameGoRepository.reviewCrewApplication(app.id, approve = true) },
          shape = RoundedCornerShape(12.dp),
          color = FameGoSuccessGreen,
          modifier = Modifier.weight(1f),
          testTag = "admin_approve_${app.id}"
        ) {
          Text(
            text = "Approve",
            color = FameGoBg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp)
          )
        }
        com.example.ui.components.FameGoPressable(
          onClick = { FameGoRepository.reviewCrewApplication(app.id, approve = false) },
          shape = RoundedCornerShape(12.dp),
          color = FameGoCardElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.5f)),
          modifier = Modifier.weight(1f),
          testTag = "admin_reject_${app.id}"
        ) {
          Text(
            text = "Reject",
            color = FameGoLiveRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AdminPeopleScreen(
  onOpenSupport: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val crewApplications by FameGoRepository.crewApplications.collectAsState()
  val pendingApplications = crewApplications.filter { it.status == CrewApplicationStatus.UNDER_REVIEW }
  val scrollState = rememberScrollState()

  LaunchedEffect(Unit) { FameGoRepository.loadAllUsers() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "People",
        color = FameGoWhite,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Text(
        text = "Applications and user roles",
        color = FameGoTextMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
      )

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = FameGoGold,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenSupport() }
          .testTag("admin_support_inbox_button")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Support inbox",
            color = FameGoBg,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "Reply as support →",
            color = FameGoBg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Crew applications",
          color = FameGoWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
        if (pendingApplications.isNotEmpty()) {
          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(FameGoGold)
              .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${pendingApplications.size} new",
              color = FameGoBg,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))

      if (pendingApplications.isEmpty()) {
        SoftCard {
          Text(
            text = "No applications under review. New crew forms show up here.",
            color = FameGoTextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          pendingApplications.forEach { app ->
            AdminApplicationCard(app = app)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      AdminUsersSection()

      Spacer(modifier = Modifier.height(110.dp))
    }
  }
}

// =============================================================================
// ADMIN PROFILE TAB — studio identity, sign out, credits.
// =============================================================================

@Composable
fun AdminProfileScreen(
  onLogout: () -> Unit,
  onEditProfile: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val scrollState = rememberScrollState()
  var showAdminRules by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp)
        .fameGoRise()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Studio Admin",
        color = FameGoWhite,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(FameGoCard)
            .border(1.5.dp, FameGoGold.copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = currentUser.avatarInitials.ifBlank { "AD" },
            color = FameGoGold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = currentUser.name.ifBlank { "Studio Admin" },
            color = FameGoWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = currentUser.email.ifBlank { "Famebros Studio" },
            color = FameGoTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = FameGoCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { showAdminRules = true }
          .testTag("admin_rules_button")
      ) {
        Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
          Text(
            text = "House rules",
            color = FameGoTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = FameGoCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoGold.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onEditProfile() }
          .testTag("admin_edit_profile_button")
      ) {
        Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
          Text(
            text = "Edit profile",
            color = FameGoGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = FameGoCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onLogout() }
          .testTag("admin_logout_button")
      ) {
        Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
          Text(
            text = "Sign out",
            color = FameGoLiveRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(110.dp))
    }

    if (showAdminRules) {
      com.example.ui.components.HouseRulesDialog(onDismiss = { showAdminRules = false })
    }
  }
}

// =============================================================================
// USERS — admin sees every profile, searches by name, switches roles in place.
// Promoting to CREW also provisions the shooter stub (server 007 policy).
// =============================================================================

@Composable
private fun AdminUsersSection(modifier: Modifier = Modifier) {
  val allUsers by FameGoRepository.allUsers.collectAsState()
  var query by remember { mutableStateOf("") }
  var busyUserId by remember { mutableStateOf<String?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) { FameGoRepository.loadAllUsers() }

  val filtered = if (query.isBlank()) allUsers
  else allUsers.filter {
    it.name.contains(query, ignoreCase = true) ||
      it.email.contains(query, ignoreCase = true) ||
      it.phone.contains(query, ignoreCase = true)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Users",
        color = FameGoWhite,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
      if (allUsers.isNotEmpty()) {
        Text(
          text = "${filtered.size} shown",
          color = FameGoTextMuted,
          fontSize = 11.sp
        )
      }
    }
    Spacer(modifier = Modifier.height(10.dp))

    SoftCard {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        androidx.compose.material3.Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = FameGoTextMuted,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
          value = query,
          onValueChange = { query = it },
          singleLine = true,
          textStyle = androidx.compose.ui.text.TextStyle(
            color = FameGoTextPrimary,
            fontSize = 14.sp
          ),
          decorationBox = { inner ->
            if (query.isEmpty()) {
              Text(text = "Search name, email, phone…", color = FameGoTextMuted, fontSize = 14.sp)
            }
            inner()
          },
          modifier = Modifier
            .weight(1f)
            .padding(vertical = 12.dp)
            .testTag("admin_users_search")
        )
        if (query.isNotEmpty()) {
          androidx.compose.material3.IconButton(
            onClick = { query = "" },
            modifier = Modifier.size(28.dp)
          ) {
            androidx.compose.material3.Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear search",
              tint = FameGoTextMuted,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (filtered.isEmpty()) {
      SoftCard {
        Text(
          text = if (allUsers.isEmpty()) "No profiles loaded yet. Pull to retry by reopening the dashboard."
          else "No users match \"$query\".",
          color = FameGoTextMuted,
          fontSize = 13.sp,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        filtered.take(60).forEach { user ->
          SoftCard(
            isElevated = false,
            testTag = "admin_user_${user.id}"
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(FameGoCardElevated)
                    .border(1.dp, FameGoGold.copy(alpha = 0.4f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = user.avatarInitials.ifBlank { "FG" },
                    color = FameGoGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = user.name.ifBlank { "Unnamed" },
                    color = FameGoWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = user.email.ifBlank { user.phone },
                    color = FameGoTextMuted,
                    fontSize = 12.sp
                  )
                }
                if (busyUserId == user.id) {
                  androidx.compose.material3.CircularProgressIndicator(
                    color = FameGoGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Role.entries.forEach { role ->
                  val selected = user.role == role
                  FameGoPressable(
                    onClick = {
                      busyUserId = user.id
                      error = null
                      scope.launch {
                        FameGoRepository.updateUserRole(user.id, role)
                          .onFailure { e ->
                            error = "${user.name.ifBlank { "User" }}: " +
                              FameGoRepository.friendlyMessage(e)
                          }
                        busyUserId = null
                      }
                    },
                    enabled = !selected && busyUserId == null,
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) FameGoGold else FameGoSurface,
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (selected) FameGoGold else FameGoBorderSubtle
                    ),
                    modifier = Modifier.weight(1f),
                    testTag = "admin_role_${role.name.lowercase()}_${user.id}"
                  ) {
                    Text(
                      text = role.name,
                      color = if (selected) FameGoBg else FameGoTextSecondary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp,
                      modifier = Modifier.padding(vertical = 8.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    error?.let { message ->
      Text(
        text = message,
        color = FameGoLiveRed,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}
