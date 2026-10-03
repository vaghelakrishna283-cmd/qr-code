# UPI QR Payment - Android Project

This project generates a standard UPI payment QR code.

## Features
- Enter amount
- Save UPI ID locally
- Enter payee name
- Generate UPI QR with amount pre-filled
- Share QR image
- Works offline for QR generation
- Compatible with UPI apps that support standard `upi://pay` links

## Build APK online with GitHub Actions

1. Create a new GitHub repository.
2. Upload **all files and folders from this project** to the repository root.
3. Make sure `.github/workflows/build-apk.yml` is present.
4. Push the files to the `main` branch, or open **Actions → Build Android APK → Run workflow**.
5. Wait for the workflow to finish.
6. Open the completed workflow run.
7. Under **Artifacts**, download `upi-qr-payment-debug-apk`.
8. Extract the artifact to get `app-debug.apk`.

The workflow builds a debug APK and uploads it as a GitHub Actions artifact. It does **not** create a GitHub Release.

## Payment verification

This app creates a UPI payment QR. It does not independently verify whether money was received. Payment verification requires an appropriate merchant/payment-gateway integration.

## Package

`com.krishna.upiqr`
