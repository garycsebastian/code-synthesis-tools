package com.encora.codesynthesistool.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
    @Id private String id;

    @Indexed(unique = true)
    private String username;

    private String password;
    private List<String> roles;
    private boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd") // Specify the desired date format
    private LocalDate registrationDate; // Add the registrationDate field

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles() == null || getRoles().isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                : getRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
