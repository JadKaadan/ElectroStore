package com.electrostore.app;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;


public class ServiceEditorActivity extends Base {
    private ServiceType service;
    private EditText name, tips;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        long id=getIntent().getLongExtra("id",0);
        service=id==0?new ServiceType():new MaintenanceStore(this).service(id);
        if(service==null){finish();return;}
        LinearLayout body=ServiceUi.form(this,getString(id==0?R.string.svc_add:R.string.svc_edit),getString(R.string.svc_editor_sub),v->onBackPressed());
        name=ServiceUi.field(body,R.string.svc_name,R.string.svc_name_hint,R.id.serviceName,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setSingleLine(true);name.setText(b==null?service.name:b.getString("name",""));
        if(!service.presetKey.isEmpty()) {
            name.setHint(ServiceCatalog.title(this,service.presetKey));
            ServiceUi.gap(body,8);ServiceUi.body(body,getString(R.string.svc_override_help));
        }
        tips=ServiceUi.field(body,R.string.svc_steps,R.string.svc_steps_hint,R.id.serviceTips,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        tips.setMinLines(6);tips.setText(b==null?service.tips:b.getString("tips",""));
        ServiceUi.gap(body,8);ServiceUi.body(body,getString(R.string.svc_steps_help));
        ServiceUi.label(body,getString(R.string.svc_default_guide));
        ServiceUi.body(body,ServiceCatalog.guide(this,service.presetKey));
        ServiceUi.gap(body,24);body.addView(ServiceUi.button(this,getString(R.string.svc_save),true,v->save()),ServiceUi.match());
    }
    private void save() {
        String title=name.getText().toString().trim(),guide=tips.getText().toString().trim();
        if(title.isEmpty()&&service.presetKey.isEmpty()){name.setError(getString(R.string.svc_required));name.requestFocus();return;}
        int steps=0;for(String s:guide.split("\n"))if(!s.trim().isEmpty())steps++;
        if(steps>30){tips.setError(getString(R.string.svc_too_many_steps));return;}
        service.name=title;service.tips=guide;
        try {new MaintenanceStore(this).saveService(service);}
        catch(RuntimeException e){Ui.toast(this,getString(R.string.svc_save_failed));return;}
        Ui.hideKeyboard(this);finish();
    }
    @Override protected void onSaveInstanceState(Bundle b) {
        b.putString("name",name.getText().toString());b.putString("tips",tips.getText().toString());super.onSaveInstanceState(b);
    }
    @Override public void onBackPressed() {
        boolean dirty=!name.getText().toString().equals(service.name)||!tips.getText().toString().equals(service.tips);
        if(!dirty){super.onBackPressed();return;}
        Ui.confirm(this,getString(R.string.job_discard_title),getString(R.string.job_discard_message),getString(R.string.job_discard),()->finish());
    }
}
