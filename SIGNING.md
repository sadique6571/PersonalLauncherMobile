# Permanent release signing

The initial Actions artifacts used a fresh development key on each runner. Certificates differ between builds, so those APKs cannot always update one another. The old private debug key was not retained. There is no safe way to derive it from an installed APK.

Create and back up one release key locally. Never commit the keystore, paste it into chat/issues, or upload it as a public artifact. Store only its encoded bytes and password in GitHub's encrypted repository Actions secrets.

## Windows / Android Studio

Android Studio normally includes keytool here. Run in PowerShell:

```powershell
& "$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -storetype PKCS12 -keystore "$env:USERPROFILE\Documents\PersonalLauncherMobile-release.p12" -alias personal-mobile -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=PersonalLauncherMobile"
```

Enter a strong password locally and retain it privately. If Android Studio is installed elsewhere, use its actual `jbr\bin\keytool.exe` path. Back up the keystore and password independently; losing either prevents future compatible updates.

## Repository secrets

In GitHub → PersonalLauncherMobile → Settings → Secrets and variables → Actions, add:

- `MOBILE_KEYSTORE_BASE64`: base64 of the `.p12` file. This command copies it to your Windows clipboard without printing it:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Documents\PersonalLauncherMobile-release.p12")) | Set-Clipboard
```

- `MOBILE_KEYSTORE_PASSWORD`: the password entered locally.

Do not send either value in this chat. After both secrets are saved, run the **Mobile launcher checks** workflow manually on `main`. Once tests, lint, APK builds and Android 16 routing/swipe checks pass, the release job signs and publishes `PersonalLauncherMobile.apk`. The keystore is decoded only in the temporary runner directory and removed afterward. PR builds cannot publish releases or receive these signing secrets.

## Migrating the first test installation

A release key cannot update the initial APK signed with the lost debug key. Migration requires a one-time replacement installation after backing up the app's preferences. Do not uninstall the current Home before arranging that backup and a working stock-Home fallback. Android-controlled Home, notification, accessibility and photo permissions may require granting again after reinstalling. All later APKs signed with this same release key can update normally and retain app data.

The TV project's key/package are separate and are not used here.
