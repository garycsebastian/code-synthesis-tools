# Code Synthesis Tool (Encora) - Backend

This Spring Boot application provides a robust and secure backend API for the Code Synthesis Tool, built using Spring WebFlux, Spring Data MongoDB Reactive, and Spring Security.
All generated with IA.

## Features

- **User Authentication and Authorization:**
    - Secure user registration (`/api/auth/signup`).
    - JWT-based authentication with refresh tokens (`/api/auth/login`, `/api/auth/refreshtoken`).
    - Secure logout with JWT blacklisting (`/api/auth/logout`).
- **Task Management:**
    - Create, read, update, and delete tasks (CRUD operations).
    - Filter tasks by status and due date range.
    - Sort tasks by various fields.
- **Security:**
    - Protection against common web vulnerabilities (XSS, CSRF).
    - Input validation and sanitization.
    - JWT authentication and authorization with token blacklisting.
    - Password hashing using BCrypt.
    - Rate limiting on authentication endpoints to prevent brute-force attacks.
- **Technology Stack:**
    - Spring Boot
    - Spring WebFlux (Reactive Programming Model)
    - Spring Data MongoDB Reactive
    - Spring Security
    - Java 17
    - Lombok (for reduced boilerplate code)
    - JSON Web Tokens (JWT)
    - Bucket4j (for rate limiting)
    - Swagger (for API documentation)

## Prerequisites

- **Java 17:** Ensure you have Java 17 or later installed.
- **Gradle 8.4 (or compatible):** Used for building and running the application.
- **MongoDB:** You'll need a running MongoDB instance for data persistence.

## Getting Started

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/your-username/code-synthesis-tool.git
   cd code-synthesis-tool
   ```

2. **Configuration (`application.yml`):**
    - **MongoDB Settings:**
      ```yaml
      spring:
        data:
          mongodb:
            uri: mongodb://your-mongodb-host:your-mongodb-port/your-database-name
      ```
        - Replace placeholders with your actual MongoDB connection details.
    - **JWT Secret:**
      ```yaml
      jwt:
        secret: your-jwt-secret-key
        token-expiration-time: 86400000 # Access token expiration time in milliseconds (e.g., 1 day)
        refresh-expiration-time: 604800000 # Refresh token expiration time in milliseconds (e.g., 7 days)
      ```
        - Set a strong, secret key for JWT signing.
        - Configure token expiration times as needed.
    - **Rate Limiting:**
      ```yaml
      rate-limiting:
        # Configure rate limiting properties here if needed
      ```

3. **Build and Run:**
   ```bash
   ./gradlew build && ./gradlew bootRun
   ```

## API Documentation

The API documentation will be available after running the application. You can access it typically at:

- Swagger UI:  `http://localhost:8080/swagger-ui.html`

## Important Considerations

- **Error Handling:** The application includes basic error handling. Consider adding more robust error handling and logging for production environments.
- **Testing:**  Write unit and integration tests to ensure the application's functionality and security.
- **Deployment:**  Configure your application for deployment to your chosen environment (e.g., Docker, cloud platform).

## Contributing

Contributions are welcome! Please open an issue or submit a pull request if you have any suggestions or improvements.
