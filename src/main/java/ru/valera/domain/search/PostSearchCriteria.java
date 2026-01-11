package ru.valera.domain.search;

import java.util.Set;

public record PostSearchCriteria(
        Set<String> title,
        Set<TagName> tags) {
}
