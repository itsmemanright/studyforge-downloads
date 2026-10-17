package app.studyforge.mobile;

import org.json.*;
import java.util.*;

/** Review every item, returning edits rather than a second copy of unchanged content. */
final class GenerationReview {
    static final String RULES="""
        [2차 독립 검수의 반환 형식 — 위의 전체 결과 JSON 반환 지시 대신 이 형식을 따른다]
        모든 원본 근거와 전체 초안(개념 정리 포함)을 대조한다. 각 문항을 독립적으로 풀어 정답의 유일성, 모든 보기 해설의 타당성, 범위, 인용, 그럴듯한 오답지, 정답 노출·모호한 질문·순서 단서를 검수한다. 문항 간 중복·정답 위치 편중도 확인한다.
        검수를 생략하지 않으며 문제가 없다는 이유로 전체 지문·보기·해설·인용을 다시 쓰지 않는다. 변경이 필요한 필드만 patch에 반환한다. 문항을 추가·삭제·재정렬하지 않는다.
        {"checks":[{"index":0,"answer":true,"evidence":true,"scope":true,"options":true,"wording":true,"patch":{}}],"document_patch":{},"review_notes":["실제 확인·수정한 내용"]}
        checks는 초안의 0기준 문항마다 정확히 하나 필요하다. 다섯 검수 값은 검수·수정 후 통과했으면 true다. 근거 부족 등 고칠 수 없으면 false를 쓰고 review_notes에 이유를 쓴다. 통과했다고 가장하지 않는다.
        patch에는 수정할 stem, topic, kind, original_number, options, answer, explanation, option_explanations, citations, answer_citation, past만 쓸 수 있다. 변경 배열은 배열 전체를 반환한다. 바꿀 것이 없으면 {}다.
        document_patch는 title, overview, summary, directives, conflicts, past_coverage만 변경할 수 있다. 교수 지시는 slides/transcript의 직접 발언만 허용한다. 잘못된 교재 기반 지시는 directives:[]로 제거한다. 개념 정리도 인용·범위·정확성을 검수하되 문제가 없는 항목은 재작성하지 않는다.
        원문 모드에서는 원문 보존, 족보 활용 모드에서는 추론 표시·단서·정답 근거·활용 목록의 연결까지 확인한다. question_indices는 기존 인덱스를 유지한다.
        JSON 객체 하나만 반환한다. 불필요한 재진술로 결과를 늘리지 말되 정답·오답 설명의 근거를 줄이거나 요약해서 없애지 않는다.
        """;
    private static final Set<String> QUESTION=Set.of("stem","topic","kind","original_number","options","answer","explanation","option_explanations","citations","answer_citation","past");
    private static final Set<String> DOCUMENT=Set.of("title","overview","summary","directives","conflicts","past_coverage");
    static JSONObject apply(JSONObject draft,JSONObject review)throws Exception{
        if(review.has("error"))throw new Exception("2차 검수에서 출제를 보류했습니다. "+review.optString("error"));
        JSONObject result=Json.copy(draft);JSONArray questions=result.getJSONArray("questions"),checks=review.getJSONArray("checks");
        if(checks.length()!=questions.length())throw new Exception("2차 검수에서 빠진 문항이 있어 결과를 보류했습니다.");
        Set<Integer> seen=new HashSet<>();
        for(int i=0;i<checks.length();i++){
            JSONObject check=checks.getJSONObject(i);Object raw=check.get("index");
            if(!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new Exception("검수 문항 번호가 잘못되었습니다.");
            int index=((Number)raw).intValue();if(index<0||index>=questions.length()||!seen.add(index))throw new Exception("중복되거나 잘못 연결된 문항 검수를 보류했습니다.");
            for(String key:new String[]{"answer","evidence","scope","options","wording"})if(!Boolean.TRUE.equals(check.opt(key)))throw new Exception("2차 검수를 통과하지 못한 문항이 있어 결과를 보류했습니다.");
            merge(questions.getJSONObject(index),check.getJSONObject("patch"),QUESTION);
        }
        merge(result,review.getJSONObject("document_patch"),DOCUMENT);
        JSONArray notes=review.getJSONArray("review_notes");if(notes.length()==0)throw new Exception("2차 검수 내역이 누락되었습니다.");
        for(int i=0;i<notes.length();i++)if(!(notes.get(i) instanceof String)||notes.getString(i).trim().isEmpty())throw new Exception("검수 내역이 잘못되었습니다.");
        return result.put("review_notes",new JSONArray(notes.toString()));
    }
    private static void merge(JSONObject target,JSONObject patch,Set<String> allowed)throws Exception{
        for(Iterator<String> keys=patch.keys();keys.hasNext();){String key=keys.next();
            if(!allowed.contains(key)||patch.isNull(key))throw new Exception("허용되지 않은 검수 수정이 있어 결과를 보류했습니다.");
            target.put(key,patch.get(key));
        }
    }
    static JSONArray preparedSummary(JSONArray sources)throws Exception{
        JSONArray summary=new JSONArray();
        for(int i=0;i<sources.length();i++){
            JSONObject source=sources.getJSONObject(i);JSONArray evidence=source.optJSONArray("evidence");
            if(evidence==null)return null;
            for(int j=0;j<evidence.length();j++){
                JSONObject note=evidence.getJSONObject(j);
                summary.put(Json.object("title",note.optString("topic","개념 정리"),"text",note.getString("text"),"source_id",source.getString("id"),"page",note.getInt("page"),"quote",note.getString("quote")));
            }
        }
        return summary.length()==0?null:summary;
    }
}
