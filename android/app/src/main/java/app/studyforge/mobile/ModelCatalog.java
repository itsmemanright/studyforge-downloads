package app.studyforge.mobile;
import org.json.*;
import java.util.*;

final class ModelCatalog {
    static JSONArray parse(JSONObject response) throws Exception {
        JSONArray rows=response.optJSONArray("models"),out=new JSONArray();boolean subscription=rows!=null;if(rows==null)rows=response.optJSONArray("data");if(rows==null)throw new Exception("모델 목록의 응답 형식을 읽지 못했습니다. 다시 불러오기를 눌러 주세요.");Set<String> seen=new HashSet<>();
        for(int i=0;i<rows.length();i++){JSONObject m=rows.optJSONObject(i);if(m==null)continue;if(subscription&&!"list".equals(m.optString("visibility")))continue;String slug=m.optString(subscription?"slug":"id").trim();if(slug.isEmpty()||slug.length()>200||!slug.matches("[a-zA-Z0-9._:/-]+")||!seen.add(slug))continue;String name=m.optString("display_name",slug).trim();out.put(Json.object("slug",slug,"name",name.isEmpty()?slug:name));}
        if(out.length()==0)throw new Exception("현재 계정에서 표시할 수 있는 모델이 없습니다. 구독 사용 권한과 계정 지원 여부를 확인한 뒤 다시 불러오세요.");return out;
    }
}
