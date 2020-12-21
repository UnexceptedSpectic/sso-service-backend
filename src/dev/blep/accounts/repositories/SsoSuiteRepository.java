package dev.blep.accounts.repositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import dev.blep.accounts.exceptions.BadRequestException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import static dev.blep.accounts.repositories.Repository.fieldValueExists;

@Repository
public class SsoSuiteRepository {

    @Autowired
    private Environment env;

    @Autowired
    private AccountRepository accountRepository;

    private MongoCollection<Document> collection;

    private static final String COLLECTION_NAME = "sso_suites";
    private static final String NAME_FIELD = "name";
    private static final String JWT_FIELD = "jwt";

    @PostConstruct
    public void init() {

        String HOST = env.getProperty("spring.data.mongodb.host");
        String PORT = env.getProperty("spring.data.mongodb.port");
        String DATABASE = env.getProperty("spring.data.mongodb.database");
        String USERNAME = env.getProperty("spring.data.mongodb.username");
        String PASSWORD = env.getProperty("spring.data.mongodb.password");
        String AUTH_SOURCE = env.getProperty("spring.data.mongodb.authentication-database");
        MongoClient mongoClient = MongoClients.create("mongodb://" + USERNAME + ":" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8) + "@" + HOST + ":" + PORT + "/?authSource=" + AUTH_SOURCE);
        MongoDatabase db = mongoClient.getDatabase(Objects.requireNonNull(DATABASE));
        this.collection = db.getCollection(COLLECTION_NAME);
    }

    public String createSsoSuite(String userIdField, String userId, String apiKey, String ssoSuite) throws BadRequestException {

        Document doc = new Document(NAME_FIELD, ssoSuite);

        // Only dev accounts can create sso suites
        Document accountDocument = accountRepository.getDocument(apiKey);
        if (accountDocument != null && accountDocument.get(userIdField).equals(userId)) {
            if (fieldValueExists(collection, NAME_FIELD, ssoSuite)) {
                throw new BadRequestException(String.format("An SSO suite with the name '%s' already exists",
                        ssoSuite));
            } else {
                collection.insertOne(doc);
                return collection.find(doc).first().get("_id").toString();
            }
        } else {
            throw new BadRequestException("The apiKey provided is invalid");
        }
    }

    public boolean ssoSuiteExists(String ssoSuiteId) throws BadRequestException {
        try {
            return collection.find(Filters.eq("_id", new ObjectId(ssoSuiteId))).first() != null;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The ssoSuiteId provided is invalid");
        }
    }

}
