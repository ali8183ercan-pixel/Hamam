package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BoardCatalog
import com.example.data.model.CharacterCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureProfileInitialized_authenticatedUser_createsProfileAndAllowsUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = GameRepository(firestore, auth)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileInitialized("AliceRoach")
    }
    assertEquals(uid, profile.userId)
    assertEquals(CharacterCatalog.DEFAULT_ID, profile.selectedCharacterId)
    assertEquals(BoardCatalog.DEFAULT_ID, profile.selectedBoardId)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recordFinishedRun(
        currentProfile = profile,
        score = 3200L,
        distanceMeters = 640L,
        coinsCollected = 85L,
        hoverboardsUsed = 1L,
      )
    }

    val updated = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProfile(uid).first { it != null && it.totalRuns >= 1L }
    }
    assertNotNull(updated)
    assertTrue(updated!!.highScore >= 3200L)

    val runs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeRuns(uid).first { it.isNotEmpty() }
    }
    assertTrue(runs.any { it.score == 3200L })
  }

  @Test
  fun buyOrEquipCharacter_sufficientCoins_unlocksAndEquips() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = GameRepository(firestore, auth)
    val initial = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileInitialized("AliceRoach")
    }
    // Grant enough coins via a finished run
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recordFinishedRun(initial, score = 1000L, distanceMeters = 200L, coinsCollected = 200L, hoverboardsUsed = 0L)
    }
    val enriched = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProfile(uid).first { it != null && it.totalCoins >= 150L }!!
    }

    val bought = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.buyOrEquipCharacter(enriched, "neon_roach")
    }
    assertTrue(bought)

    val afterBuy = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProfile(uid).first { it != null && it.selectedCharacterId == "neon_roach" }!!
    }
    assertTrue(afterBuy.unlockedCharacters.contains("neon_roach"))
  }

  @Test
  fun observeProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = GameRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureProfileInitialized("AliceRoach")
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = GameRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeProfile(aliceUid).first()
      }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  @Test
  fun observeProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = GameRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeProfile("some_user_123").first()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated profile read")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_roach@test.com"
    const val BOB_EMAIL = "bob_roach@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
