package dao;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bson.Document;

/**
 * Created by jblanton on 4/7/2015.
 */
public abstract class BaseDAO {

    public MongoCollection<Document> getCollection(String collName) {
        Config conf = ConfigFactory.load();

        final MongoClient mongoClient = new MongoClient(new MongoClientURI(conf.getString("mongodb.url")));
        final MongoDatabase baseDatabase = mongoClient.getDatabase(conf.getString("mongodb.database"));
        return baseDatabase.getCollection(collName);
    }
}
