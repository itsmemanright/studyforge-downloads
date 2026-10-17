package app.studyforge.mobile;

import org.json.*;
import java.util.*;

final class BatchPlan {
    static final int PAGES=4,CHARS=18000;
    static JSONArray create(JSONArray sources) throws Exception {
        JSONArray result=new JSONArray();Set<String> ids=new HashSet<>();
        for(int i=0;i<sources.length();i++){
            JSONObject source=sources.getJSONObject(i);String id=source.getString("id");if(!ids.add(id))throw new Exception("자료 ID가 중복되었습니다.");JSONObject doc=source.optJSONObject("document");
            if(doc!=null&&"pdf".equals(doc.optString("kind"))){int pages=doc.getInt("pages");if(pages<1)throw new Exception("빈 PDF입니다.");for(int p=1;p<=pages;p+=PAGES)result.put(unit(source,"pdf",p,Math.min(p+PAGES-1,pages)).put("loadStart",Math.max(1,p-1)).put("loadEnd",Math.min(p+PAGES,pages)));}
            else if(doc!=null&&"image".equals(doc.optString("kind")))result.put(unit(source,"photo",1,1).put("loadStart",1).put("loadEnd",1).put("work",1));
            else {int chars=doc==null?source.optString("text").length():doc.getInt("characters");for(int start=0;start<chars;start+=CHARS)result.put(unit(source,"text",start,Math.min(chars,start+CHARS)).put("loadStart",Math.max(0,start-300)).put("loadEnd",Math.min(chars,start+CHARS+300)));
                JSONArray images=source.optJSONArray("images");if(images!=null)for(int from=0;from<images.length();from+=PAGES)result.put(unit(source,"images",from,Math.min(images.length(),from+PAGES)));
            }
        }if(result.length()==0)throw new Exception("분석할 자료가 없습니다.");return result;
    }
    private static JSONObject unit(JSONObject source,String kind,int start,int end) throws Exception {
        String fingerprint=source.has("document")?source.getJSONObject("document").getString("sha256"):EncryptedBlocks.hash(Json.object("id",source.getString("id"),"role",source.getString("role"),"title",source.getString("title"),"text",source.optString("text"),"images",source.optJSONArray("images")).toString());String id=EncryptedBlocks.hash(source.getString("id")+fingerprint+kind+start+":"+end);
        return Json.object("id",id,"source_id",source.getString("id"),"title",source.getString("title"),"kind",kind,"start",start,"end",end,"work",kind.equals("pdf")?end-start+1:end-start,"fingerprint",fingerprint);
    }
    static JSONObject source(JSONArray sources,String id) throws Exception {for(int i=0;i<sources.length();i++)if(id.equals(sources.getJSONObject(i).optString("id")))return sources.getJSONObject(i);throw new Exception("원본 자료를 찾지 못했습니다.");}
    static int totalPages(JSONArray plan) throws Exception {int n=0;for(int i=0;i<plan.length();i++){JSONObject u=plan.getJSONObject(i);if(!"text".equals(u.getString("kind")))n+=u.getInt("work");}return n;}
}
