package app.studyforge.mobile;
import org.json.*;
final class StudyRecords {
    static JSONObject reset(JSONObject dataset) throws Exception {return Json.copy(dataset).put("answers",new JSONObject()).put("uncertain",new JSONObject()).put("resetAt",System.currentTimeMillis());}
}
