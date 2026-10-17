package app.studyforge.mobile;
import java.io.*;import java.nio.charset.StandardCharsets;import org.junit.Test;import static org.junit.Assert.*;
public class CallbackHttpTest {
    @Test public void callbackHeadersAreConsumedWithoutSwallowingTheNextBytes() throws Exception {ByteArrayInputStream input=new ByteArrayInputStream("Host: 127.0.0.1:1234\r\nUser-Agent: Mobile browser\r\nAccept: text/html\r\n\r\nnext".getBytes(StandardCharsets.US_ASCII));CallbackHttp.headers(input);assertEquals('n',input.read());}
    @Test public void malformedOrOversizedHeadersCannotHoldTheListenerForever() throws Exception {assertThrows(IOException.class,()->CallbackHttp.headers(new ByteArrayInputStream("truncated".getBytes(StandardCharsets.US_ASCII))));assertThrows(IOException.class,()->CallbackHttp.headers(new ByteArrayInputStream(("x".repeat(33000)+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII))));}
}
