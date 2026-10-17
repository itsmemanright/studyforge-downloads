package app.studyforge.mobile;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

final class SecureVault {
    private final Context context;
    SecureVault(Context c){context=c.getApplicationContext();}
    synchronized SecretKey fileKey() throws Exception {return key();}
    private SecretKey key() throws Exception {KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias("studyforge-v1"))return (SecretKey)store.getKey("studyforge-v1",null);KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("studyforge-v1",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());return g.generateKey();}
    private AtomicFile file(String name){if(!name.matches("[a-zA-Z0-9_-]+"))throw new IllegalArgumentException();return new AtomicFile(new File(context.getNoBackupFilesDir(),name+".vault"));}
    synchronized void put(String name,JSONObject value) throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] encrypted=c.doFinal(value.toString().getBytes(StandardCharsets.UTF_8));AtomicFile target=file(name);FileOutputStream out=null;try{out=target.startWrite();out.write(c.getIV().length);out.write(c.getIV());out.write(encrypted);target.finishWrite(out);}catch(Exception e){if(out!=null)target.failWrite(out);throw e;}}
    synchronized JSONObject get(String name) throws Exception {AtomicFile target=file(name);if(!target.getBaseFile().exists())return new JSONObject();byte[] bytes=target.readFully();if(bytes.length<30)throw new IOException("저장된 자료를 읽을 수 없습니다.");int n=bytes[0]&255;if(n!=12||bytes.length<n+17)throw new IOException("저장 형식이 올바르지 않습니다.");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,bytes,1,n));return new JSONObject(new String(c.doFinal(bytes,1+n,bytes.length-1-n),StandardCharsets.UTF_8));}
    synchronized void clear(String name) throws Exception {put(name,new JSONObject());}
}
