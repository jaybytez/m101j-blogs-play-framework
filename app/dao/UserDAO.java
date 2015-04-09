package dao;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;

public class UserDAO extends BaseDAO {
    private final MongoCollection<Document> usersCollection;
    private Random random = new SecureRandom();

    public UserDAO() {
        usersCollection = getCollection("users");
    }

    // validates that username is unique and insert into db
    public boolean addUser(String username, String password, String email) {

        String passwordHash = makePasswordHash(password, Integer.toString(random.nextInt()));

        Document user = new Document();

        user.append("_id", username).append("password", passwordHash);

        if (email != null && !email.equals("")) {
            // the provided email address
            user.append("email", email);
        }

        try {
            usersCollection.insertOne(user);
            return true;
        } catch (MongoWriteException e) {
            if (e.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY)) {
                return false;
            }
            throw e;
        }
    }

    public Document validateLogin(String username, String password) {
        Document user = usersCollection.find(eq("_id", username)).first();

        if (user == null) {
            return null;
        }

        String hashedAndSalted = user.get("password").toString();

        String salt = hashedAndSalted.split(",")[1];

        if (!hashedAndSalted.equals(makePasswordHash(password, salt))) {
            return null;
        }

        return user;
    }


    private String makePasswordHash(String password, String salt) {
        try {
            String saltedAndHashed = password + "," + salt;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(saltedAndHashed.getBytes());
            byte hashedBytes[] = (new String(digest.digest(), "UTF-8")).getBytes();
            return Base64.getEncoder().encodeToString(hashedBytes) + "," + salt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is not available", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 unavailable?  Not a chance", e);
        }
    }
}
