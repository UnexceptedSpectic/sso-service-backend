package dev.blep.accounts.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import dev.blep.accounts.entities.AccountEntity;
import dev.blep.accounts.exceptions.BadRequestException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Objects;

import static dev.blep.accounts.util.AccountValidator.generateJwt;
import static dev.blep.accounts.util.AccountValidator.verifyJwt;

/**
 * Database interaction
 */
@Repository
@Log4j2
public class AccountRepository {

    @Autowired
    private Environment env;

    private String SECRET_KEY;
    private String HOST;
    private String PORT;
    private String DATABASE;

    private MongoClient mongoClient;
    private MongoDatabase db;
    private MongoCollection<Document> collection;

    private static final String COLLECTION_NAME = "accounts";
    private static final String EMAIL_FIELD = "email";
    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    @PostConstruct
    public void init() {

        this.SECRET_KEY = env.getProperty("app.security.jwtSecret");
        this.HOST = env.getProperty("spring.data.mongodb.host");
        this.PORT = env.getProperty("spring.data.mongodb.port");
        this.DATABASE = env.getProperty("spring.data.mongodb.database");
        this.mongoClient = MongoClients.create("mongodb://" + HOST + ":" + PORT);
        this.db = mongoClient.getDatabase(Objects.requireNonNull(DATABASE));
        this.collection = db.getCollection(COLLECTION_NAME);
    }

    private boolean fieldValueExists(String filterField, String filterValue) {
        return collection.countDocuments(
                Filters.eq(filterField, filterValue)) != 0;
    }

    /**
     * Searches collection for doc containing "filterField": "filterValue"
     * Returns "field" element of first search result*/
    private String getFieldValue(String field, String filterField, String filterValue) throws JSONException {

        /*TODO: find more modern means of extracting field value from db search doc*/
        String json = collection.find(Filters.eq(filterField, filterValue)).first().toJson();
        JSONObject jsonObject = new JSONObject(json);
        try {
            return (String) jsonObject.get(field);
        } catch (JSONException e) {
            return null;
        }

    }

    public void createAccount(AccountEntity accountEntity) throws Exception {

        Document doc = new Document(EMAIL_FIELD, accountEntity.getEmail())
                .append(USERNAME_FIELD, accountEntity.getUsername())
                .append(PASSWORD_FIELD, accountEntity.getPassword());

        if (fieldValueExists(USERNAME_FIELD, accountEntity.getUsername())) {
            throw new BadRequestException(String.format("account with username %s already exists",
                    accountEntity.getUsername()));
        } else if (fieldValueExists(EMAIL_FIELD, accountEntity.getEmail())) {
            throw new BadRequestException(String.format("account with email %s already exists",
                    accountEntity.getEmail()));
        } else {
            collection.insertOne(doc);
        }

    }

    enum UserIdType {
        EMAIL,
        USERNAME;
    }

    public String authenticateAccount(AccountEntity accountEntity) throws BadRequestException {

        if (accountEntity.getEmail() != null) {
            if (fieldValueExists(EMAIL_FIELD, accountEntity.getEmail())) {
                return updateJwt(accountEntity, UserIdType.EMAIL);
            } else {
                throw new BadRequestException(String.format("An account with the email %s doesn't exist", accountEntity.getEmail()));
            }
        } else if (accountEntity.getUsername() != null) {
            if (fieldValueExists(USERNAME_FIELD, accountEntity.getUsername())) {
                return updateJwt(accountEntity, UserIdType.USERNAME);
            } else {
                throw new BadRequestException(String.format("An account with the email %s doesn't exist", accountEntity.getUsername()));
            }
        } else {
                throw new BadRequestException("No email/username provided");
        }
    }

    /**
     * Main authentication logic, generalized to work with either userId of 'email' or 'username'
     * Returns jwt from db if unexpired or generates and returns new jwt
     * */
    private String updateJwt(AccountEntity accountEntity, UserIdType userIdType) throws BadRequestException {
        String filterValue;
        String userIdTypeLower = userIdType.toString().toLowerCase();
        switch (userIdType) {
            case EMAIL:
                filterValue = accountEntity.getEmail();
                break;
            case USERNAME:
                filterValue = accountEntity.getUsername();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + userIdTypeLower);
        }

        if (BCrypt.checkpw(accountEntity.getPassword(), getFieldValue("password", userIdTypeLower, filterValue))) {
            String jwt = getFieldValue("jwt", userIdTypeLower, filterValue);
            boolean expired = false;
            if (jwt != null) {
                try {
                    verifyJwt(jwt, SECRET_KEY);
                } catch (ExpiredJwtException e) {
                    expired = true;
                }
            }
            if (!expired && jwt != null) {
                return jwt;
            } else {
                accountEntity.setJwt(generateJwt(SECRET_KEY, accountEntity));
                collection.updateOne(Filters.eq(userIdTypeLower, filterValue),
                        new Document("$set", new Document("jwt", accountEntity.getJwt())));
                log.info("JWT renewed");
                return accountEntity.getJwt();
            }

        } else {
            throw new BadRequestException(String.format("Invalid password for %s %s", userIdTypeLower, filterValue));
        }
    }

}
