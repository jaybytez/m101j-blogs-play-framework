package models;

import play.data.validation.Constraints;

public class Login {
    @Constraints.Required
//    @Constraints.Pattern(value = "^[a-zA-Z0-9_-]{3,20}$")
    public String username;
    @Constraints.Required
//    @Constraints.Pattern(value = "^.{3,20}$")
    public String password;
}
