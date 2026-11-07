package com.sadique.launcher.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;

/** Real Android task/window regression, using the framework runner without runtime dependencies. */
public final class PanelRoutingInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private Bundle status(String test,int current) {
        Bundle b=new Bundle(); b.putString("class",getClass().getName()); b.putString("test",test);
        b.putInt("numtests",2); b.putInt("current",current); b.putString("id","InstrumentationTestRunner"); return b;
    }
    @Override public void onStart() {
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
            focus(home);
            ActivityMonitor monitor=addMonitor(NotificationPanelActivity.class.getName(),null,false);
            runOnMainSync(()->owner.findViewById(R.id.open_panel).performClick());
            Activity homePanel=monitor.waitForActivityWithTimeout(5000); removeMonitor(monitor);
            if(homePanel==null) throw new AssertionError("Home panel did not open");
            focus(homePanel);
            if(homePanel.getTaskId()!=home.getTaskId()) throw new AssertionError("Home button panel left the Home task");
            runOnMainSync(()->homePanel.findViewById(R.id.close_panel).performClick());
            focus(home);
            sendStatus(0,status(test,current));
            outcome.putString("stream","OK (2 task-routing tests)\n");
        } catch(Throwable failure) {
            Bundle failed=status(test,current); failed.putString("stack",android.util.Log.getStackTraceString(failure)); sendStatus(-2,failed);
            outcome.putString("shortMsg",failure.toString()); resultCode=Activity.RESULT_CANCELED;
        } finally {
            Activity h=home,u=underlay; runOnMainSync(()-> { if(u!=null) u.finishAndRemoveTask(); if(h!=null) h.finishAndRemoveTask(); });
        }
        finish(resultCode,outcome);
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
