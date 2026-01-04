package model;

public abstract class AbstractUser {
    protected String username;
    protected String password;

    public AbstractUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    // metoda abstrakcyjna
    public abstract String getRole();
}
