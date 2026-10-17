package app.studyforge.mobile;

import java.net.URI;
import java.io.IOException;
import org.json.*;

/** Accept complete transcript blocks, never a login page or the visible/virtualized DOM. */
final class NoteTranscript {
    static boolean allowedShare(String url) {
        try {URI u=URI.create(url);return "https".equals(u.getScheme())&&"clovanote.naver.com".equals(u.getHost())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&u.getPath().matches("/s/[A-Za-z0-9_-]+/?");} catch(Exception e){return false;}
    }
    static boolean noteResponse(String url) {
        try {URI u=URI.create(url);String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("clovanote.naver.com")||h.endsWith(".naver.com"))&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&u.getPath().matches("/v2/w/[^/]+/notes/[^/]+/?");}catch(Exception e){return false;}
    }
    static JSONObject parse(String response) throws Exception {
        JSONObject data=new JSONObject(response).getJSONObject("contents");
        JSONArray blocks=data.getJSONObject("script").getJSONArray("blockList");
        if(blocks.length()==0)throw new IOException("전사본이 비어 있거나 아직 변환 중입니다.");
        StringBuilder text=new StringBuilder();int count=0;
        for(int i=0;i<blocks.length();i++) {
            JSONObject block=blocks.getJSONObject(i);
            if(!(block.opt("text") instanceof String))throw new IOException("전사본 일부를 읽지 못했습니다. TXT로 가져와 주세요.");
            String line=block.getString("text").trim();if(line.isEmpty())continue;
            if(text.length()>0)text.append("\n\n");
            if(block.has("start")){long seconds=Math.max(0,block.getLong("start")/1000);text.append(String.format(java.util.Locale.ROOT,"[%02d:%02d:%02d] ",seconds/3600,(seconds/60)%60,seconds%60));}
            text.append(line);count++;
            if(text.length()>100000)throw new IOException("전사본이 10만 자를 넘습니다. 전체 내용을 유지하려면 TXT 파일로 추가해 주세요.");
        }
        if(text.length()<30)throw new IOException("분석할 전사 내용이 너무 짧습니다.");
        JSONObject info=data.optJSONObject("noteInfo");String title=info==null?"":info.optString("noteName").trim();
        return Json.object("title",title.isEmpty()?"클로바노트 전사본":title,"text",text.toString(),"blocks",count);
    }
}
