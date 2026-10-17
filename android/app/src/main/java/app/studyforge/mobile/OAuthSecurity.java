package app.studyforge.mobile;

import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import org.json.*;

final class OAuthSecurity {
    static final String ISSUER="https://auth.openai.com", AUTHORIZE=ISSUER+"/api/accounts/authorize", TOKEN=ISSUER+"/api/accounts/oauth/token", RESOURCE="https://api.openai.com/v1";
    static final String SCOPE="openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    static String random() {byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    static String challenge(String verifier) throws Exception {return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));}
    static String form(Map<String,String> values){StringJoiner j=new StringJoiner("&");values.forEach((k,v)->j.add(encoded(k)+"="+encoded(v)));return j.toString();}
    private static String encoded(String value) {try{return URLEncoder.encode(value,"UTF-8");}catch(java.io.UnsupportedEncodingException e){throw new IllegalStateException(e);}}
    static Map<String,String> parseQuery(String query) throws Exception {Map<String,String> out=new HashMap<>();for(String pair:query.split("&")){String[] p=pair.split("=",2);String key=URLDecoder.decode(p[0],"UTF-8"),value=p.length>1?URLDecoder.decode(p[1],"UTF-8"):"";if(out.put(key,value)!=null)throw new Exception("중복된 로그인 응답입니다.");}return out;}
    static boolean equal(String a,String b) {return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    static boolean planPermission(String scopes) {return Arrays.asList(scopes.split("\\s+")).contains("chatgpt.tokens.use.direct");}
    static JSONObject verifyIdToken(String jwt,JSONObject jwks,String clientId,String nonce,long nowSeconds) throws Exception {
        String[] parts=jwt.split("\\.");if(parts.length!=3)throw new Exception("잘못된 계정 증명입니다.");
        JSONObject header=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8));
        if(!"RS256".equals(header.optString("alg")))throw new Exception("지원하지 않는 계정 서명입니다.");
        String kid=Json.required(header,"kid");JSONObject key=null;JSONArray keys=jwks.getJSONArray("keys");for(int i=0;i<keys.length();i++){JSONObject k=keys.getJSONObject(i);if(kid.equals(k.optString("kid"))&&"RSA".equals(k.optString("kty"))&&(!k.has("use")||"sig".equals(k.optString("use")))&&(!k.has("alg")||"RS256".equals(k.optString("alg"))))key=k;}
        if(key==null)throw new Exception("계정 서명 키를 확인할 수 없습니다.");
        PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(new BigInteger(1,Base64.getUrlDecoder().decode(key.getString("n"))),new BigInteger(1,Base64.getUrlDecoder().decode(key.getString("e")))));
        Signature signature=Signature.getInstance("SHA256withRSA");signature.initVerify(publicKey);signature.update((parts[0]+"."+parts[1]).getBytes(StandardCharsets.US_ASCII));if(!signature.verify(Base64.getUrlDecoder().decode(parts[2])))throw new Exception("계정 서명을 검증하지 못했습니다.");
        JSONObject claims=new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1]),StandardCharsets.UTF_8));
        boolean audience=clientId.equals(claims.optString("aud"));if(claims.opt("aud") instanceof JSONArray){JSONArray a=claims.getJSONArray("aud");for(int i=0;i<a.length();i++)audience|=clientId.equals(a.getString(i));if(a.length()>1&&!clientId.equals(claims.optString("azp")))audience=false;}
        if(!ISSUER.equals(claims.optString("iss"))||!audience||claims.optLong("exp",0)<=nowSeconds||claims.optLong("iat",nowSeconds)>nowSeconds+120||claims.optLong("nbf",0)>nowSeconds+120||claims.optString("sub").trim().isEmpty())throw new Exception("계정 증명의 발급자·대상·유효기간을 확인하지 못했습니다.");
        if(nonce!=null&&!equal(nonce,claims.optString("nonce")))throw new Exception("로그인 요청과 응답이 일치하지 않습니다.");return claims;
    }
}
