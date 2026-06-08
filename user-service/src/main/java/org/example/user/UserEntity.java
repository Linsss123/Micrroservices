package org.example.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id; // UUID som String

    @Column(length = 100, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String password; // OBS: för demo – i verkligheten: hasha

    protected UserEntity() {
    }

    public UserEntity(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}
