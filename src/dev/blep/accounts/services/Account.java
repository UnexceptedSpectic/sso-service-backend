package dev.blep.accounts.services;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blep.accounts.exceptions.BadRequestException;
import dev.blep.accounts.repositories.AccountRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.naming.AuthenticationException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static dev.blep.accounts.util.AccountValidator.*;

/**
 * The business object that operates on accounts.
 */
@Service
public class Account {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private Environment env;

    @Validated
    public void createAccount(JsonNode requestBody) throws BadRequestException {

        JsonNode usernameNode = requestBody.get("username");
        JsonNode emailNode = requestBody.get("email");
        JsonNode passwordNode = requestBody.get("password");
        JsonNode accountTypeNode = requestBody.get("type");
        String accountType;
        if (accountTypeNode == null) {
            accountType = "user";
        } else {
            accountType = accountTypeNode.asText();
        }
        if (usernameNode == null || emailNode == null) {
            throw new BadRequestException("Both username and email are required");
        } else {
            if (!isValidEmail(emailNode.asText())) {
                throw new BadRequestException("The email provided is invalid");
            } else if (!isValidPassword(passwordNode.asText())) {
                throw new BadRequestException("The password must have 8 characters and at least one capital letter, one symbol, and one number");
            }
            String hashedPassword = BCrypt.hashpw(passwordNode.asText(),
                    // 13 log_rounds required on my i7 6700k, but not necessarily on production server
                    // use test.AccountSecurity to determine secure number of log_rounds
                    BCrypt.gensalt(13, new SecureRandom()));

            this.accountRepository.createAccount(usernameNode.asText(), emailNode.asText(), hashedPassword, accountType);
        }

    }

    public String authenticateAndGetJwt(JsonNode requestBody) throws BadRequestException, AuthenticationException {

        String userIdField;
        String userId;
        JsonNode ssoSuiteNode;

        // Authentication using JWT
        JsonNode jwtNode = requestBody.get("jwt");
        JsonNode renewNode = requestBody.get("renew");
        if (jwtNode != null) {
            boolean renew = false;
            if (renewNode != null && renewNode.asText().equals("true")) {
                renew = true;
            }
            // Return the input jwt, if valid
            try {
                return this.accountRepository.verifyJwtAndGetJwt(jwtNode.asText(), renew);
            } catch (ExpiredJwtException e) {
                throw new AuthenticationException("The JWT provided has expired. Visit the web app to sign in/get an updated JWT");
            }
        }

        // Authentication using user credentials
        Map<String, String> userMap = getUserIdFieldAndUserIdMap(requestBody);
        userIdField = userMap.get("userIdField");
        userId = userMap.get("userId");
        JsonNode passwordNode = requestBody.get("password");
        ssoSuiteNode = requestBody.get("ssoSuiteId");

        if (passwordNode == null) {
            throw new BadRequestException("No password provided");
        } else if (ssoSuiteNode == null) {
            throw new BadRequestException("Must specify 'ssoSuiteId' in your request. Create a SSO suite at /sso-suite/create");
        }

        return this.accountRepository.loginAndGetJwt(
                userIdField,
                userId,
                passwordNode.asText(),
                ssoSuiteNode.asText());
    }

    public String changeAccountType(JsonNode requestBody) throws BadRequestException, AuthenticationException {

        Map<String, String> userMap = getUserIdFieldAndUserIdMap(requestBody);
        String userIdField = userMap.get("userIdField");
        String userId = userMap.get("userId");
        JsonNode passwordNode = requestBody.get("password");

        if (passwordNode == null) {
            throw new BadRequestException("No password provided");
        }

        this.accountRepository.verifyCredentials(
                userIdField,
                userId,
                passwordNode.asText());

        return this.accountRepository.changeAccountType(userIdField, userId, requestBody.get("type").asText());
    }

    public void verifyCredentials(JsonNode requestBody) throws BadRequestException, AuthenticationException {

        Map<String, String> userMap = getUserIdFieldAndUserIdMap(requestBody);
        String userIdField = userMap.get("userIdField");
        String userId = userMap.get("userId");
        JsonNode passwordNode = requestBody.get("password");

        if (passwordNode == null) {
            throw new BadRequestException("No password provided");
        }

        this.accountRepository.verifyCredentials(
                userIdField,
                userId,
                passwordNode.asText());
    }

    public Map<String, String> getUserIdFieldAndUserIdMap(JsonNode requestBody) throws BadRequestException {
        JsonNode usernameNode = requestBody.get("username");
        JsonNode emailNode = requestBody.get("email");
        Map<String, String> map = new HashMap<>();
        if (usernameNode != null) {
            map.put("userIdField", "username");
            map.put("userId", usernameNode.asText());
        } else if (emailNode != null) {
            map.put("userIdField", "email");
            map.put("userId", emailNode.asText());
        } else {
            throw new BadRequestException("No username or email provided");
        }
        return map;
    }

    public void signOut(JsonNode requestBody) throws BadRequestException, AuthenticationException {

        JsonNode jwtNode = requestBody.get("jwt");

        if (jwtNode != null) {
            try {
                this.accountRepository.verifyJwtAndGetJwt(jwtNode.asText(), false);
                this.accountRepository.signOut(jwtNode.asText());
            } catch (ExpiredJwtException e) {
                this.accountRepository.signOut(jwtNode.asText());
            }
        } else {
            throw new BadRequestException("No JWT was specified");
        }

    }
}

