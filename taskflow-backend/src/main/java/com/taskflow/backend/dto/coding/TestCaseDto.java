package com.taskflow.backend.dto.coding;
public class TestCaseDto { public Long id; public String input; public String expectedOutput; public boolean hidden; public TestCaseDto(){} public TestCaseDto(Long id,String i,String e,boolean h){this.id=id;input=i;expectedOutput=e;hidden=h;} }
