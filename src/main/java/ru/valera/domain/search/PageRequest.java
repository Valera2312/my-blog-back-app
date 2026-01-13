package ru.valera.domain.search;

public record PageRequest(
        int page,
        int size) {

    public static PageRequest of(int page, int size) {
        if (page < 1) throw new IllegalArgumentException("page must be more or equal 1");
        if (size <= 0) throw new IllegalArgumentException("size is negative or zero");
        return new PageRequest(page, size);
    }
    public int offset() {
        return (page - 1) * size;
    }
}
