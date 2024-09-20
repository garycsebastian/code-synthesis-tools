package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.TaskRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public Flux<Task> getAllTask() {
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(ctx -> log.info("SecurityContext: {}", ctx))
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMapMany(user -> taskRepository.findByUserId(user.getId()));
    }

    @Override
    public Flux<Task> getCompletedTask(Boolean completed) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMapMany(user -> taskRepository.findByUserIdAndCompleted(user.getId(), completed));
    }

    @Override
    public Mono<Task> findById(String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMap(user -> taskRepository.findByUserIdAndId(user.getId(), id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found")));
    }

    @Override
    public Mono<Task> save(Task task) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMap(user -> {
                    task.setUserId(user.getId()); // Set the task's userId
                    return taskRepository.save(task); // Save the task
                });
    }

    @Override
    public Mono<Task> update(String id, Task task) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMap(user -> taskRepository.findByUserIdAndId(user.getId(), id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized")))
                .flatMap(existingTask -> {
                    // Update only modifiable fields
                    existingTask.setDescription(task.getDescription());
                    existingTask.setCompleted(task.isCompleted());
                    // ... update other allowed fields ...
                    return taskRepository.save(existingTask);
                });

    }

    @Override
    public Mono<Void> deleteById(String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class)
                .flatMap(user -> taskRepository.findByUserIdAndId(user.getId(), id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized")))
                .flatMap(task -> taskRepository.deleteById(task.getId()));
    }
}
