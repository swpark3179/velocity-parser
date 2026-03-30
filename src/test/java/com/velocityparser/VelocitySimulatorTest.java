package com.velocityparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [기능2] VelocitySimulator 테스트
 */
@DisplayName("Feature 2: Velocity 시뮬레이션 테스트")
class VelocitySimulatorTest {

    private VelocitySimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new VelocitySimulator();
    }

    @Test
    @DisplayName("모든 변수를 제공했을 때 주문 쿼리가 정상 렌더링되어야 한다")
    void testSimulateOrderQueryAllVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "ACTIVE");
        vars.put("customerId", 12345L);
        vars.put("startDate", "2024-01-01");
        vars.put("endDate", "2024-12-31");
        vars.put("minAmount", 50000);
        vars.put("orderTypes", Arrays.asList("ONLINE", "MOBILE"));
        vars.put("customerGrade", "VIP");
        vars.put("sortField", "date");
        vars.put("sortDesc", true);

        String result = simulator.simulate(TestTemplates.ORDER_QUERY, vars);

        System.out.println("=== 주문 쿼리 전체 변수 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("AND o.status = 'ACTIVE'"),        "status 조건 포함");
        assertTrue(result.contains("AND o.customer_id = 12345"),      "customerId 조건 포함");
        assertTrue(result.contains("BETWEEN TO_DATE('2024-01-01'"),   "날짜 범위 BETWEEN 포함");
        assertTrue(result.contains("AND o.total_amount >= 50000"),    "minAmount 조건 포함");
        assertTrue(result.contains("'ONLINE'"),                        "orderTypes 첫 번째 값 포함");
        assertTrue(result.contains("'MOBILE'"),                        "orderTypes 두 번째 값 포함");
        assertTrue(result.contains("VIP"),                             "customerGrade 조건 포함");
        assertTrue(result.contains("ORDER BY o.order_date"),           "sortField=date 정렬 포함");
        assertTrue(result.contains("DESC"),                            "sortDesc=true => DESC 포함");
    }

    @Test
    @DisplayName("status만 제공했을 때 해당 조건만 포함되어야 한다")
    void testSimulateOrderQueryStatusOnly() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("status", "PENDING");

        String result = simulator.simulate(TestTemplates.ORDER_QUERY, vars);

        System.out.println("=== status만 제공한 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("AND o.status = 'PENDING'"),      "status 조건 포함");
        assertFalse(result.contains("AND o.customer_id"),            "customerId 조건 미포함");
        assertFalse(result.contains("AND o.order_date"),             "날짜 조건 미포함");
        assertFalse(result.contains("AND o.total_amount"),           "minAmount 조건 미포함");
        assertFalse(result.contains("AND o.order_type IN"),          "orderTypes 조건 미포함");
        assertTrue(result.contains("ORDER BY o.order_id ASC"),       "기본 정렬 사용");
    }

    @Test
    @DisplayName("startDate만 있고 endDate 없을 때 >= 조건이 사용되어야 한다")
    void testSimulateOrderQueryStartDateOnly() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("startDate", "2024-06-01");

        String result = simulator.simulate(TestTemplates.ORDER_QUERY, vars);

        System.out.println("=== startDate만 제공한 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("AND o.order_date >= TO_DATE('2024-06-01'"),
                "startDate만 있을 때 >= 조건 사용");
        assertFalse(result.contains("BETWEEN"), "BETWEEN은 사용되지 않아야 함");
    }

    @Test
    @DisplayName("#foreach 루프가 정상적으로 동작해야 한다")
    void testSimulateForeachLoop() {
        Map<String, Object> vars = new HashMap<>();
        List<String> types = Arrays.asList("ONLINE", "STORE", "MOBILE");
        vars.put("orderTypes", types);

        String result = simulator.simulate(TestTemplates.ORDER_QUERY, vars);

        System.out.println("=== #foreach 루프 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("'ONLINE'"), "ONLINE 포함");
        assertTrue(result.contains("'STORE'"),  "STORE 포함");
        assertTrue(result.contains("'MOBILE'"), "MOBILE 포함");
        // 마지막 항목 뒤에 콤마 없어야 함
        assertFalse(result.contains("'MOBILE',"), "마지막 요소 뒤에 콤마 없어야 함");
        // 중간 항목들에는 콤마가 있어야 함
        assertTrue(result.contains("'ONLINE',") || result.contains("'STORE',"),
                "중간 요소 뒤에 콤마 있어야 함");
    }

    @Test
    @DisplayName("직원 통계 쿼리 - departmentId와 includeSubDept 동시 제공")
    void testSimulateEmployeeQueryWithSubDept() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("departmentId", 10);
        vars.put("includeSubDept", true);
        vars.put("orderBy", "salary");

        String result = simulator.simulate(TestTemplates.EMPLOYEE_STATS_QUERY, vars);

        System.out.println("=== 직원 통계 쿼리 (하위부서 포함) 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("AND e.department_id = 10"),         "departmentId 조건 포함");
        assertTrue(result.contains("OR e.department_id IN"),            "하위부서 조건 포함");
        assertTrue(result.contains("WHERE parent_dept_id = 10"),        "하위부서 조인 조건 포함");
        assertTrue(result.contains("e.salary DESC"),                    "salary 정렬 포함");
    }

    @Test
    @DisplayName("직원 통계 쿼리 - hire_date 범위 조건")
    void testSimulateEmployeeQueryHireDateRange() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("hireYearFrom", 2020);
        vars.put("hireYearTo", 2023);
        vars.put("orderBy", "hire_date");

        String result = simulator.simulate(TestTemplates.EMPLOYEE_STATS_QUERY, vars);

        System.out.println("=== 직원 통계 쿼리 (입사연도 범위) 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("BETWEEN 2020 AND 2023"), "입사연도 BETWEEN 조건 포함");
        assertTrue(result.contains("e.hire_date ASC"),        "hire_date 정렬 포함");
    }

    @Test
    @DisplayName("null 변수 맵 입력 시 조건 없는 기본 쿼리가 반환되어야 한다")
    void testSimulateWithNullVariables() {
        String result = simulator.simulate(TestTemplates.SIMPLE_QUERY, null);

        System.out.println("=== null 변수 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("SELECT * FROM products"), "기본 SELECT 구문 포함");
        assertFalse(result.contains("AND category_id"), "categoryId 조건 미포함");
        assertFalse(result.contains("AND sale_yn"),     "onSale 조건 미포함");
    }

    @Test
    @DisplayName("빈 문자열 템플릿은 빈 문자열을 반환해야 한다")
    void testSimulateEmptyTemplate() {
        String result = simulator.simulate("", new HashMap<>());
        assertEquals("", result, "빈 템플릿은 빈 결과를 반환해야 한다");
    }

    @Test
    @DisplayName("$sortField = 'amount' 정렬 조건 테스트")
    void testSimulateSortByAmount() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("sortField", "amount");
        vars.put("sortDesc", false);

        String result = simulator.simulate(TestTemplates.ORDER_QUERY, vars);

        System.out.println("=== amount 정렬 렌더링 결과 ===");
        System.out.println(result);

        assertTrue(result.contains("ORDER BY o.total_amount"), "amount 정렬 적용");
        assertTrue(result.contains("ASC"), "sortDesc=false => ASC 적용");
    }
}
