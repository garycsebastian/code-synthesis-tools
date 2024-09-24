package com.encora.codesynthesistool.repository;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.model.TaskStatus;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskRepository extends ReactiveMongoRepository<Task, String> {

    Flux<Task> findByUserId(String userId, Sort sort);

    Mono<Task> findByUserIdAndId(String userId, String id);

    Flux<Task> findByUserIdAndStatus(String userId, TaskStatus status);

    // Filtering and Sorting by status and dueDate
    Flux<Task> findByUserIdAndStatusAndDueDateBetween(
            String userId, TaskStatus status, LocalDate fromDate, LocalDate toDate, Sort sort);

    Flux<Task> findByUserIdAndStatus(String userId, TaskStatus status, Sort sort);

    Flux<Task> findByUserIdAndDueDateBetween(
            String userId, LocalDate fromDate, LocalDate toDate, Sort sort);
}
