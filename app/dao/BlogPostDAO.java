package dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BlogPostDAO extends BaseDAO {
    MongoCollection<Document> postsCollection;

    public BlogPostDAO() {
        postsCollection = getCollection("posts");
    }

    // Return a single post corresponding to a permalink
    public Document findByPermalink(String permalink) {
    	return postsCollection.find(Filters.eq("permalink", permalink)).limit(1).first();
    }

    // Return a list of posts in descending order. Limit determines
    // how many posts are returned.
    public List<Document> findByDateDescending(int limit) {

        // Return a list of DBObjects, each one a post from the posts collection
        List<Document> posts = new ArrayList<>();
        postsCollection.find().sort(new BasicDBObject("date",-1)).limit(limit).iterator().forEachRemaining(doc -> {
        	posts.add(doc);
        });
        return posts;
    }


    public String addPost(String title, String body, List tags, String username) {

        String permalink = title.replaceAll("\\s", "_"); // whitespace becomes _
        permalink = permalink.replaceAll("\\W", ""); // get rid of non alphanumeric
        permalink = permalink.toLowerCase();

        // Remember that a valid post has the following keys:
        // author, body, permalink, tags, comments, date
        //
        // A few hints:
        // - Don't forget to create an empty list of comments
        // - for the value of the date key, today's datetime is fine.
        // - tags are already in list form that implements suitable interface.
        // - we created the permalink for you above.

        // Build the post object and insert it
		Document post = new Document();
		post.append("author", username).append("title", title)
				.append("body", body).append("permalink", permalink)
				.append("tags", tags).append("date", new Date())
				.append("comments", new ArrayList<Object>());
		postsCollection.insertOne(post);

        return permalink;
    }

    // Append a comment to a blog post
    public void addPostComment(final String name, final String email, final String body,
                               final String permalink) {

        // Hints:
        // - email is optional and may come in NULL. Check for that.
        // - best solution uses an update command to the database and a suitable
        //   operator to append the comment on to any existing list of comments
		BasicDBObject comment = new BasicDBObject().append("author", name).append("body", body);
		if (email != null) {
			comment.append("email", email);
		}
		postsCollection.updateOne(new BasicDBObject("permalink", permalink),
				new BasicDBObject("$push", new BasicDBObject("comments", comment)));
    }
}
