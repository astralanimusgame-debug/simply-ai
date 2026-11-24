# Настоящий вход через Google — настройка (один раз, ~10 минут)

Код входа уже в приложении (Credential Manager, «Sign in with Google»). Не хватает только регистрации приложения в Google Cloud под вашим аккаунтом.

## 1. Проект
1. Откройте https://console.cloud.google.com и создайте проект, например «Simply AI».

## 2. Экран согласия (Google Auth Platform)
1. Меню → **APIs & Services → OAuth consent screen** (может называться **Google Auth Platform**) → **Get started**.
2. App name: `Simply AI`, User support email: ваша почта.
3. Audience: **External**. Contact email: ваша почта. Создайте.
4. В разделе **Audience** нажмите **Publish app**. Пока приложение в режиме «Testing», войти смогут только добавленные тестовые пользователи. Для базовых данных (имя, email) проверка Google не нужна.

## 3. Клиент для Android
**Clients → Create client**:
- Application type: **Android**
- Package name: `com.simplyai.app`
- SHA-1: `23:EC:43:2A:95:A6:E5:A2:6F:A8:46:9A:4F:C1:17:8E:B2:7B:23:14` (ключ демо-сборок)

## 4. Клиент Web (его ID нужен приложению)
**Clients → Create client**:
- Application type: **Web application**, имя любое
- После создания скопируйте **Client ID** — строка вида `1234567890-abc...apps.googleusercontent.com`

## 5. Вписать ID
Вставьте Client ID в `gradle.properties` в строку `GOOGLE_WEB_CLIENT_ID=` и сохраните — GitHub соберёт новый APK.

## Перед публикацией в Google Play
- В Play Console включится **Play App Signing**: Google переподписывает приложение своим ключом. Возьмите его SHA-1 (Play Console → Test and release → App integrity) и добавьте ещё один **Android**-клиент с этим SHA-1.
- Для публикации нужен свой секретный ключ загрузки (upload key), который не хранится в репозитории, и страница с политикой конфиденциальности: приложение получает имя и email.
- Название пакета `com.simplyai.app` после публикации поменять нельзя.
