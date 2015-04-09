package dao;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.security.SecureRandom;
import java.util.Base64;

import static com.mongodb.client.model.Filters.eq;

public class SessionDAO extends BaseDAO {
    private final MongoCollection<Document> sessionsCollection;

    public SessionDAO() {
        sessionsCollection = getCollection("sessions");
    }


    public String findUserNameBySessionId(String sessionId) {
        Document session = getSession(sessionId);

        if (session == null) {
            return null;
        } else {
            return session.get("username").toString();
        }
    }


    // starts a new session in the sessions table
    public String startSession(String username) {

        // get 32 byte random number. that's a lot of bits.
        SecureRandom generator = new SecureRandom();
        byte randomBytes[] = new byte[32];
        generator.nextBytes(randomBytes);

        String sessionID = Base64.getEncoder().encodeToString(randomBytes);

        // build the BSON object
        Document session = new Document("username", username)
                           .append("_id", sessionID);

        sessionsCollection.insertOne(session);

        return session.getString("_id");
    }

    // ends the session by deleting it from the sesisons table
    public void endSession(String sessionID) {
        sessionsCollection.deleteOne(eq("_id", sessionID));
    }

    // retrieves the session from the sessions table
    public Document getSession(String sessionID) {
        return sessionsCollection.find(eq("_id", sessionID)).first();
    }
}
