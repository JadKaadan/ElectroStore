package com.electrostore.app;


public class Product {
    public long id = 0;
    public int displayNumber(android.content.Context c) { return Db.get(c).productNumber(id); }
    public String name = "";
    public String category = "";
    public String color = "";
    public String description = "";

    public double costPrice = 0;

    public double unitPrice = 0;

    public double retailPrice = 0;
    public int quantity = 0;
    public String imagePath = "";
    public long createdAt = 0;


    public String displayName(android.content.Context c) {
        if (name == null || name.trim().length() == 0) {
            return c.getString(R.string.product_number, displayNumber(c));
        }
        return name;
    }


    public double bestPrice() {
        if (unitPrice > 0) return unitPrice;
        return costPrice;
    }

    public double profitPerUnit() {
        return costPrice > 0 ? bestPrice() - costPrice : 0;
    }
}
