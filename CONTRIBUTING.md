# Contributing to Real-Time Chat Application

Thank you for your interest in contributing to the **Real-Time Chat Application**! We welcome bug reports, feature requests, documentation improvements, and pull requests.

---

## 🛠️ Development Setup

### Prerequisites
- **Java SE Development Kit (JDK):** Version 17 or higher
- **Apache Maven:** Version 3.8+
- **MySQL Server:** Version 8.0+
- **Git**

### Steps
1. Fork and clone the repository:
   ```bash
   git clone https://github.com/Dhanushree-1977/Basic-Chat-Application-Mini-project-2-.git
   cd Basic-Chat-Application-Mini-project-2-
   ```
2. Set up local MySQL database:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
3. Copy the database credentials template:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
   Edit `src/main/resources/db.properties` with your local database credentials.
4. Build and test:
   ```bash
   mvn clean test
   ```
5. Package the executable Fat JAR:
   ```bash
   mvn clean package -DskipTests
   ```

---

## 🌿 Branching & Git Workflow

1. Create a descriptive feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/your-bugfix-name
   ```
2. Follow Conventional Commits:
   - `feat:` New features
   - `fix:` Bug fixes
   - `docs:` Documentation updates
   - `test:` Unit/integration tests
   - `refactor:` Code refactoring without behavior changes
3. Ensure all tests pass before committing:
   ```bash
   mvn test
   ```
4. Push your branch and open a Pull Request against `main`.

---

## 📝 Code Conventions

- **Java Style:** Follow standard Java naming and formatting conventions.
- **Security:**
  - Never commit raw passwords or database secrets.
  - Always use `PreparedStatement` for SQL queries to maintain immunity against SQL injection.
  - Use `BCrypt.hashpw()` with salt factor 12 for password handling.
- **Concurrency:** Ensure shared data structures in `ClientManager` and services remain thread-safe (`ConcurrentHashMap`, atomic primitives, synchronized blocks where appropriate).
