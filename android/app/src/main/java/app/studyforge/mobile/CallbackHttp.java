package app.studyforge.mobile;
import java.io.*;

final class CallbackHttp {
    /** Drain headers before closing the response, avoiding a reset with unread request bytes. */
    static void headers(InputStream in) throws IOException {int total=0;for(int lines=0;lines<100;lines++){StringBuilder line=new StringBuilder();while(true){int c=in.read();if(c<0)throw new EOFException("로그인 응답이 끊겼습니다.");if(++total>32768)throw new IOException("로그인 헤더가 너무 큽니다.");if(c=='\n')break;line.append((char)c);}if(line.toString().trim().isEmpty())return;}throw new IOException("로그인 헤더가 너무 많습니다.");}
}
