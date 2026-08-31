package com.taskflow.backend.service;

import com.taskflow.backend.dto.coding.*;
import com.taskflow.backend.exception.*;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Service
public class CodingSubmissionService {
    private static final String USER_NOT_FOUND = "User not found";
    private final TaskRepository tasks;
    private final UserRepository users;
    private final CodingTestCaseRepository tests;
    private final CodeSubmissionRepository subs;
    private final DsaAssessmentAgent agent;
    private final AssignmentRepository assignments;

    public CodingSubmissionService(TaskRepository t, UserRepository u, CodingTestCaseRepository tc,
            CodeSubmissionRepository s, DsaAssessmentAgent a,
            AssignmentRepository assignments) {
        tasks = t;
        users = u;
        tests = tc;
        subs = s;
        agent = a;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public RunCodeResponse runVisible(Long taskId, String email, CodeSubmissionRequest req) {
        getCodingTask(taskId);
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        requireActiveAssignment(user.getId(), taskId);
        if (req == null || req.code == null || req.code.isBlank())
            throw new BadRequestException("Write some code before running tests");

        List<CodingTestCase> visible = tests.findByTaskIdOrderBySortOrderAsc(taskId).stream()
                .filter(tc -> !tc.isHidden()).toList();
        if (visible.isEmpty())
            throw new BadRequestException("No visible test cases configured by admin");

        return agent.runVisible(req.language, req.code, visible);
    }

    @Transactional
    public SubmissionResponse submit(Long taskId, String email, CodeSubmissionRequest req) {
        Task task = getCodingTask(taskId);
        User user = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        Assignment assignment = requireActiveAssignment(user.getId(), taskId);
        if (req == null || req.code == null || req.code.isBlank())
            throw new BadRequestException("Write some code before submitting");

        List<CodingTestCase> cases = tests.findByTaskIdOrderBySortOrderAsc(taskId);
        if (cases.isEmpty())
            throw new BadRequestException("No test cases configured by admin");

        var evaluation = agent.evaluate(task.getTitle(), req.language, req.code, cases);
        int passed = evaluation.passed();

        CodeSubmission s = new CodeSubmission();
        s.setTask(task);
        s.setUser(user);
        s.setLanguage(req.language);
        s.setCode(req.code);
        s.setPassedTests(passed);
        s.setTotalTests(cases.size());
        s.setScore(evaluation.score());
        s.setCorrectnessScore(evaluation.correctness());
        s.setEfficiencyScore(evaluation.efficiency());
        s.setQualityScore(evaluation.quality());
        s.setTimeComplexity(evaluation.timeComplexity());
        s.setSpaceComplexity(evaluation.spaceComplexity());
        s.setFeedback(evaluation.feedback());
        CodeSubmission saved = subs.save(s);

        // Coding submissions replace proof-URL completion. A successful submission
        // marks the assignment completed.
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setSubmittedAt(LocalDateTime.now(java.time.ZoneId.systemDefault()));
        assignment.setProofUrl(null);
        assignments.save(assignment);

        return map(saved);
    }

    public List<SubmissionResponse> mine(String email) {
        User u = users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return subs.findByUserIdOrderBySubmittedAtDesc(u.getId()).stream().map(this::map).toList();
    }

    public List<LeaderboardEntry> leaderboard(int limit) {
        Map<Long, List<CodeSubmission>> by = subs.findAll().stream()
                .collect(Collectors.groupingBy(x -> x.getUser().getId()));
        List<LeaderboardEntry> list = new ArrayList<>();
        for (var e : by.entrySet()) {
            var attempts = e.getValue();
            Map<Long, Double> best = new HashMap<>();
            for (var s : attempts)
                best.merge(s.getTask().getId(), s.getScore(), Math::max);
            LeaderboardEntry x = new LeaderboardEntry();
            x.userId = e.getKey();
            x.userName = attempts.get(0).getUser().getName();
            x.totalScore = Math.round(best.values().stream().mapToDouble(Double::doubleValue).sum() * 10) / 10.0;
            x.solved = best.values().stream().filter(v -> v > 0).count();
            x.averageScore = best.isEmpty() ? 0 : Math.round(x.totalScore / best.size() * 10) / 10.0;
            list.add(x);
        }
        list.sort(Comparator.comparingDouble((LeaderboardEntry x) -> x.totalScore).reversed());
        for (int i = 0; i < list.size(); i++)
            list.get(i).rank = i + 1;
        return list.stream().limit(Math.max(1, Math.min(limit, 50))).toList();
    }

    private Task getCodingTask(Long taskId) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.getTaskType() != TaskType.CODING)
            throw new BadRequestException("This is not a coding task");
        return task;
    }

    private Assignment requireActiveAssignment(Long userId, Long taskId) {
        return assignments.findByUserIdAndIsActiveTrue(userId).stream()
                .filter(a -> a.getTask() != null && Objects.equals(a.getTask().getId(), taskId))
                .findFirst()
                .orElseThrow(
                        () -> new BadRequestException("Assign this coding task before running or submitting code"));
    }

    private SubmissionResponse map(CodeSubmission s) {
        SubmissionResponse r = new SubmissionResponse();
        r.id = s.getId();
        r.taskId = s.getTask().getId();
        r.taskTitle = s.getTask().getTitle();
        r.userName = s.getUser().getName();
        r.language = s.getLanguage();
        r.score = s.getScore();
        r.correctnessScore = s.getCorrectnessScore();
        r.efficiencyScore = s.getEfficiencyScore();
        r.qualityScore = s.getQualityScore();
        r.passedTests = s.getPassedTests();
        r.totalTests = s.getTotalTests();
        r.feedback = s.getFeedback();
        r.timeComplexity = s.getTimeComplexity();
        r.spaceComplexity = s.getSpaceComplexity();
        r.submittedAt = s.getSubmittedAt();
        return r;
    }
}
