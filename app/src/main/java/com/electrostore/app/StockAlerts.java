package com.electrostore.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;


final class StockAlerts {
    private static final String CHANNEL = "low_stock";
    private static final int REQUEST = 8842;

    static void createChannel(Context c) {
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL,c.getString(R.string.stock_channel),NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(c.getString(R.string.stock_channel_description));
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    static void requestPermission(Activity activity) {
        if(Build.VERSION.SDK_INT>=33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST);
    }

    static void afterStockChange(Context c, Product before, Product after, int delta) {
        if(before==null||after==null||delta>=0)return;
        int limit;
        try{limit=Integer.parseInt(Db.get(c).setting("low_stock","3"));}catch(Exception e){limit=3;}
        limit=Math.max(0,limit);
        if(before.quantity<=limit||after.quantity>limit)return;
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        Intent open=new Intent(c,ProductDetailActivity.class);open.putExtra("id",after.id);
        PendingIntent pending=PendingIntent.getActivity(c,(int)(after.id&0x7fffffff),open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        builder.setSmallIcon(R.drawable.ic_warn).setContentTitle(c.getString(R.string.stock_notification_title))
                .setContentText(c.getString(R.string.stock_notification_body,after.displayName(c),after.quantity))
                .setAutoCancel(true).setContentIntent(pending);
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(12000+(int)(after.id%10000),builder.build());
    }
}
