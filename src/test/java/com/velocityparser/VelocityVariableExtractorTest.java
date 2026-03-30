package com.velocityparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [기능1] VelocityVariableExtractor 테스트
 */
@DisplayName("Feature 1: Velocity 변수 추출 테스트")
class VelocityVariableExtractorTest {

    private VelocityVariableExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new VelocityVariableExtractor();
    }

    @Test
    @DisplayName("주문 조회 쿼리에서 모든 변수가 추출되어야 한다")
    void testExtractFromOrderQuery() {
        List<String> vars = extractor.extract(TestTemplates.ORDER_QUERY);

        System.out.println("=== 주문 조회 쿼리 추출 변수 (" + vars.size() + "개) ===");
        for (String v : vars) {
            System.out.println("  $" + v);
        }

        // 핵심 변수 포함 여부 확인
        assertTrue(vars.contains("status"),          "$status 포함되어야 함");
        assertTrue(vars.contains("customerId"),      "$customerId 포함되어야 함");
        assertTrue(vars.contains("startDate"),       "$startDate 포함되어야 함");
        assertTrue(vars.contains("endDate"),         "$endDate 포함되어야 함");
        assertTrue(vars.contains("minAmount"),       "$minAmount 포함되어야 함");
        assertTrue(vars.contains("orderTypes"),      "$orderTypes 포함되어야 함");
        assertTrue(vars.contains("type"),            "$type (#foreach 루프변수) 포함되어야 함");
        assertTrue(vars.contains("customerGrade"),   "$customerGrade 포함되어야 함");
        assertTrue(vars.contains("sortField"),       "$sortField 포함되어야 함");
        assertTrue(vars.contains("sortDesc"),        "$sortDesc 포함되어야 함");
        assertTrue(vars.contains("foreach"),         "$foreach (루프 내장 변수) 포함되어야 함");

        // 중복 없음 확인
        long distinct = vars.stream().distinct().count();
        assertEquals(distinct, vars.size(), "중복 변수가 없어야 한다");
    }

    @Test
    @DisplayName("직원 통계 쿼리에서 모든 변수가 추출되어야 한다")
    void testExtractFromEmployeeStatsQuery() {
        List<String> vars = extractor.extract(TestTemplates.EMPLOYEE_STATS_QUERY);

        System.out.println("=== 직원 통계 쿼리 추출 변수 (" + vars.size() + "개) ===");
        for (String v : vars) {
            System.out.println("  $" + v);
        }

        assertTrue(vars.contains("departmentId"),    "$departmentId 포함되어야 함");
        assertTrue(vars.contains("positionCode"),    "$positionCode 포함되어야 함");
        assertTrue(vars.contains("includeSubDept"),  "$includeSubDept 포함되어야 함");
        assertTrue(vars.contains("hireYearFrom"),    "$hireYearFrom 포함되어야 함");
        assertTrue(vars.contains("hireYearTo"),      "$hireYearTo 포함되어야 함");
        assertTrue(vars.contains("excludeEmpIds"),   "$excludeEmpIds 포함되어야 함");
        assertTrue(vars.contains("orderBy"),         "$orderBy 포함되어야 함");
        // #set 으로 정의된 변수도 포함
        assertTrue(vars.contains("hasFilter"),       "$hasFilter (#set 변수) 포함되어야 함");
    }

    @Test
    @DisplayName("단순 쿼리에서 변수가 정확히 추출되어야 한다")
    void testExtractFromSimpleQuery() {
        List<String> vars = extractor.extract(TestTemplates.SIMPLE_QUERY);

        System.out.println("=== 단순 쿼리 추출 변수 ===");
        for (String v : vars) {
            System.out.println("  $" + v);
        }

        assertTrue(vars.contains("categoryId"), "$categoryId 포함되어야 함");
        assertTrue(vars.contains("onSale"),     "$onSale 포함되어야 함");
        assertEquals(2, vars.stream().distinct().count(), "정확히 2개의 고유 변수여야 함");
    }

    @Test
    @DisplayName("Velocity 없는 단순 텍스트에서는 빈 목록을 반환해야 한다")
    void testExtractFromPlainText() {
        String plain = "SELECT * FROM orders WHERE order_id = 1";
        List<String> vars = extractor.extract(plain);
        assertTrue(vars.isEmpty(), "Velocity 변수가 없으면 빈 목록을 반환해야 한다");
    }

    @Test
    @DisplayName("null 또는 빈 템플릿에서는 빈 목록을 반환해야 한다")
    void testExtractFromNullOrEmpty() {
        assertTrue(extractor.extract(null).isEmpty(),  "null 입력 시 빈 목록");
        assertTrue(extractor.extract("").isEmpty(),    "빈 문자열 시 빈 목록");
        assertTrue(extractor.extract("   ").isEmpty(), "공백 문자열 시 빈 목록");
    }

    @Test
    @DisplayName("프로퍼티 접근($obj.field)은 루트 변수명만 추출해야 한다")
    void testPropertyAccessExtractsRootOnly() {
        String template = "#if ($user.age > 18)\nHello $user.name\n#end";
        List<String> vars = extractor.extract(template);

        System.out.println("=== 프로퍼티 접근 테스트 변수 ===");
        for (String v : vars) System.out.println("  $" + v);

        assertTrue(vars.contains("user"), "$user 루트 변수 포함되어야 함");
        assertFalse(vars.contains("user.age"),  "user.age 형태는 포함되지 않아야 함");
        assertFalse(vars.contains("user.name"), "user.name 형태는 포함되지 않아야 함");
    }
}
