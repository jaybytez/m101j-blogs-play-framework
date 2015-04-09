package models;

import play.data.validation.Constraints;

public class Comment {
    @Constraints.Required
    public String name;
    @Constraints.Email
    public String email;
    public String body;
    public String permalink;
}
