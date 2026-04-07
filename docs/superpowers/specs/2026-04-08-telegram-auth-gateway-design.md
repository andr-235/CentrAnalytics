# Telegram Auth Gateway Design

## Goal

Вынести Telegram user authorization из Spring Boot backend в отдельный сервис, который использует стабильный MTProto-клиент и отдает backend простой HTTP API для шагов `start`, `confirm`, `current`, `reset`.

## Problem

Текущая авторизация Telegram user session в `CentrAnalytics` реализована через `TDLib`/`tdlight-java` внутри backend. На production это приводит к эксплуатационной сложности:

- требуется отдельная настройка proxy и сетевого доступа до Telegram DC;
- состояние `WAIT_CODE` не гарантирует, что код реально дошел до пользователя;
- процесс авторизации тесно связан с runtime backend и Docker networking;
- отладка сетевого маршрута и TDLib-состояний занимает непропорционально много времени.

В соседнем проекте `parseVK` уже используется отдельный Telegram auth flow на `GramJS`, который работает напрямую по MTProto и лучше подходит для шага user authorization.

## Recommendation

Сделать отдельный сервис `telegram-auth-gateway` на `Node.js + TypeScript + GramJS`.

Spring Boot backend не должен больше напрямую заниматься MTProto-авторизацией. Он будет обращаться к gateway по HTTP и хранить только бизнес-значимое состояние, если это потребуется. Вся Telegram-специфика шага login/code/password/session должна жить в gateway.

## Alternatives

### 1. Продолжать чинить TDLib в Java

Плюсы:
- не нужен новый сервис;
- сохраняется единый backend.

Минусы:
- auth flow остается связан с TDLib runtime;
- тяжело диагностировать сетевые и proxy-сценарии;
- высокая операционная сложность для задачи, которая по сути ограничивается `sendCode/signIn`.

### 2. Отдельный сервис на GramJS

Плюсы:
- использует уже знакомый и рабочий паттерн из `parseVK`;
- auth flow проще: `sendCode`, `signInUser`, `signInWithPassword`, `StringSession`;
- Telegram auth изолирован от основного backend;
- проще тестировать и деплоить отдельно.

Минусы:
- появляется еще один сервис в инфраструктуре;
- нужна интеграция backend -> gateway.

### 3. Писать MTProto-клиент вручную

Отклонено.

Это инженерно нецелесообразно: Telegram user auth не предоставляет простой REST API, а ручная реализация MTProto создаст больше рисков, чем принесет пользы.

## Architecture

### New Service

Новый сервис `telegram-auth-gateway` живет в корне репозитория как отдельное приложение рядом с `frontend/` и Java backend.

Предлагаемая структура:

```text
/telegram-auth-gateway
  package.json
  tsconfig.json
  src/
    app.ts
    config/
    telegram/
      telegram-auth.service.ts
      telegram-session.repository.ts
      telegram-auth.controller.ts
      telegram-auth.types.ts
```

### Responsibilities

`telegram-auth-gateway` отвечает только за:

- запуск новой auth transaction по номеру телефона;
- подтверждение кода;
- подтверждение 2FA-пароля;
- хранение текущей session string;
- сброс текущей session;
- возврат понятного статуса для backend.

Сервис не должен заниматься ingestion сообщений, sync чатов или UI.

### Backend Role

Spring Boot backend получает новый интеграционный client к gateway и проксирует frontend-операции:

- `POST /api/admin/integrations/telegram-user/start`
- `POST /api/admin/integrations/telegram-user/{id}/code`
- `POST /api/admin/integrations/telegram-user/{id}/password`
- `GET /api/admin/integrations/telegram-user/current`
- `DELETE /api/admin/integrations/telegram-user/current` или эквивалентный reset endpoint

На первом этапе backend может полностью делегировать auth lifecycle gateway-у и не использовать текущий `TDLib auth` path.

## HTTP Contract

### `POST /session/start`

Request:

```json
{
  "phoneNumber": "+7924..."
}
```

Response:

```json
{
  "transactionId": "uuid",
  "nextType": "app",
  "codeLength": 5,
  "timeoutSec": null
}
```

### `POST /session/confirm`

Request:

```json
{
  "transactionId": "uuid",
  "code": "12345",
  "password": "optional"
}
```

Response:

```json
{
  "session": "string-session",
  "userId": 123456789,
  "username": "optional",
  "phoneNumber": "+7924..."
}
```

### `GET /session/current`

Response:

```json
{
  "session": "string-session",
  "userId": 123456789,
  "username": "optional",
  "phoneNumber": "+7924..."
}
```

Или `null`, если session отсутствует.

### `DELETE /session/current`

Полностью удаляет текущую session и активные auth transactions.

## Persistence

На первом этапе хранение делаем минимальным и надежным:

- `session`: текущая успешная `StringSession`;
- `auth transaction`: `transactionId`, `phoneNumber`, `phoneCodeHash`, временная session, `apiId`, `apiHash`, `createdAt`.

Для MVP достаточно file-based JSON storage или SQLite/Postgres. Так как основной проект уже использует Postgres и Docker Compose, предпочтительнее сохранить состояние в том же Postgres через простую таблицу или отдельную SQLite/file storage в volume.

Рекомендация для старта: file-based storage в volume gateway сервиса.

Причина:
- проще поднять быстро;
- не требует миграций в Java backend;
- легко заменить позже.

## Error Handling

Gateway должен нормализовать Telegram auth ошибки в понятные коды:

- `PHONE_NUMBER_REQUIRED`
- `API_ID_AND_HASH_REQUIRED`
- `TRANSACTION_NOT_FOUND_OR_EXPIRED`
- `PASSWORD_REQUIRED`
- `TELEGRAM_SEND_CODE_FAILED`
- `TELEGRAM_INVALID_CODE`
- `TELEGRAM_PASSWORD_INVALID`
- `TELEGRAM_SESSION_RESET_FAILED`

Backend не должен пробрасывать frontend-у сырые stack traces от GramJS.

## Security

Gateway не должен быть публично открыт наружу.

Рекомендация:
- запускать его только во внутренней Docker-сети;
- backend обращается к нему по service name;
- frontend с gateway напрямую не общается.

Секреты:
- `TELEGRAM_API_ID`
- `TELEGRAM_API_HASH`

Они должны храниться в env сервиса и не попадать в репозиторий.

## Deployment

В production compose добавляется новый сервис:

- `telegram-auth-gateway`

Связи:
- `app` -> `telegram-auth-gateway` по внутреннему hostname;
- внешний nginx/frontend его не публикует напрямую.

CI/CD:
- отдельный Dockerfile;
- selective build/deploy только при изменениях в `telegram-auth-gateway/**` и связанных backend proxy-client файлах.

## Testing Strategy

### Gateway

- unit tests на `telegram-auth.service`;
- tests на repository/storage layer;
- HTTP tests на `start/current/reset`;
- моки GramJS для auth flow.

### Backend

- tests на HTTP client к gateway;
- tests на controller/service mapping для integration UI.

### Manual Verification

1. Старт новой session.
2. Проверка, что backend получает `transactionId`.
3. Подтверждение кода.
4. Подтверждение 2FA, если требуется.
5. Чтение `current session`.
6. Сброс session.

## Migration Plan

1. Создать новый `telegram-auth-gateway`.
2. Реализовать в нем auth flow на GramJS.
3. Подключить Java backend к gateway через HTTP client.
4. Переключить frontend integration page на backend endpoints, которые уже используют gateway.
5. Оставить текущий `TDLib` код временно, но перестать использовать его для auth.
6. После стабилизации решить, нужен ли `TDLib` вообще для дальнейшего ingestion, либо Telegram integration тоже стоит увести в отдельный сервис.
