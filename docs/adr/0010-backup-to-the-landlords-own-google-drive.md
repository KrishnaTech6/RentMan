# Backup to the landlord's own Google Drive

The app can back up to the landlord's own Google Drive. This narrows ADR 0006 and does not reverse it: there is still no server, account or sync of ours, and nobody but the landlord holds a copy. The phone stays the only place the app reads from. Drive holds a backup, the same JSON file "Back up now" shares, in a "Rent Manager" folder, replaced on every backup, so it never grows and restoring it is the same restore (one transaction, a snapshot first, Undo).

The app asks Google only for `drive.file`: the files it makes itself, nothing else in the landlord's Drive. That scope is non-sensitive, so the Google Cloud project is published without Google's review; the landlord sees "Rent Manager" not verified, and nothing else changes. The project, its consent screen and the app's two keys (iPhone, Android) are made once by the developer; a landlord only taps Connect and signs in with their own account.

Sign-in is Google's own page in Apple's secure browser sheet, with PKCE, so no secret ships in the app. The long-lived access (refresh token) lives in the iPhone Keychain, never in the database or the backup file, and Disconnect deletes it because Keychain items outlive an uninstall. Each Drive call starts from a fresh short-lived token. Access the landlord took back in their Google account is treated as "not connected" and the stored token is dropped.

Built on iPhone first. Android signs in through Play services by its package name and fingerprint and comes as its own step. Meter photos in Drive and the automatic daily backup are the next steps; an automatic backup must never upload a phone with no renters over a good copy in Drive.
