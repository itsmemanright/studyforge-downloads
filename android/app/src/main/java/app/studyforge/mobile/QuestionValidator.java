package app.studyforge.mobile;

import org.json.*;
import java.util.*;

final class QuestionValidator {
    static JSONObject validate(JSONObject result,JSONArray sources,int count,boolean original) throws Exception {
        if(result.has("error"))throw new Exception("자료가 충분하지 않아 출제하지 않았습니다. "+result.optString("error").substring(0,Math.min(180,result.optString("error").length())));
        Map<String,JSONObject> material=new HashMap<>();for(int i=0;i<sources.length();i++){JSONObject s=sources.getJSONObject(i);material.put(s.getString("id"),s);}
        Json.required(result,"title");JSONArray questions=result.getJSONArray("questions");if(questions.length()!=count)throw new Exception("요청한 수만큼 검수된 문제가 만들어지지 않았습니다. 자료를 보충하거나 문제 수를 줄여 주세요.");
        Set<String> stems=new HashSet<>();
        for(int i=0;i<questions.length();i++){JSONObject q=questions.getJSONObject(i);String stem=Json.required(q,"stem");if(!stems.add(Json.normalize(stem)))throw new Exception("중복 문제가 있어 결과를 보류했습니다.");JSONArray options=q.getJSONArray("options"),explanations=q.getJSONArray("option_explanations");if(options.length()!=5||explanations.length()!=5)throw new Exception("보기·해설 구성이 올바르지 않아 결과를 보류했습니다.");Object a=q.get("answer");if(!(a instanceof Number)||((Number)a).doubleValue()!=((Number)a).intValue()||q.getInt("answer")<0||q.getInt("answer")>4)throw new Exception("정답 번호를 확인하지 못했습니다.");Set<String> unique=new HashSet<>();for(int j=0;j<5;j++){if(!(options.get(j) instanceof String)||options.getString(j).trim().isEmpty()||!unique.add(Json.normalize(options.getString(j))))throw new Exception("중복되거나 비어 있는 보기가 있습니다.");if(explanations.getString(j).trim().isEmpty())throw new Exception("보기 해설이 부족합니다.");}
            Json.required(q,"explanation");Json.required(q,"topic");JSONArray citations=q.getJSONArray("citations");if(citations.length()==0)throw new Exception("원본 근거가 없는 문항을 보류했습니다.");for(int j=0;j<citations.length();j++)citation(citations.getJSONObject(j),material);
            if(original){JSONObject key=q.optJSONObject("answer_citation");if(key==null)throw new Exception("원문 문항의 정답표 근거를 확인하지 못했습니다.");citation(key,material);String role=material.get(key.getString("source_id")).getString("role");if(!Arrays.asList("workbook","past","answerkey").contains(role))throw new Exception("문제집·기출의 정답 또는 해설 근거가 필요합니다.");}
            else {String correct=Json.normalize(options.getString(q.getInt("answer")));if(correct.length()>=12&&Json.normalize(stem).contains(correct))throw new Exception("질문에 정답이 그대로 드러나는 문항을 보류했습니다. 다시 만들어 주세요.");if(stem.matches("(?s).*(PPT|슬라이드|교수 자료).{0,12}(따르면|제시한).*"))throw new Exception("시험 문항의 질문 표현을 다시 다듬어야 합니다.");}
        }
        JSONArray summary=result.optJSONArray("summary");if(summary==null)throw new Exception("자료 정리가 누락되었습니다.");for(int i=0;i<summary.length();i++){JSONObject point=summary.getJSONObject(i);Json.required(point,"text");citation(point,material);}
        JSONArray directives=result.optJSONArray("directives");if(directives==null){directives=new JSONArray();result.put("directives",directives);}for(int i=0;i<directives.length();i++){JSONObject d=directives.getJSONObject(i);citation(d,material);String role=material.get(d.getString("source_id")).getString("role");if(!Arrays.asList("transcript","slides").contains(role))throw new Exception("교수님 학습 지시는 수업 자료의 직접 근거가 필요합니다.");if(!Arrays.asList("remember","skip","awareness","excluded").contains(d.optString("kind")))throw new Exception("학습 지시 분류를 확인하지 못했습니다.");}
        return result;
    }
    static void citation(JSONObject c,Map<String,JSONObject> material) throws Exception {
        JSONObject source=material.get(Json.required(c,"source_id"));if(source==null)throw new Exception("없는 자료를 근거로 표시한 결과를 보류했습니다.");int page=c.getInt("page");String quote=Json.required(c,"quote");
        JSONArray evidence=source.optJSONArray("evidence");if(evidence!=null){boolean found=false;for(int i=0;i<evidence.length();i++){JSONObject note=evidence.getJSONObject(i);found|=note.getInt("page")==page&&Json.normalize(note.getString("quote")).contains(Json.normalize(quote));}if(!found)throw new Exception("분석된 원본 발췌에서 인용을 찾지 못했습니다.");return;}
        JSONArray images=source.optJSONArray("images");if(images!=null&&images.length()>0){boolean found=false;for(int i=0;i<images.length();i++)found|=images.getJSONObject(i).getInt("page")==page;if(!found)throw new Exception("선택하지 않은 원본 페이지를 인용한 문항을 보류했습니다.");}
        else if(page!=1||!Json.normalize(source.optString("text")).contains(Json.normalize(quote)))throw new Exception("텍스트 근거의 인용문을 원본에서 찾지 못해 결과를 보류했습니다.");
    }
    static JSONObject parse(String response) throws Exception {String s=response.trim();if(s.startsWith("```")){int begin=s.indexOf('\n'),end=s.lastIndexOf("```");if(begin>=0&&end>begin)s=s.substring(begin+1,end).trim();}return new JSONObject(s);}
}
