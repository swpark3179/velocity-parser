package com.velocityparser.model;

import java.util.List;
import java.util.Map;

/**
 * 기능3(전체 분기 시뮬레이션)의 각 분기 결과를 담는 DTO.
 */
public class BranchResult {

    /** 각 조건식의 true/false 상태 목록 */
    private final List<ConditionState> conditionStates;

    /** 해당 조건 조합으로 렌더링된 최종 결과 문자열 */
    private final String renderedOutput;

    public BranchResult(List<ConditionState> conditionStates, String renderedOutput) {
        this.conditionStates = conditionStates;
        this.renderedOutput = renderedOutput;
    }

    public List<ConditionState> getConditionStates() {
        return conditionStates;
    }

    public String getRenderedOutput() {
        return renderedOutput;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BranchResult ===\n");
        sb.append("Conditions:\n");
        for (ConditionState state : conditionStates) {
            sb.append(String.format("  [cond%d] %s => %s\n",
                    state.getIndex(),
                    state.getExpression(),
                    state.isValue() ? "TRUE" : "FALSE"));
        }
        sb.append("Rendered Output:\n");
        sb.append(renderedOutput);
        return sb.toString();
    }
}
