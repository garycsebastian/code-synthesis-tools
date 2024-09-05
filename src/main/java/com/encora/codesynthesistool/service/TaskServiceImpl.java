package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.repository.TaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public Flux<Task> getAllTask() {
        return taskRepository.findAll();
    }

    @Override
    public Flux<Task> getCompletedTask(Boolean completed) {
        return taskRepository.findByCompleted(completed);
    }

    @Override
    public Mono<Task> findById(String id) {
        return taskRepository.findById(id);
    }

    @Override
    public Mono<Task> save(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return taskRepository.deleteById(id);
    }
}
