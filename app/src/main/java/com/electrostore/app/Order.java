package com.electrostore.app;

import java.util.ArrayList;
import java.util.List;

public class Order {
    public long id;
    public String receiptNo = "";
    public String customer = "";
    public String phone = "";
    public double total = 0;
    public double paid = 0;
    public String status = "OPEN";
    public String day = "";
    public long ts = 0;
    public String note = "";
    public List<OrderItem> items = new ArrayList<OrderItem>();

    public double balance() { return total - paid; }
}
