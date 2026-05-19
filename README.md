
# Cartio Backend (Ktor E-Commerce API)

A robust, transactional e-commerce backend built with Ktor and PostgreSQL. Designed as the foundational API for mobile client applications, this system handles secure authentication, dynamic product catalogs, persistent shopping carts, and atomic checkout transactions.

## 🏗️ Architecture & Stack

This project strictly adheres to the **Controller-Service-Repository** pattern, completely decoupling the routing layer from the database layer. 

* **Framework:** [Ktor Server](https://ktor.io/)
* **Database:** PostgreSQL (via [Supabase](https://supabase.com/))
* **ORM:** [JetBrains Exposed DSL](https://github.com/JetBrains/Exposed)
* **Dependency Injection:** [Koin](https://insert-koin.io/)
* **Authentication:** JWT (JSON Web Tokens) & BCrypt Password Hashing
* **Serialization:** `kotlinx.serialization`
* **Testing:** Ktor Server Tests & [MockK](https://mockk.io/)

---

## 📦 Core Modules

### 1. Authentication & Users
Manages secure user registration and session state.
* Passwords are securely hashed using BCrypt before database insertion.
* Stateless session management via JWT.
* Includes a `/auth/me` endpoint for seamless mobile app session restoration.

### 2. Product Catalog
The discovery engine for the e-commerce platform.
* Supports paginated fetching to optimize mobile data transfer.
* Includes case-insensitive search across product titles and descriptions using PostgreSQL `ILIKE`.
* Endpoints for category filtering and batch seeding.

### 3. Shopping Cart
A highly mutable, persistent draft state for pre-purchase intent.
* State is stored via relational database tables rather than local device storage, acting as a single source of truth across devices.
* Securely tied to the JWT Principal (users cannot modify other users' carts).
* Utilizes SQL `INNER JOIN`s to fetch live product metadata alongside cart quantities in a single request.

### 4. Checkout & Orders
The financial backbone converting carts into permanent receipts.
* **Atomic Transactions:** The checkout flow (verifying the cart, creating the order, migrating items, and emptying the cart) runs within a single `newSuspendedTransaction`. If the server crashes mid-request, PostgreSQL guarantees an all-or-nothing rollback.
* **Immutability:** Product prices are permanently locked into the `priceAtCheckout` column. Future catalog price changes do not retroactively alter past receipts.

---

## 🚀 Getting Started

### Prerequisites
* JDK 17 or higher
* A PostgreSQL database instance (e.g., Supabase)

### Environment Variables
Create a `.env` file or configure your run configurations with the following required variables:
```properties
JWT_SECRET=your_super_secret_jwt_key
DB_URL=jdbc:postgresql://your_db_host:5432/your_db_name
DB_USER=your_db_user
DB_PASSWORD=your_db_password

```

### Running the Server

To build and start the server locally, run:

```bash
./gradlew run

```

The server will respond at `http://0.0.0.0:8080`.

### 📱 A Note on Mobile Testing

If you are consuming this API from a local mobile emulator, routing will differ:

* **Android Emulator:** Point your network client to `http://10.0.2.2:8080`
* **iOS Simulator / Physical Devices:** Point your network client to your computer's local local IP address (e.g., `http://192.168.1.X:8080`) and ensure both devices are on the same network.

---

## 🧪 Testing

The routing layer is heavily tested using Ktor's native testing engine. Database layers (`Facades`) are isolated and mocked using MockK to prevent test data from polluting the live Supabase instance.

Run the test suite via:

```bash
./gradlew test

```