package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;


final class CustomerEditor {
    static AlertDialog show(Activity a,long id,Runnable changed) {
        MaintenanceStore store=new MaintenanceStore(a);
        ServiceCustomer contact=id==0?new ServiceCustomer():store.customer(id);
        if(contact==null)return null;
        LinearLayout body=ServiceUi.column(a);ServiceUi.pad(body,24);body.setBackground(DeskUi.shape(a,R.color.desk_surface,24));
        body.addView(DeskUi.text(a,a.getString(id==0?R.string.desk_add_customer:R.string.desk_edit_customer),24,R.color.desk_ink,true));
        ServiceUi.gap(body,10);body.addView(DeskUi.text(a,a.getString(R.string.desk_customer_shared),13,R.color.desk_soft,false));
        EditText name=ServiceUi.field(body,R.string.job_customer,R.string.job_customer_hint,R.id.deskCustomerName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setSingleLine(true);
        EditText country=ServiceUi.field(body,R.string.job_country,R.string.job_country_hint,R.id.deskCustomerCountry,InputType.TYPE_CLASS_PHONE);country.setSingleLine(true);country.setText("+961");
        EditText phone=ServiceUi.field(body,R.string.job_phone,R.string.job_phone_hint,R.id.deskCustomerPhone,InputType.TYPE_CLASS_PHONE);phone.setSingleLine(true);
        for(EditText field:new EditText[]{name,country,phone})field.setBackground(DeskUi.shape(a,R.color.desk_bg,14));
        country.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);phone.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);
        name.setText(contact.name);phone.setText(contact.phone.isEmpty()?"":"+"+contact.phone);
        ServiceUi.gap(body,24);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(a,R.style.AppTheme_Dialog).setView(scroll).create();
        Button save=DeskUi.action(a,a.getString(R.string.save_changes),R.color.desk_surface,R.color.desk_accent,v->{
            if(name.getText().toString().trim().isEmpty()){name.setError(a.getString(R.string.svc_required));name.requestFocus();return;}
            String normalized=PhoneNumbers.normalize(phone.getText().toString(),country.getText().toString());
            if(normalized.isEmpty()){phone.setError(a.getString(R.string.job_phone_invalid));phone.requestFocus();return;}
            contact.name=name.getText().toString().trim();contact.phone=normalized;
            try {store.saveCustomer(contact);dialog.dismiss();changed.run();Ui.toast(a,a.getString(R.string.desk_customer_saved));}
            catch(RuntimeException e){Ui.toast(a,a.getString(R.string.svc_save_failed));}
        });save.setId(R.id.deskCustomerSave);body.addView(save,ServiceUi.match());
        ServiceUi.gap(body,8);body.addView(DeskUi.action(a,a.getString(R.string.cancel),R.color.desk_soft,R.color.desk_surface,v->dialog.dismiss()),ServiceUi.match());
        dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setLayout(-1,(int)(a.getResources().getDisplayMetrics().heightPixels*0.88f));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}
        return dialog;
    }
}
