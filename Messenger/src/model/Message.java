package model;

public class Message implements Savable {
    private String from;
    private String to;
    private String content;

    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
    }

    @Override
    public String toFileString() {
        return from + ";" + to + ";" + content;
    }

    @Override
    public String toString() {
        return from + ";" + to + ";" + content;
    }
}
