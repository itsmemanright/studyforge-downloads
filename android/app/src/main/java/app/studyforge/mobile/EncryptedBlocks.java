package app.studyforge.mobile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Independent authenticated blocks avoid buffering a whole textbook in memory. */
final class EncryptedBlocks {
    interface Check {void check() throws Exception;}
    static final int BLOCK=1_048_576, MAGIC=0x53464232;
    record Receipt(long bytes,String sha256) {}
    static Receipt write(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id,long limit) throws Exception {return write(input,output,key,id,limit,()->{});}
    static Receipt write(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id,long limit,Check check) throws Exception {return write(input,output,key,id,limit,check,n->{});}
    static Receipt write(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id,long limit,Check check,java.util.function.LongConsumer progress) throws Exception {
        DataOutputStream out=new DataOutputStream(output);out.writeInt(MAGIC);byte[] buffer=new byte[BLOCK];long total=0;int index=0;MessageDigest digest=MessageDigest.getInstance("SHA-256");
        while(true){check.check();int length=0,n;while(length<BLOCK&&(n=input.read(buffer,length,BLOCK-length))!=-1){length+=n;}if(length==0)break;if(total+length>limit)throw new IOException("파일 크기 제한을 넘었습니다. PDF·TXT는 512MB, 사진은 20MB까지 추가할 수 있습니다.");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);cipher.updateAAD((id+":"+index++).getBytes(StandardCharsets.UTF_8));byte[] data=cipher.doFinal(buffer,0,length);out.writeInt(length);out.write(cipher.getIV());out.write(data);digest.update(buffer,0,length);total+=length;progress.accept(total);
        }out.writeInt(0);out.flush();return new Receipt(total,hex(digest.digest()));
    }
    static Receipt read(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id) throws Exception {return read(input,output,key,id,()->{});}
    static Receipt read(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id,Check check) throws Exception {return read(input,output,key,id,check,n->{});}
    static Receipt read(InputStream input,OutputStream output,javax.crypto.SecretKey key,String id,Check check,java.util.function.LongConsumer progress) throws Exception {
        DataInputStream in=new DataInputStream(input);if(in.readInt()!=MAGIC)throw new IOException("원본 저장 형식이 올바르지 않습니다.");long total=0;int index=0;MessageDigest digest=MessageDigest.getInstance("SHA-256");
        while(true){check.check();int length=in.readInt();if(length==0)break;if(length<1||length>BLOCK)throw new IOException("손상된 원본 블록입니다.");byte[] iv=new byte[12],data=new byte[length+16];in.readFully(iv);in.readFully(data);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));cipher.updateAAD((id+":"+index++).getBytes(StandardCharsets.UTF_8));byte[] plain=cipher.doFinal(data);output.write(plain);digest.update(plain);total+=length;progress.accept(total);}
        if(in.read()!=-1)throw new IOException("원본 끝부분이 손상되었습니다.");output.flush();return new Receipt(total,hex(digest.digest()));
    }
    static String hash(String s) {try{return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
