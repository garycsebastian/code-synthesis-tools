package com.encora.codesynthesistool.controller;

import static com.encora.codesynthesistool.util.Utils.isValidStatus;
import static com.encora.codesynthesistool.util.Utils.sanitizeDescription;
import static com.encora.codesynthesistool.util.Utils.sanitizeTitle;

import com.encora.codesynthesistool.model.Task;
import com.encora.codesynthesistool.model.TaskStatus;
import com.encora.codesynthesistool.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
@AllArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Get all tasks",
            description = "Retrieve a list of tasks with optional filtering and sorting.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "List of tasks retrieved successfully",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Task.class))
                        }),
                @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content)
            })
    @GetMapping
    public Flux<Task> getAllTasks(
            @Parameter(description = "Filter tasks by status")
                    @RequestParam(value = "status", required = false)
                    String status,
            @Parameter(
                            description = "Filter tasks from this date (inclusive)",
                            example = "yyyy-MM-dd")
                    @RequestParam(value = "fromDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fromDate,
            @Parameter(
                            description = "Filter tasks to this date (inclusive)",
                            example = "yyyy-MM-dd")
                    @RequestParam(value = "toDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate toDate,
            @Parameter(
                            description = "Sort tasks by this field (default: dueDate)",
                            example = "dueDate")
                    @RequestParam(value = "sortBy", defaultValue = "dueDate")
                    String sortBy,
            @Parameter(description = "Sort direction (asc or desc, default: asc)", example = "asc")
                    @RequestParam(value = "sortDirection", defaultValue = "asc")
                    String sortDirection) {
        return taskService.getAllTask(status, fromDate, toDate, sortBy, sortDirection);
    }

    @Operation(summary = "Get a task by ID", description = "Returns a task based on its ID.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Task found",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Task.class))
                        }),
                @ApiResponse(
                        responseCode = "404",
                        description = "Task not found",
                        content = @Content)
            })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Task>> getTaskById(@PathVariable String id) {
        return taskService
                .findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create a new task",
            description = "Creates a new task with the provided details.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Task created successfully",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Task.class))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid task status provided or bad request",
                        content = @Content)
            })
    @PostMapping
    public Mono<ResponseEntity<Task>> createTask(@Validated @RequestBody Task task) {
        // Validate TaskStatus during creation
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING); // Default to PENDING if not provided
        } else if (!isValidStatus(task.getStatus())) {
            return Mono.error(new IllegalArgumentException("Invalid task status provided."));
        }

        // Sanitize input
        sanitizeTaskInput(task);

        return taskService
                .save(task)
                .map(createdTask -> ResponseEntity.status(HttpStatus.CREATED).body(createdTask));
    }

    @Operation(
            summary = "Update a task",
            description = "Updates an existing task with the provided details.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Task updated successfully",
                        content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = Task.class))
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid task status provided or bad request",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Task not found",
                        content = @Content)
            })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Task>> updateTask(@PathVariable String id, @RequestBody Task task) {
        // You might want to validate the provided status here as well
        if (task.getStatus() != null && !isValidStatus(task.getStatus())) {
            return Mono.error(new IllegalArgumentException("Invalid task status provided."));
        }

        // Sanitize input
        sanitizeTaskInput(task);

        return taskService
                .update(id, task)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a task", description = "Deletes a task based on its ID.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Task deleted successfully",
                        content = @Content),
                @ApiResponse(
                        responseCode = "404",
                        description = "Task not found",
                        content = @Content)
            })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable String id) {
        return taskService
                .deleteById(id)
                .thenReturn(
                        ResponseEntity.status(HttpStatus.NO_CONTENT)
                                .<Void>build()) // 204 No Content on success
                .defaultIfEmpty(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .build()); // Explicit 404 with <Void>;
    }

    private void sanitizeTaskInput(Task task) {
        // Input Validation for XSS (same as in createTask)
        if (task.getDescription() != null) {
            task.setDescription(sanitizeDescription(task.getDescription()));
        }
        if (task.getTitle() != null) {
            task.setTitle(sanitizeTitle(task.getTitle()));
        }
    }
}
