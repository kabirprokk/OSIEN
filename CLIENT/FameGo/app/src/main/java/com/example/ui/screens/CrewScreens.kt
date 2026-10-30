package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FameGoRepository
import com.example.model.AssignedCrewMember
import com.example.model.Booking
import com.example.model.BookingStatus
import com.example.model.CrewRoleType
import com.example.ui.components.FlowPill
import com.example.ui.components.FlowPillState
import com.example.ui.components.FameGoHaptics
import com.example.ui.components.GestureRequestCard
import com.example.ui.components.LiveOrb
import com.example.ui.components.rubberBand
import com.example.ui.components.SoftCard
import com.example.ui.components.StatusCapsule
import com.example.ui.components.swipeToGoBack
import com.example.ui.theme.FameGoAccentCyan
import com.example.ui.theme.FameGoBg
import com.example.ui.theme.FameGoBorder
import com.example.ui.theme.FameGoBorderSubtle
import com.example.ui.theme.FameGoCard
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

// =============================================================================
// CREW REQUESTS TAB — the shooter's inbox. Duty pill on top, every open
// paid request below with Accept / Decline. Pull down to refresh.
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewRequestsScreen(
  onViewRequestDetail: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val isAvailable by FameGoRepository.isCrewAvailable.collectAsState()
  val incomingRequests by FameGoRepository.incomingShootRequests.collectAsState()
  var isRefreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val crewSfx = LocalContext.current.applicationContext

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
  ) {
    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = {
        isRefreshing = true
        com.example.data.FameGoSfx.pop(crewSfx)
        scope.launch {
          FameGoRepository.refreshNow()
          isRefreshing = false
        }
      },
      modifier = Modifier.fillMaxSize()
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .rubberBand()
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Shoot requests",
                color = FameGoWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
              )
              Text(
                text = if (incomingRequests.isEmpty()) "New paid shoots land here"
                else "${incomingRequests.size} waiting for a crew",
                color = FameGoTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
            StatusCapsule(
              isAvailable = isAvailable,
              onToggle = { FameGoRepository.toggleCrewAvailability() }
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        if (incomingRequests.isEmpty()) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              LiveOrb(
                color = if (isAvailable) FameGoSuccessGreen else FameGoTextMuted,
                size = 12.dp
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "No shoot requests right now",
                color = FameGoWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (isAvailable)
                  "Stay on duty — paid requests pop up fullscreen."
                else
                  "Go on duty with the capsule above to receive requests.",
                color = FameGoTextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp)
              )
            }
          }
        } else {
          items(incomingRequests, key = { it.id }) { request ->
            GestureRequestCard(
              title = request.title,
              subtitle = "${request.location.venueName}, ${request.location.address.substringBefore(",")}",
              timeText = "${request.date} • ${request.time}",
              requestedRole = request.requiredCrew.firstOrNull()?.role?.title ?: "Crew call sheet",
              payoutText = "₹${request.estimatedBudget}",
              onAccept = { onViewRequestDetail(request.id) },
              onDecline = {
                FameGoHaptics.micro(haptic)
                FameGoRepository.declineShootRequest(request.id)
              }
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(110.dp))
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// 17. CREW EVOLVING JOB CARD (Visual timeline morphing without replacing screen)
// -----------------------------------------------------------------------------
@Composable
fun CrewEvolvingJobCard(
  booking: Booking,
  onOpenBooking: () -> Unit,
  onOpenChat: () -> Unit,
  onAdvanceStatus: (BookingStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  SoftCard(
    modifier = modifier.fillMaxWidth(),
    isElevated = true,
    shape = RoundedCornerShape(22.dp),
    testTag = "crew_evolving_job_card"
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      // Status header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          LiveOrb(
            color = when (booking.status) {
              BookingStatus.IN_PROGRESS -> FameGoGold
              BookingStatus.CONFIRMED -> FameGoSuccessGreen
              else -> FameGoAccentCyan
            },
            size = 8.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = when (booking.status) {
              BookingStatus.CONFIRMED -> "Crew confirmed"
              BookingStatus.IN_PROGRESS -> "Shoot in progress"
              BookingStatus.COMPLETED -> "Wrap completed"
              else -> "Confirmed"
            },
            color = when (booking.status) {
              BookingStatus.IN_PROGRESS -> FameGoGold
              BookingStatus.CONFIRMED -> FameGoSuccessGreen
              else -> FameGoTextSecondary
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Text(
          text = booking.date,
          color = FameGoTextMuted,
          fontSize = 12.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = booking.title,
        color = FameGoWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "${booking.time} • ${booking.durationHours} hours • ${booking.location.venueName}",
        color = FameGoTextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 3.dp)
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Live location sharing (crew controls what the client sees)
      if (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.IN_PROGRESS) {
        val sharingIds by FameGoRepository.liveSharing.collectAsState()
        val sharing = booking.id in sharingIds
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = if (sharing) FameGoSuccessGreen else FameGoTextMuted,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Share live location",
                color = FameGoWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (sharing) "Client can see you on the map" else "Client sees venue only",
                color = FameGoTextMuted,
                fontSize = 11.sp
              )
            }
          }
          androidx.compose.material3.Switch(
            checked = sharing,
            onCheckedChange = { FameGoRepository.setLiveSharing(booking.id, it) },
            modifier = Modifier.testTag("crew_share_location_toggle"),
            colors = androidx.compose.material3.SwitchDefaults.colors(
              checkedThumbColor = FameGoGold,
              checkedTrackColor = FameGoGoldContainer,
              uncheckedThumbColor = FameGoTextMuted,
              uncheckedTrackColor = FameGoSurface
            )
          )
        }
      }

      // Progressive action button based on shoot timeline
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Chat with producer
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = FameGoSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
          modifier = Modifier
            .clickable { onOpenChat() }
            .padding(0.dp)
        ) {
          Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = "Message",
              tint = FameGoTextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        // Timeline advancement
        val nextActionText = when (booking.status) {
          BookingStatus.CONFIRMED -> "Start shoot"
          BookingStatus.IN_PROGRESS -> "Wrap shoot"
          else -> "View shoot"
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (booking.status == BookingStatus.IN_PROGRESS) Color(0xFF142E20) else FameGoGold,
          modifier = Modifier
            .weight(1f)
            .clickable {
              when (booking.status) {
                BookingStatus.CONFIRMED -> onAdvanceStatus(BookingStatus.IN_PROGRESS)
                BookingStatus.IN_PROGRESS -> onAdvanceStatus(BookingStatus.COMPLETED)
                else -> onOpenBooking()
              }
            }
            .testTag("crew_advance_job_button")
        ) {
          Box(
            modifier = Modifier.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = nextActionText,
              color = if (booking.status == BookingStatus.IN_PROGRESS) FameGoSuccessGreen else FameGoBg,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

// =============================================================================
// CREW REQUEST DETAIL SCREEN
// =============================================================================

@Composable
fun CrewRequestDetailScreen(
  requestId: String,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val incomingRequests by FameGoRepository.incomingShootRequests.collectAsState()
  val bookings by FameGoRepository.bookings.collectAsState()
  // Never display an unrelated request for a stale ID: fall back to the
  // booking pool, otherwise show an empty state.
  val request = incomingRequests.firstOrNull { it.id == requestId }
    ?: bookings.firstOrNull { it.id == requestId }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .statusBarsPadding()
      .navigationBarsPadding()
      .swipeToGoBack(onBack = onBack)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = FameGoTextSecondary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Call Sheet Request",
          color = FameGoWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (request != null) {
        SoftCard(
          shape = RoundedCornerShape(22.dp),
          isElevated = true,
          modifier = Modifier.fillMaxWidth()
        ) {          Column(modifier = Modifier.padding(22.dp)) {
            Text(
              text = "${request.category.title.uppercase()} SHOOT",
              color = FameGoGold,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = request.title,
              color = FameGoWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "SCHEDULE & VENUE",
              color = FameGoTextMuted,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "${request.date} • Call: ${request.time} (${request.durationHours} Hours)",
              color = FameGoTextPrimary,
              fontSize = 14.sp,
              modifier = Modifier.padding(top = 2.dp)
            )
            Text(
              text = "${request.location.venueName}, ${request.location.address}",
              color = FameGoTextSecondary,
              fontSize = 13.sp,
              modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "BRIEF",
              color = FameGoTextMuted,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = request.brief,
              color = FameGoTextSecondary,
              fontSize = 13.sp,
              modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "Guaranteed Payout", color = FameGoTextSecondary, fontSize = 14.sp)
              Text(
                text = "₹${request.estimatedBudget}",
                color = FameGoGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      } else {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Request no longer available", color = FameGoWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "It may have been assigned or cancelled.",
              color = FameGoTextMuted, fontSize = 13.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Actions
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = FameGoSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorderSubtle),
          modifier = Modifier
            .weight(1f)
            .clickable(enabled = request != null) { onDecline() }
            .testTag("request_detail_decline_button")
        ) {
          Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "Decline", color = FameGoTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
          }
        }

        Surface(
          shape = RoundedCornerShape(24.dp),
          color = FameGoGold,
          modifier = Modifier
            .weight(1.5f)
            .clickable(enabled = request != null) { onAccept() }
            .testTag("request_detail_accept_button")
        ) {
          Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "Accept Call Sheet", color = FameGoBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

// =============================================================================
// CREW SCHEDULE / JOBS SCREEN
// =============================================================================

@Composable
fun CrewJobsScreen(
  onOpenBooking: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val bookings by FameGoRepository.bookings.collectAsState()
  val crewJobs = bookings.filter { it.assignedCrew.isNotEmpty() }
  val activeJobs = crewJobs.filter {
    it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.IN_PROGRESS ||
      it.status == BookingStatus.UPCOMING
  }
  val completedJobs = crewJobs.filter { it.status == BookingStatus.COMPLETED }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .padding(horizontal = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Your Production Calls",
      color = FameGoWhite,
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Confirmed and past shoots",
      color = FameGoTextMuted,
      fontSize = 13.sp,
      modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
    )

    if (crewJobs.isEmpty()) {
      Spacer(modifier = Modifier.height(40.dp))
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Nothing scheduled yet.",
          color = FameGoWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "New production bookings will appear here.",
          color = FameGoTextMuted,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    } else {
      if (activeJobs.isNotEmpty()) {
        Text(
          text = "Active shoots",
          color = FameGoWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        activeJobs.forEach { job ->
          CrewJobRow(job = job, onOpenBooking = onOpenBooking)
        }
        if (completedJobs.isNotEmpty()) {
          Text(
            text = "Completed",
            color = FameGoWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
          )
          completedJobs.forEach { job ->
            CrewJobRow(job = job, onOpenBooking = onOpenBooking)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(110.dp))
  }
}

@Composable
private fun CrewJobRow(
  job: Booking,
  onOpenBooking: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  SoftCard(
    onClick = { onOpenBooking(job.id) },
    testTag = "crew_job_item_${job.id}",
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = job.title,
          color = FameGoWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "₹${job.estimatedBudget}",
          color = FameGoGold,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "${job.date} • ${job.time} • ${job.location.venueName}",
        color = FameGoTextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

// =============================================================================
// 18. CREW PROFILE SCREEN (Grouped Floating Sections)
// =============================================================================

@Composable
fun CrewProfileScreen(
  onLogout: () -> Unit = {},
  onEditProfile: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val currentUser by FameGoRepository.currentUser.collectAsState()
  val crewProfiles by FameGoRepository.crewProfiles.collectAsState()
  val bookings by FameGoRepository.bookings.collectAsState()
  val isAvailable by FameGoRepository.isCrewAvailable.collectAsState()
  val appContext = LocalContext.current
  val scrollState = rememberScrollState()

  val me = crewProfiles.firstOrNull { it.userId == currentUser.id }
  val myJobs = bookings.filter { b -> me != null && b.assignedCrew.any { it.crewId == me.id } }
  val completedCount = myJobs.count { it.status == BookingStatus.COMPLETED }
  val activeCount = myJobs.count {
    it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.IN_PROGRESS
  }
  val displayName = me?.fullName?.ifBlank { null }
    ?: currentUser.name.ifBlank { "FameGo Crew" }
  val initials = displayName.split(" ").filter { it.isNotBlank() }.take(2)
    .joinToString("") { it.first().uppercase() }.ifEmpty { "CR" }
  val dispatchOn = com.example.data.FameGoDispatchStore(appContext).isDispatchOn()
  var showCrewRules by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Transparent)
      .verticalScroll(scrollState)
      .padding(horizontal = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

    // Top Profile Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(FameGoCardElevated)
          .border(1.dp, FameGoGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = initials, color = FameGoGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = displayName, color = FameGoWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(if (isAvailable) FameGoSuccessGreen else FameGoTextMuted)
          )
        }
        Text(
          text = (me?.primaryRole?.title ?: "Shooter") +
            (if (me?.verificationStatus == com.example.model.VerificationStatus.VERIFIED) " • Verified" else ""),
          color = FameGoTextSecondary, fontSize = 13.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Real stats strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      CrewStatCard(value = "$completedCount", label = "Completed", modifier = Modifier.weight(1f))
      CrewStatCard(
        value = if (me != null && me.rating > 0) "%.1f".format(me.rating) else "—",
        label = "Rating",
        modifier = Modifier.weight(1f)
      )
      CrewStatCard(value = "$activeCount", label = "Active", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(text = "DUTY", color = FameGoTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(8.dp))

    SoftCard {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { FameGoRepository.toggleCrewAvailability() }
            .padding(vertical = 12.dp)
            .testTag("crew_profile_duty_row"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Availability", color = FameGoWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
          Text(
            text = if (isAvailable) "Ready for shoots" else "Off duty",
            color = if (isAvailable) FameGoSuccessGreen else FameGoTextMuted,
            fontSize = 13.sp
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              // Shoot alerts need background running: point at battery settings.
              runCatching {
                appContext.startActivity(
                  android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
              }
            }
            .padding(vertical = 12.dp)
            .testTag("crew_profile_battery_row"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Shoot alerts", color = FameGoWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
              text = if (dispatchOn) "Listening — keep battery unrestricted" else "Tap to keep alerts alive",
              color = FameGoTextMuted, fontSize = 12.sp
            )
          }
          Text(
            text = if (dispatchOn) "ON" else "OFF",
            color = if (dispatchOn) FameGoSuccessGreen else FameGoGold,
            fontSize = 12.sp, fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = FameGoCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { showCrewRules = true }
        .testTag("crew_rules_button")
    ) {
      Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
        Text(text = "House rules", color = FameGoTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = FameGoCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onEditProfile() }
        .testTag("crew_edit_profile_button")
    ) {
      Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
        Text(text = "Edit profile", color = FameGoGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = FameGoSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, FameGoLiveRed.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onLogout() }
        .testTag("crew_logout_button")
    ) {
      Box(modifier = Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
        Text(text = "Sign out", color = FameGoLiveRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(110.dp))
  }

  if (showCrewRules) {
    com.example.ui.components.HouseRulesDialog(onDismiss = { showCrewRules = false })
  }
}

@Composable
private fun CrewStatCard(value: String, label: String, modifier: Modifier = Modifier) {
  SoftCard(modifier = modifier) {
    Column(
      modifier = Modifier.padding(vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = value, color = FameGoGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      Text(text = label, color = FameGoTextMuted, fontSize = 11.sp)
    }
  }
}

@Composable
private fun ProfileRowItem(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, color = FameGoWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    Text(text = value, color = FameGoTextMuted, fontSize = 13.sp)
  }
}
