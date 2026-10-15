package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;


final class PickupMessage {
    static String text(Context c, ServiceJob j, String lang) {
        Context local=ServiceCatalog.locale(c,lang);
        String shop=Db.get(c).setting("shop_name","").trim();
        if(shop.isEmpty())shop=c.getString(R.string.app_name);
        return local.getString(R.string.job_pickup_template,j.customer,j.device,
                "ar".equals(lang)?j.nameAr:j.nameEn,shop,j.reference())+"\n\n"+local.getString(R.string.record_amounts,Money.usd(j.fee),Money.usd(j.paid),Money.usd(j.balance()));
    }
    static AlertDialog show(Activity a, ServiceJob j, Runnable opened) {
        LinearLayout body=ServiceUi.column(a);ServiceUi.pad(body,20);
        ServiceUi.body(body,a.getString(R.string.job_message_to,"+"+j.phone));
        ServiceUi.gap(body,8);
        EditText message=new EditText(a);message.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        message.setTextSize(16);message.setTextColor(Ui.color(a,R.color.ink));message.setMinLines(4);
        message.setText(text(a,j,Lang.current(a)));message.setId(R.id.jobPickupMessage);
        body.addView(message,ServiceUi.match());
        LinearLayout languages=ServiceUi.row(a);
        languages.addView(ServiceUi.button(a,"English",false,v->message.setText(text(a,j,"en"))),ServiceUi.weight());
        LinearLayout.LayoutParams lp=ServiceUi.weight();lp.setMarginStart(Ui.dp(a,8));
        languages.addView(ServiceUi.button(a,"العربية",false,v->message.setText(text(a,j,"ar"))),lp);body.addView(languages);
        ServiceUi.gap(body,12);ServiceUi.body(body,a.getString(R.string.job_message_help));
        ScrollView scroll=new ScrollView(a);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(a,R.style.AppTheme_Dialog).setTitle(R.string.job_message_title).setView(scroll)
                .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.job_open_whatsapp,null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(message.getText().toString().trim().isEmpty()){message.setError(a.getString(R.string.svc_required));return;}
            if(!j.phone.matches("[1-9][0-9]{6,14}")){Ui.toast(a,a.getString(R.string.job_phone_invalid));return;}
            Uri uri=Uri.parse("https://wa.me/"+j.phone+"?text="+Uri.encode(message.getText().toString()));
            if(open(a,uri)){
                new MaintenanceStore(a).recordDraftOpened(j.id);dialog.dismiss();opened.run();
            } else Ui.toast(a,a.getString(R.string.job_whatsapp_missing));
        }));dialog.show();return dialog;
    }
    private static boolean open(Activity a, Uri uri) {
        for(String pkg:new String[]{"com.whatsapp","com.whatsapp.w4b",null}) {
            Intent i=new Intent(Intent.ACTION_VIEW,uri);
            if(pkg!=null)i.setPackage(pkg);
            try{a.startActivity(i);return true;}catch(ActivityNotFoundException|SecurityException ignored){}
        }
        return false;
    }
}
