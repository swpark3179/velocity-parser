package com.velocityparser;

/**
 * 테스트에서 공통으로 사용하는 Velocity 템플릿 상수 모음.
 *
 * <p>MyBatis/Oracle SQL 기반의 복잡한 Velocity 템플릿을 제공합니다.</p>
 */
public final class TestTemplates {

    private TestTemplates() {}

    /**
     * 주문 조회 쿼리 - 다중 #if / #elseif / #else / #foreach 포함
     */
    public static final String ORDER_QUERY =
            "<query id=\"getUserOrders\">\n" +
            "<statement>\n" +
            "<![CDATA[\n" +
            "SELECT o.order_id,\n" +
            "       o.order_date,\n" +
            "       o.status,\n" +
            "       o.total_amount,\n" +
            "       c.customer_name,\n" +
            "       c.email,\n" +
            "       c.grade\n" +
            "  FROM orders o\n" +
            "  JOIN customers c ON o.customer_id = c.customer_id\n" +
            " WHERE 1=1\n" +
            "#if ($status && $status != '')\n" +
            "   AND o.status = '$status'\n" +
            "#end\n" +
            "#if ($customerId)\n" +
            "   AND o.customer_id = $customerId\n" +
            "#end\n" +
            "#if ($startDate && $endDate)\n" +
            "   AND o.order_date BETWEEN TO_DATE('$startDate', 'YYYY-MM-DD')\n" +
            "                        AND TO_DATE('$endDate', 'YYYY-MM-DD')\n" +
            "#elseif ($startDate)\n" +
            "   AND o.order_date >= TO_DATE('$startDate', 'YYYY-MM-DD')\n" +
            "#elseif ($endDate)\n" +
            "   AND o.order_date <= TO_DATE('$endDate', 'YYYY-MM-DD')\n" +
            "#end\n" +
            "#if ($minAmount)\n" +
            "   AND o.total_amount >= $minAmount\n" +
            "#end\n" +
            "#if ($orderTypes && !$orderTypes.isEmpty())\n" +
            "   AND o.order_type IN (\n" +
            "   #foreach ($type in $orderTypes)\n" +
            "     '$type'#if ($foreach.hasNext),#end\n" +
            "   #end\n" +
            "   )\n" +
            "#end\n" +
            "#if ($customerGrade)\n" +
            "   AND c.grade = '$customerGrade'\n" +
            "#end\n" +
            "#if ($sortField == 'date')\n" +
            " ORDER BY o.order_date #if ($sortDesc) DESC #else ASC #end\n" +
            "#elseif ($sortField == 'amount')\n" +
            " ORDER BY o.total_amount #if ($sortDesc) DESC #else ASC #end\n" +
            "#else\n" +
            " ORDER BY o.order_id ASC\n" +
            "#end\n" +
            "]]>\n" +
            "</statement>\n" +
            "</query>";

    /**
     * 직원 통계 쿼리 - #set, 중첩 #if, #foreach 포함
     */
    public static final String EMPLOYEE_STATS_QUERY =
            "<query id=\"getEmployeeStats\">\n" +
            "<statement>\n" +
            "<![CDATA[\n" +
            "#set ($hasFilter = $departmentId || $positionCode)\n" +
            "SELECT e.emp_id,\n" +
            "       e.emp_name,\n" +
            "       e.department_id,\n" +
            "       d.dept_name,\n" +
            "       e.position_code,\n" +
            "       e.salary,\n" +
            "       e.hire_date,\n" +
            "       NVL(e.bonus_rate, 0) AS bonus_rate,\n" +
            "       CASE\n" +
            "         WHEN e.salary >= 8000000 THEN 'HIGH'\n" +
            "         WHEN e.salary >= 5000000 THEN 'MIDDLE'\n" +
            "         ELSE 'LOW'\n" +
            "       END AS salary_grade\n" +
            "  FROM employees e\n" +
            "  JOIN departments d ON e.department_id = d.dept_id\n" +
            " WHERE e.use_yn = 'Y'\n" +
            "#if ($departmentId)\n" +
            "   AND e.department_id = $departmentId\n" +
            "  #if ($includeSubDept == true)\n" +
            "   OR e.department_id IN (\n" +
            "       SELECT dept_id FROM departments\n" +
            "        WHERE parent_dept_id = $departmentId\n" +
            "   )\n" +
            "  #end\n" +
            "#end\n" +
            "#if ($positionCode)\n" +
            "   AND e.position_code = '$positionCode'\n" +
            "#end\n" +
            "#if ($hireYearFrom && $hireYearTo)\n" +
            "   AND EXTRACT(YEAR FROM e.hire_date) BETWEEN $hireYearFrom AND $hireYearTo\n" +
            "#elseif ($hireYearFrom)\n" +
            "   AND EXTRACT(YEAR FROM e.hire_date) >= $hireYearFrom\n" +
            "#end\n" +
            "#if ($excludeEmpIds && !$excludeEmpIds.isEmpty())\n" +
            "   AND e.emp_id NOT IN (\n" +
            "   #foreach ($eid in $excludeEmpIds)\n" +
            "     $eid#if ($foreach.hasNext),#end\n" +
            "   #end\n" +
            "   )\n" +
            "#end\n" +
            " ORDER BY\n" +
            "#if ($orderBy == 'salary')\n" +
            "   e.salary DESC, e.emp_id ASC\n" +
            "#elseif ($orderBy == 'hire_date')\n" +
            "   e.hire_date ASC, e.emp_id ASC\n" +
            "#else\n" +
            "   e.emp_name ASC\n" +
            "#end\n" +
            "]]>\n" +
            "</statement>\n" +
            "</query>";

    /**
     * 단순 조건만 있는 경량 템플릿 (빠른 테스트용)
     */
    public static final String SIMPLE_QUERY =
            "SELECT * FROM products\n" +
            " WHERE 1=1\n" +
            "#if ($categoryId)\n" +
            "   AND category_id = $categoryId\n" +
            "#end\n" +
            "#if ($onSale == true)\n" +
            "   AND sale_yn = 'Y'\n" +
            "#end";
}
