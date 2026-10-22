# Compatibility

Beta means one thing here: the app has been exercised on a single TV. Everything else below is **Untested** — not "compatible", not "incompatible". Untested hardware may work fine, or fail at checks the TV itself enforces (signature, Tizen version, free space).

## Legend

- **Tested** — tried by the author on real hardware, with notes on what happened.
- **Untested** — no report yet. Trying it and filing an issue is the fastest way to move a row.

## Tested devices

| Brand | Model | Year | Tizen | Status | Notes |
|---|---|---|---|---|---|
| Samsung | UE50TU7170UXZT | 2020 | 5.5 | Tested | Author's TV. Connection, validation, upload and installer replies verified. Large packages need free TV storage (error 116); unsigned packages are auto-signed by the app. |

## Untested

Any Samsung Tizen TV not listed above — including other sizes in the TU/2020 lineup and all other model years — is **Untested**. Non-Tizen platforms (LG webOS, Android TV / Fire TV, pre-2015 Samsung Orsay) are not supported by design: the app blocks installation there and explains why.

## Reporting a result

Open an issue with the [bug report template](.github/ISSUE_TEMPLATE/bug_report.yml) (for failures) or a [feature request](.github/ISSUE_TEMPLATE/feature_request.yml). Include the TV model, the Tizen version shown on the app's status card, and what the TV answered. Scrub logs first — see [SECURITY.md](SECURITY.md).
