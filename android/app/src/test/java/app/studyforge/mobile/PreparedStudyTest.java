package app.studyforge.mobile;

import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;

public class PreparedStudyTest {
    JSONObject workspace()throws Exception{return Json.object("subject","간호","scope","기말","priority","textbook","generationMode","concept","count",5,"sources",Json.array(Json.object("id","book","title","교재","role","textbook","text","원문을 읽고 분석한 교재 내용입니다. 자료 분석 결과와 출제를 분리합니다.","images",new JSONArray())));}
    JSONObject artifact(JSONObject w)throws Exception{JSONObject source=Json.copy(w.getJSONArray("sources").getJSONObject(0)).put("evidence",Json.array(Json.object("id","evidence","source_id","book","page",1,"quote","교재 내용","text","분석한 근거","topic","개념","kind","concept")));return PreparedStudy.build(w,new AnalysisPipeline.Result(Json.array(source),"완료한 분석 개요",new JSONArray(),Json.object("key","analysis_key","phase","ready","done",1,"total",1)),"prepared_key");}
    @Test public void analysisArtifactPersistsAndCanBeReusedWithDifferentQuestionSettings()throws Exception{
        JSONObject w=workspace();w.put("dataset",Json.object("answers",Json.object("old",1)));JSONObject prepared=artifact(w),restored=new JSONObject(prepared.toString());w.put("count",15).put("generationMode","past_expand");
        assertEquals(1,PreparedStudy.require(restored,w).length());assertEquals("prepared_key",restored.getJSONObject("receipt").getString("preparedKey"));assertEquals(1,w.getJSONObject("dataset").getJSONObject("answers").getInt("old"));
    }
    @Test public void changedScopeOrSourceContentCannotGenerateFromAStaleAnalysis()throws Exception{
        JSONObject w=workspace(),prepared=artifact(w);w.put("scope","다른 범위");assertThrows(Exception.class,()->PreparedStudy.require(prepared,w));w.put("scope","기말");w.getJSONArray("sources").getJSONObject(0).put("text","교재 원문이 달라졌습니다.");assertThrows(Exception.class,()->PreparedStudy.require(prepared,w));
    }
    @Test public void emptyOrWrongArtifactRequiresAnalysisInsteadOfStartingReading()throws Exception{
        JSONObject w=workspace();assertThrows(Exception.class,()->PreparedStudy.require(new JSONObject(),w));JSONObject prepared=artifact(w);prepared.getJSONArray("sources").getJSONObject(0).put("evidence",new JSONArray());assertThrows(Exception.class,()->PreparedStudy.require(prepared,w));
    }
}
