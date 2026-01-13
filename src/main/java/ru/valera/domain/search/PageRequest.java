package ru.valera.domain.search;

public record PageRequest(
        int page,
        int size) {

    public static PageRequest of(int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page is negative");
        if (size <= 0) throw new IllegalArgumentException("size is negative or zero");
        return new PageRequest(page, size);
    }
    public int offset() {
        return page * size;
    }
}
