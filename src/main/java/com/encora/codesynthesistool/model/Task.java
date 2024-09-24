package com.encora.codesynthesistool.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tasks") // Specify the collection name
@Data // Lombok annotation for getters, setters, etc.
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    @Id private String id;
    private String userId; // Add userId to track the task owner

    @NotNull(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    @JsonFormat(pattern = "yyyy-MM-dd") // Specify the desired date format
    private LocalDate dueDate;

    private TaskStatus status; // Use the enum instead of boolean
}
