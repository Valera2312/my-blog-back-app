package ru.valera.application.pagination;

import org.springframework.stereotype.Component;
import ru.valera.application.dto.PageResultDto;
import ru.valera.domain.search.PageRequest;

import java.util.List;

/**
 * Сервис для работы с пагинацией
 */
@Component
public class PaginationService {
    
    /**
     * Создает результат пагинации
     * 
     * @param items элементы текущей страницы
     * @param total общее количество элементов
     * @param page запрос пагинации
     * @param <T> тип элементов
     * @return результат пагинации
     */
    public <T> PageResultDto<T> createPageResult(List<T> items, long total, PageRequest page) {
        long lastPage = calculateLastPage(total, page.size());
        boolean hasPrev = calculateHasPrev(page.page(), lastPage);
        boolean hasNext = calculateHasNext(page.page(), lastPage);
        
        return PageResultDto.<T>builder()
                .posts(items)
                .lastPage(lastPage)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .build();
    }
    
    private long calculateLastPage(long total, int pageSize) {
        if (total == 0) {
            return 1;
        }
        return (long) Math.ceil((double) total / pageSize);
    }
    
    private boolean calculateHasPrev(int currentPage, long lastPage) {
        return currentPage > 1 && currentPage <= lastPage;
    }
    
    private boolean calculateHasNext(int currentPage, long lastPage) {
        return currentPage < lastPage;
    }
}
