package com.electrostore.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BtPrinter {


    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String KEY_MAC = "bt_printer_mac";
    public static final String KEY_NAME = "bt_printer_name";
    public static final String KEY_WIDTH = "paper_width";

    public static final int REQ_PERMISSION = 8801;

    public interface Result {
        void done(boolean ok, String message);
    }


    public static String savedMac(Context c) { return Db.get(c).setting(KEY_MAC, ""); }

    public static String savedName(Context c) { return Db.get(c).setting(KEY_NAME, ""); }

    public static boolean isConfigured(Context c) { return savedMac(c).length() > 0; }

    public static void save(Context c, String mac, String name) {
        Db.get(c).setSetting(KEY_MAC, mac == null ? "" : mac);
        Db.get(c).setSetting(KEY_NAME, name == null ? "" : name);
    }

    public static void forget(Context c) { save(c, "", ""); }


    public static int dotsWide(Context c) {
        return "80".equals(Db.get(c).setting(KEY_WIDTH, "58")) ? 576 : 384;
    }

    public static int paperMm(Context c) {
        return "80".equals(Db.get(c).setting(KEY_WIDTH, "58")) ? 80 : 58;
    }

    public static void setPaperMm(Context c, int mm) {
        Db.get(c).setSetting(KEY_WIDTH, mm == 80 ? "80" : "58");
    }


    public static boolean hasPermission(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void askPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            a.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQ_PERMISSION);
        }
    }


    public static class Item {
        public final String name, mac;
        Item(String n, String m) { name = n; mac = m; }
    }

    private static BluetoothAdapter adapter(Context c) {
        try {
            BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bm != null && bm.getAdapter() != null) return bm.getAdapter();
        } catch (Throwable ignored) { }
        try {
            return BluetoothAdapter.getDefaultAdapter();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean bluetoothOn(Context c) {
        BluetoothAdapter a = adapter(c);
        try {
            return a != null && a.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }


    public static List<Item> paired(Context c) {
        List<Item> out = new ArrayList<Item>();
        BluetoothAdapter a = adapter(c);
        if (a == null || !hasPermission(c)) return out;
        try {
            Set<BluetoothDevice> set = a.getBondedDevices();
            if (set == null) return out;
            for (BluetoothDevice d : set) {
                String name = d.getName();
                out.add(new Item(name == null || name.length() == 0 ? d.getAddress() : name,
                        d.getAddress()));
            }
        } catch (Throwable ignored) { }
        return out;
    }


    public static void send(final Context c, final byte[] payload, final Result cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final String mac = savedMac(c);

        new Thread(new Runnable() {
            public void run() {
                String error = null;
                BluetoothSocket socket = null;
                try {
                    if (mac.length() == 0) throw new IllegalStateException("no printer chosen");
                    if (!hasPermission(c)) throw new SecurityException("no bluetooth permission");
                    BluetoothAdapter a = adapter(c);
                    if (a == null || !a.isEnabled()) throw new IllegalStateException("bluetooth off");

                    BluetoothDevice dev = a.getRemoteDevice(mac);
                    socket = open(dev);

                    OutputStream out = socket.getOutputStream();


                    int chunk = 512;
                    for (int i = 0; i < payload.length; i += chunk) {
                        int n = Math.min(chunk, payload.length - i);
                        out.write(payload, i, n);
                        out.flush();
                        try { Thread.sleep(24); } catch (InterruptedException ignored) { }
                    }
                    try { Thread.sleep(260); } catch (InterruptedException ignored) { }
                } catch (Throwable t) {
                    error = t.getMessage() == null ? t.toString() : t.getMessage();
                } finally {
                    if (socket != null) {
                        try { socket.close(); } catch (Throwable ignored) { }
                    }
                }

                final String err = error;
                ui.post(new Runnable() {
                    public void run() {
                        if (cb != null) cb.done(err == null, err);
                    }
                });
            }
        }).start();
    }


    private static BluetoothSocket open(BluetoothDevice dev) throws Exception {
        Exception first;
        try {
            BluetoothSocket s = dev.createRfcommSocketToServiceRecord(SPP);
            s.connect();
            return s;
        } catch (Exception e) {
            first = e;
        }
        try {
            Method m = dev.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            BluetoothSocket s = (BluetoothSocket) m.invoke(dev, Integer.valueOf(1));
            s.connect();
            return s;
        } catch (Exception e) {
            throw first;
        }
    }
}
