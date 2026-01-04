package model;

public class User extends AbstractUser implements Savable {

    public User(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "USER";
    }

    @Override
    public String toFileString() {
        return username + ";" + password;
    }

    public boolean checkPassword(String pass) {
        return password.equals(pass);
    }
}
