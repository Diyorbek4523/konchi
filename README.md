# Konchi — Minecraft 1.21.11 (Fabric) uchun mod

Funksiyalar (menyu: **o'ng Shift**):
- **Fullbright** — qorong'ida ham hammasi yorug' ko'rinadi.
- **Rudalar** — atrofdagi rudalar devor orqali rangli qutida ko'rinadi (olmos — havorang, zumrad — yashil, oltin — sariq, temir — och jigarrang, qizil tosh — qizil, lazurit — ko'k, mis — to'q sariq, ko'mir — kulrang, qadimiy qoldiq — jigarrang).
- **Sandiqlar** — sandiq, bochka, shulker qutisi (to'q sariq) va ender sandig'i (binafsha).
- **O'yinchilar** — boshqa o'yinchilar devor orqali ko'rinadi.
- **Ruda radiusi** — 16 / 24 / 32 blok.

Sozlamalar `config/konchi.properties` fayliga saqlanadi.

## .jar faylni yig'ish

### 1-usul: kompyuterda
1. **JDK 21** o'rnating (masalan, adoptium.net saytidan Temurin 21).
2. Shu papkani oching va terminalda ishga tushiring:
   - Windows: `gradlew.bat build`
   - Linux/Mac: `./gradlew build`
3. Birinchi marta 5–15 daqiqa ketadi (Minecraft va kutubxonalar yuklanadi).
4. Tayyor fayl: `build/libs/konchi-1.0.0.jar` (`-sources` bilan tugaydiganini emas!).

IntelliJ IDEA ishlatsangiz: papkani oching, Gradle o'zi sozlanadi, keyin o'ngdagi Gradle paneldan `build` ni bosing.

### 2-usul: GitHub orqali (kompyuterga hech narsa o'rnatmasdan)
1. GitHub'da yangi repozitoriy yarating va shu papkadagi barcha fayllarni yuklang (`.github` papkasi bilan birga).
2. **Actions** bo'limida build avtomatik ishga tushadi.
3. Tugagach, o'sha build sahifasidan **Artifacts** ni yuklab oling — ichida `.jar` bo'ladi.

## O'rnatish
`konchi-1.0.0.jar` va **Fabric API** (1.21.11) ni `.minecraft/mods` papkasiga tashlang.
