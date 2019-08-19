package dev.blep.accounts.service;

import dev.blep.accounts.entities.AccountEntity;
import dev.blep.accounts.exceptions.BadRequestException;
import dev.blep.accounts.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;

import static dev.blep.accounts.util.AccountValidator.isValidEmail;
import static dev.blep.accounts.util.AccountValidator.isValidPassword;

/**
 * The business object that operates on accounts.
 */
@Service
public class Account {

    @Autowired
    private AccountRepository accountRepository;

    @Validated
    public void createAccount(AccountEntity accountEntity) throws Exception {

        if (accountEntity.getUsername() == null && accountEntity.getEmail() == null) {
            throw new BadRequestException("No username/email provided");
        } else {
            if (!isValidEmail(accountEntity.getEmail())) {
                throw new BadRequestException("The email provided is invalid");
            } else if (!isValidPassword(accountEntity.getPassword())) {
                throw new BadRequestException("The password provided is invalid");
            }
            accountEntity.setPassword(BCrypt.hashpw(accountEntity.getPassword(),
                    // 13 log_rounds required on my i7 6700k, but not necessarily on production server
                    // use test.AccountSecurity to determine secure number of log_rounds
                    BCrypt.gensalt(13, new SecureRandom())));
            this.accountRepository.createAccount(accountEntity);
        }

    }

    public String authenticateAccount(AccountEntity accountEntity) throws BadRequestException {

        if (accountEntity.getUsername() == null
        && accountEntity.getEmail() == null) {
            throw new BadRequestException("No username/email provided");
        } else if (accountEntity.getPassword() == null) {
            throw new BadRequestException("No password provided");
        }

        try {
            return this.accountRepository.authenticateAccount(accountEntity);
        } catch (BadRequestException e) {
            throw e;
        }
    }
}
