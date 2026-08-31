package com.taskflow.backend.model;
import jakarta.persistence.*;
@Entity @Table(name="coding_test_cases")
public class CodingTestCase {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id",nullable=false) private Task task;
 @Column(columnDefinition="TEXT",nullable=false) private String input;
 @Column(name="expected_output",columnDefinition="TEXT",nullable=false) private String expectedOutput;
 @Column(nullable=false) private boolean hidden=false;
 private int sortOrder=0;
 public Long getId(){return id;} public void setId(Long id){this.id=id;} public Task getTask(){return task;} public void setTask(Task task){this.task=task;}
 public String getInput(){return input;} public void setInput(String input){this.input=input;} public String getExpectedOutput(){return expectedOutput;} public void setExpectedOutput(String v){this.expectedOutput=v;}
 public boolean isHidden(){return hidden;} public void setHidden(boolean hidden){this.hidden=hidden;} public int getSortOrder(){return sortOrder;} public void setSortOrder(int v){this.sortOrder=v;}
}
