package com.taskflow.backend.repository;
import com.taskflow.backend.model.CodingTestCase; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CodingTestCaseRepository extends JpaRepository<CodingTestCase,Long>{ List<CodingTestCase> findByTaskIdOrderBySortOrderAsc(Long taskId); void deleteByTaskId(Long taskId); }
