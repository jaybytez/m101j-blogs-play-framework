package models;

import play.data.validation.Constraints;

public class Post {
    @Constraints.Required
    public String subject;
    public String tags;
    @Constraints.Required
    public String body;
}
