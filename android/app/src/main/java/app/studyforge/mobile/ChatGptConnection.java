package app.studyforge.mobile;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ChatGptConnection {
    interface Listener {void authorized();void failed(String message);}
    private final SecureVault vault;
    private final String hostId,appAuthScheme;
    private JSONObject session;
    private volatile ServerSocket callbackServer;
    ChatGptConnection(Context context,SecureVault vault) throws Exception {this.vault=vault;appAuthScheme=context.getPackageName();session=vault.get("connection");var prefs=context.getSharedPreferences("installation",Context.MODE_PRIVATE);String id=prefs.getString("host_id",null);if(id==null){id="urn:uuid:"+UUID.randomUUID();prefs.edit().putString("host_id",id).commit();}hostId=id;}
    synchronized boolean connected(){return !session.optString("access_token").isEmpty();}
    boolean pending(){return callbackServer!=null;}
    synchronized String accountKey() throws Exception {if(!connected())return "draft";return "account_"+hex(java.security.MessageDigest.getInstance("SHA-256").digest((session.getString("sub")+"|"+session.getString("client_id")).getBytes(StandardCharsets.UTF_8))).substring(0,32);}
    static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    synchronized JSONObject publicInfo() throws Exception {return Json.object("connected",connected(),"sharing",OAuthSecurity.planPermission(session.optString("scope")),"email",session.optString("email"),"name",session.optString("name"));}
    String start(boolean newAccount,Listener listener) throws Exception {
        cancel();JSONObject registration=newAccount?new JSONObject():vault.get("registration");String savedClient=registration.optString("client_id");boolean fresh=savedClient.isEmpty();String client=fresh?"dynamic_agent_client":savedClient;
        ServerSocket server=new ServerSocket();server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0));server.setSoTimeout(1000);callbackServer=server;
        String redirect="http://127.0.0.1:"+server.getLocalPort()+"/auth/callback",state=OAuthSecurity.random(),nonce=OAuthSecurity.random(),verifier=OAuthSecurity.random();
        Map<String,String> params=new LinkedHashMap<>();params.put("client_id",client);if(fresh)params.put("agent_name_hint","StudyForge");params.put("ext_agent_host_id",hostId);params.put("response_type","code");params.put("redirect_uri",redirect);params.put("scope",OAuthSecurity.SCOPE);params.put("resource",OAuthSecurity.RESOURCE);params.put("state",state);params.put("nonce",nonce);params.put("code_challenge_method","S256");params.put("code_challenge",OAuthSecurity.challenge(verifier));
        new Thread(()->{try{long until=System.currentTimeMillis()+10*60*1000;while(callbackServer==server&&System.currentTimeMillis()<until){try(Socket socket=server.accept()){socket.setSoTimeout(3000);String request=readLine(socket.getInputStream());if(request==null)continue;CallbackHttp.headers(socket.getInputStream());String[] fields=request.split(" ");if(fields.length!=3||!fields[0].equals("GET")){respond(socket,400,"잘못된 요청입니다.");continue;}URI uri;try{uri=URI.create(fields[1]);}catch(IllegalArgumentException bad){respond(socket,400,"잘못된 로그인 응답입니다.");continue;}if(!"/auth/callback".equals(uri.getPath())||uri.getRawQuery()==null){respond(socket,404,"로그인 응답을 기다리고 있습니다.");continue;}Map<String,String> query;try{query=OAuthSecurity.parseQuery(uri.getRawQuery());}catch(Exception bad){respond(socket,400,"잘못된 로그인 응답입니다.");continue;}if(!OAuthSecurity.equal(state,query.get("state"))){respond(socket,400,"로그인 요청과 응답이 일치하지 않습니다.");continue;}if(query.containsKey("error")){respond(socket,200,"연결이 승인되지 않았습니다. 앱으로 돌아가 다시 시도할 수 있습니다.");throw new IOException("ChatGPT 연결을 취소했거나 권한이 승인되지 않았습니다.");}
                    String code=query.get("code"),issued=query.getOrDefault("client_id",fresh?"":client);if(code==null||issued.isEmpty()||issued.equals("dynamic_agent_client")||issued.length()>256){respond(socket,400,"등록이 완료되지 않았습니다.");throw new IOException("이 앱의 ChatGPT 등록이 완료되지 않았습니다. 계정·앱 지원 여부를 확인해 주세요.");}
                    // Release the browser immediately; token validation can take longer than navigation.
                    respond(socket,200,"승인 응답을 받았습니다. 앱에서 연결을 확인하는 중입니다. 스터디포지로 돌아가 완료 여부를 확인하세요.");socket.shutdownOutput();
                    // The issued public-client ID is retained before code exchange; no client secret exists.
                    vault.put("registration",Json.object("client_id",issued));
                    Map<String,String> exchange=new LinkedHashMap<>();exchange.put("grant_type","authorization_code");exchange.put("client_id",issued);exchange.put("code",code);exchange.put("code_verifier",verifier);exchange.put("redirect_uri",redirect);exchange.put("resource",OAuthSecurity.RESOURCE);
                    JSONObject tokens=Api.json(OAuthSecurity.TOKEN,"POST","application/x-www-form-urlencoded",OAuthSecurity.form(exchange),null);
                    JSONObject jwks=Api.json(OAuthSecurity.ISSUER+"/.well-known/jwks.json","GET",null,null,null),claims=OAuthSecurity.verifyIdToken(tokens.getString("id_token"),jwks,issued,nonce,System.currentTimeMillis()/1000);
                    if(!fresh&&!registration.optString("sub").isEmpty()&&!registration.optString("sub").equals(claims.getString("sub")))throw new IOException("기존 연결과 다른 계정입니다. 다른 계정 연결을 선택해 다시 시도하세요.");
                    if(!OAuthSecurity.planPermission(tokens.optString("scope")))throw new IOException("ChatGPT 구독 사용 권한이 승인되지 않았습니다. Plus·Pro 자격과 연결 권한을 확인하세요.");
                    JSONObject next=makeSession(tokens,claims,issued,null);synchronized(this){session=next;vault.put("connection",next);vault.put("registration",Json.object("client_id",issued,"sub",claims.getString("sub")));}
                    listener.authorized();return;
                }catch(SocketTimeoutException ignored){}}
                if(callbackServer==server)listener.failed("로그인 대기 시간이 지났습니다. 앱에서 다시 연결해 주세요.");
            }catch(Exception e){if(callbackServer==server)listener.failed(e.getMessage()==null?"로그인을 완료하지 못했습니다.":e.getMessage());}finally{try{server.close();}catch(Exception ignored){}if(callbackServer==server)callbackServer=null;}},"StudyForge-OAuth").start();
        return OAuthSecurity.AUTHORIZE+"?"+OAuthSecurity.form(params);
    }
    private JSONObject makeSession(JSONObject tokens,JSONObject claims,String client,JSONObject old) throws Exception {String scope=tokens.has("scope")?tokens.getString("scope"):old==null?"":old.optString("scope");if(!OAuthSecurity.planPermission(scope))throw new IOException("구독 사용 권한이 철회되었습니다. 다시 연결하세요.");return Json.object("client_id",client,"sub",claims.getString("sub"),"email",claims.optString("email",old==null?"":old.optString("email")),"name",claims.optString("name",old==null?"":old.optString("name")),"access_token",tokens.getString("access_token"),"refresh_token",tokens.optString("refresh_token",old==null?"":old.optString("refresh_token")),"scope",scope,"expires_at",System.currentTimeMillis()+Math.max(60,tokens.optLong("expires_in",3600))*1000);}
    synchronized String accessToken() throws Exception {
        if(!connected())throw new IOException("먼저 ChatGPT를 연결하세요.");
        if(session.optLong("expires_at")>System.currentTimeMillis()+60000)return session.getString("access_token");
        String refresh=session.optString("refresh_token");if(refresh.isEmpty())throw new IOException("다시 ChatGPT에 로그인해 주세요.");
        Map<String,String> form=new LinkedHashMap<>();form.put("grant_type","refresh_token");form.put("client_id",session.getString("client_id"));form.put("refresh_token",refresh);form.put("resource",OAuthSecurity.RESOURCE);
        JSONObject tokens=Api.json(OAuthSecurity.TOKEN,"POST","application/x-www-form-urlencoded",OAuthSecurity.form(form),null),claims;
        if(tokens.has("id_token")){JSONObject jwks=Api.json(OAuthSecurity.ISSUER+"/.well-known/jwks.json","GET",null,null,null);claims=OAuthSecurity.verifyIdToken(tokens.getString("id_token"),jwks,session.getString("client_id"),null,System.currentTimeMillis()/1000);if(!claims.getString("sub").equals(session.getString("sub")))throw new IOException("계정이 바뀌어 연결을 중단했습니다.");}else claims=Json.object("sub",session.getString("sub"));
        session=makeSession(tokens,claims,session.getString("client_id"),session);vault.put("connection",session);return session.getString("access_token");
    }
    JSONArray models() throws Exception {return ModelCatalog.parse(Api.json(OAuthSecurity.RESOURCE+"/models","GET",null,null,accessToken()));}
    synchronized void logout() throws Exception {cancel();session=new JSONObject();vault.clear("connection");}
    void cancel(){ServerSocket s=callbackServer;callbackServer=null;if(s!=null)try{s.close();}catch(Exception ignored){}}
    private static String readLine(InputStream in) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();for(int i=0;i<8192;i++){int c=in.read();if(c<0||c=='\n')return out.toString("UTF-8").trim();out.write(c);}throw new IOException("로그인 응답이 너무 큽니다.");}
    private void respond(Socket socket,int code,String message) throws IOException {byte[] b=("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>스터디포지 연결</title><body style='font:18px/1.8 system-ui;padding:40px;background:#f7f8f5;color:#243830'><h2>스터디포지</h2><p>"+message+"</p><p><a href='"+appAuthScheme+"://auth/complete'>스터디포지 앱으로 돌아가기</a></p>").getBytes(StandardCharsets.UTF_8);OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 "+code+" OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+b.length+"\r\nCache-Control: no-store\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(b);out.flush();}
}
