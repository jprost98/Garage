# migrate-legacy-data

One-off Node script to copy data from the old app's Realtime Database
into this app's Firestore schema. Not part of the Android app - run it
once from your machine, then forget it exists.

## Steps

1. `npm install`
2. Get a service account key for your **old** Firebase project
   (Project settings → Service accounts → Generate new private key),
   save as `old-service-account.json` in this folder.
3. Do the same for the **new** project, save as `new-service-account.json`.
4. Copy `user-map.example.json` to `user-map.json` and fill in each
   person's `email`, `oldUid`, and `newUid`. Find these in each project's
   Authentication tab in the Firebase console - match rows by email.
   Everyone needs to have already created their account in the new
   project before you do this step.
5. `npm run migrate:dry-run` - prints what it would do, writes nothing.
   Read through the output, especially the category guesses and any
   "approximated" frequency conversions, and fix up the old data by hand
   first if something looks wrong.
6. `npm run migrate` - actually writes to the new Firestore project.

## What gets migrated vs. left blank

See the comment block at the top of `migrate.js` - short version: dates,
mileage, frequencies, and vehicle odometer (derived from your highest
logged record) all carry over. Cost, receipt photos, and per-task
"last done odometer" didn't exist in the old app, so they come across as
empty and you can fill them in over time as you log new records.

## Safety

- Never commit `old-service-account.json`, `new-service-account.json`,
  or `user-map.json` - they're gitignored, keep it that way.
- Run `--dry-run` first, always.
- This only writes to the new project; your old data isn't touched or
  deleted.
