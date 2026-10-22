# Security

## Reporting

Found a vulnerability? Don't open a public issue — reach out through the contact page on [layiix.com](https://layiix.com) with "Tizen Bridge security" in the subject and a short description. No account or personal data needed beyond what it takes to reply to you.

## What's in the repo (and what isn't)

- **Not in the repo, by design:** the release signing key (`keystore/` + `keystore.properties`). Releases are signed locally; without those files nobody can ship updates under the same signature. Backups live offline with the author.
- **In the repo on purpose:** two shared *community* signing certificates (`app/src/main/assets/certs/`) with their passwords in `CertStore.kt`. They exist so the app can sign packages out of the box, and anyone can extract them from the APK — treat them as public. They only work where the TV accepts community signatures; packages tied to a specific TV still need their own certificate.
- **User data:** certificates imported in Settings → Signing, logs, and preferences stay in the app's private storage on the phone. Nothing is uploaded anywhere.

## Filing issues safely

Before pasting logs into a bug report: delete public IPs, Wi-Fi details beyond the subnet you already share, Samsung account data, certificate passwords, DUIDs, and anything that identifies you. The bug template reminds you; the reminder is here in case you file from elsewhere.
