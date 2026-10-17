package app.studyforge.mobile;
import org.json.*;

final class FeatureBuilder {
    interface Model {JSONObject call(String instructions,String text) throws Exception;}
    static JSONObject build(String request,JSONObject previous,JSONObject workspace,Model model,Api.Progress progress) throws Exception {
        String context="현재 출제 설정: "+Json.object("generationMode",workspace.optString("generationMode","concept"),"count",workspace.optInt("count",5),"settings",workspace.optJSONObject("settings"))+"\n기능 요청: "+request;
        if(previous!=null)context+="\n수정할 기존 기능: "+Json.object("program",previous.getJSONObject("program"),"checks",previous.optJSONObject("checks"));
        progress.update("1/2 요청한 기능의 화면과 실제 동작 코드를 만들고 있어요…");JSONObject raw=model.call(FeatureProgram.RULES,context),first;try{first=FeatureProgram.validate(raw);}catch(Exception e){progress.update("기능 구조 검사에서 발견한 오류를 한 번 수정합니다…");first=FeatureProgram.validate(model.call(FeatureProgram.RULES,context+"\n아래 초안의 구조 오류를 수정하고 전체 프로그램을 다시 반환하라. 오류: "+e.getMessage()+"\n초안: "+raw));}
        progress.update("2/2 요구사항·동작·저장·실패 처리를 다시 검수하고 있어요…");return FeatureProgram.validate(model.call(FeatureProgram.RULES,context+"\n초안을 검토하고 같은 스키마의 전체 프로그램을 반환하라. 단순 가짜 버튼·미구현·답 노출·현재 데이터 누락·테스트의 형식적 true·기존 데이터 삭제·자동 AI 호출을 확인하고 고쳐라. 실제 런타임 검사는 아직 하지 않았다고 명확히 구분하라.\n초안: "+first));
    }
    static JSONObject context(JSONObject workspace,JSONObject program,JSONObject state) throws Exception {
        JSONObject data=new JSONObject();JSONObject dataset=workspace.optJSONObject("dataset");if(FeatureProgram.permits(program,"study.read")&&dataset!=null){for(String key:new String[]{"id","title","questions","answers","uncertain","summary","directives"})if(dataset.has(key))data.put(key,dataset.get(key));if(data.toString().length()>3_000_000)throw new Exception("이 학습실은 기능 실행에 전달할 자료가 너무 큽니다. 작은 학습실로 나누어 주세요.");}
        return Json.object("data",Json.copy(data),"state",state==null?new JSONObject():Json.copy(state));
    }
    static JSONObject state(JSONObject value) throws Exception {if(value.toString().length()>32000)throw new Exception("기능 보관 데이터는 32KB까지입니다.");return Json.copy(value);}
    static JSONArray practice(JSONObject workspace,JSONArray ids) throws Exception {JSONObject d=workspace.optJSONObject("dataset");if(d==null||ids.length()<1||ids.length()>200)throw new Exception("재풀이할 문제 1~200개를 골라 주세요.");JSONArray result=new JSONArray();java.util.Set<String> seen=new java.util.HashSet<>();for(int i=0;i<ids.length();i++){String id=ids.getString(i);boolean exists=false;JSONArray questions=d.getJSONArray("questions");for(int j=0;j<questions.length();j++)if(id.equals(questions.getJSONObject(j).optString("id")))exists=true;if(!exists)throw new Exception("현재 학습실에 없는 문제입니다.");if(!seen.add(id))throw new Exception("재풀이 목록에 중복된 문제가 있습니다.");result.put(id);}return result;}
}
