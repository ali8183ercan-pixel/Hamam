package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.PlayerProfile
import com.example.data.repository.GameRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.MidnightSubway
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoachRushTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      RoachRushTheme {
        AppRoot()
      }
    }
  }
}

@Composable
fun AppRoot() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser }
    )
  } else {
    key(user.uid) {
      AuthenticatedAppContent(
        currentUserId = user.uid,
        displayNameHint = user.displayName,
      )
    }
  }
}

@Composable
private fun AuthenticatedAppContent(
  currentUserId: String,
  displayNameHint: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: MainViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        MainViewModel(
          repository = GameRepository(db),
          currentUserId = currentUserId,
          displayNameHint = displayNameHint,
        )
      }
    },
  )

  val destination by viewModel.destination.collectAsStateWithLifecycle()
  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val runsState by viewModel.runsState.collectAsStateWithLifecycle()
  val snapshot by viewModel.gameSnapshot.collectAsStateWithLifecycle()
  val isNewHighScore by viewModel.isNewHighScore.collectAsStateWithLifecycle()
  val shopMessage by viewModel.shopMessage.collectAsStateWithLifecycle()

  when (val state = profileState) {
    is UiState.Loading -> {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MidnightSubway),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = NeonCyan)
      }
    }
    is UiState.Error, is UiState.Success -> {
      val profile = (state as? UiState.Success)?.data
        ?: PlayerProfile(id = currentUserId, userId = currentUserId, displayName = displayNameHint ?: "Böcek Koşucusu")

      when (destination) {
        AppDestination.GAME -> {
          GameScreen(
            profile = profile,
            snapshot = snapshot,
            isNewHighScore = isNewHighScore,
            viewModel = viewModel,
            onSignOutClick = {
              signOut(
                credentialManager = credentialManager,
                scope = scope,
              )
            },
          )
        }
        AppDestination.SHOP -> {
          ShopScreen(
            profile = profile,
            shopMessage = shopMessage,
            viewModel = viewModel,
            onBack = { viewModel.navigateTo(AppDestination.GAME) },
          )
        }
        AppDestination.HISTORY -> {
          HistoryScreen(
            profile = profile,
            runsState = runsState,
            onBack = { viewModel.navigateTo(AppDestination.GAME) },
          )
        }
      }
    }
  }
}
