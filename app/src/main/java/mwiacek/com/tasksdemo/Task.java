package mwiacek.com.tasksdemo;

import android.app.job.JobScheduler;
import android.content.Context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class Task implements Serializable {
    public enum TaskStatus {
        NOT_STARTED, EXECUTING, COMPLETED
    }

    // Version of the Task structure for serialization
    static final long serialVersionUID = 43L;

    private ArrayList<String> mKeywords;
    private Date mStartTime;
    private int mUniqueID;
    private long mExecutionLengthInMilis;
    private String mDescription;
    private String mFileName;
    private String mName;
    private TaskStatus mStatus;

    Task() {
        mUniqueID = -1;
        mName = "";
        mDescription = "";
        mKeywords = new ArrayList<>();
        mStartTime = null;
        mExecutionLengthInMilis = 0;
        mFileName = "";
        mStatus = TaskStatus.NOT_STARTED;
    }

    TaskStatus getTaskStatus() {
        return mStatus;
    }

    ArrayList<String> getKeywords() {
        return mKeywords;
    }

    long getTaskExecutionLengthInMilis() {
        return mExecutionLengthInMilis;
    }

    String getFileName() {
        return mFileName;
    }

    String getName() {
        return mName;
    }

    String getDescription() {
        return mDescription;
    }

    int getUniqueID() {
        return mUniqueID;
    }

    void setFileName(String fName) {
        mFileName = fName;
    }

    void setName(String name) {
        mName = name;
    }

    void setDescription(String description) {
        mDescription = description;
    }

    void setUniqueID(int uniqueID) {
        mUniqueID = uniqueID;
    }

    void StartTask() {
        StartTask(0);
    }

    void StartTask(int postponeInMillis) {
        mStatus = TaskStatus.EXECUTING;
        mStartTime = new Date(Calendar.getInstance().getTimeInMillis() + postponeInMillis);
    }

    void CompleteTask() {
        if (mStatus != TaskStatus.EXECUTING) {
            return;
        }
        mExecutionLengthInMilis += (Calendar.getInstance().getTimeInMillis() -
                mStartTime.getTime());
        mStatus = TaskStatus.COMPLETED;
    }

    void createUpdateNotification(Context context) {
        if (getTaskStatus() == TaskStatus.COMPLETED ||
                (getTaskStatus() == TaskStatus.EXECUTING &&
                        Calendar.getInstance().getTimeInMillis() >= mStartTime.getTime())) {
            Util.createUpdateTaskNotification(context, getUniqueID(), getFileName(),
                    mStartTime.getTime(), (getTaskStatus() == TaskStatus.EXECUTING),
                    getName());
        }

        if (getTaskStatus() == TaskStatus.EXECUTING) {
            Util.scheduleUpdatingTaskNotificationJobServiceStartingFromL(context, getUniqueID(),
                    getFileName(), mStartTime.getTime(), getName());
        } else {
            // Stop updating notification for task
            JobScheduler jobScheduler =
                    (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(getUniqueID());
            }
        }
    }

    String saveToInternalStorage(Context context) {
        if (mFileName.equals("")) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            mFileName = sdf.format(new Date());
        }
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(
                    context.openFileOutput(mFileName, Context.MODE_PRIVATE));
            outputStream.writeObject(this);
            outputStream.close();
        } catch (Exception ignoreException) {
            return "";
        }
        return mFileName;
    }

    private void readObject(ObjectInputStream inputStream)
            throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
    }
}
