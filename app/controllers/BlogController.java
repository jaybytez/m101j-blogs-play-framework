package controllers;

import dao.BlogPostDAO;
import dao.SessionDAO;
import dao.UserDAO;
import models.Comment;
import models.Login;
import models.Post;
import models.Signup;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bson.Document;
import play.Logger;
import play.api.data.validation.ValidationError;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static play.data.Form.form;

/**
 * This class encapsulates the controllers for the blog web application.  It delegates all interaction with MongoDB
 * to three Data Access Objects (DAOs).
 * <p>
 * It is also the entry point into the web application.
 */
public class BlogController extends Controller {

    public static Result posts() {
        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        BlogPostDAO blogPostDAO = new BlogPostDAO();
        List<Document> posts = blogPostDAO.findByDateDescending(10);
        return ok(blog_template.render(posts, username));
    }

    public static Result welcome() {
        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        if (username == null) {
            Logger.debug("welcome() can't identify the user, redirecting to signup");
            redirect(routes.BlogController.signup());
        }
        return ok(welcome.render(username));
    }

    public static Result post(String permalink) {
        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        BlogPostDAO blogPostDAO = new BlogPostDAO();
        Document post = blogPostDAO.findByPermalink(permalink);
        if (post == null) {
            return ok(post_not_found.render());
        }
        return ok(entry_template.render(post, username, form(Comment.class)));
    }

    public static Result signup() {
        return ok(signup.render(form(Signup.class)));
    }

    public static Result postNotFound() {
        return ok(post_not_found.render());
    }

    public static Result internalError() {
        return ok(error_template.render("System has encountered an error."));
    }

    public static Result logout() {
        SessionDAO sessionDAO = new SessionDAO();
        String sessionID = getSessionCookie(request());
        if (sessionID != null) {
            sessionDAO.endSession(sessionID);
            response().discardCookie("session");
        }
        return redirect(routes.BlogController.login());
    }

    public static Result login() {
        return ok(login.render(form(Login.class)));
    }

    public static Result signupSubmit() {
        Form<Signup> signupForm = form(Signup.class).bindFromRequest();
        if(signupForm.hasErrors()) {
            return badRequest(signup.render(signupForm));
        }
        UserDAO userDAO = new UserDAO();
        if(!userDAO.addUser(signupForm.get().username, signupForm.get().password, signupForm.get().email)) {
            signupForm.reject("username", "Username already in use, Please choose another");
            return badRequest(signup.render(signupForm));
        }
        SessionDAO sessionDAO = new SessionDAO();
        String sessionID = sessionDAO.startSession(signupForm.get().username);
        response().setCookie("session", sessionID, 3600);
        return redirect(routes.BlogController.welcome());
    }

    public static Result newpost() {
        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        if (username == null) {
            Logger.debug("welcome() can't identify the user, redirecting to signup");
            redirect(routes.BlogController.login());
        }
        return ok(newpost_template.render(username, form(Post.class)));
    }

    public static Result newpostSubmit() {
        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        if (username == null) {
            Logger.debug("welcome() can't identify the user, redirecting to signup");
            redirect(routes.BlogController.login());
        }
        Form<Post> postForm = form(Post.class).bindFromRequest();
        if(postForm.hasErrors()) {
            return badRequest(newpost_template.render(username, postForm));
        }
        Post post = postForm.get();
        // extract tags
        ArrayList<String> tagsArray = extractTags(post.tags);

        // substitute some <p> for the paragraph breaks
        post.body = post.body.replaceAll("\\r?\\n", "<p>");

        BlogPostDAO blogPostDAO = new BlogPostDAO();
        String permalink = blogPostDAO.addPost(post.subject, post.body, tagsArray, username);
        return redirect(routes.BlogController.post(permalink));
    }


    public static Result newcomment() {
        BlogPostDAO blogPostDAO = new BlogPostDAO();
        Form<Comment> commentForm = form(Comment.class).bindFromRequest();
        Document post = blogPostDAO.findByPermalink(commentForm.data().get("permalink"));
        if(post == null) {
            redirect(routes.BlogController.postNotFound());
        }

        SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));
        if(commentForm.hasErrors()) {
            return badRequest(entry_template.render(post, username, commentForm));
        }
        Comment comment = commentForm.get();
        blogPostDAO.addPostComment(comment.name, comment.email, comment.body, comment.permalink);
        return redirect(routes.BlogController.post(comment.permalink));
    }


    public static Result loginSubmit() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();
        if(loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        }
        Logger.debug("Login: User submitted: {} {}", loginForm.get().username, loginForm.get().password);
        UserDAO userDAO = new UserDAO();
        Document user = userDAO.validateLogin(loginForm.get().username, loginForm.get().password);
        if(user == null) {
            loginForm.reject("username", "Invalid Login");
            return badRequest(login.render(loginForm));
        }
        SessionDAO sessionDAO = new SessionDAO();

        // valid user, let's log them in
        String sessionID = sessionDAO.startSession(user.get("_id").toString());
        if (sessionID == null) {
            return redirect(routes.BlogController.internalError());
        }
        response().setCookie("session", sessionID, 3600);
        return redirect(routes.BlogController.welcome());
    }
    
    
    public static Result tag(String tag) {
    	SessionDAO sessionDAO = new SessionDAO();
        String username = sessionDAO.findUserNameBySessionId(getSessionCookie(request()));

        BlogPostDAO blogPostDAO = new BlogPostDAO();
        List<Document> posts = blogPostDAO.findByTagDateDescending(tag);
        return ok(blog_template.render(posts, username));
    }

    // tags the tags string and put it into an array
    private static ArrayList<String> extractTags(String tags) {

        // probably more efficent ways to do this.
        //
        // whitespace = re.compile('\s')

        tags = tags.replaceAll("\\s", "");
        String tagArray[] = tags.split(",");

        // let's clean it up, removing the empty string and removing dups
        ArrayList<String> cleaned = new ArrayList<String>();
        for (String tag : tagArray) {
            if (!tag.equals("") && !cleaned.contains(tag)) {
                cleaned.add(tag);
            }
        }

        return cleaned;
    }

    // helper function to get session cookie as string
    private static String getSessionCookie(Http.Request request) {
        if (request.cookies() == null) {
            return null;
        }
        Iterator<Http.Cookie> cookieIterator = request.cookies().iterator();
        while (cookieIterator.hasNext()) {
            Http.Cookie cookie = cookieIterator.next();
            if (cookie.name().equals("session")) {
                return cookie.value();
            }
        }
        return null;
    }
}
