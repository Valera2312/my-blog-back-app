package ru.valera.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.valera.domain.repository.PostQueryRepository;
import ru.valera.domain.repository.PostRepository;


@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostQueryRepository postQueryRepository;
}
