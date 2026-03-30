package com.velocityparser;

import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.node.ASTReference;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * [기능1] Velocity 템플릿 문자열에서 사용된 변수명 목록을 추출하는 클래스.
 *
 * <p>Apache Velocity Runtime의 AST(Abstract Syntax Tree)를 파싱하여,
 * {@code ASTReference} 노드를 재귀 탐색함으로써 모든 변수 참조를 수집합니다.</p>
 *
 * <p>지원 형태:</p>
 * <ul>
 *   <li>{@code $variable} - 단순 변수</li>
 *   <li>{@code $obj.field} - 프로퍼티 접근 (루트 변수명 {@code obj} 추출)</li>
 *   <li>{@code $obj.method()} - 메서드 호출 (루트 변수명 {@code obj} 추출)</li>
 *   <li>{@code #foreach ($item in $list)} - foreach 루프 변수 포함</li>
 *   <li>{@code #set ($var = ...)} - set 지시자 변수 포함</li>
 * </ul>
 */
public class VelocityVariableExtractor {

    private final RuntimeInstance runtimeInstance;

    public VelocityVariableExtractor() {
        Properties props = new Properties();
        props.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.NullLogChute");
        props.setProperty("runtime.log", "");
        // Velocity 2.x 에서 로그 억제
        props.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.AvalonLogChute");

        runtimeInstance = new RuntimeInstance();
        try {
            // 로그 출력 억제
            props.setProperty(RuntimeInstance.RUNTIME_LOG_INSTANCE, "");
            runtimeInstance.init(props);
        } catch (Exception e) {
            // init 실패 시에도 동작 가능하도록 재시도
            try {
                runtimeInstance.init();
            } catch (Exception ex) {
                throw new RuntimeException("VelocityRuntime 초기화 실패", ex);
            }
        }
    }

    /**
     * Velocity 템플릿 문자열에서 사용된 변수명 목록을 반환합니다.
     *
     * @param template Velocity 문법이 적용된 문자열
     * @return 중복 제거된 변수명 목록 (삽입 순서 유지)
     */
    public List<String> extract(String template) {
        if (template == null || template.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> variables = new LinkedHashSet<>();
        try {
            org.apache.velocity.Template dummyTemplate = new org.apache.velocity.Template();
            dummyTemplate.setName("template");
            try { dummyTemplate.setRuntimeServices(runtimeInstance); } catch (Exception e) {}
            SimpleNode root = runtimeInstance.parse(new StringReader(template), dummyTemplate);
            root.init(new org.apache.velocity.context.InternalContextAdapterImpl(new org.apache.velocity.VelocityContext()), runtimeInstance);
            collectReferences(root, variables);
        } catch (Exception e) {
            throw new RuntimeException("Velocity 템플릿 파싱 중 오류 발생: " + e.getMessage(), e);
        }
        return new ArrayList<>(variables);
    }

    /**
     * AST 노드를 재귀 탐색하여 ASTReference 노드에서 변수명을 수집합니다.
     */
    private void collectReferences(Node node, Set<String> variables) {
        if (node == null) {
            return;
        }

        if (node instanceof ASTReference) {
            ASTReference ref = (ASTReference) node;
            // getRootString() 은 "$" 를 제외한 루트 변수명을 반환 (예: "obj" for "$obj.field")
            String rootString = ref.getRootString();
            if (rootString != null && !rootString.isEmpty()) {
                variables.add(rootString);
            }
        }

        int childCount = node.jjtGetNumChildren();
        for (int i = 0; i < childCount; i++) {
            collectReferences(node.jjtGetChild(i), variables);
        }
    }
}
