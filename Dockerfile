FROM openjdk:17-jdk-alpine

# Set environment variables
ENV APP_HOME /app
ENV SPRING_PROFILES_ACTIVE default

# Create application directory
WORKDIR $APP_HOME

# Copy JAR file into the container
COPY build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]

