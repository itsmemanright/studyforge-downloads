package app.studyforge.mobile;

import org.json.*;
final class Json {
    static JSONObject object(Object... pairs) throws JSONException { JSONObject o=new JSONObject();for(int i=0;i<pairs.length;i+=2)o.put((String)pairs[i],pairs[i+1]);return o; }
    static JSONArray array(Object... values) { JSONArray a=new JSONArray();for(Object v:values)a.put(v);return a; }
    static JSONObject copy(JSONObject o) throws JSONException {return new JSONObject(o.toString());}
    static String normalize(String s) { return s.replaceAll("\\s+","").toLowerCase(java.util.Locale.ROOT); }
    static String required(JSONObject o,String key) throws Exception {String s=o.optString(key,"").trim();if(s.isEmpty()||s.length()>60000)throw new Exception("응답의 "+key+" 항목이 올바르지 않습니다.");return s;}
}
