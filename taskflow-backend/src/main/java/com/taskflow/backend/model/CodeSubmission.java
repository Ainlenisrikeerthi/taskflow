package com.taskflow.backend.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="code_submissions")
public class CodeSubmission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="task_id",nullable=false) private Task task;
 @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false) private String language;
 @Column(columnDefinition="TEXT",nullable=false) private String code;
 private double score; private double correctnessScore; private double efficiencyScore; private double qualityScore;
 private int passedTests; private int totalTests;
 @Column(columnDefinition="TEXT") private String feedback;
 @Column(name="time_complexity") private String timeComplexity;
 @Column(name="space_complexity") private String spaceComplexity;
 private LocalDateTime submittedAt;
 @PrePersist void create(){submittedAt=LocalDateTime.now();}
 public Long getId(){return id;} public Task getTask(){return task;} public void setTask(Task v){task=v;} public User getUser(){return user;} public void setUser(User v){user=v;}
 public String getLanguage(){return language;} public void setLanguage(String v){language=v;} public String getCode(){return code;} public void setCode(String v){code=v;}
 public double getScore(){return score;} public void setScore(double v){score=v;} public double getCorrectnessScore(){return correctnessScore;} public void setCorrectnessScore(double v){correctnessScore=v;}
 public double getEfficiencyScore(){return efficiencyScore;} public void setEfficiencyScore(double v){efficiencyScore=v;} public double getQualityScore(){return qualityScore;} public void setQualityScore(double v){qualityScore=v;}
 public int getPassedTests(){return passedTests;} public void setPassedTests(int v){passedTests=v;} public int getTotalTests(){return totalTests;} public void setTotalTests(int v){totalTests=v;}
 public String getFeedback(){return feedback;} public void setFeedback(String v){feedback=v;} public String getTimeComplexity(){return timeComplexity;} public void setTimeComplexity(String v){timeComplexity=v;}
 public String getSpaceComplexity(){return spaceComplexity;} public void setSpaceComplexity(String v){spaceComplexity=v;} public LocalDateTime getSubmittedAt(){return submittedAt;}
}
