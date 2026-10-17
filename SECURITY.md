# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

---

## 🔒 Security Architecture Highlights

The **Real-Time Chat Application** is engineered with defense-in-depth principles:

1. **Transport Layer Security (TLSv1.3 & TLSv1.2):**
   - All network traffic over TCP port 5000 is strictly encrypted using `SSLServerSocket` and `SSLSocket`.
   - Cleartext socket fallback is disabled by design.
   - Self-signed PKCS#12 Keystore (`server.keystore`) and Client Truststore (`client.truststore`) secure the channel against Man-In-The-Middle (MITM) attacks.

2. **Zero-Trust Sender Identity:**
   - Client payloads cannot spoof sender identity. The server forcefully overrides the `sender` property with the server-authenticated session username before routing or persistence.

3. **Cryptographic Password Hashing:**
   - Passwords are never stored in plaintext. They are protected using **BCrypt** with 12 salt rounds (`BCrypt.gensalt(12)`).

4. **SQL Injection Immunity:**
   - All database interactions in `UserRepository` and `MessageRepository` strictly enforce parameterized `PreparedStatement`. No string concatenation is used for dynamic queries.

5. **Access-Controlled Message History:**
   - Chat history retrieval enforces participant authorization checks, ensuring users cannot query or intercept private conversations between third parties.

---

## 🛡️ Reporting a Vulnerability

If you discover a security vulnerability within this project, please report it responsibly:

1. **Do not create a public GitHub issue.**
2. Send an email to the repository maintainer (`dhanushreevm63@gmail.com`) with the subject line `[SECURITY] Potential Vulnerability in Real-Time Chat App`.
3. Include detailed steps to reproduce the issue and an assessment of its impact.
4. We will acknowledge receipt within 48 hours and work with you on a resolution.
