package dev.blep.accounts.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import dev.blep.accounts.entities.AccountEntity;
import dev.blep.accounts.exceptions.BadRequestException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.naming.AuthenticationException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Updates.set;
import static dev.blep.accounts.repositories.Repository.fieldValueExists;
import static dev.blep.accounts.repositories.Repository.getFieldValue;
import static dev.blep.accounts.util.AccountValidator.*;

/**
 * Database interaction
 */
@Repository
@Log4j2
public class AccountRepository {

    @Autowired
    private Environment env;

    @Autowired
    private SsoSuiteRepository ssoSuiteRepository;

    private String SECRET_KEY;

    private MongoCollection<Document> collection;

    private static final String COLLECTION_NAME = "accounts";
    private static final String EMAIL_FIELD = "email";
    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";
    private static final String TYPE_FIELD = "type";
    private static final String SSO_FIELD = "ssoSuiteJwt";

    @PostConstruct
    public void init() {

        this.SECRET_KEY = env.getProperty("app.security.jwtSecret");
        String HOST = env.getProperty("spring.data.mongodb.host");
        String PORT = env.getProperty("spring.data.mongodb.port");
        String DATABASE = env.getProperty("spring.data.mongodb.database");
        String USERNAME = env.getProperty("spring.data.mongodb.username");
        String PASSWORD = env.getProperty("spring.data.mongodb.password");
        String AUTH_SOURCE = env.getProperty("spring.data.mongodb.authentication-database");
        MongoClient mongoClient = MongoClients.create("mongodb://" + USERNAME + ":" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8) + "@" + HOST + ":" + PORT + "/?authSource=" + AUTH_SOURCE);
        System.out.print("mongodb://" + USERNAME + ":" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8) + "@" + HOST + ":" + PORT + "/?authSource=" + AUTH_SOURCE);
        MongoDatabase db = mongoClient.getDatabase(Objects.requireNonNull(DATABASE));
        this.collection = db.getCollection(COLLECTION_NAME);
    }

    public void createAccount(String username, String email, String hashedPassword, String accountType) throws BadRequestException {

        Document doc = new Document(EMAIL_FIELD, email)
                .append(USERNAME_FIELD, username)
                .append(PASSWORD_FIELD, hashedPassword)
                .append(TYPE_FIELD, accountType)
                .append(SSO_FIELD, new JSONObject().toString());

        if (fieldValueExists(collection, USERNAME_FIELD, username)) {
            throw new BadRequestException(String.format("An account with the username '%s' already exists",
                    username));
        } else if (fieldValueExists(collection, EMAIL_FIELD, email)) {
            throw new BadRequestException(String.format("An account with the email '%s' already exists",
                    email));
        } else {
            collection.insertOne(doc);
        }

    }

    public String loginAndGetJwt(String userIdField, String userId, String password, String ssoSuiteId) throws BadRequestException, AuthenticationException {

        verifyCredentials(userIdField, userId, password);

        Document accountDoc = collection.find(Filters.eq(userIdField, userId)).first();
        JSONObject ssoSuiteJwtJson = getSsoSuiteJwtJson(accountDoc);

        if (!ssoSuiteRepository.ssoSuiteExists(ssoSuiteId)) {
            throw new BadRequestException("The SSO suite specified has not been registered. To register a " +
                    "SSO suite a developer account can use the /sso-suite/create endpoint");
        }
        String dbJwt;
        try {
            dbJwt = ssoSuiteJwtJson.get(ssoSuiteId).toString();
        } catch (JSONException e) {
            dbJwt = null;
        }

        // if the user is already signed in and jwt is unexpired, return jwt
        if (dbJwt != null && !dbJwt.isEmpty()) {
            try {
                jwtIsValid(dbJwt, SECRET_KEY);
                return dbJwt;
            } catch (ExpiredJwtException e) {}
        }
        // if user wasn't signed in or their database jwt expired, sign in and return new jwt
        String newJwt = generateJwt(SECRET_KEY, userId, ssoSuiteId);
        ssoSuiteJwtJson.put(ssoSuiteId, newJwt);
        collection.updateOne(accountDoc, set(SSO_FIELD, ssoSuiteJwtJson.toString()));
        return newJwt;
    }

    private JSONObject getSsoSuiteJwtJson(Document accountDoc) {
        // Initialized during account creation. Will never be null
        Map<String, String> result = null;
        try {
            result = new ObjectMapper().readValue(accountDoc.get(SSO_FIELD).toString(), HashMap.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new JSONObject(result);
    }

    public void verifyCredentials(String userIdField, String userId, String inputPassword) throws AuthenticationException {
        if (!fieldValueExists(collection, userIdField, userId)) {
            if (userIdField.equals("email")) {
                throw new AuthenticationException(String.format("An account with the email '%s' doesn't exist", userId));
            } else if (userIdField.equals("username")) {
                throw new AuthenticationException(String.format("An account with the username '%s' doesn't exist", userId));
            }
        }

        // If the account exists, as ensured above, it will always have an associated password
        if (!BCrypt.checkpw(inputPassword, getFieldValue(collection, "password", userIdField, userId))) {
            throw new AuthenticationException("The password provided is incorrect");
        }
    }

    public String verifyJwtAndGetJwt(String jwt, boolean renew) throws AuthenticationException, BadRequestException {

        Map<String, String> userInfoMap;
        try {
            userInfoMap = getUserInfo(jwt);
        } catch (ExpiredJwtException e) {
            // handled by calling function
            throw e;
        }
        String userIdField = userInfoMap.get("userIdField");
        String userId = userInfoMap.get("userId");
        String ssoSuiteId = userInfoMap.get("ssoSuiteId");

        Document accountDoc = collection.find(Filters.eq(userIdField, userId)).first();
        JSONObject ssoSuiteJwtJson = getSsoSuiteJwtJson(accountDoc);
        String expireTime = new JSONObject(getJwtPayload(jwt)).get("exp").toString();
        String dbJwt;
        try {
            dbJwt = ssoSuiteJwtJson.get(ssoSuiteId).toString();
        } catch (JSONException e) {
            dbJwt = null;
        }

        // if user account associated with query jwt is signed in
        if (dbJwt != null && !dbJwt.isEmpty()) {
            try {
                jwtIsValid(jwt, SECRET_KEY);

                if (renew) {
                     if (Integer.parseInt(expireTime) - Instant.now().getEpochSecond() <= 30) {
                         String newJwt = generateJwt(SECRET_KEY, userId, ssoSuiteId);
                         ssoSuiteJwtJson.put(ssoSuiteId, newJwt);
                         collection.updateOne(accountDoc, set(SSO_FIELD, ssoSuiteJwtJson.toString()));
                         return newJwt;
                     } else {
                         throw new BadRequestException("JWT tokens can only be renewed if they expire in 30 or fewer seconds");
                     }
                }

                return dbJwt;
            } catch (ExpiredJwtException e) {
                // handle by calling function
                throw e;
            }
        } else {
            throw new AuthenticationException("The account associated with the provided JWT is signed out. " +
                    "Sign in using the web app");
        }
    }

    private Map<String, String> getUserInfo(String jwt) {

        Map<String, String> userInfoMap = new HashMap<>();
        JSONObject jwtPayload = new JSONObject(getJwtPayload(jwt));
        String userId = jwtPayload.get("userId").toString();
        String ssoSuiteId = jwtPayload.get("ssoSuiteId").toString();
        String userIdField;
        if (isValidEmail(userId)) {
            userIdField = "email";
        } else {
            userIdField = "username";
        }
        userInfoMap.put("userIdField", userIdField);
        userInfoMap.put("userId", userId);
        userInfoMap.put("ssoSuiteId", ssoSuiteId);
        return userInfoMap;
    }

    public String changeAccountType(String filterField, String filterValue, String accountType) throws BadRequestException {
        Field[] types = AccountEntity.Types.class.getDeclaredFields();
        List<String> typeStrings = new ArrayList<>();
        for (Field f: types) {
            try {
                typeStrings.add((String) f.get(AccountEntity.Types.class));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (typeStrings.contains(accountType)) {
            Document doc = collection.find(Filters.eq(filterField, filterValue)).first();
            String previousType = getFieldValue(doc, "type");
            if (!accountType.equals(previousType)) {
                collection.updateOne(doc,
                        new Document("$set", new Document("type", accountType)));
                if (accountType.equals(AccountEntity.Types.DEVELOPER)) {
                    return doc.get("_id").toString();
                } else {
                    return null;
                }
            } else {
                throw new BadRequestException("Cannot change account type to the existing type. Specify a 'type' field " +
                        "in your request with a value from: " + typeStrings.toString());
            }
        } else {
            throw new BadRequestException("Valid account types are: " + typeStrings.toString());
        }
    }

    public Document getDocument(String objectID) {

        return collection.find(Filters.eq("_id", new ObjectId(objectID))).first();
    }

    public void signOut(String jwt) {

        Map<String, String> userInfoMap = getUserInfo(jwt);
        String userIdField = userInfoMap.get("userIdField");
        String userId = userInfoMap.get("userId");
        String ssoSuiteId = userInfoMap.get("ssoSuiteId");

        Document accountDoc = collection.find(Filters.eq(userIdField, userId)).first();
        JSONObject ssoSuiteJwtJson = getSsoSuiteJwtJson(accountDoc);
        ssoSuiteJwtJson.put(ssoSuiteId, "");

        collection.updateOne(accountDoc, set(SSO_FIELD, ssoSuiteJwtJson.toString()));
    }
}
