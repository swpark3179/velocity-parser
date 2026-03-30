package com.velocityparser.model;

/**
 * 하나의 조건식과 그 시뮬레이션 값을 나타내는 DTO.
 */
public class ConditionState {

    /** 조건의 인덱스 (0부터 시작) */
    private final int index;

    /** 원래 조건식 텍스트 (예: "$status != null && $status != ''") */
    private final String expression;

    /** 이 시뮬레이션에서 해당 조건의 값 */
    private final boolean value;

    public ConditionState(int index, String expression, boolean value) {
        this.index = index;
        this.expression = expression;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("ConditionState{index=%d, expression='%s', value=%s}",
                index, expression, value);
    }
}
