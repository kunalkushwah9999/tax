package com.taxplatform.startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Operator-pinned, signed TEST attestations. This is not an ICAI verification service. */
public final class ProfessionalRegistry {
    private final Path path;private final String encodedKey;private final PublicKey publicKey;private final Clock clock;
    private ProfessionalRegistry(Path path,String encodedKey,PublicKey key,Clock clock){this.path=path;this.encodedKey=encodedKey;publicKey=key;this.clock=clock;}
    public static ProfessionalRegistry disabled(){return new ProfessionalRegistry(null,"",null,Clock.systemUTC());}
    private static Map<String,Object> read(Path path)throws IOException{
        if(Files.isSymbolicLink(path)||Files.size(path)>131072)throw new IOException("bounded operator registry required");
        if(Files.getFileStore(path).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(path).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("registry must be private");
        return Json.object(Files.readAllBytes(path));
    }
    public static ProfessionalRegistry load(Path path,Clock clock)throws Exception{
        Map<String,Object> cfg=read(path);Workspace.fields(cfg,"mode","public_key","attestations");if(!"TEST".equals(cfg.get("mode")))throw new IOException("live professional verification is not released");
        String key=Workspace.text(cfg,"public_key",20,256);PublicKey parsed=KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
        if(Json.list(cfg.get("attestations")).size()>100)throw new IOException("registry limit");return new ProfessionalRegistry(path,key,parsed,clock);
    }
    public boolean configured(){return path!=null;}
    public Map<String,Object> attestation(String userId,String topic){
        if(path==null)return null;try{
            Map<String,Object> cfg=read(path);Workspace.fields(cfg,"mode","public_key","attestations");if(!"TEST".equals(cfg.get("mode"))||!encodedKey.equals(cfg.get("public_key")))return null;
            List<Object> entries=Json.list(cfg.get("attestations"));if(entries.size()>100)return null;Map<String,Object> found=null;
            for(Object raw:entries){Map<String,Object> entry=Json.map(raw);Workspace.fields(entry,"payload_b64","signature_b64");String payload=Workspace.text(entry,"payload_b64",1,4096),encoded=Workspace.text(entry,"signature_b64",1,200);byte[] bytes=Base64.getDecoder().decode(payload);Signature signature=Signature.getInstance("Ed25519");signature.initVerify(publicKey);signature.update(bytes);if(!signature.verify(Base64.getDecoder().decode(encoded)))return null;
                Map<String,Object> claim=Json.object(bytes);Workspace.fields(claim,"subject_id","credential_ref","skills","not_before","expires_at","status","purpose");
                if(!"TEST_PREPARATION_ONLY".equals(claim.get("purpose")))return null;if(!userId.equals(claim.get("subject_id")))continue;
                if(found!=null)return null;found=claim;
            }
            if(found==null||!"ACTIVE".equals(found.get("status")))return null;Instant start=Instant.parse((String)found.get("not_before")),end=Instant.parse((String)found.get("expires_at"));if(clock.instant().isBefore(start)||!clock.instant().isBefore(end)||!end.isAfter(start)||Duration.between(start,end).compareTo(Duration.ofDays(30))>0)return null;
            String ref=Workspace.text(found,"credential_ref",1,80);if(!ref.matches("[A-Za-z0-9._:-]+"))return null;List<Object> skills=Json.list(found.get("skills"));if(skills.isEmpty()||skills.size()>2||!Set.of("ITR","GST").containsAll(skills)||topic!=null&&!skills.contains(topic))return null;
            return Map.of("subject_id",userId,"credential_ref",ref,"skills",skills,"expires_at",end.toString(),"verification","SIGNED_TEST_ATTESTATION_ONLY","real_ca_verified",false);
        }catch(Exception e){return null;}
    }
    public boolean valid(String userId,String topic){return attestation(userId,topic)!=null;}
}
