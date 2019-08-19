package dev.blep.accounts.controllers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.blep.accounts.entities.AccountEntity;
import dev.blep.accounts.exceptions.BadRequestException;
import dev.blep.accounts.service.Account;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an API for interaction with accounts.
 */
@RestController
@Log4j2
public class AccountController {

    /** Injected account service into swift application context. */
    @Autowired
    private Account account;

    @RequestMapping(value = "/account/create", method = RequestMethod.POST)
    public ResponseEntity createAccount(@RequestBody String requestBody) throws Exception {
        try {
            ObjectMapper mapper = new ObjectMapper();
            AccountEntity accountEntity = mapper.readValue(requestBody, AccountEntity.class);
            this.account.createAccount(accountEntity);
            return ResponseEntity.ok()
                    .body(HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("Failed due to: " + e);
        }
        return ResponseEntity.ok().body(HttpStatus.OK);
    }

    @RequestMapping(value = "/account/authenticate", method = RequestMethod.POST)
    public ResponseEntity <String> authenticateAccount(@RequestBody String requestBody) throws BadRequestException, JsonParseException, JsonProcessingException, Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        Map<String, String> body = new HashMap<String, String>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            AccountEntity accountEntity = mapper.readValue(requestBody, AccountEntity.class);
            String jwt = this.account.authenticateAccount(accountEntity);
            responseHeaders.set("Authorization", "JWT " + jwt);
            return ResponseEntity.ok().headers(responseHeaders).build();
        } catch (BadRequestException e) {
            body.put("timestamp", new Date().toString());
            body.put("status", HttpStatus.UNAUTHORIZED.toString());
            body.put("message", e.getLocalizedMessage());
            body.put("path", "/account/authenticate");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(responseHeaders).body(new Gson().toJson(body));
        } catch (JsonParseException e) {
            body.put("timestamp", new Date().toString());
            body.put("status", HttpStatus.BAD_REQUEST.toString());
            body.put("message", "Invalid json");
            body.put("path", "/account/authenticate");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(responseHeaders).body(new Gson().toJson(body));
        } catch (JsonProcessingException e) {
            log.info("An issue occurred when mapping the request body json to an AccountEntity");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            // Should never execute
            log.info("Something went terribly wrong: " + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
