package com.velocityparser;

import com.velocityparser.model.BranchResult;
import com.velocityparser.model.ConditionState;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.parser.node.ASTElseIfStatement;
import org.apache.velocity.runtime.parser.node.ASTIfStatement;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * [기능3] Velocity 템플릿에서 등장하는 모든 #if / #elseif 분기 조건을 수집하고,
 * 각 조건에 대한 true/false 모든 조합(2^n)을 시뮬레이션하여 결과 목록을 반환하는 클래스.
 *
 * <h3>동작 원리</h3>
 * <ol>
 *   <li>AST 탐색으로 {@code ASTIfStatement} / {@code ASTElseIfStatement} 노드의
 *       첫 번째 자식(조건 노드)의 원문 텍스트를 수집합니다.</li>
 *   <li>수집된 n개의 조건 각각을 {@code $__cond0__}, {@code $__cond1__} 등과 같은
 *       단순 boolean 변수로 치환한 변형 템플릿을 생성합니다.</li>
 *   <li>2^n 가지의 true/false 조합마다 VelocityContext에 boolean 값을 주입하여
 *       변형 템플릿을 렌더링합니다.</li>
 *   <li>각 렌더링 결과를 {@link BranchResult}로 래핑하여 반환합니다.</li>
 * </ol>
 *
 * <h3>제약사항</h3>
 * <ul>
 *   <li>조건식 텍스트에 단따옴표({@code '}) 또는 특수문자 조합이 포함된 경우에도
 *       원문을 그대로 {@link ConditionState#getExpression()}으로 제공합니다.</li>
 *   <li>중첩 #if 는 모두 독립적인 조건으로 처리됩니다.</li>
 * </ul>
 */
public class VelocityBranchSimulator {

    private static final String COND_PREFIX = "__cond";
    private static final String COND_SUFFIX = "__";

    private final RuntimeInstance runtimeInstance;
    private final VelocityEngine velocityEngine;

    public VelocityBranchSimulator() {
        Properties props = buildSilentProperties();

        runtimeInstance = new RuntimeInstance();
        try {
            runtimeInstance.init(props);
        } catch (Exception e) {
            try {
                runtimeInstance.init();
            } catch (Exception ex) {
                throw new RuntimeException("RuntimeInstance 초기화 실패", ex);
            }
        }

        velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init(props);
        } catch (Exception e) {
            throw new RuntimeException("VelocityEngine 초기화 실패", e);
        }
    }

    /**
     * Velocity 템플릿의 모든 분기 조합을 시뮬레이션합니다.
     *
     * @param template Velocity 문법이 적용된 문자열
     * @return 각 조건 조합별 {@link BranchResult} 목록 (최대 2^n개)
     */
    public List<BranchResult> simulateAllBranches(String template) {
        if (template == null || template.trim().isEmpty()) {
            return new ArrayList<>();
        }

        SimpleNode root;
        try {
            org.apache.velocity.Template dummyTemplate = new org.apache.velocity.Template();
            dummyTemplate.setName("branch-template");
            try { dummyTemplate.setRuntimeServices(runtimeInstance); } catch (Exception e) {}
            root = runtimeInstance.parse(new StringReader(template), dummyTemplate);
        } catch (Exception e) {
            throw new RuntimeException("템플릿 파싱 오류: " + e.getMessage(), e);
        }

        // 1단계: AST 탐색으로 조건식 텍스트 수집
        List<String> conditions = new ArrayList<>();
        collectConditionNodes(root, template, conditions);

        if (conditions.isEmpty()) {
            // 분기 없음 → 단순 렌더링 (변수 없이)
            VelocityContext ctx = new VelocityContext();
            String output = renderTemplate(template, ctx);
            List<ConditionState> emptyStates = new ArrayList<>();
            return Arrays.asList(new BranchResult(emptyStates, output));
        }

        // 2단계: 조건식을 $__condN__ 변수로 치환한 변형 템플릿 생성
        String transformedTemplate = buildTransformedTemplate(root, template, conditions);

        // 3단계: 2^n 가지 조합으로 렌더링
        int n = conditions.size();
        int total = 1 << n; // 2^n
        List<BranchResult> results = new ArrayList<>(total);
        VelocityContext ctx = new VelocityContext();

        for (int mask = 0; mask < total; mask++) {
            List<ConditionState> states = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                boolean value = ((mask >> i) & 1) == 1;
                String condVarName = COND_PREFIX + i + COND_SUFFIX;
                ctx.put(condVarName, value);
                states.add(new ConditionState(i, conditions.get(i), value));
            }

            String output = renderTemplate(transformedTemplate, ctx);
            results.add(new BranchResult(states, output));
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // 내부 구현 메서드
    // -------------------------------------------------------------------------

    /**
     * AST를 재귀 탐색하여 조건식 노드를 수집합니다.
     */
    private void collectConditionNodes(Node node, String template,
                                       List<String> conditions) {
        if (node == null) return;

        boolean isIf = node instanceof ASTIfStatement;
        boolean isElseIf = node instanceof ASTElseIfStatement;

        if ((isIf || isElseIf) && node.jjtGetNumChildren() > 0) {
            // 첫 번째 자식 노드가 조건식
            Node condNode = node.jjtGetChild(0);
            String condText = extractNodeText(condNode, template);
            if (condText != null && !condText.trim().isEmpty()) {
                conditions.add(condText.trim());
            }
        }

        int childCount = node.jjtGetNumChildren();
        for (int i = 0; i < childCount; i++) {
            collectConditionNodes(node.jjtGetChild(i), template, conditions);
        }
    }

    /**
     * AST 노드의 소스 위치(column/line)를 이용해 원본 템플릿에서 텍스트를 추출합니다.
     * Velocity AST 노드는 firstToken / lastToken 으로 소스 위치를 가집니다.
     */
    private String extractNodeText(Node node, String template) {
        try {
            org.apache.velocity.runtime.parser.Token first = ((SimpleNode) node).getFirstToken();
            org.apache.velocity.runtime.parser.Token last = ((SimpleNode) node).getLastToken();

            if (first == null || last == null) return null;

            // 토큰의 image를 연결하여 조건식 텍스트 복원
            StringBuilder sb = new StringBuilder();
            org.apache.velocity.runtime.parser.Token t = first;
            while (t != null) {
                sb.append(t.image);
                if (t == last) break;
                t = t.next;
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 원본 템플릿에서 #if(condExpr) 와 #elseif(condExpr) 의 조건식 부분을
     * $__condN__ 형태의 boolean 변수 참조로 치환합니다.
     *
     * 치환은 단순 String 치환이 아닌, 토큰 재구성을 통해 수행합니다.
     * 단순 문자열 치환은 동일한 조건식이 반복될 때 오치환 위험이 있으므로
     * 첫 번째 등장 순서대로 처리합니다.
     */
    private String buildTransformedTemplate(SimpleNode root, String template, List<String> conditions) {
        // 기존 AST를 사용하여 각 조건 노드의 소스 위치를 수집
        List<int[]> condRanges = new ArrayList<>(); // [startOffset, endOffset]

        try {
            collectConditionRanges(root, template, condRanges);
        } catch (Exception e) {
            throw new RuntimeException("변형 템플릿 생성 중 범위 수집 오류: " + e.getMessage(), e);
        }

        if (condRanges.size() != conditions.size()) {
            // 범위 수집 실패 시 단순 문자열 치환 폴백
            return fallbackTransform(template, conditions);
        }

        // 범위가 많은 쪽부터 뒤에서부터 치환 (인덱스 변화 방지)
        StringBuilder sb = new StringBuilder(template);
        // condRanges 는 순서대로 수집됨. 뒤에서부터 치환.
        for (int i = condRanges.size() - 1; i >= 0; i--) {
            int start = condRanges.get(i)[0];
            int end = condRanges.get(i)[1];
            if (start >= 0 && end >= start && end <= sb.length()) {
                String replacement = "$" + COND_PREFIX + i + COND_SUFFIX;
                sb.replace(start, end, replacement);
            }
        }
        return sb.toString();
    }

    /**
     * AST 노드에서 조건 노드의 오프셋(문자 위치)을 수집합니다.
     */
    private void collectConditionRanges(Node node, String template,
                                        List<int[]> condRanges) {
        if (node == null) return;

        boolean isIf = node instanceof ASTIfStatement;
        boolean isElseIf = node instanceof ASTElseIfStatement;

        if ((isIf || isElseIf) && node.jjtGetNumChildren() > 0) {
            Node condNode = node.jjtGetChild(0);
            int[] range = getNodeCharRange(condNode, template);
            if (range != null) {
                condRanges.add(range);
            }
        }

        int childCount = node.jjtGetNumChildren();
        for (int i = 0; i < childCount; i++) {
            collectConditionRanges(node.jjtGetChild(i), template, condRanges);
        }
    }

    /**
     * 노드의 토큰 정보를 이용하여 템플릿 문자열 내 시작/종료 문자 오프셋을 반환합니다.
     * Velocity의 토큰은 line/column 기반이므로, 이를 절대 오프셋으로 변환합니다.
     */
    private int[] getNodeCharRange(Node node, String template) {
        try {
            org.apache.velocity.runtime.parser.Token first = ((SimpleNode) node).getFirstToken();
            org.apache.velocity.runtime.parser.Token last = ((SimpleNode) node).getLastToken();
            if (first == null || last == null) return null;

            int startOffset = lineColumnToOffset(template, first.beginLine, first.beginColumn);
            // last 토큰의 끝 = last 다음 토큰의 시작, 또는 last.image.length 이용
            int endOffset = lineColumnToOffset(template, last.endLine, last.endColumn);
            if (endOffset == Integer.MIN_VALUE) {
                // endLine/endColumn이 지원되지 않으면 image 길이로 계산
                int lastStart = lineColumnToOffset(template, last.beginLine, last.beginColumn);
                if (lastStart < 0) return null;
                endOffset = lastStart + last.image.length();
            } else {
                endOffset += 1; // endColumn은 마지막 문자 포함이므로 +1
            }

            if (startOffset < 0 || endOffset < startOffset) return null;
            return new int[]{startOffset, endOffset};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 1-based line/column을 0-based 문자 오프셋으로 변환합니다.
     */
    private int lineColumnToOffset(String text, int line, int column) {
        if (line <= 0 || column <= 0) return Integer.MIN_VALUE;
        int currentLine = 1;
        int offset = 0;
        char[] chars = text.toCharArray();
        while (offset < chars.length) {
            if (currentLine == line) {
                // column은 1-based
                return offset + (column - 1);
            }
            if (chars[offset] == '\n') {
                currentLine++;
            }
            offset++;
        }
        if (currentLine == line) {
            return offset + (column - 1);
        }
        return Integer.MIN_VALUE;
    }

    /**
     * 오프셋 수집이 실패했을 때 단순 문자열 치환으로 폴백합니다.
     * 동일한 조건식이 반복되는 경우 첫 번째 등장만 치환(replaceFirst 방식)합니다.
     */
    private String fallbackTransform(String template, List<String> conditions) {
        String result = template;
        for (int i = 0; i < conditions.size(); i++) {
            String condExpr = conditions.get(i);
            String replacement = "$" + COND_PREFIX + i + COND_SUFFIX;
            // #if 또는 #elseif 괄호 내부의 조건식을 치환
            String ifPattern = "#if\\s*\\(\\s*" + java.util.regex.Pattern.quote(condExpr) + "\\s*\\)";
            String elseIfPattern = "#elseif\\s*\\(\\s*" + java.util.regex.Pattern.quote(condExpr) + "\\s*\\)";
            result = result.replaceFirst(ifPattern, "#if(" + replacement + ")");
            result = result.replaceFirst(elseIfPattern, "#elseif(" + replacement + ")");
        }
        return result;
    }

    private String renderTemplate(String template, VelocityContext ctx) {
        StringWriter writer = new StringWriter();
        try {
            velocityEngine.evaluate(ctx, writer, "branch-sim", template);
        } catch (Exception e) {
            return "[렌더링 오류: " + e.getMessage() + "]";
        }
        return writer.toString();
    }

    private Properties buildSilentProperties() {
        Properties props = new Properties();
        props.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.NullLogChute");
        props.setProperty("runtime.log", "");
        return props;
    }
}
