package dev.blep.accounts.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blep.accounts.exceptions.BadRequestException;
import dev.blep.accounts.services.Account;
import dev.blep.accounts.services.SingleSignOnSuite;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an API for interaction with accounts.
 */
@RestController
@Log4j2
public class AccountController {

    /** Injected account service into spring application context. */
    @Autowired
    private Account account;

    @Autowired
    private SingleSignOnSuite ssoSuite;

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/account/create", method = RequestMethod.POST)
    public ResponseEntity createAccount(@RequestBody String requestBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        String endpoint = "/account/create";
        body.put("path", endpoint);
        try {
            JsonNode requestBodyJN = getRequestBodyJsonNode(requestBody, body);
            this.account.createAccount(requestBodyJN);
            body.put("status", HttpStatus.OK.toString());
            return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (BadRequestException e) {
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (Exception e) {
            // Should never execute
            return genericExceptionResponse(body, responseHeaders, endpoint, e);
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/account/authenticate", method = RequestMethod.POST)
    public ResponseEntity <String> authenticateAccount(@RequestBody String requestBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        String endpoint = "/account/authenticate";
        body.put("path", endpoint);
        try {
            JsonNode requestBodyJN = getRequestBodyJsonNode(requestBody, body);
            String jwt = this.account.authenticateAndGetJwt(requestBodyJN);
            body.put("jwt", jwt);
            body.put("status", HttpStatus.OK.toString());
            return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (BadRequestException e) {
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (AuthenticationException e) {
            body.put("status", HttpStatus.UNAUTHORIZED.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (Exception e) {
            // Should never execute
            return genericExceptionResponse(body, responseHeaders, endpoint, e);
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/account/signOut", method = RequestMethod.POST)
    public ResponseEntity signOutAccount(@RequestBody String requestBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        String endpoint = "/account/signOut";
        body.put("path", endpoint);
        try {
            JsonNode requestBodyJN = getRequestBodyJsonNode(requestBody, body);
            this.account.signOut(requestBodyJN);
            body.put("status", HttpStatus.OK.toString());
            return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (BadRequestException e) {
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (AuthenticationException e) {
            body.put("status", HttpStatus.UNAUTHORIZED.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (Exception e) {
            // Should never execute
            return genericExceptionResponse(body, responseHeaders, endpoint, e);
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/account/changeType", method = RequestMethod.POST)
    public ResponseEntity modifyAccount(@RequestBody String requestBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        String endpoint = "/account/changeType";
        body.put("path", endpoint);
        try {
            JsonNode requestBodyJN = getRequestBodyJsonNode(requestBody, body);
            String apiKey = this.account.changeAccountType(requestBodyJN);
            body.put("apiKey", apiKey);
            body.put("status", HttpStatus.OK.toString());
            return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (BadRequestException e) {
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (AuthenticationException e) {
            body.put("status", HttpStatus.UNAUTHORIZED.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (Exception e) {
            // Should never execute
            return genericExceptionResponse(body, responseHeaders, endpoint, e);
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/sso-suite/create", method = RequestMethod.POST)
    public ResponseEntity createSsoSuite(@RequestBody String requestBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<>();
        String endpoint = "/sso-suite/create";
        body.put("path", endpoint);
        try {
            JsonNode requestBodyJN = getRequestBodyJsonNode(requestBody, body);
            String ssoSuiteId = this.ssoSuite.createSsoSuite(requestBodyJN);
            body.put("status", HttpStatus.OK.toString());
            body.put("ssoSuiteId", ssoSuiteId);
            return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (BadRequestException e) {
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (AuthenticationException e) {
            body.put("status", HttpStatus.UNAUTHORIZED.toString());
            body.put("error", e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(responseHeaders).body(new JSONObject(body).toString());
        } catch (Exception e) {
            // Should never execute
            return genericExceptionResponse(body, responseHeaders, endpoint, e);
        }
    }

    private JsonNode getRequestBodyJsonNode(String requestBody, Map<String, String> responseBody) throws BadRequestException {
        Map<String, String> requestBodyMap;
        try {
            requestBodyMap = new ObjectMapper().readValue(requestBody, HashMap.class);
            return new ObjectMapper().readTree(new JSONObject(requestBodyMap).toString());
        } catch (Exception e) {
            throw new BadRequestException("Request body json is malformed");
        }
    }

    private ResponseEntity<String> genericExceptionResponse(Map<String, String> responseBody, HttpHeaders responseHeaders, String endpoint, Exception e) {
        log.info(String.format("Unforeseen error at %s: ", endpoint));
        e.printStackTrace();
        responseBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.toString());
        responseBody.put("error", "An unknown error occurred. Please report the steps to reproduce this issue to support@email.com");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(responseHeaders).body(new JSONObject(responseBody).toString());
    }
}
