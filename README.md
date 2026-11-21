# Pixel Sudoku

Офлайн-судоку для Android в пиксельном стиле (Kotlin, Jetpack Compose, Room, DataStore). Минимальная версия: Android 8.0 (API 26).

## Сборка APK через GitHub

1. Создайте репозиторий на GitHub и загрузите в него содержимое этой папки (ветка `main`).
2. Создайте keystore (один раз, на своём компьютере):
   ```
   keytool -genkeypair -v -keystore release.jks -alias pixelsudoku -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 release.jks     # на Windows: certutil -encode release.jks out.txt
   ```
   Храните `release.jks` и пароли в надёжном месте. Не добавляйте их в репозиторий.
3. В репозитории: **Settings → Secrets and variables → Actions → New repository secret**. Добавьте четыре секрета:
   - `ANDROID_KEYSTORE_BASE64` — вывод `base64` из шага 2
   - `ANDROID_KEYSTORE_PASSWORD` — пароль keystore
   - `ANDROID_KEY_ALIAS` — `pixelsudoku`
   - `ANDROID_KEY_PASSWORD` — пароль ключа
4. Откройте вкладку **Actions → Build Release APK → Run workflow** (или просто сделайте push в `main`).
5. Дождитесь зелёной галочки, откройте запуск, внизу в разделе **Artifacts** скачайте `Pixel-Sudoku-APK` (zip с `Pixel-Sudoku-release.apk`).
6. Распакуйте архив, перенесите APK на телефон и установите (разрешите установку из неизвестных источников).

Если секреты не заданы, workflow честно предупредит и выдаст `Pixel-Sudoku-release-UNSIGNED.apk`: такой файл на телефон не установится.

## Релиз по тегу

`git tag v1.0.0 && git push origin v1.0.0` — workflow `release.yml` соберёт подписанный APK и прикрепит его к GitHub Release.

## Gradle Wrapper

Файл `gradle-wrapper.jar` бинарный, и в этой среде его нельзя было сгенерировать. Workflow использует `gradle/actions/setup-gradle` с Gradle 8.9. Чтобы добавить wrapper в репозиторий, выполните локально `gradle wrapper --gradle-version 8.9` и закоммитьте `gradlew`, `gradlew.bat` и папку `gradle/wrapper`.
