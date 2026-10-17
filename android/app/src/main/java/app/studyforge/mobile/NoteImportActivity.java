package app.studyforge.mobile;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Reads the supplied share link without exposing an app bridge or a browsing screen. */
public final class NoteImportActivity extends Activity {
    private WebView web;private boolean ended;private final Handler timer=new Handler(Looper.getMainLooper());
    private volatile HttpURLConnection request;
    static boolean allowedNote(String url){return NoteTranscript.allowedShare(url);}
    private void stop(String message){if(ended)return;ended=true;timer.removeCallbacksAndMessages(null);if(web!=null)web.stopLoading();setResult(RESULT_CANCELED,new Intent().putExtra("error",message));finish();}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);String url=getIntent().getStringExtra("url");if(!allowedNote(url)){stop("클로바노트에서 복사한 공유 링크를 입력해 주세요.");return;}
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,24,24,24);ScreenInsets.apply(layout,24);
        TextView label=new TextView(this);label.setText("공유 링크에서 전사본을 가져오는 중…\n전체 음성 기록을 읽으면 자료에 추가합니다.");layout.addView(label);layout.addView(new ProgressBar(this));Button cancel=new Button(this);cancel.setText("취소");cancel.setOnClickListener(v->stop("가져오기를 취소했어요."));layout.addView(cancel);
        web=new WebView(this);web.setVisibility(View.INVISIBLE);layout.addView(web,new LinearLayout.LayoutParams(1,1));setContentView(layout);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);settings.setMediaPlaybackRequiresUserGesture(true);android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();boolean allowed="https".equals(u.getScheme())&&"clovanote.naver.com".equals(u.getHost());if(!allowed&&r.isForMainFrame())runOnUiThread(()->stop("이 공유 링크는 네이버 로그인이나 추가 권한이 필요합니다. 클로바노트의 음성 기록을 TXT로 내려받아 추가해 주세요."));return !allowed;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){if(!"GET".equals(r.getMethod())||!NoteTranscript.noteResponse(r.getUrl().toString()))return null;return readTranscript(r);}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())stop("공유 링크에 연결하지 못했습니다. 인터넷 연결과 링크를 확인해 주세요.");}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){web=null;v.destroy();stop("전사본을 읽는 화면이 종료됐습니다. 다시 시도해 주세요.");return true;}
        });
        timer.postDelayed(()->stop("전사본을 불러오지 못했습니다. 공유 비밀번호·로그인·공유 권한이 필요한지 확인해 주세요. 음성 기록 TXT 파일이나 전사본 붙여넣기로도 추가할 수 있어요."),45000);
        web.loadUrl(url);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->stop("가져오기를 취소했어요."));
    }
    private WebResourceResponse readTranscript(WebResourceRequest r){HttpURLConnection c=null;try{
        c=(HttpURLConnection)new URL(r.getUrl().toString()).openConnection();request=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
        for(Map.Entry<String,String> e:r.getRequestHeaders().entrySet())if(!e.getKey().equalsIgnoreCase("Accept-Encoding")&&!e.getKey().equalsIgnoreCase("Host"))c.setRequestProperty(e.getKey(),e.getValue());
        c.setRequestProperty("Accept-Encoding","identity");String cookies=android.webkit.CookieManager.getInstance().getCookie(r.getUrl().toString());if(cookies!=null)c.setRequestProperty("Cookie",cookies);
        int status=c.getResponseCode();String body=Api.read(status<400?c.getInputStream():c.getErrorStream(),20000000);
        if(status==200){try{JSONObject note=NoteTranscript.parse(body);runOnUiThread(()->{if(ended)return;ended=true;timer.removeCallbacksAndMessages(null);setResult(RESULT_OK,new Intent().putExtra("title",note.optString("title")).putExtra("text",note.optString("text")));finish();});}catch(Exception e){String message=e.getMessage();runOnUiThread(()->stop(message!=null&&message.startsWith("전사")?message:"음성 기록 전체를 읽지 못했습니다. 변환 완료와 공유 권한을 확인하거나 TXT로 추가해 주세요."));}}
        else if(status==401||status==403)runOnUiThread(()->stop("이 공유 링크는 로그인·비밀번호 또는 공유 권한이 필요합니다. 음성 기록 TXT 파일로 추가해 주세요."));
        else runOnUiThread(()->stop("공유 노트를 불러오지 못했습니다. 링크가 만료되었거나 삭제됐는지 확인해 주세요."));
        return new WebResourceResponse("application/json","UTF-8",new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }catch(Exception e){runOnUiThread(()->stop("전사본을 가져오지 못했습니다. 인터넷 연결을 확인하거나 음성 기록 TXT 파일을 추가해 주세요."));return new WebResourceResponse("application/json","UTF-8",new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));}
    finally{if(c!=null)c.disconnect();request=null;}}
    @Override protected void onDestroy(){ended=true;timer.removeCallbacksAndMessages(null);HttpURLConnection c=request;if(c!=null)c.disconnect();if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
    @Override public void onBackPressed(){stop("가져오기를 취소했어요.");}
}
