# Code Synthesis Tool (Encora)

This Spring Boot application provides a foundation for a code synthesis tool, featuring a secure API built with Spring WebFlux and Spring Data MongoDB Reactive.

## Features

- **User Authentication and Authorization:**
   - Secure user registration (`/api/auth/signup`).
   - JWT-based authentication (`/api/auth/login`).
   - Secure logout with JWT blacklisting (`/api/auth/logout`).
- **Task Management:**
   - Create, read, update, and delete tasks (CRUD operations).
   - Filter tasks by status and due date range.
   - Sort tasks by various fields.
- **Security:**
   - Protection against common web vulnerabilities (XSS, CSRF).
   - Input validation and sanitization.
   - JWT authentication and authorization.
   - Password hashing.
- **Technology Stack:**
   - Spring Boot
   - Spring WebFlux (Reactive Programming Model)
   - Spring Data MongoDB Reactive
   - Spring Security
   - Java 17
   - Lombok (for reduced boilerplate code)
   - JSON Web Tokens (JWT)

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
     ```
      - Set a strong, secret key for JWT signing.

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