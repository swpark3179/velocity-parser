package com.velocityparser;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

/**
 * [기능2] Velocity 템플릿 문자열과 변수 맵을 입력받아 렌더링 결과를 반환하는 클래스.
 *
 * <p>Apache Velocity Engine을 사용하여 템플릿을 평가합니다.
 * 변수는 {@code Map<String, Object>} 형태로 제공하며,
 * 맵의 키가 변수명, 값이 변수값이 됩니다.</p>
 *
 * <p>사용 예:</p>
 * <pre>{@code
 * VelocitySimulator simulator = new VelocitySimulator();
 * Map<String, Object> vars = new HashMap<>();
 * vars.put("status", "ACTIVE");
 * vars.put("customerId", 12345L);
 * String result = simulator.simulate(template, vars);
 * }</pre>
 */
public class VelocitySimulator {

    private final VelocityEngine velocityEngine;

    public VelocitySimulator() {
        Properties props = new Properties();
        // 콘솔 로그 억제
        props.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.NullLogChute");
        props.setProperty("runtime.log", "");

        velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init(props);
        } catch (Exception e) {
            throw new RuntimeException("VelocityEngine 초기화 실패", e);
        }
    }

    /**
     * Velocity 템플릿을 주어진 변수로 렌더링하여 결과 문자열을 반환합니다.
     *
     * @param template  Velocity 문법이 적용된 문자열
     * @param variables 변수명 → 변수값 맵 (null 허용 시 빈 컨텍스트 사용)
     * @return 렌더링된 결과 문자열
     */
    public String simulate(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }

        VelocityContext context = new VelocityContext();
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }

        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(context, writer, "velocity-simulator", template);
        } catch (Exception e) {
            throw new RuntimeException("Velocity 템플릿 렌더링 중 오류 발생: " + e.getMessage(), e);
        }
        return writer.toString();
    }
}
