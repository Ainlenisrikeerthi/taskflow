package com.taskflow.backend.repository;

import com.taskflow.backend.model.Task;
import com.taskflow.backend.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    Optional<Task> findByIdAndStatus(Long id, TaskStatus status);
    long countByStatus(TaskStatus status);
}
