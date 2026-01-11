package ru.valera.domain.search;

public record PageRequest(
        int page,
        int size) {

    public int offset() {
        return page * size;
    }
}
