package me.ihqqq.rotatingheads.exception;

public final class MessageException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final String[] replacements;

    public MessageException(String key, String... replacements) {
        super(key);
        this.key = key;
        this.replacements = replacements;
    }

    public String key() {
        return key;
    }

    public String[] replacements() {
        return replacements.clone();
    }
}