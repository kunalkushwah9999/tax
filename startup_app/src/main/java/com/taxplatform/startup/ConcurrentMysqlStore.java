package com.taxplatform.startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.zip.CRC32;

/** Opt-in schema-2 store: optimistic work, validated point/scan dependencies and short shard locks. */
public final class ConcurrentMysqlStore implements DataStore {
    static final List<String> NAMES=List.of("users","cases","grants","tickets","prices","quotes","receipts","operations","audit");
    private static final byte[] PROBE="tax-platform/mysql-key/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final int SHARDS=32,ATTEMPTS=6;
    private final SqlConfig config;private final RecordCrypto crypto;private final boolean isolated;private final MysqlStore.CommitHook hook;private final ReentrantReadWriteLock lifecycle=new ReentrantReadWriteLock();private boolean closed;
    private static final class Conflict extends RuntimeException {private static final long serialVersionUID=1L;}
    private static final class DatabaseFailure extends RuntimeException {private static final long serialVersionUID=1L;DatabaseFailure(Exception e){super("database unavailable",e);}}
    private record Original(long revision,String json,int bytes){ }
    private record Control(long epoch,long count,long bytes){ }
    private record Change(String namespace,String id,Original old,Object value,RecordCrypto.Encrypted encrypted,byte[] tag){ }
    public ConcurrentMysqlStore(SqlConfig config,Path key)throws IOException,GeneralSecurityException,SQLException{this(config,key,c->{ },false);}
    ConcurrentMysqlStore(SqlConfig config,Path key,MysqlStore.CommitHook hook)throws IOException,GeneralSecurityException,SQLException{this(config,key,hook,false);}
    private ConcurrentMysqlStore(SqlConfig config,Path key,MysqlStore.CommitHook hook,boolean isolated)throws IOException,GeneralSecurityException,SQLException{
        this.config=config;crypto=new RecordCrypto(key,false);this.hook=hook;this.isolated=isolated;
        try(Connection c=config.connect()){supported(c);verify(c,crypto);recovery(c,isolated,false);try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM workspace_concurrency")){r.next();if(r.getLong(1)!=NAMES.size()*SHARDS)throw new SQLException("concurrency controls incomplete");}}
        catch(SQLException|GeneralSecurityException e){crypto.close();throw e;}
    }
    private static void supported(Connection c)throws SQLException{if(!"MySQL".equals(c.getMetaData().getDatabaseProductName())||!c.getMetaData().getDatabaseProductVersion().startsWith("8.4."))throw new SQLException("MySQL 8.4 required");}
    private static void verify(Connection c,RecordCrypto key)throws SQLException,GeneralSecurityException{
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT schema_version,key_nonce,key_probe FROM workspace_meta WHERE id=1")){if(!r.next()||r.getInt(1)!=2)throw new SQLException("concurrent schema-2 initialization required; legacy and concurrent profiles cannot share a database");if(!java.security.MessageDigest.isEqual(key.decrypt("meta","key",1,r.getBytes(2),r.getBytes(3)),PROBE))throw new GeneralSecurityException("key binding invalid");}
    }
    private static void recovery(Connection c,boolean isolated,boolean write)throws SQLException{try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT state FROM workspace_recovery WHERE id=1"+(write?" FOR SHARE":""))){if(!r.next()||!(isolated?"ISOLATED":"ACTIVE").equals(r.getString(1)))throw new SQLException("recovery isolation prevents application access");}}
    public static void initialize(SqlConfig config,Path key)throws IOException,SQLException,GeneralSecurityException{initialize(config,key,false);}
    static void initialize(SqlConfig config,Path key,boolean isolated)throws IOException,SQLException,GeneralSecurityException{
        try(Connection c=config.connect()){
            supported(c);long tables;try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")){r.next();tables=r.getLong(1);}
            if(tables!=0){if(isolated)throw new SQLException("restore requires an empty database");try(RecordCrypto existing=new RecordCrypto(key,false)){verify(c,existing);recovery(c,false,false);}return;}
            if(Files.exists(key)||Files.isSymbolicLink(key))throw new IOException("fresh concurrent database requires a new record key");
            for(String name:List.of("V001__encrypted_records.sql","V002__recovery_isolation.sql","V003__startup_operations.sql","V004__concurrent_records.sql")){
                String script;try(var in=ConcurrentMysqlStore.class.getResourceAsStream("/db/"+name)){if(in==null)throw new IOException("migration unavailable");script=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
                if(name.startsWith("V001"))script=script.replace("schema_version = 1","schema_version IN (1,2)");
                for(String sql:script.split(";"))if(!sql.isBlank())try(Statement q=c.createStatement()){q.execute(sql);}
            }
            try(RecordCrypto created=new RecordCrypto(key,true)){
                c.setAutoCommit(false);try{RecordCrypto.Encrypted proof=created.encrypt("meta","key",1,PROBE);
                    try(PreparedStatement q=c.prepareStatement("INSERT INTO workspace_meta(id,schema_version,key_nonce,key_probe,payload_bytes) VALUES(1,2,?,?,0)")){q.setBytes(1,proof.nonce());q.setBytes(2,proof.ciphertext());q.executeUpdate();}
                    try(PreparedStatement q=c.prepareStatement("INSERT INTO workspace_recovery(id,state) VALUES(1,?)")){q.setString(1,isolated?"ISOLATED":"ACTIVE");q.executeUpdate();}
                    try(PreparedStatement q=c.prepareStatement("INSERT INTO workspace_concurrency(namespace,shard) VALUES(?,?)")){for(String ns:NAMES)for(int shard=0;shard<SHARDS;shard++){q.setString(1,ns);q.setInt(2,shard);q.addBatch();}q.executeBatch();}c.commit();
                }catch(SQLException|GeneralSecurityException e){rollback(c);throw e;}
            }
        }
    }
    static int shard(String id){CRC32 crc=new CRC32();crc.update(id.getBytes(java.nio.charset.StandardCharsets.US_ASCII));return (int)(crc.getValue()%SHARDS);}
    private static String controlKey(String namespace,int shard){return namespace+":"+String.format(java.util.Locale.ROOT,"%02d",shard);}
    @Override public String backend(){return "MYSQL_CONCURRENT_RECORDS_SCHEMA2";}
    @Override public <T>T read(Function<Map<String,Object>,T> work){return unit(false,work);}
    @Override public <T>T transaction(Function<Map<String,Object>,T> work){return unit(true,work);}
    private <T>T unit(boolean write,Function<Map<String,Object>,T> work){
        lifecycle.readLock().lock();try{if(closed)throw new Workspace.Problem(503,"STORAGE_CLOSED");
            for(int attempt=0;attempt<(write?ATTEMPTS:1);attempt++){
                boolean commitStarted=false;try(Connection c=config.connect()){
                    c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setReadOnly(!write);c.setAutoCommit(false);
                    try{
                        try(Statement q=c.createStatement()){q.execute("SET SESSION innodb_lock_wait_timeout=3");}verify(c,crypto);recovery(c,isolated,write);
                        State state=new State(c,write);T result=work.apply(state.root);if(write)state.flush();commitStarted=true;c.commit();if(write)hook.acknowledged(c);return result;
                    }catch(Workspace.Problem e){rollback(c);throw e;}
                    catch(Conflict e){rollback(c);if(attempt+1==ATTEMPTS)throw new Workspace.Problem(409,"CONCURRENT_WRITE_RETRY");}
                    catch(SQLException|GeneralSecurityException|DatabaseFailure e){rollback(c);SQLException sql=e instanceof SQLException s?s:e.getCause() instanceof SQLException s?s:null;
                        if(write&&!commitStarted&&sql!=null&&Set.of(1213,1205,1062).contains(sql.getErrorCode())&&attempt+1<ATTEMPTS)continue;
                        throw new Workspace.Problem(503,write&&commitStarted?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE");
                    }catch(RuntimeException e){rollback(c);throw e;}
                }catch(SQLException e){throw new Workspace.Problem(503,write&&commitStarted?"STORAGE_OUTCOME_UNKNOWN":"MYSQL_STORAGE_UNAVAILABLE");}
            }throw new Workspace.Problem(409,"CONCURRENT_WRITE_RETRY");
        }finally{lifecycle.readLock().unlock();}
    }
    private static void rollback(Connection c){try{c.rollback();}catch(SQLException ignored){/* Never retries an uncertain commit. */}}
    @Override public void close(){lifecycle.writeLock().lock();try{closed=true;crypto.close();}finally{lifecycle.writeLock().unlock();}}
    private final class State {
        final Connection c;final boolean write;final Map<String,Object> root=new LinkedHashMap<>();final Map<String,Table> tables=new LinkedHashMap<>();final Map<String,Control> snapshot=new LinkedHashMap<>();final Set<String> scans=new LinkedHashSet<>();List<Object> events;
        State(Connection c,boolean write){this.c=c;this.write=write;root.put("schema",1L);for(String ns:NAMES){Table table=new Table(this,ns);tables.put(ns,table);if(!ns.equals("audit"))root.put(ns,table);}root.put("audit",new java.util.AbstractList<Object>(){
            private List<Object> values(){if(events==null)events=new ArrayList<>(tables.get("audit").values());return events;}
            @Override public Object get(int i){return values().get(i);}@Override public int size(){return values().size();}
            @Override public boolean add(Object v){if(!write)throw new IllegalStateException("read only");return values().add(v);}
            @Override public Object remove(int i){if(!write)throw new IllegalStateException("read only");return values().remove(i);}
        });}
        Control snapshot(String ns,int shard){String key=controlKey(ns,shard);Control cached=snapshot.get(key);if(cached!=null)return cached;try(PreparedStatement q=c.prepareStatement("SELECT epoch,record_count,payload_bytes FROM workspace_concurrency WHERE namespace=? AND shard=?")){q.setString(1,ns);q.setInt(2,shard);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("missing shard control");Control value=new Control(r.getLong(1),r.getLong(2),r.getLong(3));snapshot.put(key,value);return value;}}catch(SQLException e){throw new DatabaseFailure(e);}}
        void scan(String ns){if(!write||ns.equals("audit"))return;for(int i=0;i<SHARDS;i++)snapshot(ns,i);scans.add(ns);}
        void flush()throws SQLException,GeneralSecurityException{
            if(events!=null){Table audit=tables.get("audit");audit.values.clear();for(Object e:events)audit.values.put((String)Json.map(e).get("id"),e);}
            List<Change> changes=new ArrayList<>();for(Table table:tables.values())table.changes(changes);
            Map<String,Boolean> locks=new TreeMap<>();for(Table table:tables.values()){
                if(!table.ns.equals("audit"))for(String id:table.observed())locks.putIfAbsent(controlKey(table.ns,shard(id)),false);
                if(scans.contains(table.ns)||table.capacity!=null)for(int i=0;i<SHARDS;i++)locks.putIfAbsent(controlKey(table.ns,i),false);
            }for(Change change:changes)locks.put(controlKey(change.namespace(),shard(change.id())),true);
            Map<String,Control> current=new LinkedHashMap<>();for(var lock:locks.entrySet()){
                int colon=lock.getKey().lastIndexOf(':');String ns=lock.getKey().substring(0,colon);int shard=Integer.parseInt(lock.getKey().substring(colon+1));
                try(PreparedStatement q=c.prepareStatement("SELECT epoch,record_count,payload_bytes FROM workspace_concurrency WHERE namespace=? AND shard=?"+(lock.getValue()?" FOR UPDATE":" FOR SHARE"))){q.setString(1,ns);q.setInt(2,shard);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("missing shard");current.put(lock.getKey(),new Control(r.getLong(1),r.getLong(2),r.getLong(3)));}}
            }
            for(String ns:scans)for(int i=0;i<SHARDS;i++)if(current.get(controlKey(ns,i)).epoch()!=snapshot(ns,i).epoch())throw new Conflict();
            for(Table table:tables.values())if(!table.ns.equals("audit"))table.validate();
            for(Table table:tables.values())if(table.capacity!=null){long count=0,delta=0;for(int i=0;i<SHARDS;i++)count+=current.get(controlKey(table.ns,i)).count();for(Change change:changes)if(change.namespace().equals(table.ns))delta+=change.old()==null?1:change.value()==null?-1:0;if(count+delta>table.capacity)throw new Workspace.Problem(409,table.capacityCode);}
            Map<String,long[]> deltas=new TreeMap<>();for(Change change:changes){apply(c,change);long[] delta=deltas.computeIfAbsent(controlKey(change.namespace(),shard(change.id())),ignored->new long[2]);delta[0]+=change.old()==null?1:change.value()==null?-1:0;delta[1]+=(change.value()==null?0:change.encrypted().ciphertext().length)-(change.old()==null?0:change.old().bytes());}
            for(var entry:deltas.entrySet()){int colon=entry.getKey().lastIndexOf(':');try(PreparedStatement q=c.prepareStatement("UPDATE workspace_concurrency SET epoch=epoch+1,record_count=record_count+?,payload_bytes=payload_bytes+? WHERE namespace=? AND shard=?")){q.setLong(1,entry.getValue()[0]);q.setLong(2,entry.getValue()[1]);q.setString(3,entry.getKey().substring(0,colon));q.setInt(4,Integer.parseInt(entry.getKey().substring(colon+1)));if(q.executeUpdate()!=1)throw new SQLException("shard accounting missing");}}
        }
    }
    private Change change(String ns,String id,Original old,Object value)throws GeneralSecurityException{
        if(value==null)return new Change(ns,id,old,null,null,null);byte[] plain=Json.bytes(value);if(plain.length>262144)throw new Workspace.Problem(409,"MYSQL_RECORD_LIMIT");RecordCrypto.Encrypted encrypted;try{encrypted=crypto.encrypt(ns,id,old==null?1:old.revision()+1,plain);}finally{java.util.Arrays.fill(plain,(byte)0);}byte[] tag=ns.equals("users")?crypto.usernameTag((String)Json.map(value).get("username")):null;return new Change(ns,id,old,value,encrypted,tag);
    }
    private static void apply(Connection c,Change value)throws SQLException{
        if(value.value()==null){try(PreparedStatement q=c.prepareStatement("DELETE FROM workspace_records WHERE namespace=? AND record_id=? AND db_revision=?")){q.setString(1,value.namespace());q.setString(2,value.id());q.setLong(3,value.old().revision());if(q.executeUpdate()!=1)throw new Conflict();}return;}
        long revision=value.old()==null?1:value.old().revision()+1;
        if(value.old()==null)try(PreparedStatement q=c.prepareStatement("INSERT INTO workspace_records(namespace,record_id,db_revision,lookup_tag,nonce,ciphertext) VALUES(?,?,?,?,?,?)")){q.setString(1,value.namespace());q.setString(2,value.id());q.setLong(3,revision);q.setBytes(4,value.tag());q.setBytes(5,value.encrypted().nonce());q.setBytes(6,value.encrypted().ciphertext());q.executeUpdate();}
        else try(PreparedStatement q=c.prepareStatement("UPDATE workspace_records SET db_revision=?,lookup_tag=?,nonce=?,ciphertext=? WHERE namespace=? AND record_id=? AND db_revision=?")){q.setLong(1,revision);q.setBytes(2,value.tag());q.setBytes(3,value.encrypted().nonce());q.setBytes(4,value.encrypted().ciphertext());q.setString(5,value.namespace());q.setString(6,value.id());q.setLong(7,value.old().revision());if(q.executeUpdate()!=1)throw new Conflict();}
    }
    private final class Table extends AbstractMap<String,Object> implements Capacity.Checked {
        final State state;final String ns;final Map<String,Object> values=new LinkedHashMap<>();final Map<String,Original> original=new LinkedHashMap<>();final Set<String> missing=new LinkedHashSet<>();boolean complete;Long capacity;String capacityCode;
        Table(State state,String ns){this.state=state;this.ns=ns;}
        Set<String> observed(){Set<String> ids=new LinkedHashSet<>(original.keySet());ids.addAll(missing);return ids;}
        private void accept(ResultSet r)throws SQLException,GeneralSecurityException{String id=r.getString("record_id");if(original.containsKey(id)||values.containsKey(id))return;long revision=r.getLong("db_revision");byte[] plain=crypto.decrypt(ns,id,revision,r.getBytes("nonce"),r.getBytes("ciphertext"));try{Map<String,Object> row=Json.object(plain);original.put(id,new Original(revision,Json.encode(row),r.getInt("nbytes")));values.put(id,row);}finally{java.util.Arrays.fill(plain,(byte)0);}}
        @Override public Object get(Object value){if(!(value instanceof String id)||id.length()>180)return null;if(values.containsKey(id)||original.containsKey(id)||missing.contains(id)||complete)return values.get(id);try(PreparedStatement q=state.c.prepareStatement("SELECT record_id,db_revision,nonce,ciphertext,OCTET_LENGTH(ciphertext) nbytes FROM workspace_records WHERE namespace=? AND record_id=?")){q.setString(1,ns);q.setString(2,id);try(ResultSet r=q.executeQuery()){if(r.next())accept(r);else missing.add(id);}return values.get(id);}catch(SQLException|GeneralSecurityException e){throw new DatabaseFailure(e);}}
        private void all(){if(complete)return;state.scan(ns);try(PreparedStatement q=state.c.prepareStatement("SELECT record_id,db_revision,nonce,ciphertext,OCTET_LENGTH(ciphertext) nbytes FROM workspace_records WHERE namespace=? ORDER BY ordinal")){q.setString(1,ns);try(ResultSet r=q.executeQuery()){while(r.next())accept(r);}complete=true;}catch(SQLException|GeneralSecurityException e){throw new DatabaseFailure(e);}}
        @Override public Set<Entry<String,Object>> entrySet(){all();return values.entrySet();}
        @Override public Object put(String id,Object row){if(!state.write)throw new IllegalStateException("read only");if(id==null||!id.matches("[A-Za-z0-9._:/-]{1,180}")||!(row instanceof Map<?,?>))throw new IllegalArgumentException("invalid record");Object old=get(id);values.put(id,row);return old;}
        @Override public Object remove(Object id){if(!state.write)throw new IllegalStateException("read only");Object old=get(id);values.remove(id);return old;}
        @Override public void requireSpace(long maximum,String code){if(!state.write)throw new IllegalStateException("capacity guard requires transaction");long count=0;for(int i=0;i<SHARDS;i++)count+=state.snapshot(ns,i).count();for(String id:values.keySet())if(!original.containsKey(id))count++;for(String id:original.keySet())if(!values.containsKey(id))count--;if(count>=maximum)throw new Workspace.Problem(409,code);if(capacity==null||maximum<capacity){capacity=maximum;capacityCode=code;}}
        void validate()throws SQLException{for(String id:observed())try(PreparedStatement q=state.c.prepareStatement("SELECT db_revision FROM workspace_records WHERE namespace=? AND record_id=? FOR SHARE")){q.setString(1,ns);q.setString(2,id);try(ResultSet r=q.executeQuery()){Original old=original.get(id);if(old==null){if(r.next())throw new Conflict();}else if(!r.next()||r.getLong(1)!=old.revision())throw new Conflict();}}}
        void changes(List<Change> out)throws GeneralSecurityException{Set<String> ids=new LinkedHashSet<>(original.keySet());ids.addAll(values.keySet());for(String id:ids){Original old=original.get(id);Object row=values.get(id);if(old==null&&row==null)continue;if(old!=null&&row!=null&&Json.map(row).equals(Json.object(old.json().getBytes(java.nio.charset.StandardCharsets.UTF_8))))continue;out.add(change(ns,id,old,row));}}
    }
    @FunctionalInterface interface RowVisitor {void accept(String namespace,String id,Map<String,Object> value)throws IOException,GeneralSecurityException;}
    void visitSnapshot(RowVisitor visitor)throws IOException,GeneralSecurityException,SQLException{
        lifecycle.readLock().lock();try{if(closed)throw new IOException("store closed");try(Connection c=config.connect()){c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);try{verify(c,crypto);recovery(c,isolated,false);try(Statement q=c.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY)){q.setFetchSize(Integer.MIN_VALUE);try(ResultSet r=q.executeQuery("SELECT namespace,record_id,db_revision,nonce,ciphertext FROM workspace_records ORDER BY ordinal")){while(r.next()){byte[] plain=crypto.decrypt(r.getString(1),r.getString(2),r.getLong(3),r.getBytes(4),r.getBytes(5));try{visitor.accept(r.getString(1),r.getString(2),Json.object(plain));}finally{java.util.Arrays.fill(plain,(byte)0);}}}}c.commit();}catch(IOException|GeneralSecurityException|SQLException|RuntimeException e){rollback(c);throw e;}}}finally{lifecycle.readLock().unlock();}
    }
    static Map<String,Object> inspectIsolated(SqlConfig config,Path key)throws IOException,GeneralSecurityException,SQLException{try(ConcurrentMysqlStore store=new ConcurrentMysqlStore(config,key,c->{ },true)){return store.read(state->Json.object(Json.bytes(state),25_165_824));}}
    static Map<String,Object> restoreFramed(SqlConfig config,Path key,Path archive,Path backupKey)throws Exception{
        if(Files.exists(key)||Files.isSymbolicLink(key)||key.toAbsolutePath().normalize().equals(backupKey.toAbsolutePath().normalize()))throw new IOException("fresh separate record key required");
        // Fully verify once before any target mutation; repeat authentication while importing.
        FramedBackup.verify(archive,backupKey);initialize(config,key,true);
        try(ConcurrentMysqlStore store=new ConcurrentMysqlStore(config,key,c->{ },true);Connection c=config.connect()){
            c.setAutoCommit(false);try{FramedBackup.read(archive,backupKey,(ns,id,row)->{
                try{Change change=store.change(ns,id,null,row);apply(c,change);try(PreparedStatement q=c.prepareStatement("UPDATE workspace_concurrency SET epoch=epoch+1,record_count=record_count+1,payload_bytes=payload_bytes+? WHERE namespace=? AND shard=?")){q.setLong(1,change.encrypted().ciphertext().length);q.setString(2,ns);q.setInt(3,shard(id));if(q.executeUpdate()!=1)throw new SQLException("missing shard");}}
                catch(SQLException e){throw new IOException("isolated import failed",e);}
            });c.commit();}catch(Exception e){rollback(c);throw e;}
        }
        return Map.of("status","RESTORED_ISOLATED","application_start_allowed",false,"production_recovery_approved",false,"deletion_revocation_reconciled",false);
    }
}
