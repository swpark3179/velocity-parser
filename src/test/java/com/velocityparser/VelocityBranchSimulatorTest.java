package com.velocityparser;

import com.velocityparser.model.BranchResult;
import com.velocityparser.model.ConditionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [기능3] VelocityBranchSimulator 테스트
 */
@DisplayName("Feature 3: Velocity 전체 분기 시뮬레이션 테스트")
class VelocityBranchSimulatorTest {

    private VelocityBranchSimulator branchSimulator;

    @BeforeEach
    void setUp() {
        branchSimulator = new VelocityBranchSimulator();
    }

    @Test
    @DisplayName("단순 쿼리(2개 조건) - 4가지 분기가 반환되어야 한다")
    void testSimpleQueryBranchCount() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.SIMPLE_QUERY);

        System.out.println("=== 단순 쿼리 전체 분기 시뮬레이션 (" + results.size() + "가지) ===");
        for (BranchResult r : results) {
            System.out.println(r);
            System.out.println("--------------------------------------------------");
        }

        // SIMPLE_QUERY: #if ($categoryId != null), #if ($onSale == true) → 2^2 = 4가지
        assertEquals(4, results.size(), "2개 조건 → 4가지 분기");
    }

    @Test
    @DisplayName("각 BranchResult에 조건 상태가 정확히 포함되어야 한다")
    void testBranchResultContainsConditionStates() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.SIMPLE_QUERY);

        for (BranchResult result : results) {
            List<ConditionState> states = result.getConditionStates();
            assertNotNull(states, "conditionStates는 null이 아니어야 한다");
            assertEquals(2, states.size(), "2개 조건이 있어야 한다");

            for (ConditionState state : states) {
                assertNotNull(state.getExpression(), "expression은 null이 아니어야 한다");
                assertFalse(state.getExpression().isEmpty(), "expression이 비어있지 않아야 한다");
                System.out.printf("  [cond%d] '%s' => %s%n",
                        state.getIndex(), state.getExpression(), state.isValue() ? "TRUE" : "FALSE");
            }

            assertNotNull(result.getRenderedOutput(), "renderedOutput은 null이 아니어야 한다");
        }
    }

    @Test
    @DisplayName("모든 조건이 true일 때 모든 조건 블록이 포함되어야 한다")
    void testAllConditionsTrueCase() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.SIMPLE_QUERY);

        // mask = 0b11 = 3 → 모든 조건 true (cond0=true, cond1=true)
        BranchResult allTrue = results.stream()
                .filter(r -> r.getConditionStates().stream().allMatch(ConditionState::isValue))
                .findFirst()
                .orElse(null);

        assertNotNull(allTrue, "모든 조건이 true인 경우가 존재해야 한다");

        System.out.println("=== 모든 조건 TRUE 분기 ===");
        System.out.println(allTrue);

        String output = allTrue.getRenderedOutput();
        assertTrue(output.contains("AND category_id"), "categoryId 조건 포함");
        assertTrue(output.contains("AND sale_yn = 'Y'"), "onSale 조건 포함");
    }

    @Test
    @DisplayName("모든 조건이 false일 때 조건 블록이 제외되어야 한다")
    void testAllConditionsFalseCase() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.SIMPLE_QUERY);

        // mask = 0b00 = 0 → 모든 조건 false
        BranchResult allFalse = results.stream()
                .filter(r -> r.getConditionStates().stream().noneMatch(ConditionState::isValue))
                .findFirst()
                .orElse(null);

        assertNotNull(allFalse, "모든 조건이 false인 경우가 존재해야 한다");

        System.out.println("=== 모든 조건 FALSE 분기 ===");
        System.out.println(allFalse);

        String output = allFalse.getRenderedOutput();
        assertTrue(output.contains("SELECT * FROM products"), "기본 SELECT 구문 포함");
        assertFalse(output.contains("AND category_id"),       "categoryId 조건 미포함");
        assertFalse(output.contains("AND sale_yn"),           "onSale 조건 미포함");
    }

    @Test
    @DisplayName("조건 없는 템플릿은 1가지 결과만 반환해야 한다")
    void testTemplateWithNoConditions() {
        String plain = "SELECT * FROM employees WHERE use_yn = 'Y' ORDER BY emp_id";
        List<BranchResult> results = branchSimulator.simulateAllBranches(plain);

        assertEquals(1, results.size(), "조건 없으면 결과 1개");
        assertTrue(results.get(0).getConditionStates().isEmpty(), "조건 상태 목록이 비어있어야 함");
        assertTrue(results.get(0).getRenderedOutput().contains("SELECT * FROM employees"),
                "그대로 렌더링되어야 함");
    }

    @Test
    @DisplayName("주문 조회 쿼리 전체 분기 시뮬레이션 - 조건 수 및 결과 검증")
    void testOrderQueryAllBranches() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.ORDER_QUERY);

        System.out.println("=== 주문 조회 쿼리 전체 분기 시뮬레이션 ===");
        System.out.println("총 분기 수: " + results.size());
        System.out.println("조건 수: " + (results.isEmpty() ? 0 : results.get(0).getConditionStates().size()));

        // 조건식 목록 출력
        if (!results.isEmpty()) {
            System.out.println("조건 목록:");
            for (ConditionState cs : results.get(0).getConditionStates()) {
                System.out.printf("  [%d] %s%n", cs.getIndex(), cs.getExpression());
            }
        }

        // 분기 수는 2^n (n = 조건 개수)
        assertFalse(results.isEmpty(), "결과가 비어있지 않아야 한다");
        int condCount = results.get(0).getConditionStates().size();
        int expected = 1 << condCount;
        assertEquals(expected, results.size(),
                "분기 수는 2^" + condCount + " = " + expected + " 이어야 한다");

        // 각 결과의 기본 구조 확인
        for (BranchResult r : results) {
            assertNotNull(r.getRenderedOutput(), "렌더링 결과는 null이 아니어야 한다");
            assertTrue(r.getRenderedOutput().contains("FROM orders o"),
                    "모든 분기에 FROM 절 포함");
            assertTrue(r.getRenderedOutput().contains("JOIN customers c"),
                    "모든 분기에 JOIN 절 포함");
            assertEquals(condCount, r.getConditionStates().size(),
                    "모든 분기의 조건 수가 동일해야 한다");
        }
    }

    @Test
    @DisplayName("직원 통계 쿼리 분기 시뮬레이션 - 중첩 #if 포함")
    void testEmployeeStatsQueryAllBranches() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.EMPLOYEE_STATS_QUERY);

        System.out.println("=== 직원 통계 쿼리 전체 분기 시뮬레이션 ===");
        System.out.println("총 분기 수: " + results.size());
        System.out.println("조건 수: " + (results.isEmpty() ? 0 : results.get(0).getConditionStates().size()));

        if (!results.isEmpty()) {
            System.out.println("조건 목록:");
            for (ConditionState cs : results.get(0).getConditionStates()) {
                System.out.printf("  [%d] %s%n", cs.getIndex(), cs.getExpression());
            }
        }

        assertFalse(results.isEmpty(), "결과가 비어있지 않아야 한다");
        int condCount = results.get(0).getConditionStates().size();
        System.out.println("  → 2^" + condCount + " = " + (1 << condCount) + " 가지 분기");
        assertEquals(1 << condCount, results.size(), "분기 수 = 2^n 이어야 한다");

        // 모든 분기에 공통 구조가 포함되어야 함
        for (BranchResult r : results) {
            assertTrue(r.getRenderedOutput().contains("FROM employees e"), "FROM 절 포함");
        }
    }

    @Test
    @DisplayName("각 분기의 renderedOutput이 서로 달라야 한다 (단, 일부는 같을 수 있음)")
    void testBranchResultsHaveDiverseOutputs() {
        List<BranchResult> results = branchSimulator.simulateAllBranches(TestTemplates.SIMPLE_QUERY);

        // 4가지 분기 중 적어도 2가지 이상의 서로 다른 출력이 있어야 함
        long distinctOutputs = results.stream()
                .map(BranchResult::getRenderedOutput)
                .distinct()
                .count();

        System.out.println("단순 쿼리 고유 출력 수: " + distinctOutputs + " / " + results.size());
        assertTrue(distinctOutputs > 1, "조건이 다르면 최소 2가지 이상의 서로 다른 출력이 있어야 함");
    }

    @Test
    @DisplayName("null 또는 빈 템플릿 입력 시 빈 목록 반환")
    void testNullOrEmptyTemplate() {
        assertTrue(branchSimulator.simulateAllBranches(null).isEmpty(),  "null → 빈 목록");
        assertTrue(branchSimulator.simulateAllBranches("").isEmpty(),    "빈 문자열 → 빈 목록");
        assertTrue(branchSimulator.simulateAllBranches("   ").isEmpty(), "공백 → 빈 목록");
    }
}
