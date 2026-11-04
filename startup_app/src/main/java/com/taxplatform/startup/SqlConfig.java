package com.taxplatform.startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/** Trusted operator file only. No JDBC URL, arbitrary driver properties or credentials in command arguments. */
public final class SqlConfig {
    private final String host,database,user,password,trustStore,trustPassword;private final int port;
    private SqlConfig(String host,int port,String database,String user,String password,String trustStore,String trustPassword){this.host=host;this.port=port;this.database=database;this.user=user;this.password=password;this.trustStore=trustStore;this.trustPassword=trustPassword;}
    public static SqlConfig load(Path file)throws IOException{
        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file)||Files.size(file)>8192)throw new IOException("private SQL configuration required");
        if(Files.getFileStore(file).supportsFileAttributeView("posix")&&Files.getPosixFilePermissions(file).stream().anyMatch(p->p.name().startsWith("GROUP_")||p.name().startsWith("OTHERS_")))throw new IOException("SQL configuration must be private to its owner");
        Map<String,Object> data=Json.object(Files.readAllBytes(file));Workspace.fields(data,"host","port","database","user","password","trust_store","trust_store_password");
        String host=Workspace.text(data,"host",1,253),database=Workspace.text(data,"database",1,48),user=Workspace.text(data,"user",1,64),password=Workspace.text(data,"password",1,256),trust=Workspace.text(data,"trust_store",1,4096),trustPassword=Workspace.text(data,"trust_store_password",1,256);
        long port=Workspace.number(data,"port");if(!host.matches("[A-Za-z0-9][A-Za-z0-9.-]*")||!database.matches("[A-Za-z][A-Za-z0-9_]*")||port<1||port>65535)throw new IOException("invalid SQL endpoint");
        Path trustFile=Path.of(trust);if(!trustFile.isAbsolute()||Files.isSymbolicLink(trustFile)||!Files.isRegularFile(trustFile))throw new IOException("absolute SQL trust store required");
        return new SqlConfig(host,(int)port,database,user,password,trust,trustPassword);
    }
    Connection connect()throws SQLException{
        Properties p=new Properties();p.setProperty("user",user);p.setProperty("password",password);p.setProperty("sslMode","VERIFY_IDENTITY");
        p.setProperty("trustCertificateKeyStoreUrl",Path.of(trustStore).toUri().toString());p.setProperty("trustCertificateKeyStoreType","PKCS12");p.setProperty("trustCertificateKeyStorePassword",trustPassword);p.setProperty("fallbackToSystemTrustStore","false");
        p.setProperty("allowPublicKeyRetrieval","false");p.setProperty("allowLoadLocalInfile","false");p.setProperty("allowMultiQueries","false");p.setProperty("autoReconnect","false");p.setProperty("connectTimeout","3000");p.setProperty("socketTimeout","10000");p.setProperty("connectionTimeZone","UTC");
        return DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+database,p);
    }
    @Override public String toString(){return "SqlConfig[redacted]";}
}
