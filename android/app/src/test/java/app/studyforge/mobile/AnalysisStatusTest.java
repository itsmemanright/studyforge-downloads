package app.studyforge.mobile;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class AnalysisStatusTest {
 @Test public void failureReasonAndCompletedCountsSurviveFinallyAndReload()throws Exception{JSONObject s=Json.object("phase","reading","done",2,"total",3,"pagesDone",8,"pagesTotal",12);AnalysisStatus.fail(s,new Exception("9쪽의 인용을 확인하지 못했습니다."));AnalysisStatus.pause(s,"일반 중단 문구");JSONObject restored=new JSONObject(s.toString());AnalysisStatus.pause(restored,"앱 재실행");assertTrue(restored.getString("message").contains("9쪽의 인용"));assertEquals(2,restored.getInt("done"));assertEquals(8,restored.getInt("pagesDone"));assertEquals("paused",restored.getString("phase"));}
 @Test public void manualPauseUsesItsOwnMessageAndDoesNotInventAnError()throws Exception{JSONObject s=Json.object("phase","reading");AnalysisStatus.pause(s,"사용자가 중단했어요.");assertEquals("사용자가 중단했어요.",s.getString("message"));assertFalse(s.has("error"));}
 @Test public void readyArtifactIsNeverChangedToPausedByCleanup()throws Exception{JSONObject s=Json.object("phase","ready","preparedKey","prepared_x","message","분석 완료");AnalysisStatus.pause(s,"중단");AnalysisStatus.fail(s,new Exception("이전 작업 실패"));assertEquals("ready",s.getString("phase"));assertEquals("분석 완료",s.getString("message"));assertFalse(s.has("error"));}
}
