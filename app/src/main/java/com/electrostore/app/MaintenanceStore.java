package com.electrostore.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;


public final class MaintenanceStore {
    private final Context context;
    private final SQLiteDatabase db;
    public MaintenanceStore(Context c) { context=c.getApplicationContext();db = Db.get(c).getWritableDatabase(); }

    static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE services (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "preset_key TEXT UNIQUE, name TEXT NOT NULL DEFAULT '', tips TEXT NOT NULL DEFAULT '',"
                + "active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE service_jobs (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "service_id INTEGER NOT NULL DEFAULT 0, preset_key TEXT NOT NULL DEFAULT '',"
                + "name_en TEXT NOT NULL DEFAULT '', name_ar TEXT NOT NULL DEFAULT '',"
                + "tips_en TEXT NOT NULL DEFAULT '', tips_ar TEXT NOT NULL DEFAULT '',"
                + "customer TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',"
                + "device TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '',"
                + "due_day TEXT NOT NULL DEFAULT '', status TEXT NOT NULL DEFAULT 'RECEIVED',"
                + "checked_steps INTEGER NOT NULL DEFAULT 0, fee REAL NOT NULL DEFAULT 0,"
                + "paid REAL NOT NULL DEFAULT 0, created_at INTEGER NOT NULL DEFAULT 0,"
                + "updated_at INTEGER NOT NULL DEFAULT 0, message_opened_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_jobs_status_due ON service_jobs(status,due_day)");
        db.execSQL("CREATE INDEX idx_jobs_service ON service_jobs(service_id)");
        for (String key : ServiceCatalog.KEYS) {
            ContentValues v = new ContentValues(); v.put("preset_key", key);
            db.insertOrThrow("services", null, v);
        }
    }


    static void createCustomers(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE service_customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL)");
        db.execSQL("ALTER TABLE service_jobs ADD COLUMN customer_id INTEGER NOT NULL DEFAULT 0");
        db.execSQL("CREATE INDEX idx_jobs_customer ON service_jobs(customer_id)");

        db.execSQL("INSERT INTO service_customers(name,phone) SELECT TRIM(customer),phone FROM service_jobs GROUP BY TRIM(customer),phone ORDER BY MIN(id)");
        db.execSQL("UPDATE service_jobs SET customer_id=(SELECT id FROM service_customers WHERE name=TRIM(service_jobs.customer) AND phone=service_jobs.phone LIMIT 1)");
    }
    static void createPayments(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE service_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,job_id INTEGER NOT NULL,amount REAL NOT NULL,day TEXT NOT NULL,ts INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_service_payments_day ON service_payments(day)");
        db.execSQL("CREATE INDEX idx_service_payments_job ON service_payments(job_id)");
        db.execSQL("INSERT INTO service_payments(job_id,amount,day,ts) SELECT id,paid,CASE WHEN created_at>0 THEN date(created_at/1000,'unixepoch','localtime') ELSE date('now','localtime') END,created_at FROM service_jobs WHERE paid>0");
        db.execSQL("CREATE TRIGGER service_job_delete_payments AFTER DELETE ON service_jobs BEGIN DELETE FROM service_payments WHERE job_id=OLD.id; END");
    }
    private static ServiceCustomer customerRow(Cursor c) {
        ServiceCustomer out = new ServiceCustomer(); out.id=l(c,"id"); out.name=s(c,"name"); out.phone=s(c,"phone");
        if(c.getColumnIndex("job_count")>=0) { out.jobs=(int)l(c,"job_count"); out.activeJobs=(int)l(c,"active_count"); }
        return out;
    }
    public List<ServiceCustomer> customers(String query) {
        String sql="SELECT c.*,COUNT(j.id) AS job_count,COALESCE(SUM(j.status NOT IN ('COLLECTED','CANCELLED')),0) AS active_count"
                +" FROM service_customers c LEFT JOIN service_jobs j ON j.customer_id=c.id"
                +" WHERE c.name LIKE ? OR c.phone LIKE ? GROUP BY c.id ORDER BY c.name COLLATE NOCASE,c.id";
        String q="%"+(query==null?"":query.trim())+"%";
        Cursor cur=db.rawQuery(sql,new String[]{q,q}); List<ServiceCustomer> out=new ArrayList<ServiceCustomer>();
        try { while(cur.moveToNext())out.add(customerRow(cur)); } finally { cur.close(); } return out;
    }
    public ServiceCustomer customer(long id) {
        Cursor cur=db.rawQuery("SELECT c.*,(SELECT COUNT(*) FROM service_jobs WHERE customer_id=c.id) AS job_count,"
                +"(SELECT COUNT(*) FROM service_jobs WHERE customer_id=c.id AND status NOT IN ('COLLECTED','CANCELLED')) AS active_count FROM service_customers c WHERE id=?",new String[]{String.valueOf(id)});
        try { return cur.moveToFirst()?customerRow(cur):null; } finally {cur.close();}
    }
    private long matchingCustomer(String name,String phone) {
        Cursor cur=db.rawQuery("SELECT id FROM service_customers WHERE name=? AND phone=? ORDER BY id LIMIT 1",new String[]{name.trim(),phone});
        try {return cur.moveToFirst()?cur.getLong(0):0;} finally {cur.close();}
    }
    public long saveCustomer(ServiceCustomer customer) {
        if(customer.name.trim().isEmpty()||!customer.phone.matches("[1-9][0-9]{6,14}"))throw new IllegalArgumentException("Name and international phone required");
        long id=customer.id;
        db.beginTransaction();
        try {
            if(id==0)id=matchingCustomer(customer.name,customer.phone);
            ContentValues values=new ContentValues(); values.put("name",customer.name.trim());values.put("phone",customer.phone);
            if(id==0)id=db.insertOrThrow("service_customers",null,values);
            else if(db.update("service_customers",values,"id=?",new String[]{String.valueOf(id)})!=1)throw new IllegalStateException("Customer missing");
            ContentValues jobs=new ContentValues();jobs.put("customer",customer.name.trim());jobs.put("phone",customer.phone);jobs.put("updated_at",System.currentTimeMillis());
            db.update("service_jobs",jobs,"customer_id=?",new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        customer.id=id; return id;
    }

    public void deleteCustomer(long id) {
        db.beginTransaction();
        try {
            db.delete("service_jobs","customer_id=?",new String[]{String.valueOf(id)});
            db.delete("service_customers","id=?",new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    public List<ServiceJob> customerJobs(long id) {
        Cursor cur=db.rawQuery("SELECT * FROM service_jobs WHERE customer_id=? ORDER BY created_at DESC,id DESC",new String[]{String.valueOf(id)});
        List<ServiceJob> out=new ArrayList<ServiceJob>();try {while(cur.moveToNext())out.add(job(cur));}finally {cur.close();}return out;
    }

    private static String s(Cursor c, String key) { return c.getString(c.getColumnIndexOrThrow(key)); }
    private static long l(Cursor c, String key) { return c.getLong(c.getColumnIndexOrThrow(key)); }
    private static double d(Cursor c, String key) { return c.getDouble(c.getColumnIndexOrThrow(key)); }
    private static ServiceType type(Cursor c) {
        ServiceType s = new ServiceType();
        s.id = l(c, "id"); s.presetKey = s(c, "preset_key");
        if (s.presetKey == null) s.presetKey = "";
        s.name = s(c, "name"); s.tips = s(c, "tips"); s.active = l(c, "active") == 1;
        return s;
    }
    public List<ServiceType> services(boolean includeRemoved) {
        List<ServiceType> out = new ArrayList<ServiceType>();
        Cursor c = db.rawQuery("SELECT * FROM services" + (includeRemoved ? "" : " WHERE active=1") + " ORDER BY id", null);
        try { while (c.moveToNext()) out.add(type(c)); } finally { c.close(); }
        return out;
    }
    public ServiceType service(long id) {
        Cursor c = db.rawQuery("SELECT * FROM services WHERE id=?", new String[]{String.valueOf(id)});
        try { return c.moveToFirst() ? type(c) : null; } finally { c.close(); }
    }
    public long saveService(ServiceType s) {
        if (s.name.trim().isEmpty() && s.presetKey.isEmpty()) throw new IllegalArgumentException("Name required");
        ContentValues v = new ContentValues();
        if (s.presetKey.isEmpty()) v.putNull("preset_key"); else v.put("preset_key", s.presetKey);
        v.put("name", s.name.trim()); v.put("tips", s.tips.trim()); v.put("active", s.active ? 1 : 0);
        if (s.id == 0) s.id = db.insertOrThrow("services", null, v);
        else if (db.update("services", v, "id=?", new String[]{String.valueOf(s.id)}) != 1)
            throw new IllegalStateException("Service missing");
        return s.id;
    }
    public void setServiceActive(long id, boolean active) {
        ContentValues v = new ContentValues(); v.put("active", active ? 1 : 0);
        db.update("services", v, "id=?", new String[]{String.valueOf(id)});
    }

    private static ServiceJob job(Cursor c) {
        ServiceJob j = new ServiceJob();
        j.id = l(c, "id"); j.customerId = l(c, "customer_id"); j.serviceId = l(c, "service_id"); j.presetKey = s(c, "preset_key");
        j.nameEn = s(c, "name_en"); j.nameAr = s(c, "name_ar");
        j.tipsEn = s(c, "tips_en"); j.tipsAr = s(c, "tips_ar");
        j.customer = s(c, "customer"); j.phone = s(c, "phone"); j.device = s(c, "device");
        j.notes = s(c, "notes"); j.dueDay = s(c, "due_day"); j.status = s(c, "status");
        j.checkedSteps = l(c, "checked_steps"); j.fee = d(c, "fee"); j.paid = d(c, "paid");
        j.createdAt = l(c, "created_at"); j.updatedAt = l(c, "updated_at");
        j.messageOpenedAt = l(c, "message_opened_at");
        return j;
    }
    public ServiceJob job(long id) {
        Cursor c = db.rawQuery("SELECT * FROM service_jobs WHERE id=?", new String[]{String.valueOf(id)});
        try { return c.moveToFirst() ? job(c) : null; } finally { c.close(); }
    }
    public List<ServiceJob> jobs(String query, String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM service_jobs WHERE 1=1");
        List<String> args = new ArrayList<String>();
        if ("ACTIVE".equals(status)) sql.append(" AND status NOT IN ('COLLECTED','CANCELLED')");
        else if (status != null && !status.isEmpty()) { sql.append(" AND status=?"); args.add(status); }
        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim() + "%";
            sql.append(" AND (customer LIKE ? OR phone LIKE ? OR device LIKE ? OR name_en LIKE ?"
                    + " OR name_ar LIKE ? OR printf('M-%04d',id) LIKE ?)");
            for (int i = 0; i < 6; i++) args.add(q);
        }
        sql.append(" ORDER BY CASE WHEN status IN ('COLLECTED','CANCELLED') THEN 1 ELSE 0 END,"
                + " CASE WHEN due_day='' THEN 1 ELSE 0 END, due_day, created_at DESC, id DESC");
        List<ServiceJob> out = new ArrayList<ServiceJob>();
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        try { while (c.moveToNext()) out.add(job(c)); } finally { c.close(); }
        return out;
    }
    public int[] counts() {
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(status NOT IN ('COLLECTED','CANCELLED')),0),"
                + "COALESCE(SUM(status='READY'),0),COALESCE(SUM(status NOT IN ('COLLECTED','CANCELLED')"
                + " AND due_day<>'' AND due_day<?),0) FROM service_jobs", new String[]{Db.today()});
        try { c.moveToFirst(); return new int[]{c.getInt(0), c.getInt(1), c.getInt(2)}; } finally { c.close(); }
    }
    public long saveJob(ServiceJob j) {
        if (j.customer.trim().isEmpty() || j.device.trim().isEmpty() || !j.phone.matches("[1-9][0-9]{6,14}"))
            throw new IllegalArgumentException("Customer, device and international phone required");
        if(Double.isNaN(j.fee)||Double.isInfinite(j.fee)||Double.isNaN(j.paid)||Double.isInfinite(j.paid)||j.fee<0||j.paid<0||j.paid>j.fee)throw new IllegalArgumentException("Invalid service amounts");
        j.fee=Math.round(j.fee*100.0)/100.0;j.paid=Math.round(j.paid*100.0)/100.0;
        long originalId=j.id, originalCustomerId=j.customerId;
        db.beginTransaction();
        try {
            ServiceJob previous=j.id==0?null:job(j.id);
            double previousPaid=previous==null?0:previous.paid;
            if (j.id == 0) {
                ServiceType service = service(j.serviceId);
                if (service == null || !service.active) throw new IllegalStateException("Service unavailable");
                j.createdAt = System.currentTimeMillis();
            } else if(job(j.id)==null)throw new IllegalStateException("Job missing");
            ServiceCustomer contact=new ServiceCustomer(); contact.id=j.customerId;contact.name=j.customer;contact.phone=j.phone;
            j.customerId=saveCustomer(contact);
            j.updatedAt = System.currentTimeMillis();
            ContentValues v = new ContentValues();
            v.put("customer_id",j.customerId);v.put("service_id", j.serviceId); v.put("preset_key", j.presetKey);
            v.put("name_en", j.nameEn); v.put("name_ar", j.nameAr); v.put("tips_en", j.tipsEn); v.put("tips_ar", j.tipsAr);
            v.put("customer", j.customer.trim()); v.put("phone", j.phone); v.put("device", j.device.trim());
            v.put("notes", j.notes.trim()); v.put("due_day", j.dueDay); v.put("status", j.status);
            v.put("checked_steps", j.checkedSteps); v.put("fee", j.fee); v.put("paid", j.paid);
            v.put("created_at", j.createdAt); v.put("updated_at", j.updatedAt); v.put("message_opened_at", j.messageOpenedAt);
            if (j.id == 0) j.id = db.insertOrThrow("service_jobs", null, v);
            else if (db.update("service_jobs", v, "id=?", new String[]{String.valueOf(j.id)}) != 1)
                throw new IllegalStateException("Job missing");
            double delta=Math.round((j.paid-previousPaid)*100.0)/100.0;
            if(delta!=0){ContentValues payment=new ContentValues();payment.put("job_id",j.id);payment.put("amount",delta);payment.put("day",Db.today());payment.put("ts",System.currentTimeMillis());db.insertOrThrow("service_payments",null,payment);}
            db.setTransactionSuccessful();
        } catch(RuntimeException e) { j.id=originalId;j.customerId=originalCustomerId;throw e; }
        finally {db.endTransaction();}
        return j.id;
    }
    public List<ServiceJob> entriesOn(String day) {
        Cursor c=db.rawQuery("SELECT * FROM service_jobs WHERE date(created_at/1000,'unixepoch','localtime')=? ORDER BY created_at DESC,id DESC",new String[]{day});
        List<ServiceJob> out=new ArrayList<ServiceJob>();try{while(c.moveToNext())out.add(job(c));}finally{c.close();}return out;
    }
    public java.util.Set<String> entryDays(String month) {
        Cursor c=db.rawQuery("SELECT DISTINCT date(created_at/1000,'unixepoch','localtime') FROM service_jobs WHERE strftime('%Y-%m',created_at/1000,'unixepoch','localtime')=?",new String[]{month});
        java.util.Set<String> out=new java.util.HashSet<String>();try{while(c.moveToNext())out.add(c.getString(0));}finally{c.close();}return out;
    }

    public long saveEntry(ServiceJob entry,String title) {
        if(title.trim().isEmpty()||entry.device.trim().isEmpty()||Double.isNaN(entry.paid)||Double.isInfinite(entry.paid)||entry.paid<0||entry.paid>1000000000)throw new IllegalArgumentException("Invalid service record");
        long original=entry.id;db.beginTransaction();
        try {
            ServiceJob old=original==0?null:job(original);
            if(original!=0&&old==null)throw new IllegalStateException("Record missing");
            entry.paid=Math.round(entry.paid*100.0)/100.0;
            ContentValues v=new ContentValues();v.put("name_en",title.trim());v.put("name_ar",title.trim());
            v.put("customer",entry.customer.trim());v.put("phone",entry.phone.trim());v.put("device",entry.device.trim());v.put("paid",entry.paid);
            v.put("fee",old==null?entry.paid:Math.max(old.fee,entry.paid));v.put("updated_at",System.currentTimeMillis());
            if(old==null){entry.createdAt=System.currentTimeMillis();v.put("created_at",entry.createdAt);v.put("status","COLLECTED");entry.id=db.insertOrThrow("service_jobs",null,v);}
            else if(db.update("service_jobs",v,"id=?",new String[]{String.valueOf(original)})!=1)throw new IllegalStateException("Record missing");
            double delta=Math.round((entry.paid-(old==null?0:old.paid))*100.0)/100.0;
            if(delta!=0){ContentValues payment=new ContentValues();payment.put("job_id",entry.id);payment.put("amount",delta);payment.put("day",Db.today());payment.put("ts",System.currentTimeMillis());db.insertOrThrow("service_payments",null,payment);}
            db.setTransactionSuccessful();
        }catch(RuntimeException e){entry.id=original;throw e;}finally{db.endTransaction();}return entry.id;
    }
    public void setStatus(long id, String status) {
        boolean valid = false;
        for (String s : ServiceJob.STATUSES) if (s.equals(status)) valid = true;
        if (!valid) throw new IllegalArgumentException("Unknown status");
        ContentValues v = new ContentValues(); v.put("status", status); v.put("updated_at", System.currentTimeMillis());
        db.update("service_jobs", v, "id=?", new String[]{String.valueOf(id)});
    }
    public void setSteps(long id, long mask) {
        ContentValues v = new ContentValues(); v.put("checked_steps", mask); v.put("updated_at", System.currentTimeMillis());
        db.update("service_jobs", v, "id=?", new String[]{String.valueOf(id)});
    }
    public void recordDraftOpened(long id) {
        ContentValues v = new ContentValues(); v.put("message_opened_at", System.currentTimeMillis());
        db.update("service_jobs", v, "id=?", new String[]{String.valueOf(id)});
    }
    public void deleteJob(long id) { db.delete("service_jobs", "id=?", new String[]{String.valueOf(id)}); }
}
