package app.studyforge.mobile;
import org.json.*;
final class FeatureWire {
    static JSONObject parse(String text) throws Exception {if(text==null||text.length()>50000)throw new Exception("기능 요청이 너무 큽니다.");boolean quote=false,escape=false;int depth=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(quote){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quote=false;}else if(c=='"')quote=true;else if(c=='{'||c=='['){if(++depth>40)throw new Exception("기능 데이터 구조가 너무 깊습니다.");}else if(c=='}'||c==']')depth--;}return new JSONObject(text);}
}
