# Settings live in the database, not DataStore

The global rate, reminder time, repeat interval and last-backup time are a single row in the same Room database. Keeping them with the rest of the data means a backup or restore is one atomic transaction, and the app has one persistence mechanism instead of two. DataStore is deliberately not a dependency.
