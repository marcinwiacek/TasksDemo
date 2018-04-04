package mwiacek.com.tasksdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver for completing task. Can be executed when app is closed.
 */
public class TaskCompleteBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP_TASK = "com.mwiacek.tasksdemo.ACTION_STOP_TASK";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check that we have intent with correct data
        if (!ACTION_STOP_TASK.equals(intent.getAction())) {
            return;
        }
        String fileName = intent.getStringExtra(Util.INTENT_TASK_FILE_NAME);
        if (fileName == null) {
            return;
        }
        int uniqueLocation = intent.getIntExtra(Util.INTENT_TASK_UNIQUE_POSITION,
                -1);
        if (uniqueLocation == -1) {
            return;
        }
        Task t = Util.readTaskFromInternalStorage(context, fileName);
        if (t == null) {
            return;
        }

        t.CompleteTask();
        t.saveToInternalStorage(context);

        // Send another broadcast to force MainActivity to refresh own data
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.ACTION_EDIT_TASK)
                .putExtra(Util.INTENT_TASK_UNIQUE_POSITION, uniqueLocation)
                .putExtra(Util.INTENT_TASK_FILE_NAME, fileName);
        context.sendBroadcast(broadcastIntent);

        // Update notification
        t.createUpdateNotification(context);
    }
}