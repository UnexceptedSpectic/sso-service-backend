package dev.blep.accounts.util;

import io.jsonwebtoken.*;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.crypto.spec.SecretKeySpec;
import javax.naming.AuthenticationException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AccountValidator {

    /** Sets how long JWTs remain valid*/
    private static long jwtDuration = TimeUnit.MINUTES.toMillis(1);

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
        if (password.length() < 8) {
            return false;
        }

        // Ensure password doesn't contain spaces
        else if (Pattern.compile("[\\s]+").matcher(password).find()) {
            return false;
        }

        // Ensure password contains capital letter
        else if (! Pattern.compile("[A-Z]+").matcher(password).find()) {
            return false;
        }

        // Ensure password contains number
        else if (! Pattern.compile("[\\d]+").matcher(password).find()) {
            return false;
        }

        // Ensure password contains symbol
        else if (! Pattern.compile(String.format("[%s]+",
                Pattern.quote("`~!@#$%^&*()_+-=.,/<>?;:'\"[]{}\\|")))
                .matcher(password).find()) {
            return false;
        }

        else {
            return true;
        }

    }

    public static String generateJwt(String secretKey, String userId, String ssoSuiteId) {
        Key signingKey = new SecretKeySpec(Base64.encodeBase64(secretKey.getBytes()),
                SignatureAlgorithm.HS512.getJcaName());
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("userId", userId);
        claimsMap.put("ssoSuiteId", ssoSuiteId);
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .addClaims(claimsMap)
                .setExpiration(new Date(System.currentTimeMillis() + jwtDuration))
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    public static void jwtIsValid(String jwt, String secretKey) throws AuthenticationException {
        try {
            Jwts.parser()
                    .setSigningKey(Base64.encodeBase64(secretKey.getBytes()))
                    .parseClaimsJws(jwt).getBody();
        } catch (SignatureException e) {
            throw new AuthenticationException("The JWT provided is invalid");
        }
    }

    public static String getJwtPayload(String jwt) {
        return new String(Base64.decodeBase64(jwt.split(Pattern.quote("."))[1]));
    }

}
