package com.taskflow.backend.dto.coding;

import java.util.ArrayList;
import java.util.List;

public class RunCodeResponse {
    public int passed;
    public int total;
    public List<RunCaseResult> cases = new ArrayList<>();
}
