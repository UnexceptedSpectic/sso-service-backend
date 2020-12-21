package dev.blep.accounts.entities;

import lombok.Data;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the model entity for an account.
 */
@Data
public class AccountEntity {

    public static class Types {
        public static final String USER = "user";
        public static final String DEVELOPER = "developer";
    };

    private String email;
    private String username;
    private String password;
    private String type = Types.USER;

    // Required to map requestBody to this entity
    public AccountEntity() {
        super();
    }

    /**
     * Constructs an account entity.
     * @param email
     * @param username
     * @param password
     */
    public AccountEntity(String email, String username, String password, String type) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() { return type; }
}
