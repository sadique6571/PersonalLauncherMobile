package com.sadique.launcher.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;

/** Real Android task/window regression, using the framework runner without runtime dependencies. */
@android.annotation.TargetApi(36)
public final class PanelRoutingInstrumentation extends Instrumentation {
    private UiAutomation automation;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private Bundle status(String test,int current) {
        Bundle b=new Bundle(); b.putString("class",getClass().getName()); b.putString("test",test);
        b.putInt("numtests",4); b.putInt("current",current); b.putString("id","InstrumentationTestRunner"); return b;
    }
    @Override public void onStart() {
        automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        Bundle outcome=new Bundle(); int resultCode=Activity.RESULT_OK;
        HomeActivity home=null; RoutingUnderlayActivity underlay=null;
        String test="tileCloseRestoresUnderlyingTask"; int current=1;
        try {
            sendStatus(1,status(test,current));
            home=(HomeActivity)startActivitySync(new Intent(getTargetContext(),HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            underlay=(RoutingUnderlayActivity)startActivitySync(new Intent(getTargetContext(),RoutingUnderlayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
            focus(underlay);
            NotificationPanelActivity panel=(NotificationPanelActivity)startActivitySync(PanelIntents.fromTile(getTargetContext()));
            focus(panel);
            if(panel.getTaskId()==home.getTaskId() || panel.getTaskId()==underlay.getTaskId()) throw new AssertionError("Tile panel reused an existing task");
            runOnMainSync(()->panel.findViewById(R.id.close_panel).performClick());
            focus(underlay);
            sendStatus(0,status(test,current));

            test="homePanelClosePreservesHomeTask"; current=2; sendStatus(1,status(test,current));
            HomeActivity owner=home;
            runOnMainSync(()->owner.startActivity(new Intent(owner,HomeActivity.class)));
            focus(home); screenshot("home");
            ActivityMonitor monitor=addMonitor(NotificationPanelActivity.class.getName(),null,false);
            runOnMainSync(()->owner.findViewById(R.id.open_panel).performClick());
            Activity homePanel=monitor.waitForActivityWithTimeout(5000); removeMonitor(monitor);
            if(homePanel==null) throw new AssertionError("Home panel did not open");
            focus(homePanel); screenshot("panel");
            if(homePanel.getTaskId()!=home.getTaskId()) throw new AssertionError("Home button panel left the Home task");
            runOnMainSync(()->homePanel.findViewById(R.id.close_panel).performClick());
            focus(home);
            sendStatus(0,status(test,current));
            test="homeGridSwipeOpensPanel"; current=3; sendStatus(1,status(test,current));
            ActivityMonitor swipeMonitor=addMonitor(NotificationPanelActivity.class.getName(),null,false);
            android.util.DisplayMetrics metrics=getTargetContext().getResources().getDisplayMetrics();
            swipe(metrics.widthPixels*.35f,metrics.heightPixels*.48f,Ui.dp(getTargetContext(),150));
            Activity swipePanel=swipeMonitor.waitForActivityWithTimeout(5000); removeMonitor(swipeMonitor);
            if(swipePanel==null) {
                String[] detail={"no root"}; runOnMainSync(()-> { android.widget.FrameLayout content=owner.findViewById(android.R.id.content); android.view.View root=content.getChildAt(0); if(root instanceof GestureFrame) detail[0]=((GestureFrame)root).diagnostics(); });
                throw new AssertionError("Full Home swipe did not open the panel: "+detail[0]);
            }
            focus(swipePanel); runOnMainSync(()->swipePanel.findViewById(R.id.close_panel).performClick()); focus(home); sendStatus(0,status(test,current));

            test="edgeSwipeOverAppRestoresUnderlyingTask"; current=4; sendStatus(1,status(test,current));
            runOnMainSync(()->owner.startActivity(new Intent(owner,RoutingUnderlayActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            focus(underlay);
            shell("settings put secure enabled_accessibility_services com.sadique.launcher.mobile/.EdgeSwipeService");
            shell("settings put secure accessibility_enabled 1");
            long until=SystemClock.uptimeMillis()+8000; while(!EdgeSwipeService.active()&&SystemClock.uptimeMillis()<until) SystemClock.sleep(100);
            if(!EdgeSwipeService.active()) throw new AssertionError("User-enabled edge service did not attach");
            ActivityMonitor edgeMonitor=addMonitor(NotificationPanelActivity.class.getName(),null,false);
            swipe(metrics.widthPixels*.75f,Ui.dp(getTargetContext(),12),Ui.dp(getTargetContext(),130));
            Activity edgePanel=edgeMonitor.waitForActivityWithTimeout(5000); removeMonitor(edgeMonitor);
            if(edgePanel==null) throw new AssertionError("Global edge gesture did not open the panel");
            focus(edgePanel); runOnMainSync(()->edgePanel.findViewById(R.id.close_panel).performClick()); focus(underlay);
            runOnMainSync(EdgeSwipeService::stop); sendStatus(0,status(test,current));
            outcome.putString("stream","OK (4 routing and swipe tests)\n");
        } catch(Throwable failure) {
            Bundle failed=status(test,current); failed.putString("stack",android.util.Log.getStackTraceString(failure)); sendStatus(-2,failed);
            outcome.putString("shortMsg",failure.toString()); resultCode=Activity.RESULT_CANCELED;
        } finally {
            Activity h=home,u=underlay; runOnMainSync(()-> { if(u!=null) u.finishAndRemoveTask(); if(h!=null) h.finishAndRemoveTask(); });
        }
        finish(resultCode,outcome);
    }
    private void swipe(float x,float y,float distance) {
        long down=SystemClock.uptimeMillis();
        android.view.MotionEvent event=android.view.MotionEvent.obtain(down,down,android.view.MotionEvent.ACTION_DOWN,x,y,0); event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); if(!automation.injectInputEvent(event,true)) throw new AssertionError("Touch injection failed"); event.recycle();
        for(int i=1;i<=8;i++) { SystemClock.sleep(30); event=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_MOVE,x,y+distance*i/8,0); event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); if(!automation.injectInputEvent(event,true)) throw new AssertionError("Touch injection failed"); event.recycle(); }
        event=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,x,y+distance,0); event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN); if(!automation.injectInputEvent(event,true)) throw new AssertionError("Touch injection failed"); event.recycle();
    }
    private void shell(String command) throws Exception { try(ParcelFileDescriptor pipe=automation.executeShellCommand(command); java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(pipe)) { while(in.read()!=-1) { } } }
    private void screenshot(String name) throws Exception {
        android.graphics.Bitmap bitmap=automation.takeScreenshot(); if(bitmap==null) return;
        shell("mkdir -p /data/local/tmp/pl-routing");
        ParcelFileDescriptor[] pipes=automation.executeShellCommandRw("dd of=/data/local/tmp/pl-routing/"+name+".png");
        try(java.io.OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out); }
        try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(pipes[0])) { while(in.read()!=-1) { } }
    }
    private void focus(Activity activity) {
        long deadline=SystemClock.uptimeMillis()+7000;
        while(SystemClock.uptimeMillis()<deadline) {
            waitForIdleSync(); boolean[] focused={false}; runOnMainSync(()->focused[0]=activity.hasWindowFocus());
            if(focused[0]) return; SystemClock.sleep(100);
        }
        throw new AssertionError("Window did not return to "+activity.getClass().getSimpleName());
    }
}
