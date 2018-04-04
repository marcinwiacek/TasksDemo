package mwiacek.com.tasksdemo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.ObjectInputStream;
import java.util.Calendar;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Some useful static methods
 */
class Util {
    // ID for Notifications channel
    // (channels are available starting from Android O)
    private static final String NOTIFICATION_CHANNEL_ID = "123";

    // Intent parameter names for broadcasts and service
    static final String INTENT_TASK_FILE_NAME = "TaskFileName";
    static final String INTENT_TASK_UNIQUE_POSITION = "TaskUniquePosition";

    /**
     * Create/update notification for task,
     * doesn't initiate regular updates for it
     *
     * @param context
     * @param taskUniqueLocation
     * @param taskFileName
     * @param taskExecutionStart
     * @param taskExecuting
     * @param taskTitle
     */
    static void createUpdateTaskNotification(
            Context context, int taskUniqueLocation,
            String taskFileName, long taskExecutionStart,
            boolean taskExecuting, String taskTitle) {
        Intent openMainIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setAction(MainActivity.ACTION_OPEN_IN_PENDING);
        PendingIntent openMainPendingIntent = PendingIntent.getActivity(context,
                taskUniqueLocation, openMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent addTaskIntent = new Intent(context, AddEditTaskActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent addTaskPendingIntent = PendingIntent.getActivity(context,
                taskUniqueLocation, addTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long calculatedProgress = taskExecuting ?
                Calendar.getInstance().getTimeInMillis() - taskExecutionStart : 0;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(taskTitle)
                .setContentText(taskExecuting ?
                        String.format(context.getResources().getString(
                                R.string.notification_text_executing),
                                calculatedProgress) :
                        context.getString(R.string.notification_text_complete))
                // Work before O, starting from O we have Importance in the channel
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Disable sound or vibra when updating notification
                .setOnlyAlertOnce(true)
                // Open MainActivity after clicking notification
                .setContentIntent(openMainPendingIntent)
                // Action for opening new task screen
                .addAction(R.drawable.ic_dashboard_black_24dp,
                        context.getString(R.string.notification_button_new),
                        addTaskPendingIntent);

        if (taskExecuting) {
            Intent stopTaskIntent = new Intent(context, TaskCompleteBroadcastReceiver.class)
                    .setAction(TaskCompleteBroadcastReceiver.ACTION_STOP_TASK)
                    .putExtra(Util.INTENT_TASK_UNIQUE_POSITION, taskUniqueLocation)
                    .putExtra(Util.INTENT_TASK_FILE_NAME, taskFileName);
            PendingIntent stopTaskPendingIntent = PendingIntent.getBroadcast(
                    context, taskUniqueLocation, stopTaskIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //Action for stopping execution
            builder.addAction(R.drawable.ic_dashboard_black_24dp,
                    context.getString(R.string.notification_button_cancel),
                    stopTaskPendingIntent);

            // TODO: In the specification there is no clear info, how to mark progress.
            // Proposed short-term demo solution: moving progress bar + info in the text
            builder.setProgress(20000, (int) (calculatedProgress % 20000),
                    false);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(taskUniqueLocation, builder.build());
        }
    }

    /**
     * Read serialized Task structure from file
     * @param context
     * @param fileName
     * @return
     */
    @Nullable
    static Task readTaskFromInternalStorage(Context context, String fileName) {
        if (fileName.equals("")) {
            return null;
        }
        try {
            ObjectInputStream inputStream = new ObjectInputStream(
                    context.openFileInput(fileName));
            Task t = (Task) inputStream.readObject();
            t.setFileName(fileName);
            inputStream.close();
            return t;
        } catch (Exception ignoreException) {
            return null;
        }
    }

    /**
     * Initiate updating task notification with changed progress
     * Postpone it when task start time is in the future
     *
     * @param context
     * @param taskUniqueLocation
     * @param taskFileName
     * @param taskExecutionStart
     * @param taskTitle
     */
    static void scheduleUpdatingTaskNotificationJobServiceStartingFromL(
            Context context, int taskUniqueLocation,
            String taskFileName, long taskExecutionStart,
            String taskTitle) {
        PersistableBundle jobParameters = new PersistableBundle();
        jobParameters.putInt(INTENT_TASK_UNIQUE_POSITION, taskUniqueLocation);
        jobParameters.putString(INTENT_TASK_FILE_NAME, taskFileName);
        jobParameters.putLong(
                UpdateExecutingTaskNotificationJobService.INTENT_TASK_EXECUTION_START,
                taskExecutionStart);
        jobParameters.putString(
                UpdateExecutingTaskNotificationJobService.INTENT_TASK_EXECUTION_TITLE,
                taskTitle);

        ComponentName serviceComponent = new ComponentName(context,
                UpdateExecutingTaskNotificationJobService.class.getName());

        JobInfo.Builder builder = new JobInfo.Builder(taskUniqueLocation, serviceComponent);
        builder.setExtras(jobParameters);
        if (Calendar.getInstance().getTimeInMillis() < taskExecutionStart) {
            // setPeriodic is not used because of some compatibility issues
            builder.setMinimumLatency(taskExecutionStart - Calendar.getInstance().getTimeInMillis());
            builder.setOverrideDeadline(taskExecutionStart - Calendar.getInstance().getTimeInMillis());
        } else {
            // 1. setPeriodic is not used because of some compatibility issues
            // 2. we use small values, in normal system updates should be done
            //    only when really necessary
            builder.setMinimumLatency(500);
            builder.setOverrideDeadline(500);
        }
        // job will start after device restart
        builder.setPersisted(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService
                (Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(builder.build());
        }
    }

    /**
     * Create Notification Channel for Notifications (available from Android O)
     */
    static void createNotificationChannelStartingFromO(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.channel_notification_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.channel_notification_description));

        NotificationManager notificationManager =
                ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE));
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}
