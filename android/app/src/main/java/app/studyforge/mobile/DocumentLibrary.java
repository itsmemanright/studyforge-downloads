package app.studyforge.mobile;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

final class DocumentLibrary implements AutoCloseable {
    interface ImportProgress {void update(String stage,long done,long total);}
    private ImportProgress importProgress=(stage,done,total)->{};
    void importProgress(ImportProgress value){importProgress=value;}
    static final long MAX_BYTES=512L*1024*1024;
    private AnalysisPipeline.Guard guard=()->{};
    void guard(AnalysisPipeline.Guard value){guard=value;}
    void discard(JSONObject source) throws Exception {new AtomicFile(encrypted(source.getJSONObject("document").getString("id"))).delete();}
    private final Context context;private final SecureVault vault;private final File originals;private final Map<String,File> temporary=new HashMap<>();
    DocumentLibrary(Context c,SecureVault v,String profile){context=c;vault=v;originals=new File(c.getNoBackupFilesDir(),"originals-"+EncryptedBlocks.hash(profile));}
    private File encrypted(String id) throws Exception {if(!id.matches("[a-f0-9-]{36}"))throw new Exception("원본 ID가 올바르지 않습니다.");if(!originals.exists()&&!originals.mkdirs())throw new IOException("원본 저장 위치를 만들 수 없습니다.");return new File(originals,id+".sfb");}
    JSONObject add(Uri uri,String role,String kind) throws Exception {
        long expected=SourceImporter.size(context,uri);importProgress.update("파일 읽는 중",0,expected);String id=UUID.randomUUID().toString();AtomicFile target=new AtomicFile(encrypted(id));FileOutputStream out=null;try{
            EncryptedBlocks.Receipt receipt;try(InputStream input=context.getContentResolver().openInputStream(uri)){if(input==null)throw new IOException("파일을 열 수 없습니다.");out=target.startWrite();receipt=EncryptedBlocks.write(input,out,vault.fileKey(),id,"image".equals(kind)?20_000_000:MAX_BYTES,()->guard.check(),n->importProgress.update("파일 읽는 중",n,expected));target.finishWrite(out);out=null;}
            JSONObject source=Json.object("id",id,"title",SourceImporter.name(context,uri),"role",role,"text","","images",new JSONArray(),"createdAt",System.currentTimeMillis(),"document",Json.object("id",id,"kind",kind,"bytes",receipt.bytes(),"sha256",receipt.sha256()));
            File file=materialize(source);importProgress.update("자료 내용 확인 중",0,-1);if("pdf".equals(kind)){try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer pdf=new PdfRenderer(fd)){source.getJSONObject("document").put("pages",pdf.getPageCount());}}
            else if("image".equals(kind)){SourceImporter.renderPhoto(file);source.getJSONObject("document").put("pages",1);}
            else{int chars=0;try(Reader reader=textReader(file)){char[] buffer=new char[8192];int n;while((n=reader.read(buffer))!=-1)chars+=n;}if(chars<1)throw new IOException("빈 텍스트 파일입니다.");source.getJSONObject("document").put("characters",chars);}
            guard.check();return source;
        }catch(Exception e){if(out!=null)target.failWrite(out);else target.delete();throw e;}finally{close();}
    }
    File materialize(JSONObject source) throws Exception {
        JSONObject doc=source.getJSONObject("document");String id=doc.getString("id");if(temporary.containsKey(id))return temporary.get(id);File encrypted=encrypted(id);if(!encrypted.isFile())throw new IOException("이 기기에 원본이 없습니다. 원본 PDF·TXT를 다시 추가해 주세요. JSON 백업에는 원본 파일이 포함되지 않습니다.");
        File file=File.createTempFile("studyforge-original-",".tmp",context.getCacheDir());boolean ok=false;try(InputStream in=new FileInputStream(encrypted);OutputStream out=new FileOutputStream(file)){EncryptedBlocks.Receipt receipt=EncryptedBlocks.read(in,out,vault.fileKey(),id,()->guard.check(),n->importProgress.update("원본 확인 중",n,doc.optLong("bytes",-1)));if(receipt.bytes()!=doc.getLong("bytes")||!receipt.sha256().equals(doc.getString("sha256")))throw new IOException("원본 파일의 무결성을 확인하지 못했습니다.");ok=true;}finally{if(!ok)file.delete();}temporary.put(id,file);return file;
    }
    static Reader textReader(File file) throws Exception {return new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));}
    JSONArray load(JSONArray sources,JSONObject unit) throws Exception {
        JSONObject source=Json.copy(BatchPlan.source(sources,unit.getString("source_id")));source.remove("evidence");String kind=unit.getString("kind");source.put("text","").put("images",new JSONArray());
        if("pdf".equals(kind)){File file=materialize(source);source.put("images",SourceImporter.renderPdf(file,unit.getInt("loadStart"),unit.getInt("loadEnd")));}
        else if("photo".equals(kind)){source.put("images",SourceImporter.renderPhoto(materialize(source)));}
        else if("images".equals(kind)){JSONArray pages=BatchPlan.source(sources,source.getString("id")).getJSONArray("images");for(int i=unit.getInt("start");i<unit.getInt("end");i++)source.getJSONArray("images").put(pages.get(i));}
        else{int from=unit.getInt("loadStart"),to=unit.getInt("loadEnd");if(source.has("document")){try(Reader reader=textReader(materialize(source))){long skipped=0;while(skipped<from){long n=reader.skip(from-skipped);if(n<1)throw new IOException("텍스트 범위를 읽지 못했습니다.");skipped+=n;}char[] text=new char[to-from];int n,read=0;while(read<text.length&&(n=reader.read(text,read,text.length-read))!=-1)read+=n;if(read!=text.length)throw new IOException("텍스트가 예상보다 짧습니다.");source.put("text",new String(text));}}else source.put("text",BatchPlan.source(sources,source.getString("id")).getString("text").substring(from,to));}
        return Json.array(source);
    }
    JSONObject page(JSONArray sources,String sourceId,int page) throws Exception {JSONObject s=BatchPlan.source(sources,sourceId),d=s.optJSONObject("document");if(d==null||!Arrays.asList("pdf","image").contains(d.optString("kind"))||page<1||page>d.getInt("pages"))throw new Exception("원본 페이지가 올바르지 않습니다.");return ("image".equals(d.getString("kind"))?SourceImporter.renderPhoto(materialize(s)):SourceImporter.renderPdf(materialize(s),page,page)).getJSONObject(0);}
    void requireFiles(JSONArray sources) throws Exception {for(int i=0;i<sources.length();i++){JSONObject d=sources.getJSONObject(i).optJSONObject("document");if(d!=null&&!encrypted(d.getString("id")).isFile())throw new IOException("이 백업의 원본 PDF·TXT가 현재 기기에 없습니다. 원본을 다시 추가한 뒤 분석하세요.");}}
    static void migrate(Context context,String oldProfile,String newProfile) throws Exception {File from=new File(context.getNoBackupFilesDir(),"originals-"+EncryptedBlocks.hash(oldProfile)),to=new File(context.getNoBackupFilesDir(),"originals-"+EncryptedBlocks.hash(newProfile));if(!from.exists())return;if(!to.exists()&&from.renameTo(to))return;if(!to.exists()&&!to.mkdirs())throw new IOException("원본을 계정에 연결하지 못했습니다.");File[] files=from.listFiles();if(files!=null)for(File f:files)if(!f.renameTo(new File(to,f.getName())))throw new IOException("원본을 계정에 연결하지 못했습니다.");}
    @Override public void close(){for(File file:temporary.values())file.delete();temporary.clear();}
}
