### Distribuerad chatt – Microservices med BFF, JWT och MQ

Detta är en enkel, körbar referensimplementation av en distribuerad chattlösning uppdelad i flera microservices. Fokus ligger på tjänsteisolering, en BFF som exponerar REST för klienter, JWT‑baserad autentisering samt publicering av händelser ("message-published") till en Message Queue (RabbitMQ).

#### Arkitekturöversikt
- BFF (Backend‑for‑Frontend)
  - Exponerar ett REST‑API till klienter under `/api/**`
  - Validerar inkommande JWT
  - Proxyar anrop vidare till interna tjänster
- Auth Service
  - Inloggning och utfärdande av JWT (HS256, demo‑hemlighet)
- User Service
  - CRUD för användare (in‑memory i denna demo)
- Message Service
  - Tar emot och lagrar meddelanden (in‑memory i denna demo)
  - Publicerar eventet `message-published` till RabbitMQ Topic Exchange
- RabbitMQ (lokal broker för demo)

Porter (standard):
- BFF: 8080
- Auth Service: 8081
- User Service: 8082
- Message Service: 8083
- RabbitMQ: 5672 (AMQP), ev. 15672 (Management UI om aktiverat)

#### Tekniker
- Java 21, Maven, Spring Boot 3
- Spring Web, Spring Security (BFF), JJWT
- Spring AMQP (RabbitMQ)

#### Projektstruktur (moduler)
```
Microservice/
  ├─ pom.xml (parent)
  ├─ bff/
  ├─ auth-service/
  ├─ user-service/
  └─ message-service/
```

---

### Kom igång

#### Förutsättningar
- Java 21 (JDK)
- Maven 3.9+
- RabbitMQ lokalt (rekommenderas för att se riktig event‑publicering)
  - Standardinlogg: `guest/guest`
  - Standardport: `5672`

Obs: Om RabbitMQ inte är igång kan Message Service fortfarande köras; publiceringen fångar undantag och blockerar inte svaren.

#### Bygg
```
mvn clean package
```

#### Kör tjänsterna (i separata terminaler)
```
java -jar auth-service/target/auth-service-1.0-SNAPSHOT.jar
java -jar user-service/target/user-service-1.0-SNAPSHOT.jar
java -jar message-service/target/message-service-1.0-SNAPSHOT.jar
java -jar bff/target/bff-1.0-SNAPSHOT.jar
```

#### Snabb hälsokoll
- BFF: http://localhost:8080/health → "OK"
- Auth: http://localhost:8081/health (ingen separat health i denna tjänst; det är OK)
- User: http://localhost:8082/health → "OK"
- Message: http://localhost:8083/health → "OK"

---

### Demo‑klient (webb)

Öppna BFF‑klienten:
- http://localhost:8080

Flöde i webbsidan:
1) Logga in med valfritt användarnamn (lösenord ignoreras i demon) → BFF kallar `Auth Service` och får JWT.
2) Skicka meddelande → BFF kallar `Message Service` (med JWT). Meddelandet lagras och event publiceras till RabbitMQ.
3) Hämta meddelanden → Lista befintliga meddelanden via BFF.

---

### BFF‑API (för klienter)

Bas‑URL: `http://localhost:8080/api`

- POST `/login` (öppen)
  - Body:
    ```json
    { "username": "alice", "password": "valfritt" }
    ```
  - Svar:
    ```json
    { "token": "<JWT>" }
    ```

- POST `/users` (kräver `Authorization: Bearer <JWT>`) – skapa användare
  - Body ex:
    ```json
    { "username": "alice", "displayName": "Alice" }
    ```

- GET `/users` (kräver JWT) – lista användare

- GET `/users/{id}` (kräver JWT) – hämta en användare

- PUT `/users/{id}` (kräver JWT) – uppdatera användare

- DELETE `/users/{id}` (kräver JWT) – ta bort användare

- POST `/messages` (kräver JWT) – publicera/lagra meddelande
  - Body ex:
    ```json
    { "senderId": "alice", "text": "Hej världen" }
    ```

- GET `/messages` (kräver JWT) – lista meddelanden

- GET `/messages/{id}` (kräver JWT) – hämta meddelande

Exempel med curl:
```
# 1) Logga in och spara token i var
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"x"}' | jq -r .token)

# 2) Skapa användare
curl -s -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"alice","displayName":"Alice"}' | jq .

# 3) Publicera meddelande
curl -s -X POST http://localhost:8080/api/messages \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"senderId":"alice","text":"Hej!"}' | jq .

# 4) Lista meddelanden
curl -s http://localhost:8080/api/messages -H "Authorization: Bearer $TOKEN" | jq .
```

---

### Interna tjänster (direkt, ej via BFF)

- Auth Service
  - POST `http://localhost:8081/login` → returnerar JWT (HS256) med claim `uid`

- User Service
  - CRUD under `http://localhost:8082/users`

- Message Service
  - `http://localhost:8083/messages` (POST/GET/GET by id)
  - Publicerar till exchange (default): `chat.exchange`, routing‑key: `message.published`

---

### Konfiguration

Relevanta properties (defaultvärden finns i respektive `application.properties`):

- BFF (`bff/src/main/resources/application.properties`)
  ```properties
  server.port=8080
  services.auth.base-url=http://localhost:8081
  services.user.base-url=http://localhost:8082
  services.message.base-url=http://localhost:8083
  ```

- Message Service (`message-service/src/main/resources/application.properties`)
  ```properties
  server.port=8083
  spring.rabbitmq.host=localhost
  spring.rabbitmq.port=5672
  spring.rabbitmq.username=guest
  spring.rabbitmq.password=guest

  app.mq.exchange=chat.exchange
  app.mq.routing-key=message.published
  app.mq.queue=chat.bot
  ```

- Auth Service
  - Använder en inbyggd demo‑hemlighet (base64) för att signera JWT. För produktion bör detta flyttas till miljö‑/hemlighantering och services uppdateras att läsa konfig.

---

### Felsökning
- BFF svarar 401 Unauthorized på skyddade endpoints
  - Säkerställ att `Authorization: Bearer <JWT>` skickas och att JWT kommer från denna demo (samma hemlighet som i Auth Service används i BFF för validering).
- Message Service loggar varning om MQ
  - Starta RabbitMQ lokalt eller justera anslutningsinställningar. Om brokern saknas loggas en varning men anropet lyckas ändå.
- Portkrockar
  - Ändra `server.port` i respektive tjänsts properties.

---

### Nästa steg (för VG)
- Dockerisera varje modul (Dockerfile), ev. docker‑compose.
- Kubernetes‑manifester (Deployment, Service, ConfigMap/Secret) och driftsättning i t.ex. Minikube.
- Intern gRPC‑kommunikation (t.ex. mellan Message och User) och/eller konsument (Bot Service) av `message-published`.
- Persistens med riktiga databaser (UserDB, MessageDB).

---

### Licens
Projektet är en utbildnings/demo‑lösning. Använd och modifiera fritt inom ramen för kurs/laboration.
