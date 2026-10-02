package com.taskflow.backend.controller;

import com.taskflow.backend.dto.TaskRequest;
import com.taskflow.backend.dto.TaskResponse;
import com.taskflow.backend.dto.coding.*;
import com.taskflow.backend.exception.BadRequestException;
import com.taskflow.backend.model.*;
import com.taskflow.backend.repository.*;
import com.taskflow.backend.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/coding")
public class CodingController {
    private final DsaAssessmentAgent agent;
    private final CodingSubmissionService submissions;
    private final CodingTestCaseRepository tests;
    private final TaskRepository tasks;
    private final TaskService taskService;

    public CodingController(DsaAssessmentAgent a,CodingSubmissionService s,CodingTestCaseRepository t,TaskRepository tr,TaskService taskService){
        agent=a; submissions=s; tests=t; tasks=tr; this.taskService=taskService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public GeneratedCodingTaskResponse generate(@RequestBody GenerateCodingTaskRequest r){return agent.generateDraft(r);}

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaskResponse saveCodingTask(@RequestBody CodingTaskSaveRequest r, Principal principal) {
        validateCodingTask(r);
        TaskRequest task = new TaskRequest();
        task.setTitle(r.title);
        task.setDescription(r.description);
        task.setInstructions(r.instructions);
        task.setDeadline(r.deadline);
        task.setProofRequirement("Code submission required");
        task.setStatus(r.status == null ? "DRAFT" : r.status);
        task.setTaskType("CODING");
        task.setDifficulty(r.difficulty);
        task.setStarterCode(r.starterCode);

        TaskResponse saved = taskService.createTask(task, principal.getName());
        saveTestsInternal(saved.getId(), r.testCases);
        return saved;
    }

    @GetMapping("/tasks/{taskId}/tests")
    public List<TestCaseDto> visible(@PathVariable Long taskId,Principal p){
        return tests.findByTaskIdOrderBySortOrderAsc(taskId).stream().filter(t->!t.isHidden())
                .map(t->new TestCaseDto(t.getId(),t.getInput(),t.getExpectedOutput(),false)).toList();
    }

    @PutMapping("/tasks/{taskId}/tests")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<TestCaseDto> saveTests(@PathVariable Long taskId,@RequestBody List<TestCaseDto> dto){
        saveTestsInternal(taskId, dto);
        return tests.findByTaskIdOrderBySortOrderAsc(taskId).stream()
                .map(t->new TestCaseDto(t.getId(),t.getInput(),t.getExpectedOutput(),t.isHidden())).toList();
    }

    @PostMapping("/tasks/{taskId}/run")
    @PreAuthorize("hasRole('USER')")
    public RunCodeResponse run(@PathVariable Long taskId,@RequestBody CodeSubmissionRequest r,Principal p){
        return submissions.runVisible(taskId,p.getName(),r);
    }

    @PostMapping("/tasks/{taskId}/submit")
    @PreAuthorize("hasRole('USER')")
    public SubmissionResponse submit(@PathVariable Long taskId,@RequestBody CodeSubmissionRequest r,Principal p){
        return submissions.submit(taskId,p.getName(),r);
    }

    @GetMapping("/submissions/me")
    @PreAuthorize("hasRole('USER')")
    public List<SubmissionResponse> mine(Principal p){return submissions.mine(p.getName());}

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(@RequestParam(defaultValue="20") int limit){return submissions.leaderboard(limit);}

    private void validateCodingTask(CodingTaskSaveRequest r) {
        if (r == null) throw new BadRequestException("Coding task data is required");
        if (r.title == null || r.title.isBlank()) throw new BadRequestException("Problem title is required");
        if (r.description == null || r.description.isBlank()) throw new BadRequestException("Problem description is required");
        if (r.deadline == null) throw new BadRequestException("Deadline is required");
        if (r.testCases == null || r.testCases.isEmpty()) throw new BadRequestException("Add at least one test case before saving");
        for (int i=0; i<r.testCases.size(); i++) {
            TestCaseDto tc = r.testCases.get(i);
            if (tc.input == null || tc.input.isBlank() || tc.expectedOutput == null || tc.expectedOutput.isBlank()) {
                throw new BadRequestException("Test case " + (i+1) + " needs both input and expected output");
            }
            String in = tc.input.trim().toLowerCase();
            String out = tc.expectedOutput.trim().toLowerCase();
            if (in.startsWith("edit_") || out.startsWith("edit_") || in.contains("sample input") || out.contains("sample output")) {
                throw new BadRequestException("Test case " + (i+1) + " still contains a placeholder. Review the generated cases before saving or publishing.");
            }
        }
    }

    private void saveTestsInternal(Long taskId, List<TestCaseDto> dto) {
        Task task=tasks.findById(taskId).orElseThrow();
        tests.deleteByTaskId(taskId);
        int i=0;
        for(TestCaseDto d:dto){
            CodingTestCase t=new CodingTestCase();
            t.setTask(task); t.setInput(d.input); t.setExpectedOutput(d.expectedOutput); t.setHidden(d.hidden); t.setSortOrder(i++);
            tests.save(t);
        }
    }
}
