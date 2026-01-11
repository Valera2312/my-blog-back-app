package ru.valera.domain.Image;

public class Image {
    private final String url;

    private Image(String url) {
        this.url = url;
    }

    public static Image create(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Image URL cannot be null or blank");
        }
        return new Image(url);
    }
    public String getUrl() {
        return url;
    }
}
