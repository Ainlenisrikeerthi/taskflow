package com.taskflow.backend.repository;

import com.taskflow.backend.model.Assignment;
import com.taskflow.backend.model.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

    @Query("SELECT a FROM Assignment a WHERE a.user.id = :userId AND a.isActive = true")
    List<Assignment> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);

    @Query("SELECT a FROM Assignment a WHERE a.user.id = :userId ORDER BY a.assignedAt DESC")
    List<Assignment> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Assignment a WHERE a.task.id = :taskId ORDER BY a.assignedAt DESC")
    List<Assignment> findByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT a FROM Assignment a WHERE a.task.id = :taskId AND a.isActive = true")
    List<Assignment> findByTaskIdAndIsActiveTrue(@Param("taskId") Long taskId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Assignment a WHERE a.user.id = :userId AND a.task.id = :taskId AND a.isActive = true")
    boolean existsByUserIdAndTaskIdAndIsActiveTrue(@Param("userId") Long userId, @Param("taskId") Long taskId);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.task.id = :taskId AND a.isActive = true")
    long countByTaskIdAndIsActiveTrue(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.task.id = :taskId AND a.status = :status AND a.isActive = true")
    long countByTaskIdAndStatusAndIsActiveTrue(@Param("taskId") Long taskId, @Param("status") AssignmentStatus status);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.status = :status AND a.isActive = true")
    long countByStatusAndIsActiveTrue(@Param("status") AssignmentStatus status);

    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.isActive = true")
    long countByIsActiveTrue();

    List<Assignment> findTop10ByOrderByAssignedAtDesc();

    @Query("SELECT a FROM Assignment a JOIN FETCH a.task JOIN FETCH a.user WHERE a.isActive = true")
    List<Assignment> findAllActiveWithTaskAndUser();
}
