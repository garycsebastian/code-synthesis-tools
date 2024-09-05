package com.encora.codesynthesistool.model;

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
    @Id
    private String id;
    private String description;
    private boolean completed;
}
