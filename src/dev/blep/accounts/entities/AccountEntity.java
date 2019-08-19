package dev.blep.accounts.entities;

import lombok.Data;

/**
 * Represents the model entity for an account.
 */
@Data
public class AccountEntity {

    private String email;
    private String username;
    private String password;
    private String jwt;

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
    public AccountEntity(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
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

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
