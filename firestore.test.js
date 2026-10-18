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

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@gmail.com",
      displayName: "Bob",
      sessionId: "05a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef01",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
});

test("Authenticated user: can create and read their own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceUserDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    aliceUserDoc.set({
      userId: ALICE_UID,
      email: "alice@gmail.com",
      displayName: "Alice",
      sessionId: "05b156e80062ad9f52a4d22f97bda2a19088dc08112233445566778899aabbccdd",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(aliceUserDoc.get());
});

test("Authenticated user: can manage their own chats and messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chatDoc = aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1");

  await assertSucceeds(
    chatDoc.set({
      id: "chat_1",
      userId: ALICE_UID,
      name: "Saved Messages",
      type: "SAVED_VAULT",
      createdAt: new Date(),
    })
  );

  const msgDoc = chatDoc.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgDoc.set({
      id: "msg_1",
      userId: ALICE_UID,
      chatId: "chat_1",
      senderId: "05b156e8...",
      text: "Encrypted note",
      createdAt: new Date(),
    })
  );

  // Bob cannot read Alice's messages
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("chats").doc("chat_1").collection("messages").doc("msg_1").get()
  );
});
