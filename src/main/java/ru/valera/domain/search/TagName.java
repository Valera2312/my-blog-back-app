package ru.valera.domain.search;

public record TagName(String value) {
    public TagName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be null or blank");
        }
    }
}
