package com.pgconnect.auth;
import org.springframework.beans.factory.annotation.Value;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.web.client.RestClient;import org.springframework.web.server.ResponseStatusException;import java.util.Map;
@Service
public class EmailDelivery{
 private final String apiKey,from,webOrigin;private final RestClient client=RestClient.builder().baseUrl("https://api.resend.com").build();
 public EmailDelivery(@Value("${pgconnect.email.api-key:}")String apiKey,@Value("${pgconnect.email.from:}")String from,@Value("${pgconnect.email.web-origin:http://localhost:5173}")String webOrigin){this.apiKey=apiKey;this.from=from;this.webOrigin=webOrigin;}
 public boolean configured(){return !apiKey.isBlank()&&!from.isBlank();}
 public void sendAccountLink(String email,String name,String purpose,String token){
  if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is not configured. Set EMAIL_RESEND_API_KEY and EMAIL_FROM to continue.");
  String path="VERIFY_EMAIL".equals(purpose)?"/verify-email?token=":"/password-reset?token=";String title="VERIFY_EMAIL".equals(purpose)?"Verify your PGConnect email":"Reset your PGConnect password";
  String action="VERIFY_EMAIL".equals(purpose)?"Verify email":"Reset password";String url=webOrigin.replaceAll("/$","")+path+java.net.URLEncoder.encode(token,java.nio.charset.StandardCharsets.UTF_8);
  String html="<p>Hello "+escape(name)+",</p><p>Use this one-time link to "+action.toLowerCase()+". It expires soon and can be used once.</p><p><a href=\""+url+"\">"+action+"</a></p><p>If you did not request this, you can ignore this email.</p>";
  try{client.post().uri("/emails").header("Authorization","Bearer "+apiKey).body(Map.of("from",from,"to",new String[]{email},"subject",title,"html",html)).retrieve().toBodilessEntity();}
  catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"The email provider could not deliver the account link. Please retry.");}
 }
 private static String escape(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
