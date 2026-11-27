package repository;
import java.util.*;
public final class IdempotencyStore {
    public static final class Record {
        public final String fingerprint; public final Object response;
        Record(String f,Object r){fingerprint=f;response=r;}
    }
    private static final IdempotencyStore INSTANCE=new IdempotencyStore();
    private final Map<String,Record> records=new HashMap<>();
    private IdempotencyStore(){}
    public static IdempotencyStore getInstance(){return INSTANCE;}
    public synchronized Record get(String key){return records.get(key);}
    public synchronized void put(String key,String fingerprint,Object response){if(key!=null&&!key.isBlank())records.put(key,new Record(fingerprint,response));}
    public synchronized void clear(){records.clear();}
}
