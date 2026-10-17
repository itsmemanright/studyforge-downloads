package app.studyforge.mobile;

import org.json.*;
import java.util.*;

/** Inferred exam material has an explicit provenance contract; it is never an authenticated original. */
final class PastExpansion {
    static final String MODE="past_expand";
    static final String RULES="""
        [족보 최대한 활용 모드: 일반 모드의 '제공 자료만으로 정답 확정' 조건에 대한 명시적 예외]
        불완전한 족보라도 버리지 말고 시험 범위 안에서 최대한 학습문항으로 활용한다.
        1. 문제는 있으나 정답이 없거나 불명확하면 교재·PPT와 개념을 대조해 가장 타당한 답을 추론한다(answer_inferred).
        2. 답·정답 키워드만 있으면 '어떤 질문이었을까'를 추정해 한 개 정답의 새 문제·그럴듯한 보기를 만든다(answer_reconstructed).
        3. 이미 완전한 족보도 출제 의도·혼동 개념·사례 적용을 확장한 새 변형문항으로 활용한다(concept_variation).
        원문·정답표를 확보했다고 가장하지 않는다. 이 모드의 kind는 past_inferred, past_reconstructed, past_variation 중 하나다.
        개념은 교재를 우선하고 확정 시험 범위·제외 지시는 반드시 따른다. 근거 없이 교수 강조나 실제 출제 빈도를 만들지 않는다.
        정답표가 교재와 충돌하면 원래 표기와 교재 기준 제안 답의 차이를 설명한다. 보기 번호만 남은 답은 원래 보기를 복원할 수 없다.
        그 경우 원래 정답을 안다고 하지 말고 나머지 단서로 학습문제를 만들거나 unusable로 남긴다.
        일반 지식·추론도 보충에 사용할 수 있으나 basis.type=general_knowledge, confidence=low, 인용 없는 추론임을 명시한다.
        자료가 뒷받침하면 basis.type=materials로 실제 교재·수업자료 인용을 붙인다. 인용문은 원문 그대로, 없는 페이지·지침을 만들지 않는다.
        정답의 '확실도'는 AI 자체 판단이며 정확도 확률이 아니다. 자료가 있어도 모호하면 medium 또는 low로 표시한다.
        추가한 환자 조건·보기·질문을 added에 명시하고 실제 원문과 혼동시키지 않는다. 풀이 해설에 제안 답·추론 이유·대안 가능성을 쓴다.
        모든 문항은 기존 공통 스키마에 past 객체를 추가한다:
        "past":{"method":"answer_inferred 또는 answer_reconstructed 또는 concept_variation",
          "fragment":{"source_id":"족보 ID","page":1,"quote":"실제로 남은 족보 단서"},
          "missing":["원문에서 없는 부분"],"added":["AI가 추가·변형한 부분"],
          "basis":{"type":"materials 또는 general_knowledge","text":"제안 답을 채택한 근거 요약","citations":[{"source_id":"교재 등 ID","page":1,"quote":"원문"}]},
          "confidence":"high 또는 medium 또는 low","confidence_reason":"그 판단의 이유",
          "assumptions":["추가 가정"],"alternatives":["조건이 달라지면 가능한 다른 답 또는 질문"],"source_answer":"남아 있는 답 그대로, 없으면 빈 문자열"}
        citations에는 족보 단서와 사용한 교재 근거를 포함하되 족보 단서만으로 정답이 검증됐다고 설명하지 않는다.
        summary에는 자료에 실제로 있는 내용만 인용한다. 일반 지식 보충은 각 문항의 basis.text에만 쓴다.
        최상위 past_coverage 배열에 입력된 족보의 개별 문제·답·키워드 조각을 모두 검토해 남긴다:
        [{"fragment":{"source_id":"족보 ID","page":1,"quote":"원문 단서"},"label":"어떤 조각인지",
          "status":"used 또는 pending 또는 out_of_scope 또는 unusable","reason":"활용·보류 이유","question_indices":[0]}]
        question_indices는 이번 결과 questions의 0기준 인덱스이며 used 외에는 빈 배열이다. 각 문항은 자기 단서의 used 항목에 연결돼야 한다.
        동일 조각에서 여러 판단 목표를 만들 수 있으나 여러 문항이 생겼다고 원래 기출 수를 늘려 세지 않는다.
        이미 만든 문항 목록을 참고해 아직 활용하지 않은 단서를 우선하고 표현·순서만 바꾼 중복은 만들지 않는다.
        요청 수는 이번 최대 문항 수다. 출제 가능한 수가 적으면 억지로 채우지 말고 0~최대 수만 반환하고 남은 단서는 coverage에 보관한다.
        used는 이번 문항에 사용한 단서만 뜻한다. 이전에 썼지만 이번에 쓰지 않은 조각은 pending에 이전 사용·추가 확장 가능성을 적는다.
        자료 안에 학습 단서가 전혀 없으면 그 사실을 error로 알린다. 추론해도 판단 불가능하거나 범위 밖인 내용은 복습 후보 목록에만 남긴다.
        """;

    static void requirePast(JSONArray sources) throws Exception {for(int i=0;i<sources.length();i++)if("past".equals(sources.getJSONObject(i).optString("role")))return;throw new Exception("족보 최대한 활용 모드에는 ‘족보·기출’로 지정한 자료가 필요합니다.");}
    static String key(JSONObject c) throws Exception {return c.getString("source_id")+"/"+c.getInt("page")+"/"+Json.normalize(c.getString("quote"));}
    static Map<String,JSONObject> sourceMap(JSONArray sources) throws Exception {Map<String,JSONObject> m=new HashMap<>();for(int i=0;i<sources.length();i++){JSONObject s=sources.getJSONObject(i);m.put(s.getString("id"),s);}return m;}
    static void fragment(JSONObject c,Map<String,JSONObject> sources) throws Exception {QuestionValidator.citation(c,sources);if(!"past".equals(sources.get(c.getString("source_id")).optString("role")))throw new Exception("재구성한 문항을 실제 족보 단서와 연결하지 못했습니다.");}
    static void strings(JSONObject o,String key,boolean required) throws Exception {JSONArray a=o.getJSONArray(key);if(required&&a.length()==0)throw new Exception("족보의 추론·변형 내역이 누락되었습니다.");for(int i=0;i<a.length();i++)if(!(a.get(i) instanceof String)||a.getString(i).trim().isEmpty())throw new Exception("족보 추론 설명의 형식이 잘못되었습니다.");}
    static JSONObject validate(JSONObject result,JSONArray sources,int maximum) throws Exception {
        requirePast(sources);if(result.has("error"))return QuestionValidator.validate(result,sources,maximum,false);
        JSONArray qs=result.getJSONArray("questions");if(qs.length()>maximum)throw new Exception("이번 최대 문항 수를 초과했습니다.");
        QuestionValidator.validate(result,sources,qs.length(),false);Map<String,JSONObject> map=sourceMap(sources);
        for(int i=0;i<qs.length();i++){
            JSONObject q=qs.getJSONObject(i),p=q.getJSONObject("past");String method=Json.required(p,"method"),kind=q.optString("kind");
            String expected=switch(method){case "answer_inferred"->"past_inferred";case "answer_reconstructed"->"past_reconstructed";case "concept_variation"->"past_variation";default->throw new Exception("족보 활용 방식이 잘못되었습니다.");};
            if(!expected.equals(kind))throw new Exception("추론 문항을 원문으로 표시한 결과를 보류했습니다.");fragment(p.getJSONObject("fragment"),map);
            strings(p,"missing",!"concept_variation".equals(method));strings(p,"added",true);strings(p,"assumptions",false);strings(p,"alternatives",false);
            if(!(p.get("source_answer") instanceof String))throw new Exception("족보의 원래 답 표기가 잘못되었습니다.");
            String raw=p.getString("source_answer");if(!raw.isEmpty()&&!Json.normalize(p.getJSONObject("fragment").getString("quote")).contains(Json.normalize(raw)))throw new Exception("원래 남은 답을 족보 단서에서 확인하지 못했습니다.");
            if(!Arrays.asList("high","medium","low").contains(Json.required(p,"confidence")))throw new Exception("추론 확실도 표시가 누락되었습니다.");Json.required(p,"confidence_reason");
            JSONObject basis=p.getJSONObject("basis");Json.required(basis,"text");JSONArray cites=basis.getJSONArray("citations");String type=Json.required(basis,"type");
            if("materials".equals(type)){if(cites.length()==0)throw new Exception("교재·수업 자료의 정답 근거 인용이 필요합니다.");for(int j=0;j<cites.length();j++){JSONObject cite=cites.getJSONObject(j);QuestionValidator.citation(cite,map);if("past".equals(map.get(cite.getString("source_id")).optString("role")))throw new Exception("족보 단서만으로 정답이 검증됐다고 표시할 수 없습니다.");}}
            else if(!"general_knowledge".equals(type)||cites.length()!=0||!"low".equals(p.getString("confidence")))throw new Exception("자료 근거가 없는 추론은 낮은 확실도로 구분해야 합니다.");
        }
        JSONArray coverage=result.getJSONArray("past_coverage");if(coverage.length()==0)throw new Exception("족보 활용·보류 목록이 누락되었습니다.");Set<String> seen=new HashSet<>(),coveredSources=new HashSet<>();Set<Integer> linked=new HashSet<>();
        for(int i=0;i<coverage.length();i++){
            JSONObject row=coverage.getJSONObject(i),c=row.getJSONObject("fragment");fragment(c,map);coveredSources.add(c.getString("source_id"));String key=key(c);if(!seen.add(key))throw new Exception("같은 족보 단서를 중복 집계했습니다.");Json.required(row,"label");Json.required(row,"reason");String status=Json.required(row,"status");JSONArray indices=row.getJSONArray("question_indices");
            if(!Arrays.asList("used","pending","out_of_scope","unusable").contains(status)||("used".equals(status)?indices.length()==0:indices.length()!=0))throw new Exception("족보 활용 목록의 상태·문항 연결이 잘못되었습니다.");
            Set<Integer> rowIndices=new HashSet<>();for(int j=0;j<indices.length();j++){Object raw=indices.get(j);if(!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new Exception("족보 연결 번호가 잘못되었습니다.");int index=((Number)raw).intValue();if(index<0||index>=qs.length()||!rowIndices.add(index)||!key.equals(key(qs.getJSONObject(index).getJSONObject("past").getJSONObject("fragment"))))throw new Exception("족보 단서와 생성 문항의 연결이 맞지 않습니다.");linked.add(index);}
        }
        if(linked.size()!=qs.length())throw new Exception("활용 목록에서 빠진 족보 문항이 있습니다.");for(JSONObject source:map.values())if("past".equals(source.optString("role"))&&!coveredSources.contains(source.getString("id")))throw new Exception("검토 목록에서 빠진 족보 자료가 있습니다.");return result;
    }
    static JSONArray previous(JSONObject workspace,JSONArray sources) throws Exception {
        Set<String> ids=sourceMap(sources).keySet();JSONArray all=new JSONArray(),rooms=workspace.optJSONArray("rooms");if(rooms!=null)for(int i=0;i<rooms.length();i++)all.put(rooms.getJSONObject(i));if(workspace.optJSONObject("dataset")!=null)all.put(workspace.getJSONObject("dataset"));
        JSONArray stems=new JSONArray();for(int i=0;i<all.length();i++){JSONArray qs=all.getJSONObject(i).optJSONArray("questions");if(qs==null)continue;for(int j=0;j<qs.length();j++){JSONObject q=qs.getJSONObject(j),p=q.optJSONObject("past");if(p!=null&&ids.contains(p.getJSONObject("fragment").getString("source_id")))stems.put(q.getString("stem"));}}
        return stems;
    }
    static void rejectRepeated(JSONObject result,JSONArray previous) throws Exception {Set<String> stems=new HashSet<>();for(int i=0;i<previous.length();i++)stems.add(Json.normalize(previous.getString(i)));JSONArray qs=result.getJSONArray("questions");for(int i=0;i<qs.length();i++)if(stems.contains(Json.normalize(qs.getJSONObject(i).getString("stem"))))throw new Exception("이미 만든 족보 문항과 중복됩니다. 다른 판단 목표로 다시 요청해 주세요.");}
}
