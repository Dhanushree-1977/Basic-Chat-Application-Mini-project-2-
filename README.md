# Real-Time Chat Application Using Java Socket Programming

A high-performance, secure, desktop-based real-time messaging application engineered with **Java TCP Sockets**, **TLSv1.3 encryption**, **Java Swing GUI**, **MySQL persistence**, and **BCrypt authentication**.

---

## 🛠️ Technology Stack
- **Language:** Java 17+ (SE)
- **Networking:** Java TCP Sockets (`SSLServerSocket` & `SSLSocket`)
- **Transport Security:** TLSv1.3 with PKCS#12 Keystore & TrustStore
- **Concurrency:** Java `ExecutorService` (Cached Thread Pool)
- **Serialization:** Newline-delimited JSON (Google Gson 2.10.1)
- **User Interface:** Java Swing (Desktop client with message bubbles & unread badges)
- **Database:** MySQL 8.0+ via JDBC (`PreparedStatement` for SQL injection immunity)
- **Password Hashing:** jBCrypt (Work factor 12)
- **Testing:** JUnit 5 (Jupiter 5.10.2)

---

## 📂 Project Architecture

```text
RealTimeChatApplication/
├── database/
│   └── schema.sql
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── db.properties           (Ignored by Git)
│   │   │   ├── db.properties.example   (Template)
│   │   │   ├── schema.sql
│   │   │   └── security/
│   │   │       ├── server.keystore     (Server private key & certificate)
│   │   │       ├── client.truststore   (Client trusted server certificate)
│   │   │       └── server.cer
│   │   └── java/
│   │       └── com/realtimechat/
│   │           ├── Main.java           (Unified entrypoint)
│   │           ├── server/
│   │           │   ├── ChatServer.java
│   │           │   ├── ClientHandler.java
│   │           │   └── ClientManager.java
│   │           ├── client/
│   │           │   ├── ChatClient.java
│   │           │   └── ClientNetworkService.java
│   │           ├── ui/
│   │           │   ├── LoginFrame.java
│   │           │   ├── RegisterFrame.java
│   │           │   ├── ChatFrame.java
│   │           │   ├── UserListPanel.java
│   │           │   └── MessageBubble.java
│   │           ├── model/
│   │           │   ├── Message.java
│   │           │   ├── MessageType.java
│   │           │   ├── User.java
│   │           │   └── StoredMessage.java
│   │           ├── database/
│   │           │   ├── DatabaseConnection.java
│   │           │   ├── DatabaseTest.java
│   │           │   ├── UserRepository.java
│   │           │   └── MessageRepository.java
│   │           └── service/
│   │               ├── AuthenticationService.java
│   │               ├── MessageRouter.java
│   │               ├── PresenceManager.java
│   │               ├── NotificationService.java
│   │               └── SslContextFactory.java
│   └── test/
│       └── java/
│           └── com/realtimechat/
│               ├── model/
│               │   └── MessageTest.java
│               └── service/
│                   └── AuthenticationServiceTest.java
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### 1. Database Setup
1. Open MySQL Workbench or MySQL terminal.
2. Run the `database/schema.sql` script to create the `realtime_chat` database and tables.
3. Configure `src/main/resources/db.properties` with your local MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/realtime_chat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   db.user=root
   db.password=your_password
   ```

### 2. Verify Database Connection
Run `com.realtimechat.database.DatabaseTest.main()` in IntelliJ IDEA.

### 3. Quick Launch via Batch Scripts or Terminal
You can launch components with a single click using the included root batch scripts:
- **`run-demo.bat`**: Executes the automated 7-phase end-to-end live demonstration script.
- **`run-server.bat`**: Starts the TLS-encrypted chat server on port 5000.
- **`run-client.bat`**: Launches the Java Swing Desktop Client GUI (launch multiple times for multiple clients).
- **`run-tests.bat`**: Executes the JUnit 5 test suite (11 test cases).

### 4. Running via IntelliJ IDEA / Command Line
1. **Server:** Run `com.realtimechat.Main` with argument `server` (or `com.realtimechat.server.ChatServer`).
2. **GUI Clients:** Run `com.realtimechat.Main` with argument `client` (or default without args). Enable **"Allow multiple instances"** in IntelliJ's Run Configuration to open multiple client windows side by side.
3. **Live Demonstration:** Run `com.realtimechat.Main` with argument `demo` (or `com.realtimechat.LiveDemonstration`).
4. Register accounts (e.g., `alice` and `bob`), log in, and begin chatting!

---

## 🔒 Security Highlights
- **Zero-Trust Sender Identity:** The server forcefully overrides the `sender` field on all incoming messages using server-authenticated session tokens.
- **BCrypt Password Protection:** Passwords hashed with 12 salt rounds before persisting to disk.
- **SQL Injection Prevention:** Strictly parameterized `PreparedStatement` across all queries.
- **Transport Encryption:** TLSv1.3 enforced over all TCP sockets.
- **Access Controlled History:** Users can only query message history for conversations where they are verified participants.
