package ru.valera.application.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record PageResultDto<T>(List<T> posts,
                               boolean hasPrev,
                               boolean hasNext,
                               long lastPage) {

}
