package app.studyforge.mobile;
import android.content.Context;
import java.io.File;
final class FileCleanup {
    static void clean(Context context){File root=context.getCacheDir();File[] files=root.listFiles();if(files!=null)for(File file:files)if(file.isFile()&&file.getName().startsWith("studyforge-original-")&&file.getName().endsWith(".tmp"))file.delete();}
}
