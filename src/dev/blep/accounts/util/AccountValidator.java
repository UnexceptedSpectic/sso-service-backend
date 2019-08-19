package dev.blep.accounts.util;

import dev.blep.accounts.entities.AccountEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.tomcat.util.codec.binary.Base64;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AccountValidator {

    /** Sets how long JWTs remain valid*/
    private static long jwtDuration = TimeUnit.HOURS.toMillis(4);

    /** Verifies email syntax and limits to a maximum length*/
    public static boolean isValidEmail(String email) {
        if (Pattern.compile("^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*" +
                "(@[_A-Za-z0-9-]+@*)[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})")
                .matcher(email).matches()
                && email.length() < 254) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Ensures password entropy of ~ 32 bits:
     * Requires 8 characters; one capital, one symbol, one number
     * */
    public static boolean isValidPassword(String password) {
        // TODO: throw exceptions
        if (8 > password.length() | password.getBytes().length > 56) {
            return false;
        }

        else if (Pattern.compile("[\\s]+").matcher(password).find()) {
            return false;
        }

        else if (! Pattern.compile("[A-Z]+").matcher(password).find()) {
            return false;
        }

        else if (! Pattern.compile("[\\d]+").matcher(password).find()) {
            return false;
        }

        else if (! Pattern.compile(String.format("[%s]+",
                Pattern.quote("`~!@#$%^&*()_+-=.,/<>?;:'\"[]{}\\|")))
                .matcher(password).find()) {
            return false;
        }

        else {
            return true;
        }

    }

    public static String generateJwt(String secretKey, AccountEntity accountEntity) {
        Key signingKey = new SecretKeySpec(Base64.encodeBase64(secretKey.getBytes()),
                SignatureAlgorithm.HS512.getJcaName());
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setSubject(accountEntity.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + jwtDuration))
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    public static void verifyJwt(String jwt, String secretKey) {
        Jwts.parser()
                .setSigningKey(Base64.encodeBase64(secretKey.getBytes()))
                .parseClaimsJws(jwt).getBody();
    }

}
