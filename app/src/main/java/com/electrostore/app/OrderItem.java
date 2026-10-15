package com.electrostore.app;

public class OrderItem {
    public long id;
    public long orderId;
    public long productId;
    public String productName = "";
    public int qty = 1;
    public double unitPrice = 0;
    public double lineTotal = 0;
}
