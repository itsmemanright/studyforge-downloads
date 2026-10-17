package app.studyforge.mobile;

import org.json.JSONObject;

/** Keep the actual failure visible across pause/finally and process recreation. */
final class AnalysisStatus {
    static boolean ready(JSONObject status){return status!=null&&("complete".equals(status.optString("phase"))||("ready".equals(status.optString("phase"))&&status.has("preparedKey")));}
    static void pause(JSONObject status,String fallback)throws Exception{
        if(status==null||ready(status))return;
        String error=status.optString("error");
        status.put("phase","paused").put("message",error.isBlank()?fallback:"분석을 멈췄습니다. "+error+" 완료된 구간은 보존되어 있습니다.");
    }
    static void fail(JSONObject status,Exception failure)throws Exception{
        if(status==null||ready(status))return;
        String message=failure.getMessage();
        status.put("error",message==null||message.isBlank()?failure.getClass().getSimpleName():message);
        pause(status,"");
    }
}
