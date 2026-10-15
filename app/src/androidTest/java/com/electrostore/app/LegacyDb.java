
package com.electrostore.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class LegacyDb extends SQLiteOpenHelper {

    public static final String FILE = "store.db";
    private static final int VERSION = 3;
    private static LegacyDb sInstance;

    public static synchronized LegacyDb get(Context c) {
        if (sInstance == null) sInstance = new LegacyDb(c.getApplicationContext());
        return sInstance;
    }

    private LegacyDb(Context c) { super(c, FILE, null, VERSION); }


    public static synchronized void reset() {
        if (sInstance != null) {
            try { sInstance.close(); } catch (Throwable ignored) { }
            sInstance = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT DEFAULT '',"
                + "category TEXT DEFAULT '',"
                + "color TEXT DEFAULT '',"
                + "description TEXT DEFAULT '',"
                + "cost_price REAL DEFAULT 0,"
                + "unit_price REAL DEFAULT 0,"
                + "retail_price REAL DEFAULT 0,"
                + "quantity INTEGER DEFAULT 0,"
                + "image_path TEXT DEFAULT '',"
                + "created_at INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE sales ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "product_id INTEGER DEFAULT 0,"
                + "product_name TEXT DEFAULT '',"
                + "qty INTEGER DEFAULT 1,"
                + "unit_price REAL DEFAULT 0,"
                + "cost_price REAL DEFAULT 0,"
                + "total REAL DEFAULT 0,"
                + "profit REAL DEFAULT 0,"
                + "day TEXT DEFAULT '',"
                + "ts INTEGER DEFAULT 0,"
                + "note TEXT DEFAULT '')");

        db.execSQL("CREATE TABLE orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "receipt_no TEXT DEFAULT '',"
                + "customer TEXT DEFAULT '',"
                + "phone TEXT DEFAULT '',"
                + "total REAL DEFAULT 0,"
                + "paid REAL DEFAULT 0,"
                + "status TEXT DEFAULT 'OPEN',"
                + "day TEXT DEFAULT '',"
                + "ts INTEGER DEFAULT 0,"
                + "note TEXT DEFAULT '')");

        db.execSQL("CREATE TABLE order_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_id INTEGER DEFAULT 0,"
                + "product_id INTEGER DEFAULT 0,"
                + "product_name TEXT DEFAULT '',"
                + "qty INTEGER DEFAULT 1,"
                + "unit_price REAL DEFAULT 0,"
                + "line_total REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE settings (k TEXT PRIMARY KEY, v TEXT DEFAULT '')");

        db.execSQL("CREATE INDEX idx_sales_day ON sales(day)");
        db.execSQL("CREATE INDEX idx_orders_day ON orders(day)");
        db.execSQL("CREATE INDEX idx_items_order ON order_items(order_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS categories ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE)");
            db.execSQL("INSERT OR IGNORE INTO categories(name) "
                    + "SELECT DISTINCT TRIM(category) FROM products "
                    + "WHERE category IS NOT NULL AND TRIM(category)<>''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_day ON orders(day)");
        }
        if (oldV < 3) {


            db.execSQL("UPDATE products SET cost_price = retail_price"
                    + " WHERE (cost_price IS NULL OR cost_price = 0) AND retail_price > 0");
        }
    }


    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String s(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return "";
        return c.getString(i);
    }

    private static double d(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return 0;
        return c.getDouble(i);
    }

    private static long l(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return 0;
        return c.getLong(i);
    }

    private static int i(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return 0;
        return c.getInt(i);
    }


    public String setting(String key, String fallback) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT v FROM settings WHERE k=?", new String[]{key});
        try {
            if (c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } finally { c.close(); }
        return fallback;
    }

    public void setSetting(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("k", key);
        v.put("v", value == null ? "" : value);
        getWritableDatabase().insertWithOnConflict(
                "settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }


    private Product readProduct(Cursor c) {
        Product p = new Product();
        p.id = l(c, "id");
        p.name = s(c, "name");
        p.category = s(c, "category");
        p.color = s(c, "color");
        p.description = s(c, "description");
        p.costPrice = d(c, "cost_price");
        p.unitPrice = d(c, "unit_price");
        p.retailPrice = d(c, "retail_price");
        p.quantity = i(c, "quantity");
        p.imagePath = s(c, "image_path");
        p.createdAt = l(c, "created_at");
        return p;
    }


    public List<Product> products(String search, String category) {
        StringBuilder q = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<String> args = new ArrayList<String>();
        if (search != null && search.trim().length() > 0) {
            q.append(" AND (name LIKE ? OR category LIKE ? OR color LIKE ?"
                    + " OR description LIKE ? OR CAST(id AS TEXT)=?)");
            String like = "%" + search.trim() + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
            args.add(search.trim());
        }
        if (category != null && category.length() > 0) {
            q.append(" AND category=?");
            args.add(category);
        }
        q.append(" ORDER BY id DESC");

        List<Product> out = new ArrayList<Product>();
        Cursor c = getReadableDatabase().rawQuery(
                q.toString(), args.toArray(new String[args.size()]));
        try {
            while (c.moveToNext()) out.add(readProduct(c));
        } finally { c.close(); }
        return out;
    }

    public Product product(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM products WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return readProduct(c);
        } finally { c.close(); }
        return null;
    }

    public List<String> categories() {
        List<String> out = new ArrayList<String>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name FROM categories ORDER BY name COLLATE NOCASE", null);
        try {
            while (c.moveToNext()) out.add(c.getString(0));
        } finally { c.close(); }
        return out;
    }

    public boolean categoryExists(String name) {
        if (name == null) return false;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM categories WHERE name=? COLLATE NOCASE LIMIT 1",
                new String[]{name.trim()});
        try { return c.moveToFirst(); } finally { c.close(); }
    }


    public void renameCategory(String from, String to) {
        if (from == null || to == null) return;
        String f = from.trim(), t = to.trim();
        if (f.length() == 0 || t.length() == 0 || f.equals(t)) return;

        SQLiteDatabase db = getWritableDatabase();
        if (categoryExists(t)) {
            db.delete("categories", "name=?", new String[]{f});
        } else {
            ContentValues v = new ContentValues();
            v.put("name", t);
            db.update("categories", v, "name=?", new String[]{f});
        }
        db.execSQL("UPDATE products SET category=? WHERE TRIM(category)=?", new Object[]{t, f});
    }


    public int productsInCategory(String name) {
        if (name == null) return 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM products WHERE TRIM(category)=?",
                new String[]{name.trim()});
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }


    public void deleteCategory(String name) {
        if (name == null || name.trim().length() == 0) return;
        String clean = name.trim();
        getWritableDatabase().delete("categories", "name=?", new String[]{clean});
        getWritableDatabase().execSQL(
                "UPDATE products SET category='' WHERE TRIM(category)=?", new Object[]{clean});
    }

    public void addCategory(String name) {
        if (name == null) return;
        String clean = name.trim();
        if (clean.length() == 0) return;
        ContentValues v = new ContentValues();
        v.put("name", clean);
        getWritableDatabase().insertWithOnConflict(
                "categories", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }


    public long saveProduct(Product p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name == null ? "" : p.name.trim());
        v.put("category", p.category == null ? "" : p.category.trim());
        addCategory(p.category);
        v.put("color", p.color == null ? "" : p.color.trim());
        v.put("description", p.description == null ? "" : p.description.trim());
        v.put("cost_price", p.costPrice);
        v.put("unit_price", p.unitPrice);
        v.put("retail_price", p.retailPrice);
        v.put("quantity", p.quantity);
        v.put("image_path", p.imagePath == null ? "" : p.imagePath);

        SQLiteDatabase db = getWritableDatabase();
        if (p.id > 0) {
            db.update("products", v, "id=?", new String[]{String.valueOf(p.id)});
            return p.id;
        }
        v.put("created_at", System.currentTimeMillis());
        p.id = db.insert("products", null, v);
        return p.id;
    }

    public void deleteProduct(long id) {
        getWritableDatabase().delete("products", "id=?", new String[]{String.valueOf(id)});
    }

    public void addStock(long productId, int delta) {
        getWritableDatabase().execSQL(
                "UPDATE products SET quantity = MAX(0, quantity + ?) WHERE id=?",
                new Object[]{delta, productId});
    }

    public int productCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    public int stockCount() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(quantity),0) FROM products", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }


    public Sale sell(Product p, int qty, double unitPrice, String note) {
        if (qty < 1) qty = 1;
        Sale s = new Sale();
        s.productId = p.id;
        s.productName = (p.name != null && p.name.trim().length() > 0)
                ? p.name.trim() : "#" + p.id;
        s.qty = qty;
        s.unitPrice = unitPrice;
        s.costPrice = p.costPrice;
        s.total = unitPrice * qty;
        s.profit = p.costPrice > 0 ? (unitPrice - p.costPrice) * qty : 0;
        s.day = today();
        s.ts = System.currentTimeMillis();
        s.note = note == null ? "" : note;

        ContentValues v = new ContentValues();
        v.put("product_id", s.productId);
        v.put("product_name", s.productName);
        v.put("qty", s.qty);
        v.put("unit_price", s.unitPrice);
        v.put("cost_price", s.costPrice);
        v.put("total", s.total);
        v.put("profit", s.profit);
        v.put("day", s.day);
        v.put("ts", s.ts);
        v.put("note", s.note);
        s.id = getWritableDatabase().insert("sales", null, v);

        if (p.id > 0) addStock(p.id, -qty);
        return s;
    }

    private Sale readSale(Cursor c) {
        Sale s = new Sale();
        s.id = l(c, "id");
        s.productId = l(c, "product_id");
        s.productName = s(c, "product_name");
        s.qty = i(c, "qty");
        s.unitPrice = d(c, "unit_price");
        s.costPrice = d(c, "cost_price");
        s.total = d(c, "total");
        s.profit = d(c, "profit");
        s.day = s(c, "day");
        s.ts = l(c, "ts");
        s.note = s(c, "note");
        return s;
    }

    public List<Sale> salesOn(String day) {
        List<Sale> out = new ArrayList<Sale>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM sales WHERE day=? ORDER BY ts DESC", new String[]{day});
        try {
            while (c.moveToNext()) out.add(readSale(c));
        } finally { c.close(); }
        return out;
    }

    public void deleteSale(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT product_id, qty FROM sales WHERE id=?",
                new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                long pid = c.getLong(0);
                int qty = c.getInt(1);
                if (pid > 0) addStock(pid, qty);
            }
        } finally { c.close(); }
        getWritableDatabase().delete("sales", "id=?", new String[]{String.valueOf(id)});
    }


    public void bookOrderAsSales(Order o) {
        if (o == null) return;
        unbookOrder(o.receiptNo);
        for (OrderItem it : o.items) {
            Product p = product(it.productId);
            Sale s = new Sale();
            s.productId = it.productId;
            s.productName = it.productName;
            s.qty = it.qty;
            s.unitPrice = it.unitPrice;
            s.costPrice = p == null ? 0 : p.costPrice;
            s.total = it.lineTotal;
            s.profit = s.costPrice > 0 ? (it.unitPrice - s.costPrice) * it.qty : 0;
            s.day = today();
            s.ts = System.currentTimeMillis();
            s.note = o.receiptNo;

            ContentValues v = new ContentValues();
            v.put("product_id", s.productId);
            v.put("product_name", s.productName);
            v.put("qty", s.qty);
            v.put("unit_price", s.unitPrice);
            v.put("cost_price", s.costPrice);
            v.put("total", s.total);
            v.put("profit", s.profit);
            v.put("day", s.day);
            v.put("ts", s.ts);
            v.put("note", s.note);
            getWritableDatabase().insert("sales", null, v);

            if (it.productId > 0) addStock(it.productId, -it.qty);
        }
    }


    public void unbookOrder(String receiptNo) {
        if (receiptNo == null || receiptNo.length() == 0) return;
        List<Long> ids = new ArrayList<Long>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM sales WHERE note=?", new String[]{receiptNo});
        try {
            while (c.moveToNext()) ids.add(c.getLong(0));
        } finally { c.close(); }
        for (Long id : ids) deleteSale(id);
    }


    public List<DayRow> revenueHistory(int limitDays) {
        List<DayRow> out = new ArrayList<DayRow>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, SUM(total) rev, SUM(profit) pr, SUM(qty) items, COUNT(*) n"
                + " FROM sales GROUP BY day ORDER BY day DESC LIMIT ?",
                new String[]{String.valueOf(limitDays)});
        try {
            while (c.moveToNext()) {
                DayRow r = new DayRow();
                r.day = c.getString(0);
                r.revenue = c.getDouble(1);
                r.profit = c.getDouble(2);
                r.items = c.getInt(3);
                r.sales = c.getInt(4);
                out.add(r);
            }
        } finally { c.close(); }
        return out;
    }

    public DayRow dayTotals(String day) {
        DayRow r = new DayRow();
        r.day = day;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0), COALESCE(SUM(profit),0),"
                + " COALESCE(SUM(qty),0), COUNT(*) FROM sales WHERE day=?",
                new String[]{day});
        try {
            if (c.moveToFirst()) {
                r.revenue = c.getDouble(0);
                r.profit = c.getDouble(1);
                r.items = c.getInt(2);
                r.sales = c.getInt(3);
            }
        } finally { c.close(); }
        return r;
    }


    public double revenueSince(int days) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0) FROM sales WHERE day >= date('now','localtime','-"
                        + (days - 1) + " day')", null);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }


    public String nextReceiptNo() {
        long n;
        try {
            n = Long.parseLong(setting("receipt_seq", "0")) + 1;
        } catch (Exception e) {
            n = 1;
        }
        setSetting("receipt_seq", String.valueOf(n));
        String stamp = new SimpleDateFormat("yyMMdd", Locale.US).format(new Date());
        return String.format(Locale.US, "R-%s-%04d", stamp, n);
    }

    public long saveOrder(Order o) {
        ContentValues v = new ContentValues();
        v.put("receipt_no", o.receiptNo);
        v.put("customer", o.customer == null ? "" : o.customer.trim());
        v.put("phone", o.phone == null ? "" : o.phone.trim());
        v.put("total", o.total);
        v.put("paid", o.paid);
        v.put("status", o.status);
        v.put("day", o.day);
        v.put("ts", o.ts);
        v.put("note", o.note == null ? "" : o.note);

        SQLiteDatabase db = getWritableDatabase();
        if (o.id > 0) {
            db.update("orders", v, "id=?", new String[]{String.valueOf(o.id)});
            db.delete("order_items", "order_id=?", new String[]{String.valueOf(o.id)});
        } else {
            o.id = db.insert("orders", null, v);
        }
        for (OrderItem it : o.items) {
            ContentValues iv = new ContentValues();
            iv.put("order_id", o.id);
            iv.put("product_id", it.productId);
            iv.put("product_name", it.productName);
            iv.put("qty", it.qty);
            iv.put("unit_price", it.unitPrice);
            iv.put("line_total", it.lineTotal);
            db.insert("order_items", null, iv);
        }
        return o.id;
    }

    private Order readOrder(Cursor c) {
        Order o = new Order();
        o.id = l(c, "id");
        o.receiptNo = s(c, "receipt_no");
        o.customer = s(c, "customer");
        o.phone = s(c, "phone");
        o.total = d(c, "total");
        o.paid = d(c, "paid");
        o.status = s(c, "status");
        o.day = s(c, "day");
        o.ts = l(c, "ts");
        o.note = s(c, "note");
        return o;
    }

    public List<Order> orders(String statusFilter) {
        return orders(statusFilter, null);
    }


    public List<Order> orders(String statusFilter, String dayFilter) {
        List<Order> out = new ArrayList<Order>();
        StringBuilder q = new StringBuilder("SELECT * FROM orders WHERE 1=1");
        List<String> args = new ArrayList<String>();
        if (statusFilter != null && statusFilter.length() > 0) {
            q.append(" AND status=?");
            args.add(statusFilter);
        }
        if (dayFilter != null && dayFilter.length() > 0) {
            q.append(" AND day=?");
            args.add(dayFilter);
        }
        q.append(" ORDER BY ts DESC, id DESC");
        Cursor c = getReadableDatabase().rawQuery(
                q.toString(), args.toArray(new String[args.size()]));
        try {
            while (c.moveToNext()) out.add(readOrder(c));
        } finally { c.close(); }
        return out;
    }

    public Order order(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM orders WHERE id=?", new String[]{String.valueOf(id)});
        Order o = null;
        try { if (c.moveToFirst()) o = readOrder(c); } finally { c.close(); }
        if (o == null) return null;
        o.items = orderItems(id);
        return o;
    }

    public List<OrderItem> orderItems(long orderId) {
        List<OrderItem> out = new ArrayList<OrderItem>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM order_items WHERE order_id=? ORDER BY id",
                new String[]{String.valueOf(orderId)});
        try {
            while (c.moveToNext()) {
                OrderItem it = new OrderItem();
                it.id = l(c, "id");
                it.orderId = l(c, "order_id");
                it.productId = l(c, "product_id");
                it.productName = s(c, "product_name");
                it.qty = i(c, "qty");
                it.unitPrice = d(c, "unit_price");
                it.lineTotal = d(c, "line_total");
                out.add(it);
            }
        } finally { c.close(); }
        return out;
    }

    public void setOrderStatus(long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("orders", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteOrder(long id) {
        getWritableDatabase().delete("order_items", "order_id=?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete("orders", "id=?", new String[]{String.valueOf(id)});
    }

    public int openOrderCount() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM orders WHERE status='OPEN'", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }


    public java.util.Set<String> activeDays(String monthPrefix) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        String like = monthPrefix + "%";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT day FROM sales WHERE day LIKE ?"
                + " UNION SELECT DISTINCT day FROM orders WHERE day LIKE ?",
                new String[]{like, like});
        try {
            while (c.moveToNext()) {
                if (!c.isNull(0)) out.add(c.getString(0));
            }
        } finally { c.close(); }
        return out;
    }


    public double stockSellingValue() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN unit_price > 0 THEN unit_price"
                + " ELSE cost_price END * quantity),0) FROM products", null);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }

    public double stockValue() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(cost_price * quantity),0) FROM products", null);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }
}
