package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.repository.TaskRepository;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskService {
    Flux<Task> getAllTask();
    Flux<Task> getCompletedTask(Boolean completed);
    Mono<Task> findById(String id);
    Mono<Task> save(Task task);
    Mono<Void> deleteById(String id);
}
