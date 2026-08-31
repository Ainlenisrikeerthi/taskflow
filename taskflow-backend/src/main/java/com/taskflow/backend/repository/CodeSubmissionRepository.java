package com.taskflow.backend.repository;
import com.taskflow.backend.model.CodeSubmission; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission,Long>{ List<CodeSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId); List<CodeSubmission> findByTaskIdAndUserIdOrderBySubmittedAtDesc(Long taskId,Long userId); void deleteByTaskId(Long taskId); }
