package com.electrostore.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.List;


public class RegressionRunner extends Instrumentation {
    private Context c;
    private int passed;
    private final StringBuilder report=new StringBuilder();
    private Bundle args;
    private void check(boolean condition,String name) {
        if(!condition)throw new AssertionError(name);
        passed++;report.append("PASS ").append(name).append('\n');Log.i("ElectroRegression","PASS "+name);
        Bundle progress=new Bundle();progress.putString("stream","PASS "+name+"\n");sendStatus(1,progress);
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);args=b;start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            c=getTargetContext();
            if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish"))
                throw new IllegalStateException("Run only on a disposable emulator: tests reset store.db");
            Lang.set(c,"en");
            migrationAndStock();
            customerMigrationAndCrud();
            maintenanceAndBackup();
            uiAndDraft();
            result.putString("stream",report+"\n"+passed+" checks passed.\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            Log.e("ElectroRegression","FAILED",e);
            result.putString("stream",report+"\nFAILED: "+Log.getStackTraceString(e));
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private Product product(String name,int qty){Product p=new Product();p.name=name;p.quantity=qty;p.costPrice=4;p.unitPrice=10;Db.get(c).saveProduct(p);return p;}
    private Order order(Product p,int... quantities){
        Db db=Db.get(c);Order o=new Order();o.customer="Test Customer";o.receiptNo=db.nextReceiptNo();o.day=Db.today();o.ts=System.currentTimeMillis();
        for(int q:quantities){OrderItem it=new OrderItem();it.productId=p.id;it.productName=p.name;it.qty=q;it.unitPrice=10;it.lineTotal=10*q;o.items.add(it);o.total+=it.lineTotal;}
        db.saveOrder(o);return o;
    }
    private void migrationAndStock(){
        Db.reset();LegacyDb.reset();c.deleteDatabase(Db.FILE);
        LegacyDb old=LegacyDb.get(c);
        Product p=new Product();p.name="Legacy laptop";p.quantity=10;p.unitPrice=10;p.costPrice=4;p.category="Computers";
        old.saveProduct(p);Product spare=new Product();spare.quantity=2;old.saveProduct(spare);old.deleteProduct(spare.id);
        old.setSetting("shop_name","IMAD COMPUTER");old.setSetting("migration_marker","keep-me");
        Sale historical=old.sell(p,2,10,"");
        Order o=new Order();o.receiptNo=old.nextReceiptNo();o.day=LegacyDb.today();o.ts=System.currentTimeMillis();o.total=30;
        OrderItem it=new OrderItem();it.productId=p.id;it.productName=p.name;it.qty=3;it.unitPrice=10;it.lineTotal=30;o.items.add(it);
        old.saveOrder(o);old.bookOrderAsSales(o);old.setOrderStatus(o.id,"DONE");LegacyDb.reset();
        Db db=Db.get(c);
        check(db.getReadableDatabase().getVersion()==5,"v1.8 database migrates to schema 5");
        check(db.product(p.id).quantity==5&&db.productCount()==1,"migration preserves stock and product IDs");
        check("keep-me".equals(db.setting("migration_marker",""))&&db.categories().contains("Computers"),"migration preserves settings and categories");
        check(db.salesOn(Db.today()).size()==2&&"DONE".equals(db.order(o.id).status),"migration preserves sales and collected orders");
        check(new MaintenanceStore(c).services(false).size()==12,"migration seeds 12 services exactly once");
        db.unbookOrder(o.receiptNo);db.unbookOrder(o.receiptNo);
        check(db.product(p.id).quantity==8&&db.salesOn(Db.today()).size()==1,"legacy order undo restores stock once");
        db.deleteSale(historical.id);db.deleteSale(historical.id);
        check(db.product(p.id).quantity==10,"legacy sale undo is idempotent");
        Sale sold=db.sell(p,3,10,"");
        check(db.product(p.id).quantity==7&&db.stockCount()==7,"sale decrements live stock and total");
        check(db.productCount()==1,"selling pieces does not delete the product type");
        db.deleteSale(sold.id);
        Sale oversold=db.sell(p,15,10,"");db.addStock(p.id,4);db.deleteSale(oversold.id);db.deleteSale(oversold.id);
        check(db.product(p.id).quantity==14,"oversell + restock + undo restores only units actually deducted");
        Product zero=product("Zero stock",0);Sale zeroSale=db.sell(zero,4,10,"");db.deleteSale(zeroSale.id);
        check(db.product(zero.id).quantity==0,"undoing a zero-stock sale cannot invent stock");
        db.deleteProduct(zero.id);check(db.productCount()==1&&db.stockCount()==14,"deleting a product updates count independently of ID gaps");
        Product five=product("Five units",5);Order duplicate=order(five,4,4);
        db.bookOrderAsSales(duplicate);db.bookOrderAsSales(duplicate);
        check(db.product(five.id).quantity==0&&db.salesOn(Db.today()).size()==2,"collecting duplicate order lines is atomic and idempotent");
        db.unbookOrder(duplicate.receiptNo);db.unbookOrder(duplicate.receiptNo);
        check(db.product(five.id).quantity==5&&db.salesOn(Db.today()).isEmpty(),"reopening oversold order restores exactly five pieces");
        db.bookOrderAsSales(duplicate);db.deleteOrder(duplicate.id);
        check(db.product(five.id).quantity==5&&db.order(duplicate.id)==null&&db.salesOn(Db.today()).isEmpty(),"deleting collected order undoes its stock and sales");
        Order removed=order(five,2);db.deleteProduct(five.id);Product fresh=product("New ID",9);db.bookOrderAsSales(removed);db.deleteOrder(removed.id);
        check(fresh.id>five.id&&db.product(fresh.id).quantity==9,"deleted product ID cannot move a different product's stock");
        SQLiteDatabase sql=db.getWritableDatabase();
        sql.execSQL("CREATE TRIGGER fail_sale BEFORE INSERT ON sales BEGIN SELECT RAISE(ABORT,'test failure'); END");
        boolean failed=false;try{db.sell(p,2,10,"");}catch(RuntimeException expected){failed=true;}
        sql.execSQL("DROP TRIGGER fail_sale");
        check(failed&&db.product(p.id).quantity==14&&db.salesOn(Db.today()).isEmpty(),"failed sale leaves stock and revenue unchanged");
        sql.execSQL("CREATE TRIGGER fail_order BEFORE INSERT ON order_items WHEN NEW.qty=7 BEGIN SELECT RAISE(ABORT,'test failure'); END");
        int before=db.openOrderCount();failed=false;try{order(p,1,7);}catch(RuntimeException expected){failed=true;}
        sql.execSQL("DROP TRIGGER fail_order");
        check(failed&&db.openOrderCount()==before,"failed order line rolls back the entire new order");
    }
    private void customerMigrationAndCrud() throws Exception {
        Db.reset();LegacyDb.reset();c.deleteDatabase(Db.FILE);
        SQLiteDatabase old=LegacyDb.get(c).getWritableDatabase();
        old.execSQL("ALTER TABLE sales ADD COLUMN stock_deducted INTEGER NOT NULL DEFAULT -1");
        old.execSQL("ALTER TABLE sales ADD COLUMN order_id INTEGER NOT NULL DEFAULT 0");
        old.execSQL("CREATE INDEX idx_sales_order ON sales(order_id)");
        LegacyMaintenanceSchema.createTables(old);
        old.execSQL("INSERT INTO service_jobs(service_id,customer,phone,device,name_en,tips_en,checked_steps,status) VALUES(1,'Maya','96170123456','Dell','Format','Backup',1,'WORKING'),(1,'Maya','96170123456','HP','Format','Backup',0,'READY'),(1,'Karim','96170123456','Asus','Format','Backup',0,'RECEIVED')");
        old.setVersion(4);LegacyDb.reset();
        MaintenanceStore db=new MaintenanceStore(c);
        check(Db.get(c).getReadableDatabase().getVersion()==5&&db.customers("").size()==2,"v1.9 migration creates contacts without merging people who share a phone");
        check(db.job(1).customerId==db.job(2).customerId&&db.job(1).customerId!=db.job(3).customerId,"v1.9 jobs link to the correct stable customer ID");
        check(db.job(1).checkedSteps==1&&db.job(1).tipsEn.equals("Backup")&&db.job(2).status.equals("READY"),"customer migration preserves job progress, guides and status");
        Db.reset();db=new MaintenanceStore(c);check(db.customers("").size()==2,"reopening does not duplicate migrated contacts");
        ServiceCustomer maya=db.customer(db.job(1).customerId);maya.name="Maya Updated";maya.phone="96171111111";db.saveCustomer(maya);
        check(db.job(1).customer.equals("Maya Updated")&&db.job(2).phone.equals("96171111111")&&db.job(3).phone.equals("96170123456"),"editing a customer updates all linked jobs and no other customer");
        ServiceJob reuse=sample(db,db.service(1),"Maya Updated","Tablet","RECEIVED");

        long otherContact=reuse.customerId;db.deleteJob(reuse.id);db.deleteCustomer(otherContact);
        ServiceJob linked=new ServiceJob();linked.snapshot(c,db.service(1));linked.customerId=maya.id;linked.customer=maya.name;linked.phone=maya.phone;linked.device="Tablet";db.saveJob(linked);
        check(db.customer(maya.id).jobs==3&&linked.customerId==maya.id,"new job reuses the selected customer without duplicate contact");
        db.deleteJob(linked.id);check(db.customer(maya.id)!=null&&db.customer(maya.id).jobs==2,"deleting one job keeps the saved customer");
        SQLiteDatabase sql=Db.get(c).getWritableDatabase();
        sql.execSQL("CREATE TRIGGER fail_contact_update BEFORE UPDATE ON service_jobs WHEN OLD.id=2 BEGIN SELECT RAISE(ABORT,'test'); END");
        boolean failed=false;maya.name="Must roll back";try{db.saveCustomer(maya);}catch(RuntimeException e){failed=true;}
        sql.execSQL("DROP TRIGGER fail_contact_update");
        check(failed&&db.customer(maya.id).name.equals("Maya Updated")&&db.job(1).customer.equals("Maya Updated"),"failed customer edit rolls back profile and all linked job changes");
        sql.execSQL("CREATE TRIGGER fail_contact_delete BEFORE DELETE ON service_customers BEGIN SELECT RAISE(ABORT,'test'); END");
        failed=false;try{db.deleteCustomer(maya.id);}catch(RuntimeException e){failed=true;}
        sql.execSQL("DROP TRIGGER fail_contact_delete");
        check(failed&&db.customer(maya.id)!=null&&db.customerJobs(maya.id).size()==2,"failed customer deletion restores both contact and jobs");
        Product p=product("Customer deletion leaves stock",7);
        db.deleteCustomer(maya.id);
        check(db.customer(maya.id)==null&&db.job(1)==null&&db.job(2)==null&&db.job(3)!=null&&Db.get(c).product(p.id).quantity==7,"customer deletion removes only their maintenance jobs and keeps inventory");
        int count=db.customers("").size();sql.execSQL("CREATE TRIGGER fail_new_job BEFORE INSERT ON service_jobs BEGIN SELECT RAISE(ABORT,'test'); END");
        failed=false;try{sample(db,db.service(1),"No orphan","Test","RECEIVED");}catch(RuntimeException e){failed=true;}
        sql.execSQL("DROP TRIGGER fail_new_job");
        check(failed&&db.customers("").size()==count,"failed new job does not leave an orphan contact");
        ServiceCustomer standalone=new ServiceCustomer();standalone.name="No job yet";standalone.phone="9613012345";db.saveCustomer(standalone);
        check(db.customers("9613012345").size()==1&&db.customer(standalone.id).jobs==0,"standalone customer is saved and searchable by phone");
    }
    private ServiceJob sample(MaintenanceStore store,ServiceType s,String name,String device,String status){
        ServiceJob j=new ServiceJob();j.snapshot(c,s);j.customer=name;j.phone="96170123456";j.device=device;j.notes="Customer requested setup and a final check.";j.status=status;store.saveJob(j);return j;
    }
    private void maintenanceAndBackup() throws Exception {
        Db.reset();c.deleteDatabase(Db.FILE);
        MaintenanceStore store=new MaintenanceStore(c);Db.get(c).getWritableDatabase().enableWriteAheadLogging();Db.get(c).setSetting("shop_name","IMAD COMPUTER");
        check(store.services(false).size()==12,"fresh install creates all services");
        check("9613123456".equals(PhoneNumbers.normalize("03 123 456","+961")),"Lebanese 03 mobile becomes international digits");
        check("96170123456".equals(PhoneNumbers.normalize("٧٠ ١٢٣ ٤٥٦","+961")),"Arabic phone digits normalize");
        check("447700900123".equals(PhoneNumbers.normalize("0044 7700 900123","+961")),"explicit international number keeps its country");
        check(PhoneNumbers.normalize("070abc","+961").isEmpty()&&PhoneNumbers.normalize("","+961").isEmpty(),"invalid and empty phone numbers are rejected");
        ServiceType s=store.services(false).get(0);ServiceJob j=sample(store,s,"Maya Haddad","Dell Latitude · charger included","WORKING");
        String original=j.tipsEn;store.setSteps(j.id,3);store.setServiceActive(s.id,false);s.name="Renamed format";s.tips="New custom step";s.active=false;store.saveService(s);
        check(store.services(false).size()==11&&store.job(j.id).tipsEn.equals(original)&&store.job(j.id).checkedSteps==3,"removing/editing a service preserves the job snapshot and progress");
        store.setServiceActive(s.id,true);check(store.services(false).size()==12,"removed service restores without duplicating it");
        ServiceType custom=new ServiceType();custom.name="Console setup";custom.tips="Ask about the games.\nTest one game.";store.saveService(custom);
        check(store.services(false).size()==13,"custom service is saved");
        j=store.job(j.id);j.dueDay="2020-01-01";store.saveJob(j);store.setStatus(j.id,"READY");
        check(store.counts()[0]==1&&store.counts()[1]==1&&store.counts()[2]==1,"active, ready and overdue counts follow the job");
        store.setStatus(j.id,"COLLECTED");check(store.counts()[0]==0&&store.jobs("Maya","COLLECTED").size()==1,"collected jobs leave active board and remain searchable");
        store.setStatus(j.id,"READY");
        long id=j.id;Db.reset();store=new MaintenanceStore(c);
        check(store.job(id).checkedSteps==3&&"READY".equals(store.job(id).status),"job and checklist survive database reopen");
        Method checkpoint=Backup.class.getDeclaredMethod("checkpoint",Context.class);checkpoint.setAccessible(true);checkpoint.invoke(null,c);
        Method write=Backup.class.getDeclaredMethod("writeZip",Context.class,File.class,File.class);write.setAccessible(true);
        File zip=new File(c.getCacheDir(),"maintenance-test.zip"),work=new File(c.getCacheDir(),"maintenance-test-restore");
        write.invoke(null,c,c.getDatabasePath(Db.FILE),zip);
        Backup.Info info=Backup.inspect(c,zip,work);
        check(info.valid&&!info.isEmpty()&&info.products==0&&info.jobs==1&&info.services==13&&info.customers==1,"maintenance-only backup is recognized as nonempty");
        store.deleteJob(id);check(store.job(id)==null,"job deletion removes the job");
        Backup.install(c,work);store=new MaintenanceStore(c);
        check(store.job(id)!=null&&store.job(id).checkedSteps==3&&store.services(false).size()==13&&store.customers("").size()==1,"backup restore recovers maintenance jobs, steps and custom services");
        s=store.service(s.id);s.name="";s.tips="";store.saveService(s);
        ServiceJob next=sample(store,store.services(false).get(1),"Karim Saleh","HP laptop · app installation","RECEIVED");
        next.dueDay=Db.today();store.saveJob(next);
        sample(store,store.services(false).get(3),"Lina Nasser","Lenovo laptop · keyboard repair","WAITING");
        check(!store.job(id).nameEn.equals(store.job(id).nameAr),"job retains English and Arabic service snapshots");
        String en=PickupMessage.text(c,store.job(id),"en"),ar=PickupMessage.text(c,store.job(id),"ar");
        check(en.contains("Maya")&&en.contains("Dell")&&en.contains("IMAD COMPUTER")&&ar.contains("جاهز"),"pickup templates contain customer, device, shop and language");
        String[] permissions=c.getPackageManager().getPackageInfo(c.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
        boolean offline=true;if(permissions!=null)for(String p:permissions)if(p.equals("android.permission.INTERNET"))offline=false;
        check(offline,"app declares no internet permission");
    }
    private Activity launch(Class<?> cls,long id,String idKey){Intent i=new Intent(c,cls);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(cls==MainActivity.class)i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);if(id>0)i.putExtra(idKey,id);Activity a=startActivitySync(i);waitForIdleSync();return a;}
    private View textView(View v,String text){
        if(v instanceof TextView&&text.equals(((TextView)v).getText().toString()))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=textView(g.getChildAt(i),text);if(r!=null)return r;}}
        return null;
    }
    private CheckBox checkbox(View v){if(v instanceof CheckBox)return (CheckBox)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){CheckBox b=checkbox(g.getChildAt(i));if(b!=null)return b;}}return null;}
    private void screenshot(String name) throws Exception {
        waitForIdleSync();SystemClock.sleep(350);Bitmap b=getUiAutomation().takeScreenshot();
        if(b!=null){File folder=new File(c.getFilesDir(),"qa");folder.mkdirs();FileOutputStream out=new FileOutputStream(new File(folder,name+".png"));b.compress(Bitmap.CompressFormat.PNG,100,out);out.close();b.recycle();}
    }
    private void confirmDialog() {
        waitForIdleSync();SystemClock.sleep(250);
        android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;getUiAutomation().setServiceInfo(info);
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
        if(root==null)throw new AssertionError("No active confirmation");
        List<android.view.accessibility.AccessibilityNodeInfo> buttons=root.findAccessibilityNodeInfosByViewId("android:id/button1");
        if(buttons.isEmpty()||!buttons.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Missing confirmation button");
        waitForIdleSync();SystemClock.sleep(150);
    }
    private void uiAndDraft() throws Exception {
        MainActivity main=(MainActivity)launch(MainActivity.class,0,"");
        runOnMainSync(()->main.select(MainActivity.TAB_MAINTENANCE));waitForIdleSync();
        check(main.findViewById(R.id.nav5)!=null&&main.findViewById(R.id.maintenanceSearch)!=null,"bottom Maintenance tab opens native job board");
        screenshot("01-job-board");
        runOnMainSync(()->main.findViewById(R.id.deskServicesTab).performClick());waitForIdleSync();screenshot("02-services");
        check(textView(main.getWindow().getDecorView(),"Format & system setup")!=null,"service catalogue renders built-in services");
        check(main.findViewById(R.id.deskEditService)!=null&&main.findViewById(R.id.deskDeleteService)!=null,"service cards expose Edit and Delete directly");
        runOnMainSync(()->main.findViewById(R.id.deskCustomersTab).performClick());waitForIdleSync();screenshot("07-customers");
        check(main.findViewById(R.id.deskEditCustomer)!=null&&main.findViewById(R.id.deskDeleteCustomer)!=null,"customer cards expose Edit and Delete directly");
        ServiceCustomer editContact=new MaintenanceStore(c).customers("").get(0);
        final AlertDialog[] editor={null};
        runOnMainSync(()->editor[0]=CustomerEditor.show(main,editContact.id,()->{}));waitForIdleSync();
        runOnMainSync(()->{
            ((EditText)editor[0].findViewById(R.id.deskCustomerName)).setText(editContact.name+" Edited");
            ((EditText)editor[0].findViewById(R.id.deskCustomerPhone)).setText("+961 71 987 654");
        });screenshot("08-customer-editor");
        runOnMainSync(()->editor[0].findViewById(R.id.deskCustomerSave).performClick());waitForIdleSync();
        ServiceCustomer changed=new MaintenanceStore(c).customer(editContact.id);
        check(changed.name.endsWith(" Edited")&&changed.phone.equals("96171987654"),"native customer editor saves name and normalized phone");
        check(new MaintenanceStore(c).customerJobs(changed.id).get(0).phone.equals(changed.phone),"customer UI edit updates the linked job phone");

        new MaintenanceStore(c).saveCustomer(editContact);
        runOnMainSync(()->main.findViewById(R.id.deskJobsTab).performClick());waitForIdleSync();
        runOnMainSync(()->((android.widget.ListView)main.findViewById(R.id.deskList)).setSelection(1));waitForIdleSync();screenshot("09-job-actions");
        check(main.findViewById(R.id.deskEditJob)!=null&&main.findViewById(R.id.deskDeleteJob)!=null,"job cards expose Edit and Delete without opening another screen");

        runOnMainSync(()->main.findViewById(R.id.deskServicesTab).performClick());waitForIdleSync();
        ActivityMonitor serviceMonitor=addMonitor(ServiceEditorActivity.class.getName(),null,false);
        runOnMainSync(()->main.findViewById(R.id.deskEditService).performClick());
        ServiceEditorActivity serviceForm=(ServiceEditorActivity)waitForMonitorWithTimeout(serviceMonitor,5000);removeMonitor(serviceMonitor);
        check(serviceForm!=null,"Edit on a service card opens its editor");
        runOnMainSync(()->{((EditText)serviceForm.findViewById(R.id.serviceName)).setText("System setup plus");textView(serviceForm.getWindow().getDecorView(),serviceForm.getString(R.string.svc_save)).performClick();});waitForIdleSync();
        check(new MaintenanceStore(c).service(1).name.equals("System setup plus"),"service edit from the page persists its custom name");
        runOnMainSync(()->main.findViewById(R.id.deskDeleteService).performClick());waitForIdleSync();confirmDialog();
        check(!new MaintenanceStore(c).service(1).active&&new MaintenanceStore(c).jobs("Maya","").size()==1,"Delete on a service card removes it while keeping customer jobs");
        ServiceType first=new MaintenanceStore(c).service(1);first.active=true;first.name="";new MaintenanceStore(c).saveService(first);
        MaintenanceStore local=new MaintenanceStore(c);ServiceJob deletion=sample(local,local.service(1),"Aaron Delete","Test device","RECEIVED");
        runOnMainSync(()->main.findViewById(R.id.deskCustomersTab).performClick());waitForIdleSync();
        runOnMainSync(()->main.findViewById(R.id.deskDeleteCustomer).performClick());waitForIdleSync();screenshot("10-customer-delete");confirmDialog();
        check(new MaintenanceStore(c).customer(deletion.customerId)==null&&new MaintenanceStore(c).job(deletion.id)==null,"Delete on a customer card removes that contact and linked job after confirmation");
        check(new MaintenanceStore(c).customers("Maya").size()==1,"customer deletion from the page leaves other customers available");
        ServiceCustomer selected=new MaintenanceStore(c).customers("Karim").get(0);
        Intent intake=new Intent(c,ServiceJobEditActivity.class);intake.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);intake.putExtra("service_id",2L);intake.putExtra("customer_id",selected.id);
        ServiceJobEditActivity prefilled=(ServiceJobEditActivity)startActivitySync(intake);waitForIdleSync();
        check(((EditText)prefilled.findViewById(R.id.jobCustomer)).getText().toString().equals(selected.name)&&((EditText)prefilled.findViewById(R.id.jobPhone)).getText().toString().equals("+"+selected.phone),"new job from a customer pre-fills the saved contact");runOnMainSync(()->prefilled.finish());
        ServiceJobEditActivity form=(ServiceJobEditActivity)launch(ServiceJobEditActivity.class,2,"service_id");
        runOnMainSync(()->form.findViewById(R.id.jobSave).performClick());
        check(((EditText)form.findViewById(R.id.jobCustomer)).getError()!=null,"intake refuses missing customer");
        runOnMainSync(()->{
            ((EditText)form.findViewById(R.id.jobCustomer)).setText("Rami Test");
            ((EditText)form.findViewById(R.id.jobCustomer)).setError(null);
            ((EditText)form.findViewById(R.id.jobPhone)).setText("03 123 456");
            ((EditText)form.findViewById(R.id.jobDevice)).setText("Asus laptop");
        });screenshot("03-intake");
        runOnMainSync(()->form.findViewById(R.id.jobSave).performClick());waitForIdleSync();
        List<ServiceJob> found=new MaintenanceStore(c).jobs("Rami","");
        check(found.size()==1&&found.get(0).phone.equals("9613123456"),"native intake saves customer, phone and service job");
        ServiceJob j=new MaintenanceStore(c).jobs("Maya","").get(0);
        Activity detail=main;
        final Intent[] captured={null};
        ActivityMonitor monitor=new ActivityMonitor(){@Override public ActivityResult onStartActivity(Intent i){if(Intent.ACTION_VIEW.equals(i.getAction())){captured[0]=i;return new ActivityResult(Activity.RESULT_OK,null);}return null;}};
        addMonitor(monitor);
        final AlertDialog[] dialog={null};
        runOnMainSync(()->dialog[0]=PickupMessage.show(detail,j,()->{}));screenshot("05-pickup-message");
        runOnMainSync(()->dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick());waitForIdleSync();removeMonitor(monitor);
        check(captured[0]!=null&&"wa.me".equals(captured[0].getData().getHost())&&captured[0].getData().getPath().equals("/"+j.phone),"WhatsApp draft targets exact normalized customer number (launch intercepted)");
        check(captured[0].getData().getQueryParameter("text").contains(j.customer)&&new MaintenanceStore(c).job(j.id).messageOpenedAt>0,"draft contains customer and records opened time only");
        check("READY".equals(new MaintenanceStore(c).job(j.id).status),"opening a draft does not mark the job collected");
        runOnMainSync(()->{main.finish();});Lang.set(c,"ar");
        MainActivity arabic=(MainActivity)launch(MainActivity.class,0,"");runOnMainSync(()->arabic.select(MainActivity.TAB_MAINTENANCE));waitForIdleSync();screenshot("06-arabic-board");
        check(arabic.getResources().getConfiguration().getLayoutDirection()==View.LAYOUT_DIRECTION_RTL,"Arabic maintenance layout is right-to-left");
        runOnMainSync(()->arabic.finish());Lang.set(c,"en");
        MainActivity last=(MainActivity)launch(MainActivity.class,0,"");runOnMainSync(()->last.select(MainActivity.TAB_MAINTENANCE));waitForIdleSync();
    }
}
