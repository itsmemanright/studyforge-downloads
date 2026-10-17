package app.studyforge.mobile;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import org.junit.Test;
import static org.junit.Assert.*;

public class ApiCancellationTest {
    static final class Connection extends HttpsURLConnection {
        boolean closed;
        Connection()throws Exception{super(new URL("https://example.invalid"));}
        public void disconnect(){closed=true;}
        public boolean usingProxy(){return false;}
        public void connect(){}
        public String getCipherSuite(){return "test";}
        public Certificate[] getLocalCertificates(){return null;}
        public Certificate[] getServerCertificates(){return null;}
    }
    @Test public void stopDisconnectsEveryConcurrentResponse()throws Exception{
        Connection first=new Connection(),second=new Connection(),third=new Connection();
        try{Api.active.add(first);Api.active.add(second);Api.active.add(third);Api.cancel();assertTrue(first.closed&&second.closed&&third.closed);}finally{Api.active.clear();}
    }
    @Test public void subscriptionQuotaIsNotRetriedAsTransientRateLimit()throws Exception{
        assertFalse(Api.error(429,"{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}") instanceof AnalysisPipeline.RateLimited);
        assertTrue(Api.error(429,"{\"error\":{\"code\":\"rate_limit_exceeded\"}}") instanceof AnalysisPipeline.RateLimited);
    }
}
