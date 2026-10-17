package app.studyforge.mobile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class Sse {
    static String read(InputStream stream,Api.Progress progress) throws Exception {
        StringBuilder output=new StringBuilder(),event=new StringBuilder();boolean completed=false;int events=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(line.length()>300000)throw new IOException("응답이 너무 큽니다.");if(line.isEmpty()){if(event.length()==0)continue;String data=event.toString();event.setLength(0);if("[DONE]".equals(data))continue;JSONObject e=new JSONObject(data);String type=e.optString("type");if(type.equals("response.output_text.delta")){output.append(e.optString("delta"));if(++events%30==0)progress.update("AI 응답을 받고 있어요…");if(output.length()>250000)throw new IOException("생성 결과가 너무 큽니다.");}else if(type.equals("response.completed")){completed=true;if(output.length()==0){JSONArray items=e.getJSONObject("response").optJSONArray("output");if(items!=null)for(int i=0;i<items.length();i++){JSONArray contents=items.getJSONObject(i).optJSONArray("content");if(contents!=null)for(int j=0;j<contents.length();j++)if(contents.getJSONObject(j).optString("type").equals("output_text"))output.append(contents.getJSONObject(j).optString("text"));}}break;}else if(type.equals("response.failed")||type.equals("error")){JSONObject error=e.optJSONObject("error");if(error==null&&e.optJSONObject("response")!=null)error=e.getJSONObject("response").optJSONObject("error");throw Api.error(400,Json.object("error",error==null?Json.object("code","response_failed"):error).toString());}else if(type.equals("response.incomplete"))throw new IOException("생성이 끝나지 않았습니다. 미완성 문제는 저장하지 않았어요. 자료 범위나 문제 수를 줄여 다시 시도하세요.");}else if(line.startsWith("data:")){if(event.length()>0)event.append('\n');event.append(line.substring(5).replaceFirst("^\\s+", ""));if(event.length()>500000)throw new IOException("응답이 너무 큽니다.");}}}
        if(!completed||output.length()==0)throw new IOException("응답이 완료되기 전에 연결이 끊겼습니다. 미완성 문제는 저장하지 않았어요.");return output.toString();
    }
}
