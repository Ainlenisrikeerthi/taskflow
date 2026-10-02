package com.taskflow.backend.dto;

import java.util.List;

public class DashboardStatsResponse {
    private long totalTasks;
    private long totalUsers;
    private long totalAssignments;
    private long completedAssignments;
    private long inProgressAssignments;
    private long notStartedAssignments;
    private List<AssignmentResponse> recentAssignments;

    public DashboardStatsResponse() {}

    public DashboardStatsResponse(long totalTasks, long totalUsers, long totalAssignments,
                                  long completedAssignments, long inProgressAssignments,
                                  long notStartedAssignments, List<AssignmentResponse> recentAssignments) {
        this.totalTasks = totalTasks;
        this.totalUsers = totalUsers;
        this.totalAssignments = totalAssignments;
        this.completedAssignments = completedAssignments;
        this.inProgressAssignments = inProgressAssignments;
        this.notStartedAssignments = notStartedAssignments;
        this.recentAssignments = recentAssignments;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalAssignments() {
        return totalAssignments;
    }

    public void setTotalAssignments(long totalAssignments) {
        this.totalAssignments = totalAssignments;
    }

    public long getCompletedAssignments() {
        return completedAssignments;
    }

    public void setCompletedAssignments(long completedAssignments) {
        this.completedAssignments = completedAssignments;
    }

    public long getInProgressAssignments() {
        return inProgressAssignments;
    }

    public void setInProgressAssignments(long inProgressAssignments) {
        this.inProgressAssignments = inProgressAssignments;
    }

    public long getNotStartedAssignments() {
        return notStartedAssignments;
    }

    public void setNotStartedAssignments(long notStartedAssignments) {
        this.notStartedAssignments = notStartedAssignments;
    }

    public List<AssignmentResponse> getRecentAssignments() {
        return recentAssignments;
    }

    public void setRecentAssignments(List<AssignmentResponse> recentAssignments) {
        this.recentAssignments = recentAssignments;
    }
}
