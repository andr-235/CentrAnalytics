# Telegram Collector Gateway Design

## Goal

Заменить текущий Telegram message collection path на `GramJS`, используя уже работающий `telegram-auth-gateway` и сохраненную `StringSession`, чтобы после состояния `READY` сообщения реально попадали в общую витрину `messages`.

## Problem

После выноса авторизации в `telegram-auth-gateway` текущий collector остался в Java backend и все еще зависит от:

- `telegram_user_session` в Postgres;
- `TelegramTdLibClientManager`;
- `TelegramUserSessionBootstrap`;
- `TDLib` runtime и его локальных каталогов.

В результате:

- auth через `GramJS` работает;
- UI показывает `READY`;
- но ingestion не стартует, потому что Java collector не умеет использовать `StringSession` из gateway.

Это архитектурный разрыв: `GramJS auth` и `TDLib collector` работают с разными типами session/runtime.

## Recommendation

Не склеивать `GramJS` и `TDLib`.

Правильнее перенести и Telegram collection в тот же `telegram-auth-gateway`, который уже владеет `StringSession`.

## Architecture

### Responsibility Split

`telegram-auth-gateway` должен расшириться до двух ролей:

1. `auth`
   - `start`
   - `confirm`
   - `current`
   - `reset`

2. `collector`
   - создать `TelegramClient` из сохраненной `StringSession`
   - подключиться к Telegram
   - подписаться на новые сообщения
   - нормализовать их в общий ingestion payload
   - отправлять payload обратно в Spring Boot backend во внутренний endpoint ingestion

Spring Boot backend должен:

- перестать использовать `TDLib` для Telegram user collection;
- сохранить `IntegrationIngestionService` как центральную точку записи в БД;
- предоставить внутренний endpoint для trusted ingestion из gateway.

## Data Flow

### After Authorization

1. Пользователь проходит auth через gateway.
2. Gateway сохраняет `StringSession`.
3. Gateway поднимает `GramJS TelegramClient` на этой сессии.
4. Gateway начинает слушать новые Telegram сообщения.
5. Каждое сообщение маппится в payload, совместимый с текущим ingestion contract.
6. Gateway вызывает внутренний backend endpoint, например:

`POST /api/internal/integrations/telegram-user/events`

7. Backend передает payload в `IntegrationIngestionService`.

Итог: Telegram снова попадает в общую таблицу `messages`, но уже без `TDLib`.

## Why Pulling Collector Into Gateway Is Better

### 1. Session Ownership Is Consistent

`StringSession` уже живет в gateway. Значит именно gateway должен владеть long-lived Telegram client.

### 2. No Session Translation Layer

Моста между `GramJS StringSession` и `TDLib` здесь фактически нет. Попытка держать auth в одном клиенте, а collection в другом только усложняет систему.

### 3. Existing Ingestion Pipeline Stays Intact

Backend не нужно переписывать на уровне хранения сообщений. Нужно только дать gateway внутреннюю точку входа для уже нормализованных событий.

## Internal Backend Contract

### New Internal Endpoint

Предлагаемый endpoint:

`POST /api/internal/integrations/telegram-user/events`

Тело должно быть максимально близко к уже существующему `InboundIntegrationEvent`.

Gateway не должен писать напрямую в БД backend.

## Gateway Collector Lifecycle

### Startup

При старте gateway:

- читает сохраненную current session;
- если session есть, пытается поднять collector автоматически.

### Runtime

- один активный Telegram collector на текущую session;
- если session меняется или сбрасывается, старый client закрывается;
- при сетевой ошибке collector должен переподключаться.

### Reset

`DELETE /session/current`:

- удаляет session;
- останавливает collector;
- очищает transaction state.

## Collector Status

Gateway должен уметь вернуть backend и UI минимум такой статус:

- `STOPPED`
- `STARTING`
- `RUNNING`
- `FAILED`

И поля:

- `lastEventAt`
- `lastError`
- `selfUserId`
- `selfUsername`

## Mapping Strategy

Текущий mapper в Java:

- [TelegramUserInboundEventMapper.java](/home/pc051/IdeaProjects/CentrAnalytics/src/main/java/com/ca/centranalytics/integration/channel/telegram/user/service/TelegramUserInboundEventMapper.java)

Новый `GramJS` collector должен воспроизвести ту же семантику:

- `platform = TELEGRAM`
- уникальный `eventId`
- `conversation.externalConversationId`
- `author.externalUserId`
- `message.externalMessageId`
- `message.sentAt`
- `message.text`

Это позволит не ломать downstream ingestion code.

## Migration Strategy

### Phase 1

Сделать collector в gateway и новый internal ingestion endpoint в backend.

### Phase 2

Переключить Telegram UI status на агрегированный статус:

- auth session
- collector status

### Phase 3

Удалить `TDLib` auth/collector path:

- `TelegramTdLibClientManager`
- bootstrap
- TDLib-only session handling

## Risks

### 1. Update Semantics

`GramJS` update model отличается от TDLib, поэтому mapping/edit events нужно сначала ограничить до новых сообщений, а уже потом добавлять edits.

### 2. Long-lived Client Stability

Нужно аккуратно сделать reconnect и остановку клиента при reset/redeploy.

### 3. Internal Trust Boundary

Internal ingestion endpoint не должен быть публичным через frontend nginx.

## Recommendation

Следующий практический шаг:

1. добавить `collector status + runtime manager` в `telegram-auth-gateway`
2. добавить internal backend ingestion endpoint
3. отправить в него первое Telegram message событие из gateway
4. только потом удалять старый `TDLib` path
