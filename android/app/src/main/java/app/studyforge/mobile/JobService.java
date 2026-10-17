package app.studyforge.mobile;

import android.app.*;
import android.content.*;
import android.os.*;

/** Visible, cancellable foreground work; checkpoints survive OS termination. */
public final class JobService extends Service {
    private static volatile JobService active;private PowerManager.WakeLock wake;private long notified;private static final int ID=210;
    static void begin(StudyApp app,String text){app.startForegroundService(new Intent(app,JobService.class).putExtra("message",text));}
    static void end(StudyApp app){if(!app.busy&&!app.modelsLoading&&(app.connection==null||!app.connection.pending()))app.stopService(new Intent(app,JobService.class));}
    static void update(StudyApp app,String text){app.main.post(()->{JobService service=active;if(service==null)return;if(service.wake!=null&&!service.wake.isHeld())service.wake.acquire(600000);if(System.currentTimeMillis()-service.notified<1000)return;service.notified=System.currentTimeMillis();if(Build.VERSION.SDK_INT>=33&&service.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;service.getSystemService(NotificationManager.class).notify(ID,service.notification(text));});}
    private Notification notification(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,JobService.class).setAction("pause"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"studyforge-work").setSmallIcon(R.drawable.ic_app).setContentTitle("스터디포지").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"일시정지",stop).build()).build();}
    @Override public void onCreate(){super.onCreate();active=this;getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("studyforge-work","자료 분석·ChatGPT 연결",NotificationManager.IMPORTANCE_LOW));startForeground(ID,notification("작업 준비 중"));PowerManager power=getSystemService(PowerManager.class);wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"StudyForge:work");wake.acquire(600000);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){StudyApp app=(StudyApp)getApplication();if(intent!=null&&"pause".equals(intent.getAction())){app.cancel();stopSelf();return START_NOT_STICKY;}update(app,intent==null?"저장된 구간부터 이어서 분석합니다.":intent.getStringExtra("message"));return START_NOT_STICKY;}
    @Override public void onTimeout(int startId,int type){StudyApp app=(StudyApp)getApplication();app.cancel();app.notice("Android의 장시간 작업 한도로 일시정지했습니다. 앱을 열고 완료된 구간부터 이어서 진행하세요.");stopSelf();}
    @Override public void onDestroy(){active=null;if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
