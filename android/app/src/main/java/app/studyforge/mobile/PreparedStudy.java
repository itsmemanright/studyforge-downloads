package app.studyforge.mobile;

import org.json.*;

/** Persisted analysis artifact; question generation never starts document reading. */
final class PreparedStudy {
    static String signature(JSONObject workspace)throws Exception{
        JSONArray sources=workspace.getJSONArray("sources"),ids=new JSONArray(),plan=BatchPlan.create(sources);
        for(int i=0;i<plan.length();i++)ids.put(plan.getJSONObject(i).getString("id"));
        JSONArray meta=new JSONArray();for(int i=0;i<sources.length();i++){JSONObject source=sources.getJSONObject(i);meta.put(Json.object("id",source.getString("id"),"title",source.getString("title"),"role",source.getString("role")));}
        return EncryptedBlocks.hash(Json.object("plan",ids,"sourceMetadata",meta,"subject",workspace.optString("subject"),"scope",workspace.optString("scope"),"priority",workspace.optString("priority")).toString());
    }
    static JSONObject context(JSONObject w)throws Exception{JSONArray ids=new JSONArray();for(int i=0;i<w.getJSONArray("sources").length();i++)ids.put(w.getJSONArray("sources").getJSONObject(i).getString("id"));return Json.object("subject",w.optString("subject"),"scope",w.optString("scope"),"priority",w.optString("priority"),"sourceIds",ids);}
    static JSONObject build(JSONObject workspace,AnalysisPipeline.Result result,String key)throws Exception{
        return Json.object("signature",signature(workspace),"sources",result.sources(),"overview",result.overview(),"directives",result.directives(),"receipt",Json.copy(result.receipt()).put("preparedKey",key).put("context",context(workspace)));
    }
    static JSONArray require(JSONObject artifact,JSONObject workspace)throws Exception{
        if(!artifact.has("signature")||!artifact.getString("signature").equals(signature(workspace)))throw new Exception("자료나 시험 기준이 바뀌었습니다. 자료 분석을 먼저 완료해 주세요.");
        JSONArray active=new JSONArray(),sources=artifact.getJSONArray("sources");for(int i=0;i<sources.length();i++)if(sources.getJSONObject(i).getJSONArray("evidence").length()>0)active.put(sources.getJSONObject(i));
        if(active.length()==0)throw new Exception("분석된 근거가 없습니다. 자료 분석을 먼저 완료해 주세요.");return active;
    }
}
