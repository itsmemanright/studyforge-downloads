package app.studyforge.mobile;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

final class Api {
    interface Progress {void update(String message);}
    static final java.util.Set<HttpsURLConnection> active=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static HttpsURLConnection open(String url,String token) throws Exception {URI u=URI.create(url);if(!"https".equals(u.getScheme())||!("api.openai.com".equals(u.getHost())||"auth.openai.com".equals(u.getHost())))throw new IOException("허용되지 않은 연결입니다.");HttpsURLConnection c=(HttpsURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(240000);c.setRequestProperty("Accept","application/json");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);return c;}
    static JSONObject json(String url,String method,String contentType,String body,String token) throws Exception {HttpsURLConnection c=open(url,token);try{c.setRequestMethod(method);if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type",contentType);try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}int status=c.getResponseCode();String text=read(status<400?c.getInputStream():c.getErrorStream(),url.endsWith("/models")?12_000_000:2_000_000);if(status<200||status>=300)throw error(status,text);return new JSONObject(text);}finally{c.disconnect();}}
    static String read(InputStream stream,int limit) throws IOException {if(stream==null)return "";try(stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1){if(out.size()+n>limit)throw new IOException("응답이 너무 큽니다.");out.write(b,0,n);}return out.toString("UTF-8");}}
    static Exception error(int status,String body){String code="";try{Object e=new JSONObject(body).opt("error");code=e instanceof JSONObject?((JSONObject)e).optString("code"):String.valueOf(e);}catch(Exception ignored){}if(!code.matches("[a-zA-Z0-9_\\-]{1,100}"))code="http_"+status;
        String message=switch(code){case "subscription_sharing_usage_limit_exceeded"->"ChatGPT 구독 사용 한도에 도달했습니다. 사용량 설정을 확인하거나 한도가 회복된 뒤 다시 시도하세요.";case "subscription_sharing_usage_unavailable"->"이 계정·앱에서는 구독 사용을 이용할 수 없습니다. ChatGPT의 연결 권한과 지원 여부를 확인하세요.";case "invalid_grant"->"로그인 연결이 만료되었거나 철회되었습니다. 다시 연결해 주세요.";default->status==401?"ChatGPT 연결이 만료되었습니다. 다시 로그인해 주세요.":status==429?"현재 요청이 많거나 사용 한도에 도달했습니다. 잠시 뒤 다시 시도하세요.":"OpenAI 요청을 완료하지 못했습니다. 연결 권한·모델 지원을 확인해 주세요. ("+code+")";};return status==429&&!code.startsWith("subscription_")?new AnalysisPipeline.RateLimited(message):new IOException(message);}
    static String responses(JSONObject request,String token,Progress progress) throws Exception {if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();HttpsURLConnection c=open(OAuthSecurity.RESOURCE+"/responses",token);active.add(c);try{if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","text/event-stream");try(OutputStream out=c.getOutputStream()){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}int status=c.getResponseCode();if(status!=200)throw error(status,read(c.getErrorStream(),200000));return Sse.read(c.getInputStream(),progress);}finally{c.disconnect();active.remove(c);}}
    static void cancel(){for(HttpsURLConnection c:active)c.disconnect();}
}
