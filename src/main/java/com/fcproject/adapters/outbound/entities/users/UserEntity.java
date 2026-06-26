package com.fcproject.adapters.outbound.entities.users;


import java.time.LocalDate;
import java.util.*;

import com.fcproject.application.core.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "acess", name = "users")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name="first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name="last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 250)
    private String email;

    @Column(name="phone_number", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name="date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "password")
    private String password_hash;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDate createdAt;

    @Column(name = "update_at", insertable = false)
    private LocalDate updatedAt;

    @Column(name = "account_non_expired", columnDefinition = "bit(1)")
    private Boolean accountNonExpired;

    @Column(name = "account_non_locked", columnDefinition = "bit(1)")
    private Boolean accountNonLocked;

    @Column(name = "credentials_non_expired", columnDefinition = "bit(1)")
    private Boolean credentialsNonExpired;

    @Column(name = "enabled", columnDefinition = "bit(1)")
    private Boolean enabled;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    public UserEntity(String firstName, String lastName, String email, String phone, Gender gender, LocalDate dateOfBirth, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.password_hash = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.password_hash;
    }

    @Override
    public String getUsername() {
        return this.email; //Using email as sprint security identifier
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserEntity that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getFirstName(), that.getFirstName()) && Objects.equals(getLastName(), that.getLastName()) && Objects.equals(getEmail(), that.getEmail()) && Objects.equals(getPhone(), that.getPhone()) && getGender() == that.getGender() && Objects.equals(getDateOfBirth(), that.getDateOfBirth()) && Objects.equals(getPassword_hash(), that.getPassword_hash()) && Objects.equals(getCreatedAt(), that.getCreatedAt()) && Objects.equals(getUpdatedAt(), that.getUpdatedAt()) && Objects.equals(isAccountNonExpired(), that.isAccountNonExpired()) && Objects.equals(isAccountNonLocked(), that.isAccountNonLocked()) && Objects.equals(isCredentialsNonExpired(), that.isCredentialsNonExpired()) && Objects.equals(isEnabled(), that.isEnabled()) && Objects.equals(getRoles(), that.getRoles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFirstName(), getLastName(), getEmail(), getPhone(), getGender(), getDateOfBirth(), getPassword_hash(), getCreatedAt(), getUpdatedAt(), isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired(), isEnabled(), getRoles());
    }
}
