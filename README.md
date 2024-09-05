# Code Synthesis Tool (Encora)

This Spring Boot application provides a foundation for building a code synthesis tool, featuring secure API endpoints protected with OAuth2 using Auth0.

## Features

- **Secure API Endpoints:**
    - `/api/public/**`: Publicly accessible endpoints.
    - `/api/tasks/**`: Protected endpoints requiring authentication via Auth0.
- **OAuth2 Resource Server:** Secures the application using Auth0 as the authorization server.
- **JWT Authentication:** Validates JSON Web Tokens (JWTs) issued by Auth0.
- **Spring WebFlux:** Built using Spring's reactive web framework for non-blocking, asynchronous operations.
- **Spring Data MongoDB Reactive:**  Interacts with MongoDB using reactive programming for efficient data handling.

## Prerequisites

- **Java 17:** Ensure you have Java 17 installed.
- **Gradle 8.4 (or compatible):** Used for building and running the application.
- **Auth0 Account:** You'll need an Auth0 account to configure OAuth2 and obtain necessary credentials.
- **MongoDB:** You'll need a running MongoDB instance for data persistence.

## Getting Started

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/your-username/code-synthesis-tool.git
   cd code-synthesis-tool
   ```

2. **Auth0 Configuration:**
    - **Create an Auth0 API:** In your Auth0 dashboard, create a new API for your application.
    - **Set Audience:** Set the "Identifier" value in your Auth0 API settings to match the `okta.oauth2.audience` value in your `application.yml` file.
    - **Define Scopes:** Define the necessary scopes for your API endpoints (e.g., `read:tasks`, `write:tasks`) in your Auth0 API settings.

3. **Application Configuration (`application.yml`):**
    - **Auth0 Settings:**
      ```yaml
      okta:
        oauth2:
          issuer: https://YOUR_AUTH0_DOMAIN/ 
          audience: https://YOUR_AUTH0_DOMAIN/api/v2/ 
      ```
        - Replace `YOUR_AUTH0_DOMAIN` with your actual Auth0 domain.
    - **MongoDB Settings:**
      ```yaml
      spring:
        data:
          mongodb:
            uri: mongodb+srv://your-mongodb-connection-string
      ```
        - Replace `your-mongodb-connection-string` with your MongoDB connection string.

4. **Build and Run:**
   ```bash
   ./gradlew build && ./gradlew bootRun
   ```

## Testing

1. **Obtain an Access Token:** Use a tool like Postman to obtain an access token from your Auth0 authentication server. You'll need to provide your Auth0 client credentials and the appropriate scopes.

2. **Make API Requests:**
    - **Public Endpoint:**
      ```bash
      curl http://localhost:8080/api/public
      ```
    - **Protected Endpoint (Requires Authentication):**
      ```bash
      curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" http://localhost:8080/api/tasks
      ```
        - Replace `YOUR_ACCESS_TOKEN` with the access token you obtained from Auth0.

## Important Security Considerations

- **CSRF Protection:** The current configuration disables CSRF protection for simplicity. **Do not disable CSRF protection in a production environment!** Implement proper CSRF protection mechanisms.
- **CORS Configuration:**  Consider configuring CORS securely for your specific use case in a production environment.
- **Error Handling:** Implement robust error handling for authentication and authorization failures to provide meaningful responses to clients.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request if you have any suggestions or improvements.