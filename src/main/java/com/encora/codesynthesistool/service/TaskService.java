package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.Task;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<Task> getAllTask(
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDirection);

    Mono<Task> findById(String id);

    Mono<Task> save(Task task);

    Mono<Task> update(String id, Task task);

    Mono<Void> deleteById(String id);
}
