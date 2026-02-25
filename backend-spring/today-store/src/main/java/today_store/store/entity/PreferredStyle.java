package today_store.store.entity;

public enum PreferredStyle {
    PLAIN("plain"),
    CLEAN("clean"),
    FRIENDLY("friendly"),
    MEME("meme");

    private final String value;

    PreferredStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
