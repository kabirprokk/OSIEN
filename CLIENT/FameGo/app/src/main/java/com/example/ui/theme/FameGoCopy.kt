package com.example.ui.theme

object FameGoCopy {
  // Navigation & Core Actions
  const val BOOK_A_SHOOT = "Book a Shoot"
  const val FIND_MY_CREW = "Find My Crew"
  const val BOOK_AGAIN = "Book Again"
  const val VIEW_SHOOT = "View Shoot"
  const val MESSAGE = "Message"
  const val DONE = "Done"
  const val CONTINUE = "Continue"
  const val BACK = "Back"
  const val SAVE = "Save"
  const val EDIT = "Edit"
  const val CANCEL = "Cancel"
  const val CONFIRM = "Confirm"
  const val ACCEPT = "Accept"
  const val DECLINE = "Decline"

  // Booking Statuses (Strict Consistency across all screens)
  const val STATUS_DRAFT = "Draft"
  const val STATUS_FINDING_CREW = "Finding Crew"
  const val STATUS_CONFIRMED = "Confirmed"
  const val STATUS_UPCOMING = "Upcoming"
  const val STATUS_IN_PROGRESS = "In Progress"
  const val STATUS_COMPLETED = "Completed"
  const val STATUS_CANCELLED = "Cancelled"

  // Client Home Copy
  const val HOME_HEADLINE = "What are we shooting today?"
  const val HOME_UPCOMING = "Your next shoot"
  const val HOME_SHOOT_TOMORROW = "Your shoot is tomorrow"
  const val HOME_SHOOT_TODAY = "Shoot today"
  const val HOME_FINDING_CREW = "Finding your crew"
  const val HOME_HOW_DID_IT_GO = "How did it go?"

  // Natural Questions for Booking Flow
  const val STEP_1_QUESTION = "What are we shooting?"
  const val STEP_2_QUESTION = "When should the crew arrive?"
  const val STEP_3_QUESTION = "Where are we shooting?"
  const val STEP_4_QUESTION = "Who do you need?"
  const val STEP_5_QUESTION = "Tell us about the shoot"
  const val STEP_6_QUESTION = "Looks good?"

  // Search & Radar Copy
  const val SEARCHING_TITLE = "Finding your crew"
  const val SEARCHING_STEP_1 = "Checking availability"
  const val SEARCHING_STEP_2 = "Matching your requirements"
  const val SEARCHING_STEP_3 = "Crew are responding"
  const val SEARCHING_STEP_4 = "Confirming your crew"
  const val SEARCHING_EMPTY_TITLE = "No crew available yet"
  const val SEARCHING_EMPTY_SUBTITLE = "We'll keep checking."
  const val SEARCHING_STUDIO_CHECK = "Famebros Studio is checking this request."

  // Crew Confirmation
  const val CONFIRMED_TITLE = "Your crew is confirmed"
  const val CONFIRMED_SUBTITLE = "Everything is set for your shoot."

  // Crew App Copy
  const val CREW_READY = "Ready for shoots"
  const val CREW_READY_SUBTITLE = "You can receive new requests."
  const val CREW_OFF_DUTY = "Off duty"
  const val CREW_OFF_DUTY_SUBTITLE = "You won't receive new requests."
  const val CREW_NEW_REQUEST = "New shoot request"

  // Crew Job States
  const val JOB_STATE_NEW_REQUEST = "New Request"
  const val JOB_STATE_ACCEPTED = "Accepted"
  const val JOB_STATE_UPCOMING = "Upcoming"
  const val JOB_STATE_ARRIVED = "Arrived"
  const val JOB_STATE_IN_PROGRESS = "In Progress"
  const val JOB_STATE_COMPLETED = "Completed"

  // Admin Copy
  const val ADMIN_ACTIVE_REQUESTS = "Active Requests"
  const val ADMIN_TODAYS_SHOOTS = "Today's Shoots"
  const val ADMIN_AVAILABLE_CREW = "Available Crew"
  const val ADMIN_NEEDS_ATTENTION = "Needs Attention"
  const val ADMIN_PENDING_VERIFICATION = "Pending Verification"
  const val ADMIN_ASSIGN_CREW = "Assign Crew"
  const val ADMIN_VIEW_BOOKING = "View Booking"
  const val ADMIN_CLIENTS = "Clients"
  const val ADMIN_CREW = "Crew"

  // Profile & Settings
  const val PROFILE_TITLE = "Profile"
  const val ACCOUNT_TITLE = "Account"
  const val SAVED_LOCATIONS = "Saved Locations"
  const val NOTIFICATIONS = "Notifications"
  const val SUPPORT = "Support"
  const val PRIVACY = "Privacy"
  const val TERMS = "Terms"
  const val LOG_OUT = "Log Out"
  const val DELETE_ACCOUNT = "Delete Account"

  // Chat
  const val CHAT_TITLE = "FameGo Chat"
  const val CHAT_PLACEHOLDER = "Type a message..."
  const val CHAT_SEND = "Send"
  const val CHAT_ATTACH = "Attach"

  // Empty States
  const val EMPTY_BOOKINGS_TITLE = "Nothing scheduled yet."
  const val EMPTY_BOOKINGS_SUBTITLE = "Your next shoot starts here."
  const val EMPTY_CREW_REQUESTS_TITLE = "No new requests."
  const val EMPTY_CREW_REQUESTS_SUBTITLE = "We'll show them here when they arrive."
  const val EMPTY_NOTIFICATIONS = "You're all caught up."
  const val EMPTY_SEARCH = "No results found."

  // Error Messages
  const val ERROR_GENERIC = "Something went wrong. Try again."
  const val ERROR_CONNECTION = "Check your internet connection."
  const val ERROR_EMAIL = "Please enter your email."
  const val ERROR_DATE = "Please choose a date."
  const val ERROR_CREW = "Please add at least one crew member."
  const val ERROR_SUBMIT = "We couldn't submit your request."
  const val ERROR_UNAVAILABLE = "That shoot is no longer available."

  // Success Messages
  const val SUCCESS_BOOKING_CREATED = "Booking created"
  const val SUCCESS_REQUEST_SENT = "Request sent"
  const val SUCCESS_CREW_ASSIGNED = "Crew assigned"
  const val SUCCESS_PROFILE_UPDATED = "Profile updated"
  const val SUCCESS_SAVED = "Saved"
  const val SUCCESS_AVAILABILITY_UPDATED = "Availability updated"
  const val SUCCESS_BOOKING_CANCELLED = "Booking cancelled"

  // Confirmation Dialogs
  const val DIALOG_CANCEL_TITLE = "Cancel this booking?"
  const val DIALOG_CANCEL_TEXT = "This can't be undone."
  const val DIALOG_CANCEL_KEEP = "Keep Booking"
  const val DIALOG_CANCEL_CONFIRM = "Cancel Booking"

  const val DIALOG_DECLINE_TITLE = "Decline this shoot?"
  const val DIALOG_DECLINE_BACK = "Go Back"
  const val DIALOG_DECLINE_CONFIRM = "Decline"

  const val DIALOG_LOGOUT_TITLE = "Log out of FameGo?"
  const val DIALOG_LOGOUT_STAY = "Stay"
  const val DIALOG_LOGOUT_CONFIRM = "Log Out"

  const val DIALOG_DELETE_TITLE = "Delete your account?"
  const val DIALOG_DELETE_TEXT = "This will permanently remove your FameGo account."
  const val DIALOG_DELETE_KEEP = "Keep Account"
  const val DIALOG_DELETE_CONFIRM = "Delete Account"
}
