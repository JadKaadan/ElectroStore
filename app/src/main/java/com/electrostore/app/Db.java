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


public class Db extends SQLiteOpenHelper {

    public static final String FILE = "store.db";
    private static final int VERSION = 6;
    private static Db sInstance;
    private final Context app;

    public static synchronized Db get(Context c) {
        if (sInstance == null) sInstance = new Db(c.getApplicationContext());
        return sInstance;
    }

    private Db(Context c) { super(c, FILE, null, VERSION);app=c.getApplicationContext(); }


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
                + "stock_deducted INTEGER NOT NULL DEFAULT 0,"
                + "order_id INTEGER NOT NULL DEFAULT 0,"
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
        db.execSQL("CREATE INDEX idx_sales_order ON sales(order_id)");
        MaintenanceStore.createTables(db);
        MaintenanceStore.createCustomers(db);
        MaintenanceStore.createPayments(db);
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
        if (oldV < 4) {


            db.execSQL("ALTER TABLE sales ADD COLUMN stock_deducted INTEGER NOT NULL DEFAULT -1");
            db.execSQL("ALTER TABLE sales ADD COLUMN order_id INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE sales SET order_id=COALESCE((SELECT id FROM orders"
                    + " WHERE receipt_no=sales.note AND receipt_no<>'' LIMIT 1),0)");
            db.execSQL("CREATE INDEX idx_sales_order ON sales(order_id)");
            db.execSQL("UPDATE products SET quantity=0 WHERE quantity<0");
            MaintenanceStore.createTables(db);
        }
        if (oldV < 5) MaintenanceStore.createCustomers(db);
        if (oldV < 6) MaintenanceStore.createPayments(db);
    }


    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String currentMonth() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
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
        return products(search, category, 0, 0);
    }


    public List<Product> products(String search, String category, int limit, int offset) {
        StringBuilder q = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<String> args = new ArrayList<String>();
        if (search != null && search.trim().length() > 0) {
            q.append(" AND (name LIKE ? OR category LIKE ? OR color LIKE ?"
                    + " OR description LIKE ? OR id=?)");
            String like = "%" + search.trim() + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
            args.add(String.valueOf(productIdAtNumber(search.trim())));
        }
        if (category != null && category.length() > 0) {
            q.append(" AND category=?");
            args.add(category);
        }
        q.append(" ORDER BY id DESC");
        if (limit > 0) {
            q.append(" LIMIT ? OFFSET ?");
            args.add(String.valueOf(limit));
            args.add(String.valueOf(Math.max(0, offset)));
        }

        List<Product> out = new ArrayList<Product>();
        Cursor c = getReadableDatabase().rawQuery(
                q.toString(), args.toArray(new String[args.size()]));
        try {
            while (c.moveToNext()) out.add(readProduct(c));
        } finally { c.close(); }
        return out;
    }


    public int productCount(String search, String category) {
        StringBuilder q = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        List<String> args = new ArrayList<String>();
        if (search != null && search.trim().length() > 0) {
            q.append(" AND (name LIKE ? OR category LIKE ? OR color LIKE ?"
                    + " OR description LIKE ? OR id=?)");
            String like = "%" + search.trim() + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
            args.add(String.valueOf(productIdAtNumber(search.trim())));
        }
        if (category != null && category.length() > 0) {
            q.append(" AND category=?");
            args.add(category);
        }
        Cursor c = getReadableDatabase().rawQuery(q.toString(),
                args.toArray(new String[args.size()]));
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    public int productNumber(long id) {
        Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products WHERE id<=? AND EXISTS(SELECT 1 FROM products WHERE id=?)",new String[]{String.valueOf(id),String.valueOf(id)});
        try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
    }
    private long productIdAtNumber(String input) {
        String value=input.startsWith("#")?input.substring(1):input;long number=0;
        if(value.isEmpty())return -1;
        for(char ch:value.toCharArray()){int digit=Character.digit(ch,10);if(digit<0)return -1;number=number*10+digit;if(number>Integer.MAX_VALUE)return -1;}
        if(number<1)return -1;
        Cursor c=getReadableDatabase().rawQuery("SELECT id FROM products ORDER BY id LIMIT 1 OFFSET ?",new String[]{String.valueOf(number-1)});
        try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}
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
        v.put("quantity", Math.max(0, p.quantity));
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
        Product before = product(productId);
        getWritableDatabase().execSQL(
                "UPDATE products SET quantity = MAX(0, quantity + ?) WHERE id=?",
                new Object[]{delta, productId});
        Product after = product(productId);
        StockAlerts.afterStockChange(app,before,after,delta);
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
        if (qty < 1) throw new IllegalArgumentException("Positive quantity required");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Product current = product(p.id);
            if (current == null) throw new IllegalStateException("Product deleted");
            Sale sale = recordSale(current, current.id, current.name, qty, unitPrice,
                    unitPrice * qty, note, 0);
            db.setTransactionSuccessful();
            return sale;
        } finally { db.endTransaction(); }
    }


    private Sale recordSale(Product p, long productId, String name, int qty,
                            double unitPrice, double total, String note, long orderId) {
        if (qty < 1) throw new IllegalArgumentException("Positive quantity required");
        Sale s = new Sale();
        s.productId = productId;
        s.productName = name == null || name.trim().isEmpty() ? "#" + productId : name.trim();
        s.qty = qty; s.unitPrice = unitPrice; s.costPrice = p == null ? 0 : p.costPrice;
        s.total = total; s.profit = s.costPrice > 0 ? (unitPrice - s.costPrice) * qty : 0;
        s.day = today(); s.ts = System.currentTimeMillis(); s.note = note == null ? "" : note;
        int deducted = p == null ? 0 : Math.min(Math.max(0, p.quantity), qty);
        ContentValues v = new ContentValues();
        v.put("product_id", s.productId); v.put("product_name", s.productName);
        v.put("qty", s.qty); v.put("unit_price", s.unitPrice); v.put("cost_price", s.costPrice);
        v.put("total", s.total); v.put("profit", s.profit); v.put("day", s.day);
        v.put("ts", s.ts); v.put("note", s.note); v.put("stock_deducted", deducted);
        v.put("order_id", orderId);
        s.id = getWritableDatabase().insertOrThrow("sales", null, v);
        if (deducted > 0) addStock(productId, -deducted);
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
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT product_id, qty, stock_deducted FROM sales WHERE id=?",
                    new String[]{String.valueOf(id)});
            long pid = 0; int restore = 0;
            try {
                if (c.moveToFirst()) {
                    pid = c.getLong(0);
                    restore = c.getInt(2) < 0 ? c.getInt(1) : c.getInt(2);
                }
            } finally { c.close(); }
            if (db.delete("sales", "id=?", new String[]{String.valueOf(id)}) == 1 && pid > 0)
                addStock(pid, Math.max(0, restore));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }


    public void bookOrderAsSales(Order input) {
        if (input == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Order o = order(input.id);
            if (o != null && !"DONE".equals(o.status)) {
                unbookOrder(o.receiptNo);
                for (OrderItem it : o.items) {
                    Product p = product(it.productId);
                    recordSale(p, it.productId, it.productName, it.qty, it.unitPrice,
                            it.lineTotal, o.receiptNo, o.id);
                }
                setOrderStatus(o.id, "DONE");
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }


    public void unbookOrder(String receiptNo) {
        if (receiptNo == null || receiptNo.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<Long> ids = new ArrayList<Long>();
            Cursor c = db.rawQuery("SELECT s.id FROM sales s JOIN orders o ON s.order_id=o.id"
                    + " WHERE o.receipt_no=?", new String[]{receiptNo});
            try { while (c.moveToNext()) ids.add(c.getLong(0)); } finally { c.close(); }
            for (Long id : ids) deleteSale(id);
            ContentValues v = new ContentValues(); v.put("status", "OPEN");
            db.update("orders", v, "receipt_no=?", new String[]{receiptNo});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static final String REVENUE_SOURCE="(SELECT day,total,profit,qty,1 AS sale_count,0 AS service_amount FROM sales UNION ALL SELECT day,amount,0,0,0,amount FROM service_payments)";

    public List<DayRow> revenueHistory(int limitDays) {
        List<DayRow> out = new ArrayList<DayRow>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT day, SUM(total) rev, SUM(profit) pr, SUM(qty) items, SUM(sale_count) n, SUM(service_amount) service_paid"
                + " FROM "+REVENUE_SOURCE+" GROUP BY day ORDER BY day DESC LIMIT ?",
                new String[]{String.valueOf(limitDays)});
        try {
            while (c.moveToNext()) {
                DayRow r = new DayRow();
                r.day = c.getString(0);
                r.revenue = c.getDouble(1);
                r.profit = c.getDouble(2);
                r.items = c.getInt(3);
                r.sales = c.getInt(4);
                r.servicePaid = c.getDouble(5);
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
                + " COALESCE(SUM(qty),0), COALESCE(SUM(sale_count),0), COALESCE(SUM(service_amount),0) FROM "+REVENUE_SOURCE+" WHERE day=?",
                new String[]{day});
        try {
            if (c.moveToFirst()) {
                r.revenue = c.getDouble(0);
                r.profit = c.getDouble(1);
                r.items = c.getInt(2);
                r.sales = c.getInt(3);
                r.servicePaid = c.getDouble(4);
            }
        } finally { c.close(); }
        return r;
    }


    public double revenueSince(int days) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0) FROM "+REVENUE_SOURCE+" WHERE day >= date('now','localtime','-"
                        + (days - 1) + " day')", null);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }


    public double revenueAllTime() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0) FROM "+REVENUE_SOURCE, null);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }


    public List<ProductProfitRow> productProfitRows(String day) {
        return productProfitRows(day, null);
    }


    public List<ProductProfitRow> productProfitRows(String day, String monthPrefix) {
        List<ProductProfitRow> out = new ArrayList<ProductProfitRow>();
        String where = "";
        String[] args = null;
        if (day != null && day.length() > 0) {
            where = " WHERE day=?";
            args = new String[]{day};
        } else if (monthPrefix != null && monthPrefix.length() > 0) {
            where = " WHERE day LIKE ?";
            args = new String[]{monthPrefix + "%"};
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT product_name,COALESCE(SUM(qty),0),"
                + "COALESCE(SUM(cost_price*qty),0),COALESCE(SUM(total),0),"
                + "COALESCE(SUM(CASE WHEN cost_price>0"
                + " THEN total-(cost_price*qty) ELSE 0 END),0),"
                + "COALESCE(SUM(CASE WHEN cost_price<=0 THEN qty ELSE 0 END),0)"
                + " FROM sales" + where
                + " GROUP BY product_id,product_name"
                + " ORDER BY SUM(total) DESC,product_name COLLATE NOCASE", args);
        try {
            while (c.moveToNext()) {
                ProductProfitRow r = new ProductProfitRow();
                r.productName = c.isNull(0) || c.getString(0).trim().length() == 0
                        ? app.getString(R.string.unnamed_product) : c.getString(0);
                r.quantity = c.getInt(1);
                r.wholesaleCost = c.getDouble(2);
                r.soldFor = c.getDouble(3);
                r.profit = c.getDouble(4);
                r.missingCostQuantity = c.getInt(5);
                out.add(r);
            }
        } finally { c.close(); }
        return out;
    }


    public double servicePaymentsTotal(String fromDay, String untilDay) {
        String where = fromDay == null ? "" : " WHERE day>=? AND day<?";
        String[] args = fromDay == null ? null : new String[]{fromDay, untilDay};
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount),0) FROM service_payments" + where, args);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; }
        finally { c.close(); }
    }


    public double[] servicePaymentsYear(int year) {
        double[] months = new double[12];
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT substr(day,6,2) AS month_no,COALESCE(SUM(amount),0)"
                + " FROM service_payments WHERE day>=? AND day<? GROUP BY month_no",
                new String[]{ProfitDates.yearStart(year), ProfitDates.yearStart(year + 1)});
        try {
            while (c.moveToNext()) {
                String month = c.getString(0);
                if (month == null || !month.matches("(0[1-9]|1[0-2])")) continue;
                months[Integer.parseInt(month) - 1] = c.getDouble(1);
            }
        } finally { c.close(); }
        return months;
    }


    public ProfitTotals productProfitTotals(String fromDay, String untilDay) {
        String where = fromDay == null ? "" : " WHERE day>=? AND day<?";
        String[] args = fromDay == null ? null : new String[]{fromDay, untilDay};
        ProfitTotals result = new ProfitTotals();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT total,cost_price,qty FROM sales" + where, args);
        try {
            while (c.moveToNext()) result.addSale(c.getString(0), c.getString(1), c.getInt(2));
        } finally { c.close(); }
        return result;
    }


    public ProfitTotals[] productProfitYear(int year) {
        ProfitTotals[] months = new ProfitTotals[12];
        for (int i = 0; i < months.length; i++) months[i] = new ProfitTotals();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT day,total,cost_price,qty FROM sales WHERE day>=? AND day<?",
                new String[]{ProfitDates.yearStart(year), ProfitDates.yearStart(year + 1)});
        try {
            while (c.moveToNext()) {
                String day = c.getString(0);
                if (day == null || !day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) continue;
                int month = Integer.parseInt(day.substring(5, 7)) - 1;
                if (month >= 0 && month < 12)
                    months[month].addSale(c.getString(1), c.getString(2), c.getInt(3));
            }
        } finally { c.close(); }
        return months;
    }

    public String firstProductSaleDay() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT MIN(day) FROM sales WHERE day GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'", null);
        try { return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : ""; }
        finally { c.close(); }
    }

    private static final String MONTH_SOURCE = "(SELECT substr(day,1,7) month,"
            + "total revenue,total product_sold,cost_price*qty wholesale_cost,"
            + "CASE WHEN cost_price>0 THEN total-(cost_price*qty) ELSE 0 END product_profit,"
            + "0 service_paid,qty items,CASE WHEN cost_price<=0 THEN qty ELSE 0 END missing_cost"
            + " FROM sales UNION ALL SELECT substr(day,1,7),amount,0,0,0,amount,0,0"
            + " FROM service_payments)";


    public MonthRow monthTotals(String month) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(revenue),0),COALESCE(SUM(product_sold),0),"
                + "COALESCE(SUM(wholesale_cost),0),COALESCE(SUM(product_profit),0),"
                + "COALESCE(SUM(service_paid),0),COALESCE(SUM(items),0),"
                + "COALESCE(SUM(missing_cost),0) FROM " + MONTH_SOURCE + " WHERE month=?",
                new String[]{month});
        MonthRow r = new MonthRow();
        r.month = month;
        try {
            if (c.moveToFirst()) {
                r.revenue = c.getDouble(0);
                r.productSold = c.getDouble(1);
                r.wholesaleCost = c.getDouble(2);
                r.productProfit = c.getDouble(3);
                r.servicePaid = c.getDouble(4);
                r.items = c.getInt(5);
                r.missingCostQuantity = c.getInt(6);
            }
        } finally { c.close(); }
        return r;
    }


    public List<MonthRow> previousMonths() {
        List<MonthRow> out = new ArrayList<MonthRow>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT month,SUM(revenue),SUM(product_sold),SUM(wholesale_cost),"
                + "SUM(product_profit),SUM(service_paid),SUM(items),SUM(missing_cost)"
                + " FROM " + MONTH_SOURCE
                + " WHERE month<>'' AND month<? GROUP BY month ORDER BY month DESC",
                new String[]{currentMonth()});
        try {
            while (c.moveToNext()) {
                MonthRow r = new MonthRow();
                r.month = c.getString(0);
                r.revenue = c.getDouble(1);
                r.productSold = c.getDouble(2);
                r.wholesaleCost = c.getDouble(3);
                r.productProfit = c.getDouble(4);
                r.servicePaid = c.getDouble(5);
                r.items = c.getInt(6);
                r.missingCostQuantity = c.getInt(7);
                out.add(r);
            }
        } finally { c.close(); }
        return out;
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
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
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

        if (o.id > 0) {
            db.update("orders", v, "id=?", new String[]{String.valueOf(o.id)});
            db.delete("order_items", "order_id=?", new String[]{String.valueOf(o.id)});
        } else {
            o.id = db.insertOrThrow("orders", null, v);
        }
        for (OrderItem it : o.items) {
            ContentValues iv = new ContentValues();
            iv.put("order_id", o.id);
            iv.put("product_id", it.productId);
            iv.put("product_name", it.productName);
            iv.put("qty", it.qty);
            iv.put("unit_price", it.unitPrice);
            iv.put("line_total", it.lineTotal);
            db.insertOrThrow("order_items", null, iv);
        }
        db.setTransactionSuccessful();
        return o.id;
        } finally { db.endTransaction(); }
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
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Order o = order(id);
            if (o != null) unbookOrder(o.receiptNo);
            db.delete("order_items", "order_id=?", new String[]{String.valueOf(id)});
            db.delete("orders", "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
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
                + " UNION SELECT DISTINCT day FROM orders WHERE day LIKE ?"
                + " UNION SELECT DISTINCT day FROM service_payments WHERE day LIKE ?",
                new String[]{like, like, like});
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
