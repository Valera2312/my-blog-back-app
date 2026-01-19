package ru.valera.application.search;

import org.springframework.stereotype.Component;
import ru.valera.domain.search.PostSearchCriteria;
import ru.valera.domain.search.TagName;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Парсер поисковых запросов
 */
@Component
public class SearchQueryParser {
    
    /**
     * Парсит строку поиска и создает критерии поиска
     * 
     * @param search строка поиска (может содержать обычные слова и теги с префиксом #)
     * @return критерии поиска
     */
    public PostSearchCriteria parse(String search) {
        if (search == null || search.isBlank()) {
            return new PostSearchCriteria(Set.of(), Set.of());
        }
        
        Set<String> titles = extractTitles(search);
        Set<TagName> tags = extractTags(search);
        
        return new PostSearchCriteria(titles, tags);
    }
    
    private Set<String> extractTitles(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> !s.startsWith("#"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
    
    private Set<TagName> extractTags(String search) {
        return Arrays.stream(search.split("\\s+"))
                .filter(s -> s.startsWith("#"))
                .map(s -> s.substring(1))
                .filter(s -> !s.isEmpty())
                .map(TagName::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
