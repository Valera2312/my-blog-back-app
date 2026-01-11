package ru.valera.application.dto;

import java.util.List;

public record PageResultDto<T>(List<T> items,
                               boolean hasPrev,
                               boolean hasNext,
                               int lastPage) {
}
