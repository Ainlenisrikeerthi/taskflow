package com.taskflow.backend.repository;

import com.taskflow.backend.model.Assignment;
import com.taskflow.backend.model.AssignmentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AssignmentSpecification {

    public static Specification<Assignment> filterAdminAssignments(String searchTerm, AssignmentStatus status, Long taskId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
                Predicate userPredicate = cb.like(cb.lower(root.get("user").get("name")), pattern);
                Predicate emailPredicate = cb.like(cb.lower(root.get("user").get("email")), pattern);
                Predicate taskPredicate = cb.like(cb.lower(root.get("task").get("title")), pattern);
                predicates.add(cb.or(userPredicate, emailPredicate, taskPredicate));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (taskId != null) {
                predicates.add(cb.equal(root.get("task").get("id"), taskId));
            }

            query.orderBy(cb.desc(root.get("assignedAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
