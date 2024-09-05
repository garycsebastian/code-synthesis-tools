package com.encora.codesynthesistool.repository;

import com.encora.codesynthesistool.model.Task;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

public interface TaskRepository extends ReactiveMongoRepository<Task, String> {

    // You can add custom query methods here if needed
    Flux<Task> findByCompleted(boolean completed);
}
