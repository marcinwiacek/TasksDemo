package mwiacek.com.tasksdemo;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class UpdateExecutingTaskNotificationJobService extends JobService {
    // Names for Intent parameters
    public static final String INTENT_TASK_EXECUTION_START = "TaskExecutionStart";
    public static final String INTENT_TASK_EXECUTION_TITLE = "TaskExecutionTitle";

    //TODO: using separate thread
    @Override
    public boolean onStartJob(JobParameters params) {
        String taskFileName = params.getExtras().getString(Util.INTENT_TASK_FILE_NAME,
                "");
        if ("".equals(taskFileName)) {
            return false;
        }
        int taskUniqueLocation = params.getExtras().getInt(Util.INTENT_TASK_UNIQUE_POSITION,
                -1);
        if (taskUniqueLocation == -1) {
            return false;
        }
        Long taskExecutionStart = params.getExtras().getLong(INTENT_TASK_EXECUTION_START,
                -1);
        if (taskExecutionStart == -1) {
            return false;
        }
        String taskTitle = params.getExtras().getString(INTENT_TASK_EXECUTION_TITLE,
                "");
        if ("".equals(taskTitle)) {
            return false;
        }
        Util.createUpdateTaskNotification(getApplicationContext(),
                taskUniqueLocation, taskFileName, taskExecutionStart,
                true, taskTitle);
        Util.scheduleUpdatingTaskNotificationJobServiceStartingFromL(
                getApplicationContext(), taskUniqueLocation,
                taskFileName, taskExecutionStart, taskTitle);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}