package com.example.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.service.CrewDispatchService
import com.example.di.ServiceLocator
import com.example.domain.model.UserRole
import com.example.presentation.admin.AdminOverviewScreen
import com.example.presentation.auth.AuthMode
import com.example.presentation.auth.AuthScreen
import com.example.presentation.auth.VideoWelcomeScreen
import com.example.presentation.client.booking.BookShootWizardScreen
import com.example.presentation.client.booking.ConfirmationScreen
import com.example.presentation.client.booking.SearchingCrewScreen
import com.example.presentation.client.bookings.ClientBookingsScreen
import com.example.presentation.client.home.ClientHomeScreen
import com.example.presentation.common.BookingChatScreen
import com.example.presentation.common.BookingDetailScreen
import com.example.presentation.common.MessagesOverviewScreen
import com.example.presentation.common.NotificationDialog
import com.example.presentation.components.EmptyState
import com.example.presentation.components.FameBookTopBar
import com.example.presentation.crew.home.CrewHomeScreen
import com.example.presentation.crew.requests.CrewRequestsScreen
import com.example.presentation.crew.request.CrewIncomingRequestViewModel
import com.example.presentation.crew.request.IncomingShootRequestOverlay
import com.example.presentation.profile.ProfileScreen
import com.example.presentation.splash.OnboardingScreen
import com.example.presentation.splash.SplashScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ObsidianBlack
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class MainTab(val label: String, val icon: ImageVector, val testTag: String)

private fun tabsForRole(role: UserRole?): List<MainTab> = when (role) {
  UserRole.CREW -> listOf(
    MainTab("Home", Icons.Default.Home, "nav_tab_home"),
    MainTab("Requests", Icons.Default.Videocam, "nav_tab_requests"),
    MainTab("Schedule", Icons.Default.CalendarToday, "nav_tab_schedule"),
    MainTab("Messages", Icons.Default.Chat, "nav_tab_messages"),
    MainTab("Profile", Icons.Default.Person, "nav_tab_profile")
  )
  else -> listOf(
    MainTab("Home", Icons.Default.Home, "nav_tab_home"),
    MainTab("Bookings", Icons.Default.CalendarToday, "nav_tab_bookings"),
    MainTab("Messages", Icons.Default.Chat, "nav_tab_messages"),
    MainTab("Profile", Icons.Default.Person, "nav_tab_profile")
  )
}

sealed class Screen(val route: String) {
  object Splash : Screen("splash")
  object Onboarding : Screen("onboarding")
  object VideoWelcome : Screen("video_welcome")
  object Auth : Screen("auth/{mode}") {
    fun createRoute(mode: AuthMode = AuthMode.SIGN_IN) = "auth/${mode.name}"
  }
  object MainTabs : Screen("main_tabs")
  object BookWizard : Screen("book_wizard?templateId={templateId}") {
    fun createRoute(templateId: String = "") = "book_wizard?templateId=$templateId"
  }
  object SearchingCrew : Screen("searching_crew/{bookingId}") {
    fun createRoute(bookingId: String) = "searching_crew/$bookingId"
  }
  object Confirmation : Screen("confirmation/{bookingId}") {
    fun createRoute(bookingId: String) = "confirmation/$bookingId"
  }
  object BookingDetail : Screen("booking_detail/{bookingId}") {
    fun createRoute(bookingId: String) = "booking_detail/$bookingId"
  }
  object BookingChat : Screen("booking_chat/{bookingId}") {
    fun createRoute(bookingId: String) = "booking_chat/$bookingId"
  }
}

@Composable
fun AppNavigation(
  navController: NavHostController = rememberNavController(),
  initialBookingId: String? = null,
  onBookingIdHandled: () -> Unit = {}
) {
  val userRepository = ServiceLocator.userRepository
  val bookingRepository = ServiceLocator.bookingRepository
  val crewRepository = ServiceLocator.crewRepository
  val messageRepository = ServiceLocator.messageRepository
  val notificationRepository = ServiceLocator.notificationRepository
  val favoriteRepository = ServiceLocator.favoriteRepository

  val currentUser by userRepository.currentUser.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var showNotificationDialog by remember { mutableStateOf(false) }
  var isRestoringSession by remember { mutableStateOf(true) }
  var splashDone by remember { mutableStateOf(false) }

  // Swipeable main tabs. One pager hosts every tab so left/right swipes
  // move between tabs without extra navigation transactions.
  val tabs = remember(currentUser?.role) { tabsForRole(currentUser?.role) }
  val pagerState = rememberPagerState(pageCount = { maxOf(tabs.size, 1) })

  // Restore saved Supabase session once so reopening the app skips sign-in.
  LaunchedEffect(Unit) {
    try {
      userRepository.restoreSession()
    } catch (_: Exception) {
      // Stay signed out; user can sign in manually.
    } finally {
      isRestoringSession = false
    }
  }

  // Reset to the first tab when the tab set changes (e.g. different role
  // after re-login) so the pager never points past the last page.
  LaunchedEffect(tabs.size) {
    if (pagerState.currentPage >= tabs.size) {
      pagerState.scrollToPage(0)
    }
  }

  // Deep link handling from background notification tap. Only when signed in;
  // a signed-out tap just lands on the welcome flow.
  LaunchedEffect(initialBookingId, currentUser?.id) {
    if (!initialBookingId.isNullOrBlank() && currentUser != null) {
      navController.navigate(Screen.BookingDetail.createRoute(initialBookingId))
      onBookingIdHandled()
    } else if (!initialBookingId.isNullOrBlank() && currentUser == null && !isRestoringSession) {
      onBookingIdHandled()
    }
  }

  // Live push socket follows the session: on while signed in, off on logout.
  LaunchedEffect(currentUser?.id) {
    if (currentUser != null) ServiceLocator.startRealtime()
    else ServiceLocator.stopRealtime()
  }

  // Manage persistent Crew Dispatch Service in background when crew member is Available
  LaunchedEffect(currentUser?.id, currentUser?.role) {
    val user = currentUser
    if (user?.role == UserRole.CREW) {
      crewRepository.getCrewProfileByUserId(user.id).collectLatest { profile ->
        if (profile?.isAvailable == true) {
          CrewDispatchService.startService(context, user.id, user.name)
        } else {
          CrewDispatchService.stopService(context)
        }
      }
    } else {
      CrewDispatchService.stopService(context)
    }
  }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val isBottomBarVisible = currentRoute == Screen.MainTabs.route && currentUser != null

  Scaffold(
    containerColor = ObsidianBlack,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (isBottomBarVisible && currentUser != null) {
        FameBookTopBar(
          currentUser = currentUser,
          onNotificationClick = { showNotificationDialog = true }
        )
      }
    },
    bottomBar = {
      if (isBottomBarVisible && currentUser != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF2131622),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x33FFFFFF)),
            shadowElevation = 8.dp,
            modifier = Modifier
              .fillMaxWidth()
              .height(64.dp)
              .testTag("floating_liquid_glass_nav_bar")
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      Color(0x22FFFFFF),
                      Color(0x00000000)
                    )
                  )
                )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                tabs.forEachIndexed { index, tab ->
                  FloatingNavItem(
                    selected = pagerState.currentPage == index,
                    icon = tab.icon,
                    label = tab.label,
                    testTag = tab.testTag,
                    onClick = {
                      if (pagerState.currentPage != index) {
                        scope.launch { pagerState.animateScrollToPage(index) }
                      }
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(animationSpec = tween(180)) + slideInHorizontally(animationSpec = tween(200)) { it / 5 } },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(180)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(200)) { it / 5 } }
      ) {
        composable(Screen.Splash.route) {
          SplashScreen(onFinish = { splashDone = true })

          // Wait for both the splash animation and session restore before routing.
          // Otherwise a slow network restore loses the race and forces sign-in again.
          LaunchedEffect(splashDone, isRestoringSession, currentUser) {
            if (splashDone && !isRestoringSession) {
              if (currentUser != null) {
                navController.navigate(Screen.MainTabs.route) {
                  popUpTo(Screen.Splash.route) { inclusive = true }
                }
              } else {
                navController.navigate(Screen.VideoWelcome.route) {
                  popUpTo(Screen.Splash.route) { inclusive = true }
                }
              }
            }
          }
        }

        composable(Screen.Onboarding.route) {
          OnboardingScreen(
            onComplete = {
              navController.navigate(Screen.VideoWelcome.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
              }
            }
          )
        }

        composable(Screen.VideoWelcome.route) {
          VideoWelcomeScreen(
            onSignUpClick = { navController.navigate(Screen.Auth.createRoute(AuthMode.SIGN_UP)) },
            onSignInClick = { navController.navigate(Screen.Auth.createRoute(AuthMode.SIGN_IN)) }
          )
        }

        composable(
          route = Screen.Auth.route,
          arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
          val initialMode = runCatching {
            AuthMode.valueOf(backStackEntry.arguments?.getString("mode") ?: AuthMode.SIGN_IN.name)
          }.getOrDefault(AuthMode.SIGN_IN)
          AuthScreen(
            userRepository = userRepository,
            initialMode = initialMode,
            onLoginSuccess = { user ->
              navController.navigate(Screen.MainTabs.route) {
                popUpTo(Screen.VideoWelcome.route) { inclusive = true }
              }
            }
          )
        }

        // Swipeable main tabs: Home / Requests-or-Bookings / Messages / Profile.
        // HorizontalPager gives left/right swipe; the bottom bar animates to pages.
        composable(Screen.MainTabs.route) {
          val user = currentUser
          if (user == null) {
            LaunchedEffect(Unit) {
              navController.navigate(Screen.VideoWelcome.route) {
                popUpTo(0) { inclusive = true }
              }
            }
          } else {
            val isCrew = user.role == UserRole.CREW
            HorizontalPager(
              state = pagerState,
              modifier = Modifier.fillMaxSize(),
              key = { index -> "${user.role.name}_tab_$index" }
            ) { page ->
              when (page) {
                0 -> when (user.role) {
                  UserRole.CLIENT -> {
                    ClientHomeScreen(
                      currentUser = user,
                      bookingRepository = bookingRepository,
                      crewRepository = crewRepository,
                      favoriteRepository = favoriteRepository,
                      onBookShootClick = { navController.navigate(Screen.BookWizard.createRoute()) },
                      onActiveRequestClick = { id -> navController.navigate(Screen.SearchingCrew.createRoute(id)) },
                      onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) },
                      onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) },
                      onRebookClick = { id -> navController.navigate(Screen.BookWizard.createRoute(id)) }
                    )
                  }
                  UserRole.CREW -> {
                    CrewHomeScreen(
                      currentUser = user,
                      bookingRepository = bookingRepository,
                      crewRepository = crewRepository,
                      snackbarHostState = snackbarHostState,
                      onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) },
                      onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) }
                    )
                  }
                  UserRole.ADMIN -> {
                    AdminOverviewScreen(
                      currentUser = user,
                      bookingRepository = bookingRepository,
                      crewRepository = crewRepository,
                      userRepository = userRepository,
                      onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) }
                    )
                  }
                }
                1 -> if (isCrew) {
                  CrewRequestsScreen(
                    currentUser = user,
                    bookingRepository = bookingRepository,
                    crewRepository = crewRepository,
                    snackbarHostState = snackbarHostState,
                    onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) }
                  )
                } else {
                  ClientBookingsScreen(
                    currentUser = user,
                    bookingRepository = bookingRepository,
                    onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) },
                    onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) },
                    onBookShootClick = { navController.navigate(Screen.BookWizard.createRoute()) }
                  )
                }
                else -> if (isCrew && page == 2) {
                  ClientBookingsScreen(
                    currentUser = user,
                    bookingRepository = bookingRepository,
                    onBookingClick = { id -> navController.navigate(Screen.BookingDetail.createRoute(id)) },
                    onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) },
                    onBookShootClick = {
                      scope.launch {
                        snackbarHostState.showSnackbar("Only client accounts can book shoots.")
                      }
                    }
                  )
                } else if ((!isCrew && page == 2) || (isCrew && page == 3)) {
                  MessagesOverviewScreen(
                    currentUser = user,
                    bookingRepository = bookingRepository,
                    onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) }
                  )
                } else {
                  ProfileScreen(
                    currentUser = user,
                    userRepository = userRepository,
                    crewRepository = crewRepository,
                    onLogout = {
                      navController.navigate(Screen.VideoWelcome.route) {
                        popUpTo(0) { inclusive = true }
                      }
                    }
                  )
                }
              }
            }
          }
        }

        // BOOKING WIZARD — clients only. Crew and admin accounts are blocked here
        // (the backend policy enforces this too) so only clients can book shoots.
        composable(
          route = Screen.BookWizard.route,
          arguments = listOf(navArgument("templateId") {
            type = NavType.StringType
            defaultValue = ""
          })
        ) { backStackEntry ->
          val templateId = backStackEntry.arguments?.getString("templateId").orEmpty()
          val user = currentUser
          val templateBooking by remember(templateId) {
            if (templateId.isBlank()) kotlinx.coroutines.flow.flowOf(null)
            else bookingRepository.getBooking(templateId)
          }.collectAsState(initial = null)
          if (user != null) {
            if (user.role != UserRole.CLIENT) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                EmptyState(
                  title = "CLIENTS ONLY",
                  message = "Only client accounts can request crew. Your ${user.role.name.lowercase()} account cannot book shoots.",
                  actionText = "GO BACK",
                  onActionClick = { navController.popBackStack() }
                )
              }
            } else {
              BookShootWizardScreen(
                currentUser = user,
                templateBooking = templateBooking,
                onBackClick = { navController.popBackStack() },
                onRequestCrewSubmit = { newBooking ->
                  scope.launch {
                    val result = bookingRepository.createBooking(newBooking)
                    if (result.isSuccess) {
                      val created = result.getOrThrow()
                      navController.navigate(Screen.SearchingCrew.createRoute(created.id)) {
                        popUpTo(Screen.BookWizard.route) { inclusive = true }
                      }
                    } else {
                      snackbarHostState.showSnackbar("Failed to submit request: ${result.exceptionOrNull()?.message}")
                    }
                  }
                }
              )
            }
          }
        }

        // SEARCHING RADAR SCREEN
        composable(
          route = Screen.SearchingCrew.route,
          arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
          val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
          val user = currentUser
          if (user != null && user.role != UserRole.CLIENT) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              EmptyState(
                title = "CLIENTS ONLY",
                message = "Live request tracking is available on client accounts.",
                actionText = "GO BACK",
                onActionClick = { navController.popBackStack() }
              )
            }
          } else if (user != null) {
            SearchingCrewScreen(
              bookingId = bookingId,
              currentUser = user,
              bookingRepository = bookingRepository,
              onConfirmed = { confirmedBooking ->
                navController.navigate(Screen.Confirmation.createRoute(confirmedBooking.id)) {
                  popUpTo(Screen.SearchingCrew.createRoute(bookingId)) { inclusive = true }
                }
              },
              onCancelled = {
                scope.launch {
                  bookingRepository.cancelBooking(bookingId)
                  navController.navigate(Screen.MainTabs.route) {
                    popUpTo(Screen.MainTabs.route) { inclusive = true }
                  }
                }
              },
              onDismissToHome = {
                navController.navigate(Screen.MainTabs.route) {
                  popUpTo(Screen.MainTabs.route) { inclusive = true }
                }
              }
            )
          }
        }

        // CONFIRMATION SCREEN
        composable(
          route = Screen.Confirmation.route,
          arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
          val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
          val user = currentUser
          val bookingFlow = remember(bookingId) { bookingRepository.getBooking(bookingId) }
          val booking by bookingFlow.collectAsState(initial = null)

          if (user != null && user.role != UserRole.CLIENT) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              EmptyState(
                title = "CLIENTS ONLY",
                message = "Booking confirmations are available on client accounts.",
                actionText = "GO BACK",
                onActionClick = { navController.popBackStack() }
              )
            }
          } else if (booking != null && user != null) {
            ConfirmationScreen(
              booking = booking!!,
              currentUser = user,
              favoriteRepository = favoriteRepository,
              onOpenChat = { navController.navigate(Screen.BookingChat.createRoute(bookingId)) },
              onViewBookingDetails = { navController.navigate(Screen.BookingDetail.createRoute(bookingId)) },
              onBackToHome = {
                navController.navigate(Screen.MainTabs.route) {
                  popUpTo(Screen.MainTabs.route) { inclusive = true }
                }
              }
            )
          }
        }

        // BOOKING DETAIL SCREEN
        composable(
          route = Screen.BookingDetail.route,
          arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
          val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
          val user = currentUser
          if (user != null) {
            BookingDetailScreen(
              bookingId = bookingId,
              currentUser = user,
              bookingRepository = bookingRepository,
              favoriteRepository = favoriteRepository,
              onBackClick = { navController.popBackStack() },
              onOpenChat = { id -> navController.navigate(Screen.BookingChat.createRoute(id)) }
            )
          }
        }

        // PRODUCTION CHAT SCREEN
        composable(
          route = Screen.BookingChat.route,
          arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
          val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
          val user = currentUser
          if (user != null) {
            BookingChatScreen(
              bookingId = bookingId,
              currentUser = user,
              bookingRepository = bookingRepository,
              messageRepository = messageRepository,
              onBackClick = { navController.popBackStack() }
            )
          }
        }
      }

      // FLOATING INCOMING SHOOT REQUEST OVERLAY FOR CREW
      if (currentUser?.role == UserRole.CREW) {
        val crewUser = currentUser!!
        val incomingRequestViewModel = remember(crewUser.id) {
          CrewIncomingRequestViewModel(
            crewUserId = crewUser.id,
            bookingRepository = bookingRepository,
            crewRepository = crewRepository,
            notificationRepository = notificationRepository
          )
        }

        IncomingShootRequestOverlay(
          viewModel = incomingRequestViewModel,
          onViewShoot = { bookingId ->
            navController.navigate(Screen.BookingDetail.createRoute(bookingId))
          }
        )
      }
    }

    if (showNotificationDialog) {
      NotificationDialog(
        userId = currentUser?.id ?: "",
        notificationRepository = notificationRepository,
        onDismiss = { showNotificationDialog = false },
        onNotificationClick = { bookingId ->
          showNotificationDialog = false
          if (bookingId != null) {
            navController.navigate(Screen.BookingDetail.createRoute(bookingId))
          }
        }
      )
    }
  }
}

/**
 * Floating Liquid Glass Navigation Item with smooth capsule highlight
 */
@Composable
private fun FloatingNavItem(
  selected: Boolean,
  icon: ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.94f else 1f,
    animationSpec = tween(durationMillis = 100),
    label = "nav_item_scale"
  )

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = if (selected) Color(0x33FFB800) else Color.Transparent,
    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)) else null,
    modifier = Modifier
      .scale(scale)
      .clip(RoundedCornerShape(20.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(
        horizontal = if (selected) 14.dp else 10.dp,
        vertical = 8.dp
      ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) AmberGold else Color(0xFF8E95A5),
        modifier = Modifier.size(20.dp)
      )
      if (selected) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = AmberGold
        )
      }
    }
  }
}
