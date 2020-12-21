package dev.blep.accounts.services;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blep.accounts.exceptions.BadRequestException;
import dev.blep.accounts.repositories.SsoSuiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Map;

@Service
public class SingleSignOnSuite {

    @Autowired
    private SsoSuiteRepository ssoSuiteRepository;

    @Autowired
    private Account account;

    @Autowired
    private Environment env;

    public String createSsoSuite(JsonNode requestBody) throws BadRequestException, AuthenticationException {

        Map<String, String> userMap = account.getUserIdFieldAndUserIdMap(requestBody);
        String userIdField = userMap.get("userIdField");
        String userId = userMap.get("userId");
        JsonNode apiKeyNode = requestBody.get("apiKey");
        JsonNode ssoSuiteNode = requestBody.get("ssoSuiteName");

        if (apiKeyNode == null) {
            throw new BadRequestException("Include an 'apiKey' field in your request body JSON");
        }

        if (ssoSuiteNode == null) {
            throw new BadRequestException("Include a 'ssoSuiteName' field in your request body JSON");
        }

        account.verifyCredentials(requestBody);

        return this.ssoSuiteRepository.createSsoSuite(userIdField, userId, apiKeyNode.asText(), ssoSuiteNode.asText());
    }
}
