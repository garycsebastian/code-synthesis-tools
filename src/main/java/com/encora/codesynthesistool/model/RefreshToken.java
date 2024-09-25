package com.encora.codesynthesistool.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken {
    @Id private String id;
    private String token;
    private String username;
    private LocalDateTime expiryDate;
}
