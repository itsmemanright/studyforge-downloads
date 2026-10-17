package app.studyforge.mobile;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalLinksTest {
    @Test public void privacyAndExistingTrustedServicesAreReachable(){assertTrue(ExternalLinks.allowed(ExternalLinks.PRIVACY));assertTrue(ExternalLinks.allowed("https://chatgpt.com/settings/usage"));assertTrue(ExternalLinks.allowed("https://developers.openai.com/siwc"));assertTrue(ExternalLinks.allowed("https://learn.chatgpt.com/docs/sign-in-with-chatgpt"));}
    @Test public void privacyHostDoesNotAuthorizeOtherPagesOrSchemes(){assertFalse(ExternalLinks.allowed("https://itsmemanright.github.io/other.html"));assertFalse(ExternalLinks.allowed(ExternalLinks.PRIVACY+"?next=external"));assertFalse(ExternalLinks.allowed("http://itsmemanright.github.io/studyforge-downloads/privacy.html"));assertFalse(ExternalLinks.allowed("javascript:alert(1)"));}
    @Test public void disguisedHostsCredentialsAndUnexpectedPortsAreRejected(){assertFalse(ExternalLinks.allowed("https://chatgpt.com.example.com"));assertFalse(ExternalLinks.allowed("https://person@chatgpt.com"));assertFalse(ExternalLinks.allowed("https://chatgpt.com:444/"));assertFalse(ExternalLinks.allowed("not a URL"));}
}
