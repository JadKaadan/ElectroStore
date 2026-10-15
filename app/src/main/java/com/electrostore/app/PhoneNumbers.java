package com.electrostore.app;


public final class PhoneNumbers {
    private static String clean(String raw) {
        StringBuilder out = new StringBuilder();
        for (char c : raw.trim().toCharArray()) {
            int n = Character.digit(c, 10);
            if (n >= 0) out.append(n);
            else if (c == '+' && out.length() == 0) out.append(c);
            else if (!Character.isWhitespace(c) && c != '(' && c != ')' && c != '-' && c != '.') return "";
        }
        return out.toString();
    }
    public static String normalize(String raw, String country) {
        if (raw == null || country == null) return "";
        String n = clean(raw), cc = clean(country).replace("+", "");
        if (n.startsWith("+")) n = n.substring(1);
        else if (n.startsWith("00")) n = n.substring(2);
        else {
            if (!cc.matches("[1-9][0-9]{0,2}")) return "";

            if ("961".equals(cc) && n.startsWith("0")) n = n.substring(1);

            if (!("961".equals(cc) && n.startsWith("961") && (n.length()==10 || n.length()==11)))
                n = cc + n;
        }
        if (n.startsWith("9610") && n.length()==11) n="961"+n.substring(4);
        return n.matches("[1-9][0-9]{6,14}") ? n : "";
    }
}
