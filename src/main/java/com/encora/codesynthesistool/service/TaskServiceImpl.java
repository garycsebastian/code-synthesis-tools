package com.encora.codesynthesistool.service;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.model.TaskStatus;
import com.encora.codesynthesistool.model.User;
import com.encora.codesynthesistool.repository.TaskRepository;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
    public Flux<Task> getAllTask(
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        return ReactiveSecurityContextHolder.getContext()
                .doOnNext(ctx -> log.info("SecurityContext: {}", ctx))
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMapMany(
                        user -> {
                            try {
                                if (status != null && (fromDate != null || toDate != null)) {
                                    // Query by both status and date range
                                    TaskStatus taskStatus =
                                            TaskStatus.valueOf(status.toUpperCase());
                                    return taskRepository.findByUserIdAndStatusAndDueDateBetween(
                                            user.getId(), taskStatus, fromDate, toDate, sort);
                                } else if (status != null) {
                                    // Query by status only
                                    TaskStatus taskStatus =
                                            TaskStatus.valueOf(status.toUpperCase());
                                    return taskRepository.findByUserIdAndStatus(
                                            user.getId(), taskStatus, sort);
                                } else if (fromDate != null || toDate != null) {
                                    // Query by date range only
                                    return taskRepository.findByUserIdAndDueDateBetween(
                                            user.getId(), fromDate, toDate, sort);
                                } else {
                                    // No filters, get all tasks for the user
                                    return taskRepository.findByUserId(user.getId(), sort);
                                }
                            } catch (IllegalArgumentException e) {
                                return Flux.error(
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Invalid task status: " + status));
                            }
                        })
                .switchIfEmpty(Flux.empty());
    }

    @Override
    public Mono<Task> findById(String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMap(user -> taskRepository.findByUserIdAndId(user.getId(), id))
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Task not found")));
    }

    @Override
    public Mono<Task> save(Task task) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(User.class) // Assuming your UserDetails implements User
                .flatMap(
                        user -> {
                            if (task.getDueDate() == null) {
                                task.setDueDate(LocalDate.now()); // Set only if dueDate is null
                            }
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
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Task not found")))
                .flatMap(
                        existingTask -> {

                            // Update fields only if provided in the request
                            if (task.getDescription() != null) {
                                existingTask.setDescription(task.getDescription());
                            }
                            if (task.getStatus() != null) {
                                existingTask.setStatus(task.getStatus());
                            }
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
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Task not found")))
                .flatMap(task -> taskRepository.deleteById(task.getId()));
    }
}
