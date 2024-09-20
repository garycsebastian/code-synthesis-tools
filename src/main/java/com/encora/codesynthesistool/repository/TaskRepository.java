package com.encora.codesynthesistool.repository;

import com.encora.codesynthesistool.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskRepository extends ReactiveMongoRepository<Task, String> {

    // You can add custom query methods here if needed
    Flux<Task> findByCompleted(boolean completed);

    Flux<Task> findByUserId(String userId);

    Mono<Task> findByUserIdAndId(String userId, String id);

    Flux<Task> findByUserIdAndCompleted(String userId, Boolean completed);
}
