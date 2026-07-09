/**
 * Migrates data from the old app's Realtime Database (users/{uid}/vehicles,
 * /records, /tasks - Kotlin/Java `Vehicle`, `Record`, `Task` model shapes)
 * into the new app's Firestore schema (users/{uid}/vehicles, /records,
 * /tasks - `VehicleEntity`, `ServiceRecordEntity`, `MaintenanceTaskEntity`).
 *
 * This is a one-off local tool, not something that ships in the app. Run
 * it once per person you're migrating, review the output, then move on.
 *
 * WHAT THIS ASSUMES ABOUT YOUR OLD DATA (verified against the source,
 * but re-check if you ever changed these screens):
 *   - Record.date, Task.taskDueDate, Task.taskLastDone are "yyyy-MM-dd" strings
 *   - Task.taskFrequency is "{number} miles" or "{number} days|weeks|months|years"
 *   - Task.taskDueMileage is a plain numeric string
 *   - Record.odometer is a numeric string
 *   - vehicleId links are stored as the vehicle's integer ID, stringified
 *
 * WHAT IT DOES NOT TRY TO MIGRATE (fields that didn't exist in the old
 * app - left null/default, backfill manually if you care):
 *   - Record.cost, Record.receiptPhotoUrl
 *   - Task.lastDoneOdometer
 *   - Vehicle photoUrl
 *
 * SETUP
 *   1. npm install
 *   2. Download a service account key for the OLD Firebase project
 *      (Project settings > Service accounts > Generate new private key)
 *      and save it as ./old-service-account.json
 *   3. Do the same for the NEW project, save as ./new-service-account.json
 *   4. Copy user-map.example.json to user-map.json and fill in real
 *      oldUid/newUid/email values for each person (see that file's comment)
 *   5. node migrate.js --dry-run    (prints what it would write, writes nothing)
 *   6. node migrate.js              (actually writes to the new project)
 *
 * Both service-account JSON files and user-map.json are gitignored -
 * never commit them, they're credentials.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const DRY_RUN = process.argv.includes('--dry-run');

const oldServiceAccount = require('./old-service-account.json');
const newServiceAccount = require('./new-service-account.json');
const userMap = JSON.parse(fs.readFileSync(path.join(__dirname, 'user-map.json'), 'utf8'));

const oldApp = admin.initializeApp(
  {
    credential: admin.credential.cert(oldServiceAccount),
    databaseURL: `https://${oldServiceAccount.project_id}-default-rtdb.firebaseio.com`
  },
  'old'
);
const newApp = admin.initializeApp({ credential: admin.credential.cert(newServiceAccount) }, 'new');

const oldDb = oldApp.database();
const newDb = newApp.firestore();

// --- date / mileage parsing helpers, matching the old app's own formats ---

function parseYmd(dateStr) {
  if (!dateStr) return null;
  const [y, m, d] = dateStr.split('-').map(Number);
  if (!y || !m || !d) return null;
  return Date.UTC(y, m - 1, d);
}

function parseNumeric(str) {
  if (!str) return null;
  const n = parseInt(String(str).replace(/[^0-9]/g, ''), 10);
  return Number.isNaN(n) ? null : n;
}

// taskFrequency was free text like "5000 miles" or "3 months" - see
// AddRecurringCheckup.java. Converts to the new schema's
// intervalMiles / intervalMonths pair (days/weeks/years get approximated
// to months - flagged in the console output so you can sanity-check them).
function parseFrequency(frequency) {
  if (!frequency) return { intervalMiles: null, intervalMonths: null, approximated: false };
  const match = frequency.trim().match(/^(\d+)\s+(\w+)$/);
  if (!match) return { intervalMiles: null, intervalMonths: null, approximated: false };

  const amount = parseInt(match[1], 10);
  const unit = match[2].toLowerCase();

  if (unit.startsWith('mile')) return { intervalMiles: amount, intervalMonths: null, approximated: false };
  if (unit.startsWith('month')) return { intervalMiles: null, intervalMonths: amount, approximated: false };
  if (unit.startsWith('day')) return { intervalMiles: null, intervalMonths: Math.max(1, Math.round(amount / 30)), approximated: true };
  if (unit.startsWith('week')) return { intervalMiles: null, intervalMonths: Math.max(1, Math.round((amount * 7) / 30)), approximated: true };
  if (unit.startsWith('year')) return { intervalMiles: null, intervalMonths: amount * 12, approximated: false };

  return { intervalMiles: null, intervalMonths: null, approximated: false };
}

// The old Record has no category field - guess one from the title/description
// the same way the new app's "describe it" NL field does. See
// domain/usecase/ParseServiceEntryUseCase.kt for the Kotlin version this
// mirrors; keep them in sync if you tune the keyword list later.
const CATEGORY_KEYWORDS = [
  ['OIL_CHANGE', ['oil']],
  ['TIRES', ['tire', 'tyre', 'rotate', 'rotation']],
  ['BRAKES', ['brake', 'pad', 'rotor']],
  ['BATTERY', ['battery']],
  ['FLUIDS', ['coolant', 'transmission fluid', 'brake fluid', 'washer fluid']],
  ['INSPECTION', ['inspection', 'inspected', 'check up', 'checkup']]
];

function guessCategory(title, description) {
  const text = `${title || ''} ${description || ''}`.toLowerCase();
  for (const [category, keywords] of CATEGORY_KEYWORDS) {
    if (keywords.some((k) => text.includes(k))) return category;
  }
  return 'OTHER';
}

// --- per-user migration ---

async function migrateUser(entry) {
  const { oldUid, newUid, email } = entry;
  console.log(`\n=== ${email} (${oldUid} -> ${newUid}) ===`);

  const snapshot = await oldDb.ref(`users/${oldUid}`).once('value');
  if (!snapshot.exists()) {
    console.log('  no data found at this path - skipping');
    return;
  }

  const oldVehicles = snapshot.child('vehicles').val() || {};
  const oldRecords = snapshot.child('records').val() || {};
  const oldTasks = snapshot.child('tasks').val() || {};

  // Odometer isn't stored on the old Vehicle - derive current mileage as
  // the highest odometer reading logged in that vehicle's records.
  const maxOdometerByVehicle = {};
  Object.values(oldRecords).forEach((r) => {
    const vId = String(r.vehicle);
    const odo = parseNumeric(r.odometer);
    if (odo != null) {
      maxOdometerByVehicle[vId] = Math.max(maxOdometerByVehicle[vId] || 0, odo);
    }
  });

  const pendingWrites = [];

  Object.values(oldVehicles).forEach((v) => {
    const id = String(v.vehicleId);
    const doc = {
      id,
      year: v.year || '',
      make: v.make || '',
      model: v.model || '',
      submodel: v.submodel || null,
      engine: v.engine || null,
      notes: v.notes || null,
      odometer: maxOdometerByVehicle[id] || 0,
      photoUrl: null,
      createdAt: v.entryTime || Date.now(),
      isSynced: true,
      isDeleted: false
    };
    console.log(`  vehicle: ${doc.year} ${doc.make} ${doc.model} -> odometer ${doc.odometer} (derived from records)`);
    pendingWrites.push({ ref: newDb.collection('users').doc(newUid).collection('vehicles').doc(id), data: doc });
  });

  Object.entries(oldRecords).forEach(([key, r]) => {
    const id = `${key}`; // keep the old push-key as the new doc id, harmless either way
    const category = guessCategory(r.title, r.description);
    const doc = {
      id,
      vehicleId: String(r.vehicle),
      title: r.title || 'Service record',
      category,
      description: r.description || null,
      odometer: parseNumeric(r.odometer) || 0,
      cost: null,
      date: parseYmd(r.date) || r.entryTime || Date.now(),
      receiptPhotoUrl: null,
      createdAt: r.entryTime || Date.now(),
      isSynced: true,
      isDeleted: false
    };
    if (category === 'OTHER' && r.title) {
      console.log(`  record "${r.title}" -> category guessed as OTHER, double check this one`);
    }
    pendingWrites.push({ ref: newDb.collection('users').doc(newUid).collection('records').doc(id), data: doc });
  });

  Object.entries(oldTasks).forEach(([key, t]) => {
    const id = t.taskId || key;
    const { intervalMiles, intervalMonths, approximated } = parseFrequency(t.taskFrequency);
    const doc = {
      id,
      vehicleId: String(t.taskVehicle),
      name: t.taskName || 'Maintenance task',
      type: (t.taskType || 'single').toUpperCase() === 'RECURRING' ? 'RECURRING' : 'SINGLE',
      notes: t.taskNotes || null,
      intervalMiles,
      intervalMonths,
      lastDoneDate: parseYmd(t.taskLastDone),
      lastDoneOdometer: null,
      dueDate: parseYmd(t.taskDueDate),
      dueOdometer: parseNumeric(t.taskDueMileage),
      completed: !!t.taskCompleted,
      createdAt: t.entryTime || Date.now(),
      isSynced: true,
      isDeleted: false
    };
    if (approximated) {
      console.log(`  task "${t.taskName}" -> frequency "${t.taskFrequency}" approximated to ${intervalMonths} months`);
    }
    pendingWrites.push({ ref: newDb.collection('users').doc(newUid).collection('tasks').doc(id), data: doc });
  });

  console.log(`  ${pendingWrites.length} documents ${DRY_RUN ? 'would be written (dry run)' : 'to write'}`);

  if (!DRY_RUN) {
    // Firestore caps a single batch at 500 writes - chunk defensively even
    // though a family/friends dataset is very unlikely to get anywhere close.
    const CHUNK_SIZE = 400;
    for (let i = 0; i < pendingWrites.length; i += CHUNK_SIZE) {
      const batch = newDb.batch();
      pendingWrites.slice(i, i + CHUNK_SIZE).forEach(({ ref, data }) => batch.set(ref, data));
      await batch.commit();
    }
  }
}

async function main() {
  if (DRY_RUN) console.log('--- DRY RUN: nothing will be written ---');
  for (const entry of userMap) {
    await migrateUser(entry);
  }
  console.log('\nDone.');
  process.exit(0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
