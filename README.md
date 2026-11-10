# Tuition Apps (Android)

ටියුෂන් කළමනාකරණ පද්ධතියේ Android ඇප් දෙකක් (එකම කේතයෙන්):

| ඇප් | Package | විවෘත කරන පිටුව |
|---|---|---|
| **Tuition Staff** (හිමිකරු / ගුරුවරයා / ඔපරේටර්) | `lk.tuition.staff` | `index.php` |
| **Student Portal** (සිසු / දෙමාපිය) | `lk.tuition.portal` | `portal.html` |

ඇප් දෙකම ඔබේ වෙබ් සේවාදායකයේ පද්ධතිය WebView එකක් තුළ පෙන්වයි (QR ස්කෑන් සඳහා කැමරාව, ෆොටෝ අප්ලෝඩ්, CSV බාගත කිරීම, WhatsApp විවෘත කිරීම, A4 ප්‍රින්ට් – සියල්ල ක්‍රියා කරයි). වෙබ් පද්ධතිය යාවත්කාලීන කළ විට ඇප් එක නැවත build කිරීම අවශ්‍ය නැත.

## GitHub හරහා APK හදාගන්නා ආකාරය
1. GitHub හි අලුත් repository එකක් (private නිර්දේශ) සාදා මෙම ෆෝල්ඩරයේ සියල්ල upload / push කරන්න.
2. **Settings → Secrets and variables → Actions → Variables** හි `BASE_URL` සාදා ඔබේ ලිපිනය දෙන්න  
   (උදා: `https://senadi.online/` – **HTTPS** විය යුතුයි, කැමරාව සඳහා අවශ්‍යයි; `public/` ෆෝල්ඩරයේ `index.php` සහ `portal.html` ඇති ලිපිනය).  
   නැතිනම් `gradle.properties` හි `BASE_URL` වෙනස් කරන්න.
3. **Actions → Build Android APKs → Run workflow** (ලිපිනය එතැනම ලබා දිය හැක).
4. විනාඩි 5–10කින් ඉවර වූ විට run පිටුවේ **Artifacts → tuition-apks** බාගන්න: `tuition-staff.apk`, `student-portal.apk`.
5. දුරකථනයට යවා install කරන්න ("Install unknown apps" ඉඩ දෙන්න).

`v1.0.0` වැනි tag එකක් push කළොත් APK ගොනු Release එකටද ස්වයංක්‍රීයව එක් වේ.

## ස්ථිර (release) signing යතුරක් (Play Store / යාවත්කාලීන සඳහා නිර්දේශ)
නැතිනම් APK debug යතුරෙන් සහතික වේ (install කළ හැක, නමුත් Play Store සඳහා නොගැලපේ).
```
keytool -genkey -v -keystore release.jks -alias tuition -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks      # → KEYSTORE_BASE64
```
GitHub **Secrets** වලට: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. keystore ගොනුව repo එකට දමන්න එපා, සහ ආරක්ෂිතව තබාගන්න (නැති වුවහොත් ඇප් යාවත්කාලීන කළ නොහැක).

## සටහන්
- ඇප් නම/අයිකනය වෙනස් කිරීමට: `app/build.gradle` (`app_name`) සහ `app/src/main/res/mipmap-*/ic_launcher.png`.
- `http://` (HTTPS නැති) ලිපිනයක් දුන්නොත් ඇප් cleartext සඳහා ස්වයංක්‍රීයව ඉඩ දෙයි, නමුත් එවිට කැමරා QR ස්කෑන් ක්‍රියා නොකරයි.
- ප්‍රින්ට් බොත්තම් Android හි print සේවාව විවෘත කරයි (PDF ලෙස save කිරීමට හෝ ප්‍රින්ටරයකට).
