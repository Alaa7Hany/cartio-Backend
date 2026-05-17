# Project Context
Project Name: Cartio Backend
Description: A highly scalable, production-grade e-commerce REST API built from scratch.
Target Client: Android Mobile App (Cartio)

# Core Tech Stack
- Language: Kotlin (Server-side)
- Framework: Ktor 3.x
- Asynchrony: Kotlin Coroutines (Non-blocking I/O)
- Dependency Injection: Koin
- JSON Serialization: Kotlinx.serialization (Content Negotiation)
- Persistence: JetBrains Exposed ORM with PostgreSQL (Supabase)
- Authentication: Ktor Auth Plugin with JWT (JSON Web Tokens)

# Architectural Patterns
- Follow the Controller-Service-Repository pattern explicitly.
- Routing: Keep route definitions clean; delegate business logic to Services and data persistence to Repositories.
- Data Security: Never expose raw Database Entities to the client. Use Data Transfer Objects (DTOs) for incoming requests and outgoing responses.
- Database Access: Strictly use the Exposed DSL (`Table` objects), NOT the Exposed DAO (Entity classes).
- State Management: Handle multi-step business logic safely using transactional database blocks.

# Coding Standards & Quality
- Security: Never hardcode secrets. Always use `System.getenv()` for database credentials, JWT secrets, and API keys.
- Error Handling: Use Ktor's `StatusPages` plugin to catch custom exceptions globally and map them to clean HTTP Status Codes.
- Response Formatting: Wrap all successful API responses in a unified generic structure: `data class BaseResponse<T>(val data: T?, val message: String?, val success: Boolean)`.
- Typing: Utilize Kotlin's `Result` type or custom sealed classes for operational failures instead of relying on generic Exception throwing where possible.

# AI Behavior Rules
- Avoid introductory boilerplate or conversational pleasantries (e.g., "Sure, here is the code...", "Let me help you with that").
- Deliver clean, production-ready, fully typed Kotlin code immediately.
- If a required dependency is missing from the build configuration, explicitly state the dependency string at the top of the response.