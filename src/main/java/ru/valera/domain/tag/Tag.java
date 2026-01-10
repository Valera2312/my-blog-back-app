package ru.valera.domain.tag;

public class Tag {

    private final TagId id;
    private final String name;

    public Tag (TagId id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be null or blank");
        }
        this.id = id;
        this.name = name;
    }

    public TagId getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
