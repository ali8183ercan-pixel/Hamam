const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "RoachRunner",
    highScore: 0,
    totalCoins: 100,
    totalRuns: 0,
    bestDistanceMeters: 0,
    scoreMultiplierLevel: 1,
    magnetLevel: 1,
    jetpackLevel: 1,
    sneakersLevel: 1,
    hoverboardCount: 3,
    selectedCharacterId: "street_roach",
    selectedBoardId: "leaf_board",
    unlockedCharacters: ["street_roach"],
    unlockedBoards: ["leaf_board"],
    soundEnabled: true,
    createdAt: past,
    updatedAt: past,
  };
}

function validRun(uid) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    score: 1450,
    distanceMeters: 320,
    coinsCollected: 42,
    characterId: "street_roach",
    boardId: "leaf_board",
    createdAt: past,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profile or runs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("runs").get());
});

test("Authenticated user can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(docRef.set(validProfile(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    docRef.update({
      highScore: 2500,
      totalCoins: 240,
      totalRuns: 1,
      bestDistanceMeters: 500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user cannot read or write another user's profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(validProfile(BOB_UID)));

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).set(validProfile(BOB_UID)));
});

test("Shadow update with ghost field or invalid character is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(docRef.set(validProfile(ALICE_UID)));

  await assertFails(
    docRef.update({
      highScore: 9999,
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    docRef.update({
      selectedCharacterId: "invalid_character",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated user can create and list own runs, cannot update/delete runs, cannot access Bob runs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const runRef = aliceDb.collection("users").doc(ALICE_UID).collection("runs").doc("run_1");
  await assertSucceeds(runRef.set(validRun(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("runs").get());
  await assertFails(runRef.update({ score: 999999 }));
  await assertFails(runRef.delete());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("runs").get());
});
