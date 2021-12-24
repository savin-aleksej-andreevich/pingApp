package ru.bmstu;

public class StoreRequest {
    private final String url;
    private final Long time;

    public StoreRequest(String url, Long time) {
        this.url = url;
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public Long getTime() {
        return time;
    }
}
