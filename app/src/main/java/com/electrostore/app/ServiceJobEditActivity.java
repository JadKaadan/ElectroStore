package com.electrostore.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;


public class ServiceJobEditActivity extends Base {
    private ServiceJob entry;
    private EditText service,product,customer,phone,paid;
    private String initial;
    private boolean saved;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);long id=getIntent().getLongExtra("id",0);entry=id==0?new ServiceJob():new MaintenanceStore(this).job(id);
        if(entry==null){finish();return;}setContentView(R.layout.activity_service_entry);
        ((TextView)findViewById(R.id.simpleTitle)).setText(id==0?R.string.simple_add:R.string.simple_edit);
        findViewById(R.id.back).setOnClickListener(v->onBackPressed());
        service=(EditText)findViewById(R.id.simpleService);product=(EditText)findViewById(R.id.jobDevice);customer=(EditText)findViewById(R.id.jobCustomer);phone=(EditText)findViewById(R.id.jobPhone);paid=(EditText)findViewById(R.id.jobPaid);
        service.setText(entry.title(this));product.setText(entry.device);customer.setText(entry.customer);phone.setText(entry.phone);paid.setText(entry.paid==0?"":Money.plain(entry.paid));initial=fingerprint();
        if(state!=null){service.setText(state.getString("service"));product.setText(state.getString("product"));customer.setText(state.getString("customer"));phone.setText(state.getString("phone"));paid.setText(state.getString("paid"));}
        findViewById(R.id.jobSave).setOnClickListener(v->save());
    }
    private String fingerprint(){return service.getText()+"\n"+product.getText()+"\n"+customer.getText()+"\n"+phone.getText()+"\n"+paid.getText();}
    private boolean required(EditText field){if(field.getText().toString().trim().isEmpty()){field.setError(getString(R.string.svc_required));field.requestFocus();return false;}return true;}
    private void save(){
        if(saved||!required(service)||!required(product))return;
        StringBuilder number=new StringBuilder();for(char ch:paid.getText().toString().trim().toCharArray()){int n=Character.digit(ch,10);if(n>=0)number.append(n);else if(ch=='.'||ch=='\u066b')number.append('.');else{paid.setError(getString(R.string.simple_invalid_paid));return;}}
        double amount;try{amount=number.length()==0?0:Double.parseDouble(number.toString());}catch(NumberFormatException e){paid.setError(getString(R.string.simple_invalid_paid));return;}
        if(Double.isNaN(amount)||Double.isInfinite(amount)||amount<0||amount>1000000000){paid.setError(getString(R.string.simple_invalid_paid));return;}
        entry.customer=customer.getText().toString().trim();entry.device=product.getText().toString().trim();entry.phone=phone.getText().toString().trim();entry.paid=amount;
        try{new MaintenanceStore(this).saveEntry(entry,service.getText().toString());saved=true;Ui.hideKeyboard(this);finish();}catch(RuntimeException e){Ui.toast(this,getString(R.string.svc_save_failed));}
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putString("service",service.getText().toString());state.putString("product",product.getText().toString());state.putString("customer",customer.getText().toString());state.putString("phone",phone.getText().toString());state.putString("paid",paid.getText().toString());super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){if(saved||fingerprint().equals(initial)){super.onBackPressed();return;}Ui.confirm(this,getString(R.string.job_discard_title),getString(R.string.job_discard_message),getString(R.string.job_discard),()->finish());}
}
