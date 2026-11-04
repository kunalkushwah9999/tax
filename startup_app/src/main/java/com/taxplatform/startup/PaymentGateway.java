package com.taxplatform.startup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Explicit test-mode provider adapter. Live collection requires a separately reviewed commercial release. */
public final class PaymentGateway {
    private final URI base;private final String keyId,keySecret,webhookSecret;private final HttpClient client;private final boolean fixture;
    private PaymentGateway(URI base,String keyId,String secret,String webhook,boolean fixture){this.base=base;this.keyId=keyId;keySecret=secret;webhookSecret=webhook;this.fixture=fixture;client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();}
    public static PaymentGateway disabled(){return new PaymentGateway(null,"","","",false);}
    public static PaymentGateway load(Path file)throws IOException{
        if(Files.isSymbolicLink(file)||Files.size(file)>8192)throw new IOException("private payment configuration required");if(Files.getFileStore(file).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(file).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("payment configuration must be private");
        Map<String,Object> cfg=Json.object(Files.readAllBytes(file));Workspace.fields(cfg,"mode","api_base","key_id","key_secret","webhook_secret");if(!"TEST".equals(cfg.get("mode")))throw new IOException("live collection is not commercially released");
        String key=Workspace.text(cfg,"key_id",10,80),secret=Workspace.text(cfg,"key_secret",24,256),webhook=Workspace.text(cfg,"webhook_secret",24,256);if(!key.matches("rzp_test_[A-Za-z0-9]+"))throw new IOException("test key required");URI base=URI.create(Workspace.text(cfg,"api_base",1,200));boolean liveHost=base.toString().equals("https://api.razorpay.com/v1/");boolean fixture=base.getScheme().equals("http")&&"127.0.0.1".equals(base.getHost())&&base.getPort()>0&&base.getRawPath().equals("/v1/")&&base.getRawQuery()==null&&base.getRawUserInfo()==null&&base.getRawFragment()==null;if(!liveHost&&!fixture)throw new IOException("fixed provider or explicit loopback test endpoint required");return new PaymentGateway(base,key,secret,webhook,fixture);
    }
    public boolean enabled(){return base!=null;}
    public String mode(){return enabled()?(fixture?"TEST_FIXTURE":"RAZORPAY_TEST"):"NOT_CONFIGURED";}
    public String publicKey(){return keyId;}
    public boolean browserCheckout(){return enabled()&&!fixture;}
    static String hmac(String secret,byte[] body){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(body));}catch(GeneralSecurityException e){throw new IllegalStateException("HMAC unavailable",e);}}
    public void verifyWebhook(byte[] raw,String signature){if(!enabled()||signature==null||!signature.matches("[a-f0-9]{64}")||!MessageDigest.isEqual(hmac(webhookSecret,raw).getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII)))throw new Workspace.Problem(403,"PAYMENT_SIGNATURE_REJECTED");}
    public void verifyCallback(String providerOrder,String payment,String signature){if(signature==null||!signature.matches("[a-f0-9]{64}")||!Crypto.equal(hmac(keySecret,(providerOrder+"|"+payment).getBytes(StandardCharsets.UTF_8)),signature))throw new Workspace.Problem(403,"PAYMENT_SIGNATURE_REJECTED");}
    private Map<String,Object> request(String path,Map<String,Object> body){
        if(!enabled())throw new Workspace.Problem(503,"PAYMENTS_NOT_CONFIGURED");try{
            HttpRequest.Builder request=HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(3)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((keyId+":"+keySecret).getBytes(StandardCharsets.UTF_8))).header("Accept","application/json");
            if(body==null)request.GET();else request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(Json.bytes(body)));
            HttpResponse<java.io.InputStream> response=client.send(request.build(),HttpResponse.BodyHandlers.ofInputStream());byte[] bytes;try(var in=response.body()){bytes=in.readNBytes(65537);}if(bytes.length>65536||response.statusCode()!=200)throw new Workspace.Problem(503,body==null?"PAYMENT_PROVIDER_UNAVAILABLE":"PAYMENT_ORDER_OUTCOME_UNKNOWN");return Json.object(bytes);
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new Workspace.Problem(503,"PAYMENT_PROVIDER_UNAVAILABLE");}catch(IOException|IllegalArgumentException e){throw new Workspace.Problem(503,body==null?"PAYMENT_PROVIDER_UNAVAILABLE":"PAYMENT_ORDER_OUTCOME_UNKNOWN");}
    }
    public Map<String,Object> createOrder(long amount,String receipt,String binding){return request("orders",Map.of("amount",amount,"currency","INR","receipt",receipt,"notes",Map.of("taxdesk_binding",binding)));}
    private static String id(String id,String prefix){if(!id.matches(prefix+"[A-Za-z0-9]{1,64}"))throw new Workspace.Problem(400,"INVALID_PROVIDER_ID");return id;}
    public Map<String,Object> order(String id){return request("orders/"+id(id,"order_"),null);}
    public Map<String,Object> payment(String id){return request("payments/"+id(id,"pay_"),null);}
    public Map<String,Object> refund(String id){return request("refunds/"+id(id,"rfnd_"),null);}
}
