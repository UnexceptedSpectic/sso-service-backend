import org.springframework.security.crypto.bcrypt.BCrypt;

import java.security.SecureRandom;

public class AccountSecurity {

    public static void main(String[] args) {
        System.out.println("Function test: " + saltHashPasswordTime(10));
    }

    // Functions that can be used to test account security

    /**
     * Returns time in ms BCrypt hashing function takes to run
     * */
    public static long saltHashPasswordTime(Integer log_rounds) {
        long startTime = System.nanoTime();
        // password complexity doesn't impact hashing time
        BCrypt.hashpw("password", BCrypt.gensalt(log_rounds, new SecureRandom()));
        long endTime = System.nanoTime();
        return (endTime - startTime)/1000000;
    }

    /**
     * Returns a secure log number of hashing rounds to be used with BCrypt
     * Ensures runtime of hash function is > 241 ms
     * 241 ms calculated given f=200, p=30*24*60*60*1000, n=32 in equation at:
     * https://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256/3993#3993
     * */
    public static int logRounds() {
        /* A minimum required by BCrypt*/
        Integer log_rounds = 4;
        while (saltHashPasswordTime(log_rounds) < 241) {
            log_rounds += 1;
        }
        return log_rounds + 1;
    }
}
