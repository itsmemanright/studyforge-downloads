package app.studyforge.mobile;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class UiConfigTest {
 @Test public void defaultsAndUserLayoutRoundTrip() throws Exception {JSONObject c=UiConfig.defaults();JSONArray order=c.getJSONArray("studyOrder");Object first=order.get(0),last=order.get(7);order.put(0,last).put(7,first);c.put("fontSize",24).put("theme","blue").put("hidden",Json.array("review-card"));assertEquals(c.toString(),UiConfig.validate(c).toString());}
 @Test public void blocksCodeAndInvalidValues() throws Exception {for(JSONObject bad:new JSONObject[]{Json.object("javascript","alert(1)"),Json.object("fontSize",25),Json.object("fontSize",16.5),Json.object("showSummary","false"),Json.object("theme","url(evil)"),Json.object("hidden",Json.array("questions")),Json.object("hidden",Json.array("range-card","range-card"))}){try{UiConfig.validate(bad);fail("must reject "+bad);}catch(Exception expected){}}}
 @Test public void cannotLoseOrDuplicateRequiredSections() throws Exception {JSONObject c=UiConfig.defaults();c.getJSONArray("materialsOrder").put(0,"account-card");try{UiConfig.validate(c);fail();}catch(Exception expected){}c=UiConfig.defaults();c.getJSONArray("studyOrder").remove(4);try{UiConfig.validate(c);fail();}catch(Exception expected){}}
}
