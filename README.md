# ElectroStore

Android inventory and sales app for a small electronics shop, built with Java and SQLite.

- Keep track of products, stock quantities, pricing, and sales.
- Record repairs and other services, including customer jobs and payments.
- Check daily and monthly revenue, costs, and product profit.
- View product sales and service payments separately in the profit report (v1.9.25).
- Create receipts and print using a paired Bluetooth receipt printer.
- Manage orders, low-stock alerts, and local backups.
- Switch between English and Arabic.
- Use the app offline. There is no Firebase dependency, account system, or Internet permission.

## Run locally

- Open this folder in Android Studio with JDK 17 and Android SDK 34 installed.
- Let Gradle sync, then build and run the **debug** app on an emulator or Android device.
- The app creates a new, empty SQLite database on first launch. No shop inventory, customer records, sales, or device backups are included in this repo.

## Release builds

- The original private release-signing key is intentionally not included.
- If you want to sign a release, provide `ELECTROSTORE_KEYSTORE_PATH`, `ELECTROSTORE_KEYSTORE_PASSWORD`, `ELECTROSTORE_KEY_ALIAS`, and `ELECTROSTORE_KEY_PASSWORD` as environment variables before running Gradle or `tools/build.sh`.
- An APK signed with a different key **cannot update** an existing installation signed by the original key. Keep your original signing key private and backed up securely.
- Never commit keystores, passwords, database exports, or customer information.
