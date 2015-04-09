package models;

import play.data.validation.Constraints;

public class Signup extends Login {
    @Constraints.Email
    public String email;
    public String verify;
}
