# Moliya: shaxsiy moliya boshqaruvi (Android)

Telefonda offline ishlaydigan shaxsiy moliya ilovasi. Ma'lumotlar faqat telefonning ichki xotirasida saqlanadi, internetga chiqmaydi.

## Bo'limlar

1. **Bosh sahifa**: oy balansi, 6 oylik grafik, xarajatlar tuzilmasi, yostiq va qarz holati.
2. **Daromad / Xarajat**: oylar bo'yicha yozuvlar, kategoriyalar, filtr.
3. **Himoya yostig'i**: favqulodda zaxira maqsadi (3-12 oylik xarajat), qo'shish/yechish tarixi.
4. **Qarzlar nazorati**: qarzlar, to'lovlar, foiz va muddat, umumiy qoldiq.
5. **Backup**: barcha ma'lumotni `.xlsx` (Excel) faylga yuklab olish.

## APK olish

Kod `main` branch'ga yuklanganda GitHub Actions avtomatik APK yig'adi.
APK **Releases** bo'limida (`Moliya (so'nggi versiya)`) yoki **Actions → Artifacts** da paydo bo'ladi.

Telefonda: APK'ni yuklab oling → ochganda "noma'lum manbadan o'rnatish" ruxsatini bering → o'rnating.

## Texnologiya

Kotlin, Jetpack Compose, Material 3. Tashqi kutubxonalarsiz JSON saqlash va `.xlsx` yozish.
