package app.studyforge.mobile;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;

public class NoteTranscriptTest {
    private JSONObject note(JSONArray blocks)throws Exception{return Json.object("contents",Json.object("noteInfo",Json.object("noteName","수업 전사본"),"script",Json.object("blockList",blocks)));}
    @Test public void importsAllBlocksNotJustVisiblePage()throws Exception{
        JSONArray blocks=new JSONArray();for(int i=0;i<120;i++)blocks.put(Json.object("start",i*60000,"text","수업 내용 "+i+" : 전체 전사본의 마지막 부분도 보존합니다."));
        JSONObject result=NoteTranscript.parse(note(blocks).toString());assertEquals(120,result.getInt("blocks"));assertTrue(result.getString("text").contains("[01:59:00] 수업 내용 119"));assertEquals("수업 전사본",result.getString("title"));
    }
    @Test public void rejectsMenusLoginAndMissingBlocks()throws Exception{
        assertThrows(Exception.class,()->NoteTranscript.parse("<html>네이버 로그인</html>"));
        assertThrows(Exception.class,()->NoteTranscript.parse(Json.object("contents",Json.object("text","로그인 후 공유 노트를 확인하세요")).toString()));
        assertThrows(Exception.class,()->NoteTranscript.parse(note(Json.array(Json.object("text","충분한 내용이 있는 첫 번째 기록입니다. 긴 수업 내용이 있습니다."),Json.object("start",3000))).toString()));
    }
    @Test public void failsRatherThanTruncatingLongTranscript()throws Exception{
        assertThrows(Exception.class,()->NoteTranscript.parse(note(Json.array(Json.object("text","가".repeat(100001)))).toString()));
    }
    @Test public void onlyAcceptsActualShareLinksAndExactReadEndpoints(){
        assertTrue(NoteTranscript.allowedShare("https://clovanote.naver.com/s/example123"));
        for(String u:new String[]{"https://clovanote.naver.com/","https://clovanote.naver.com@evil.com/s/x","https://clovanote.naver.com:444/s/x","https://evil.com/s/x","http://clovanote.naver.com/s/x"})assertFalse(u,NoteTranscript.allowedShare(u));
        assertTrue(NoteTranscript.noteResponse("https://clovanote-api.naver.com/v2/w/workspace/notes/note"));
        assertFalse(NoteTranscript.noteResponse("https://evil.com/v2/w/workspace/notes/note"));
        assertFalse(NoteTranscript.noteResponse("https://clovanote-api.naver.com/v2/w/workspace/notes/note/shared-notes"));
    }
}
