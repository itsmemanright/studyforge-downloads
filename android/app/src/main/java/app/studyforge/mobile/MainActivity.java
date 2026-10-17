package app.studyforge.mobile;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private StudyApp app;private WebView web;private String fileRole="textbook",pendingName="",pendingMime="",pendingData="",pendingNoteOwner="";private static final int IMPORT=1,EXPORT=2,NOTE=3,RESTORE=4;
    @Override public void onCreate(Bundle state){super.onCreate(state);app=(StudyApp)getApplication();web=new WebView(this);web.setBackgroundColor(Color.rgb(247,248,245));FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(247,248,245));root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);ScreenInsets.apply(root,0);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(),"StudyNative");web.setWebViewClient(new WebViewClient(){@Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){if(web==view){web=null;android.view.ViewParent parent=view.getParent();if(parent instanceof android.view.ViewGroup)((android.view.ViewGroup)parent).removeView(view);view.destroy();startActivity(new Intent(MainActivity.this,MainActivity.class));finish();}return true;}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){Uri uri=request.getUrl();if(!"https".equals(uri.getScheme())||!"studyforge.local".equals(uri.getHost()))return empty();String p=uri.getPath();if(p==null||p.contains("..")||!p.matches("/[a-zA-Z0-9._-]+"))return empty();String name=p.substring(1);try{String mime=name.endsWith(".js")?"text/javascript":name.endsWith(".css")?"text/css":"text/html";return new WebResourceResponse(mime,"UTF-8",getAssets().open(name));}catch(Exception e){return empty();}}});web.loadUrl("https://studyforge.local/index.html");}
    private static WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    @Override protected void onResume(){super.onResume();app.attach(this::send);}
    @Override protected void onPause(){app.detach();super.onPause();}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("StudyNative");web.destroy();}super.onDestroy();}
    private void send(JSONObject event){if(!isFinishing()&&web!=null)web.evaluateJavascript("window.studyNativeReceive&&window.studyNativeReceive("+JSONObject.quote(event.toString())+")",null);}
    final class Bridge {@JavascriptInterface public void postMessage(String message){if(message==null||message.length()>20_000_000)return;runOnUiThread(()->{try{JSONObject command=new JSONObject(message);handle(command.getString("type"),command.optJSONObject("payload")==null?new JSONObject():command.getJSONObject("payload"));}catch(Exception e){app.notice(e.getMessage());JobService.end(app);}});}}
    private void handle(String type,JSONObject data) throws Exception {
        switch(type){
            case "ready" -> {app.state();if(app.connection!=null&&app.connection.connected()&&app.models.length()==0)app.reloadModels();}
            case "signIn" -> {String url=app.startLogin(data.optBoolean("newAccount"));Intent browser=new Intent(Intent.ACTION_VIEW,Uri.parse(url));startActivity(data.optBoolean("chooseBrowser")?Intent.createChooser(browser,"로그인할 브라우저 선택"):browser);app.progress("브라우저에서 ChatGPT 로그인과 구독 사용 권한을 확인해 주세요. 앱으로 돌아오면 연결 상태가 표시됩니다.");}
            case "logout" -> app.worker.execute(()->{try{app.logout();}catch(Exception e){app.notice(e.getMessage());}});
            case "cancel" -> app.cancel();
            case "saveSettings" -> app.worker.execute(()->{try{app.settings(data);}catch(Exception e){app.notice(e.getMessage());}});
            case "applyUi", "undoUi" -> {String owner=app.profile;app.worker.execute(()->{try{app.applyUi(data,type.equals("undoUi"),owner);}catch(Exception e){app.notice(e.getMessage());}});}
            case "proposeUi" -> app.proposeUi(data);
            case "proposeFeature" -> app.features.propose(data);
            case "featureExample" -> {String owner=app.profile;app.worker.execute(()->{try{app.features.example(owner);}catch(Exception e){app.notice(e.getMessage());}});}
            case "openFeature" -> startActivity(new Intent(this,FeatureActivity.class).putExtra("id",data.getString("id")).putExtra("preview",data.optBoolean("preview")));
            case "installFeature" -> {String owner=app.profile;app.worker.execute(()->{try{app.features.install(data.getString("id"),data.getString("hash"),owner);}catch(Exception e){app.notice(e.getMessage());}});}
            case "undoFeature" -> {String owner=app.profile;app.worker.execute(()->{try{app.features.undo(data.getString("id"),owner);}catch(Exception e){app.notice(e.getMessage());}});}
            case "clearFeaturePractice" -> {app.features.practice=null;app.state();}
            case "addText" -> app.worker.execute(()->{try{String role=data.optString("role","transcript");validateRole(role);app.addSource(SourceImporter.text(data.optString("title"),data.getString("text"),role));}catch(Exception e){app.notice(e.getMessage());}});
            case "removeSource" -> app.worker.execute(()->{try{app.removeSource(data.getString("id"));}catch(Exception e){app.notice(e.getMessage());}});
            case "pickFile" -> {if(app.busy)throw new Exception("자료 분석이 끝난 뒤 추가해 주세요.");fileRole=data.optString("role","textbook");validateRole(fileRole);Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);pick.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/*","text/*"});startActivityForResult(pick,IMPORT);}
            case "clova" -> {if(app.busy)throw new Exception("현재 작업을 마친 뒤 전사본을 추가해 주세요.");pendingNoteOwner=app.profile;String url=data.getString("url");if(!NoteImportActivity.allowedNote(url))throw new Exception("https://clovanote.naver.com/ 로 시작하는 공유 링크를 입력해 주세요.");startActivityForResult(new Intent(this,NoteImportActivity.class).putExtra("url",url),NOTE);}
            case "analyze" -> app.analyze(data);
            case "generate" -> app.generate(data);
            case "notifications" -> {if(android.os.Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},5);else app.notice("이 Android 버전은 설정에서 앱 알림을 확인할 수 있습니다.");}
            case "reloadModels" -> {if(!app.modelsLoading){JobService.begin(app,"사용 가능한 모델 목록을 불러옵니다.");app.reloadModels();}}
            case "abandonAnalysis" -> {String owner=app.profile,key=data.getString("key");app.worker.execute(()->{try{synchronized(app){if(!owner.equals(app.profile))throw new Exception("계정이 바뀌었습니다.");app.abandonAnalysis(key);}}catch(Exception e){app.notice(e.getMessage());}});}
            case "analysisReport" -> app.analysisReport(data.optInt("offset"));
            case "sourcePage" -> app.sourcePage(data.getString("sourceId"),data.getInt("page"));
            case "resetProgress" -> {if(!data.optBoolean("confirmed"))throw new Exception("초기화 확인이 필요합니다.");String id=data.getString("datasetId"),owner=app.profile;app.worker.execute(()->{try{app.resetProgress(id,owner);}catch(Exception e){app.notice(e.getMessage());}});}
            case "continuePast" -> app.continuePast(data);
            case "answer" -> app.worker.execute(()->{try{app.answer(data.getString("id"),data.getInt("selected"),null);}catch(Exception e){app.notice(e.getMessage());}});
            case "uncertain" -> app.worker.execute(()->{try{app.answer(data.getString("id"),-1,data.getBoolean("value"));}catch(Exception e){app.notice(e.getMessage());}});
            case "revise" -> app.revise(data);
            case "openRoom" -> app.worker.execute(()->{try{app.openRoom(data.getString("id"));}catch(Exception e){app.notice(e.getMessage());}});
            case "exportHtml" -> export("text/html","studyforge-study.html",data.getString("html"));
            case "exportJson" -> export("application/json","studyforge-backup.json",app.backup().toString(2));
            case "restore" -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),RESTORE);
            case "external" -> {String url=data.getString("url");if(!ExternalLinks.allowed(url))throw new Exception("허용되지 않은 외부 링크입니다.");startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
            default -> throw new Exception("지원하지 않는 동작입니다.");
        }
    }
    static void validateRole(String role) throws Exception {if(!java.util.Arrays.asList("textbook","slides","transcript","workbook","past","answerkey").contains(role))throw new Exception("자료 역할이 올바르지 않습니다.");}
    private void export(String mime,String name,String data){pendingMime=mime;pendingName=name;pendingData=data;Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(create,EXPORT);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==NOTE&&result!=RESULT_OK){if(data!=null)app.notice(data.getStringExtra("error"));return;}if(result!=RESULT_OK||data==null)return;try{
        if(request==NOTE){String text=data.getStringExtra("text"),owner=pendingNoteOwner;pendingNoteOwner="";app.worker.execute(()->{try{synchronized(app){if(!owner.equals(app.profile)||app.busy)throw new Exception("계정이나 작업 상태가 바뀌었습니다. 공유 링크에서 다시 가져와 주세요.");app.addSource(SourceImporter.text(data.getStringExtra("title"),text,"transcript"));}app.notice("전체 전사본을 녹취·필기 자료에 추가했어요.");}catch(Exception e){app.notice(e.getMessage());}});return;}
        Uri uri=data.getData();if(uri==null)return;
        if(request==EXPORT){String content=pendingData;pendingData="";app.worker.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("저장 위치를 열 수 없습니다.");out.write(content.getBytes(StandardCharsets.UTF_8));app.notice("파일을 저장했어요. HTML 파일은 브라우저에서 학습 사이트로 열 수 있습니다.");}catch(Exception e){app.notice(e.getMessage());}});}
        else if(request==RESTORE){app.worker.execute(()->{try{JSONObject backup=new JSONObject(new String(SourceImporter.bytes(this,uri,40_000_000),StandardCharsets.UTF_8));app.restore(backup);}catch(Exception e){app.notice(e.getMessage());}});}
        else if(request==IMPORT){app.importWhole(uri,fileRole,null);}

    }catch(Exception e){app.notice(e.getMessage());}}
}
