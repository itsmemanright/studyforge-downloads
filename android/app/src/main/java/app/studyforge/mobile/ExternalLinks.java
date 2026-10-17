package app.studyforge.mobile;

import java.net.URI;
import java.util.Arrays;

final class ExternalLinks {
    static final String PRIVACY="https://itsmemanright.github.io/studyforge-downloads/privacy.html";
    static boolean allowed(String url){
        try{URI uri=URI.create(url);if(!"https".equals(uri.getScheme())||uri.getRawUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))return false;
            if(PRIVACY.equals(url))return true;
            return Arrays.asList("chatgpt.com","developers.openai.com","learn.chatgpt.com").contains(uri.getHost());
        }catch(Exception invalid){return false;}
    }
}
