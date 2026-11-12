package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.model.GameRun
import com.example.data.model.PlayerProfile
import com.example.data.repository.GameRepository
import com.example.data.repository.UpgradeType
import com.example.game.GameEngine
import com.example.game.GameEvent
import com.example.game.GamePhase
import com.example.game.GameSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
  GAME,
  SHOP,
  HISTORY,
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
  private val repository: GameRepository,
  private val currentUserId: String,
  private val displayNameHint: String?,
) : ViewModel() {

  private val engine = GameEngine()
  val soundEngine = SoundEngine()

  private val _destination = MutableStateFlow(AppDestination.GAME)
  val destination: StateFlow<AppDestination> = _destination.asStateFlow()

  private val _gameSnapshot = MutableStateFlow(engine.snapshot())
  val gameSnapshot: StateFlow<GameSnapshot> = _gameSnapshot.asStateFlow()

  private val _isNewHighScore = MutableStateFlow(false)
  val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

  private val _shopMessage = MutableStateFlow<String?>(null)
  val shopMessage: StateFlow<String?> = _shopMessage.asStateFlow()

  val profileState: StateFlow<UiState<PlayerProfile>> = repository.observeProfile(currentUserId)
    .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
      val resolved = profile ?: PlayerProfile(id = currentUserId, userId = currentUserId)
      soundEngine.enabled = resolved.soundEnabled
      engine.syncProfile(resolved)
      if (_gameSnapshot.value.phase == GamePhase.MENU_IDLE) {
        _gameSnapshot.value = engine.snapshot()
      }
      UiState.Success(resolved)
    }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing profile", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "Profil yüklenemedi."))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  val runsState: StateFlow<UiState<List<GameRun>>> = repository.observeRuns(currentUserId)
    .map<List<GameRun>, UiState<List<GameRun>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing runs", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: "Koşu geçmişi yüklenemedi."))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading,
    )

  init {
    viewModelScope.launch {
      runCatching {
        repository.ensureProfileInitialized(displayNameHint)
      }.onFailure {
        Log.e(TAG, "Failed to initialize profile", it)
      }
    }
  }

  private fun currentProfileOrDefault(): PlayerProfile {
    return (profileState.value as? UiState.Success)?.data
      ?: PlayerProfile(id = currentUserId, userId = currentUserId)
  }

  fun navigateTo(dest: AppDestination) {
    if (dest != AppDestination.GAME && _gameSnapshot.value.phase == GamePhase.RUNNING) {
      engine.pause()
      _gameSnapshot.value = engine.snapshot()
    }
    _destination.value = dest
  }

  fun startGame() {
    val profile = currentProfileOrDefault()
    _isNewHighScore.value = false
    engine.startNewRun(profile)
    _gameSnapshot.value = engine.snapshot()
    soundEngine.playPowerUp()
  }

  fun returnToMenu() {
    engine.resetToMenu(currentProfileOrDefault())
    _gameSnapshot.value = engine.snapshot()
  }

  fun pauseGame() {
    engine.pause()
    _gameSnapshot.value = engine.snapshot()
  }

  fun resumeGame() {
    engine.resume()
    _gameSnapshot.value = engine.snapshot()
  }

  fun onSwipeLeft() {
    val events = mutableListOf<GameEvent>()
    engine.swipeLeft(events)
    handleEvents(events)
    _gameSnapshot.value = engine.snapshot()
  }

  fun onSwipeRight() {
    val events = mutableListOf<GameEvent>()
    engine.swipeRight(events)
    handleEvents(events)
    _gameSnapshot.value = engine.snapshot()
  }

  fun onSwipeUp() {
    val events = mutableListOf<GameEvent>()
    engine.swipeUp(events)
    handleEvents(events)
    _gameSnapshot.value = engine.snapshot()
  }

  fun onSwipeDown() {
    val events = mutableListOf<GameEvent>()
    engine.swipeDown(events)
    handleEvents(events)
    _gameSnapshot.value = engine.snapshot()
  }

  fun onActivateHoverboard() {
    val events = mutableListOf<GameEvent>()
    if (engine.activateHoverboard(events)) {
      handleEvents(events)
      _gameSnapshot.value = engine.snapshot()
    }
  }

  fun stepFrame(dtSeconds: Float) {
    val events = engine.update(dtSeconds)
    if (events.isNotEmpty()) {
      handleEvents(events)
    }
    _gameSnapshot.value = engine.snapshot()
  }

  private fun handleEvents(events: List<GameEvent>) {
    for (event in events) {
      when (event) {
        GameEvent.CoinCollected -> soundEngine.playCoin()
        GameEvent.Jumped -> soundEngine.playJump()
        GameEvent.Slid -> soundEngine.playSlide()
        GameEvent.LaneSwitched -> soundEngine.playLaneSwitch()
        is GameEvent.PowerUpPicked -> soundEngine.playPowerUp()
        GameEvent.HoverboardActivated -> soundEngine.playHoverboard()
        GameEvent.HoverboardShattered -> soundEngine.playStumble()
        GameEvent.Stumbled -> soundEngine.playStumble()
        is GameEvent.Crashed -> {
          soundEngine.playCrash()
          val current = currentProfileOrDefault()
          _isNewHighScore.value = event.finalScore > current.highScore && event.finalScore > 0L
          viewModelScope.launch {
            runCatching {
              repository.recordFinishedRun(
                currentProfile = current,
                score = event.finalScore,
                distanceMeters = event.distanceMeters,
                coinsCollected = event.coinsCollected,
                hoverboardsUsed = event.hoverboardsUsed,
              )
            }.onFailure {
              Log.e(TAG, "Failed to save run", it)
            }
          }
        }
      }
    }
  }

  fun toggleSound() {
    val current = currentProfileOrDefault()
    val next = !current.soundEnabled
    soundEngine.enabled = next
    viewModelScope.launch {
      runCatching { repository.setSoundEnabled(next) }
    }
  }

  fun buyOrEquipCharacter(characterId: String) {
    val profile = currentProfileOrDefault()
    viewModelScope.launch {
      runCatching {
        val ok = repository.buyOrEquipCharacter(profile, characterId)
        _shopMessage.value = if (ok) "Karakter seçildi!" else "Yetersiz altın!"
      }.onFailure {
        _shopMessage.value = "İşlem tamamlanamadı."
      }
    }
  }

  fun buyOrEquipBoard(boardId: String) {
    val profile = currentProfileOrDefault()
    viewModelScope.launch {
      runCatching {
        val ok = repository.buyOrEquipBoard(profile, boardId)
        _shopMessage.value = if (ok) "Kaykay seçildi!" else "Yetersiz altın!"
      }.onFailure {
        _shopMessage.value = "İşlem tamamlanamadı."
      }
    }
  }

  fun upgradePower(type: UpgradeType) {
    val profile = currentProfileOrDefault()
    viewModelScope.launch {
      runCatching {
        val ok = repository.purchaseUpgrade(profile, type)
        _shopMessage.value = if (ok) "${type.titleTr} yükseltildi!" else "Yetersiz altın veya maksimum seviye."
      }.onFailure {
        _shopMessage.value = "Yükseltme başarısız."
      }
    }
  }

  fun buyHoverboardPack() {
    val profile = currentProfileOrDefault()
    viewModelScope.launch {
      runCatching {
        val ok = repository.purchaseHoverboardPack(profile, quantity = 3L, cost = 75L)
        _shopMessage.value = if (ok) "+3 Kaykay eklendi!" else "Yetersiz altın! (75 Altın gerekli)"
      }.onFailure {
        _shopMessage.value = "Satın alma başarısız."
      }
    }
  }

  fun clearShopMessage() {
    _shopMessage.value = null
  }

  private companion object {
    const val TAG = "MainViewModel"
  }
}
