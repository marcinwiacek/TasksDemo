package mwiacek.com.tasksdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Main application class showing tasks lists and button for adding tasks.
 * We have one always available broadcast receiver here and one broadcast
 * receiver available only when we need to update something in the interface.
 */
public class MainActivity extends AppCompatActivity
        implements TaskListRecyclerViewAdapter.OnItemClicked {
    /**
     * Action for intent indicating, that new task was created
     * (after receiving it in the broadcast receiver
     * we update tasks list and RecyclerView).
     * Needs INTENT_TASK_FILE_NAME in the intent.
     */
    public static final String ACTION_ADD_NEW_TASK
            = "com.mwiacek.tasksdemo.ACTION_ADD_NEW_TASK";

    /**
     * Action for intent indicating, that new task was created
     * (after receiving it in the broadcast receiver
     * we update tasks and RecyclerView).
     * Needs INTENT_TASK_FILE_NAME & INTENT_TASK_UNIQUE_POSITION in the intent.
     */
    public static final String ACTION_EDIT_TASK
            = "com.mwiacek.tasksdemo.ACTION_EDIT_TASK";

    /**
     * Action for intent indicating, that we should show this activity
     * in the Pending tab. Doesn't need any intent params.
     * Received by broadcast receiver, which is always available
     */
    public static final String ACTION_OPEN_IN_PENDING
            = "com.mwiacek.tasksdemo.ACTION_OPEN_IN_PENDING";

    // dataSet
    private ArrayList<Task> mTasks;

    // Broadcast receiver working only when we need to update interface / dataSet.
    private BroadcastReceiver mBroadcastReceiver;

    private TaskListRecyclerViewAdapter mTaskListAdapter;
    private BottomNavigationView mTaskListBottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        // Find all interface elements
        mTaskListBottomNavigationView = findViewById(R.id.taskListBottomNavigationView);
        RecyclerView recyclerView = findViewById(R.id.taskListRecyclerView);
        FloatingActionButton taskAddFloatingActionButton = findViewById(R.id.taskAddFloatingActionButton);

        Util.createNotificationChannelStartingFromO(this);

        mTasks = new ArrayList<Task>();
        // Read all tasks from internal storage
        File[] files = getFilesDir().listFiles();
        Arrays.sort(files);
        Task t;
        for (File file : files) {
            t = Util.readTaskFromInternalStorage(this, file.getName());
            if (t != null) {
                t.setUniqueID(mTasks.size());
                mTasks.add(t);
            }
        }

        // BroadcastReceiver for ACTION_EDIT_TASK and ACTION_ADD_NEW_TASK
        registerBroadcastReceiverForAddEditTask();

        mTaskListAdapter = new TaskListRecyclerViewAdapter(this, mTasks);
        mTaskListAdapter.setOnClick(this);

        recyclerView.setAdapter(mTaskListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        setSwipeForRecyclerView(recyclerView);

        taskAddFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creating new task
                Intent intent = new Intent(getApplicationContext(),
                        AddEditTaskActivity.class);
                intent.setAction(ACTION_ADD_NEW_TASK);
                startActivity(intent);
            }
        });

        // Switching between tabs
        mTaskListBottomNavigationView.setOnNavigationItemSelectedListener(
                mOnNavigationItemSelectedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent == null || intent.getAction() == null ||
                !intent.getAction().equals(ACTION_OPEN_IN_PENDING)) {
            return;
        }
        // Switch to first tab with ACTION_OPEN_IN_PENDING intent
        mTaskListAdapter.setSupportedTaskStatus(Task.TaskStatus.NOT_STARTED);
        mTaskListBottomNavigationView.setSelectedItemId(mTaskListBottomNavigationView
                .getMenu().getItem(0).getItemId());
    }

    /**
     * Callback from TaskListRecyclerViewAdapter
     * Executed after clicking in the RecyclerView
     */
    @Override
    public void onItemClick(int position) {
        // Running AddEditTaskActivity with parameters in the Intent
        int uniquePosition = ((Task) mTaskListAdapter.getItem(position)).getUniqueID();
        Intent intent = new Intent(getApplicationContext(), AddEditTaskActivity.class)
                .setAction(ACTION_EDIT_TASK)
                .putExtra(Util.INTENT_TASK_UNIQUE_POSITION, uniquePosition)
                .putExtra(Util.INTENT_TASK_FILE_NAME,
                        mTasks.get(uniquePosition).getFileName());
        startActivity(intent);
    }

    private void setSwipeForRecyclerView(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        Task t = (Task) mTaskListAdapter
                                .getItem(viewHolder.getAdapterPosition());
                        // For completed tasks: any swipe starts execution
                        // For pending: only right swipe starts execution
                        if (t.getTaskStatus() == Task.TaskStatus.COMPLETED ||
                                direction == ItemTouchHelper.RIGHT) {
                            t.StartTask();
                        } else if (direction == ItemTouchHelper.LEFT) {
                            t.StartTask(60000);
                            // Show info for user
                            Toast.makeText(getApplicationContext(),
                                    String.format(
                                            getString(R.string.main_activity_snooze_task_message),
                                            t.getName()),
                                    Toast.LENGTH_SHORT).show();
                        }
                        t.saveToInternalStorage(getApplicationContext());
                        t.createUpdateNotification(getApplicationContext());
                        mTaskListAdapter.notifyDataSetChanged();
                    }

                    /**
                     * Create color background during swipe
                     */
                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                            RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState,
                                            boolean isCurrentlyActive) {
                        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                            return;
                        }
                        //TODO: Showing startTaskImageView and snoozeTaskImageView from layout
                        Paint p = new Paint();
                        p.setColor(Color.GREEN);
                        if (dX > 0) {
                            c.drawRect((float) viewHolder.itemView.getLeft(),
                                    (float) viewHolder.itemView.getTop(), dX,
                                    (float) viewHolder.itemView.getBottom(), p);
                        } else {
                            if (mTaskListBottomNavigationView.getSelectedItemId()
                                    == R.id.navigation_pending) {
                                p.setColor(Color.YELLOW);
                            }
                            c.drawRect((float) viewHolder.itemView.getRight() + dX,
                                    (float) viewHolder.itemView.getTop(),
                                    (float) viewHolder.itemView.getRight(),
                                    (float) viewHolder.itemView.getBottom(), p);
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Register broadcast receiver for ACTION_ADD_NEW_TASK and ACTION_EDIT_TASKS intents
     */
    private void registerBroadcastReceiverForAddEditTask() {
        class TaskBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    return;
                }
                if (!intent.getAction().equals(ACTION_ADD_NEW_TASK) &&
                        !intent.getAction().equals(ACTION_EDIT_TASK)) {
                    return;
                }
                String fileName = intent.getStringExtra(Util.INTENT_TASK_FILE_NAME);
                if (fileName == null) {
                    return;
                }
                Task t = Util.readTaskFromInternalStorage(context, fileName);
                if (t == null) {
                    return;
                }
                if (intent.getAction().equals(ACTION_ADD_NEW_TASK)) {
                    t.setUniqueID(mTasks.size());
                    mTasks.add(t);
                } else { //ACTION_EDIT_TASK
                    int uniquePosition = intent.getIntExtra(
                            Util.INTENT_TASK_UNIQUE_POSITION, -1);
                    if (uniquePosition == -1) {
                        return;
                    }
                    mTasks.set(uniquePosition, t);
                    if (t.getTaskStatus() != Task.TaskStatus.COMPLETED) {
                        t.createUpdateNotification(context);
                    }
                }
                mTaskListAdapter.notifyDataSetChanged();
            }
        }

        mBroadcastReceiver = new TaskBroadcastReceiver();
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_ADD_NEW_TASK));
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_EDIT_TASK));
    }

    /* Switching BottomNavigationView page */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            mTaskListAdapter.setSupportedTaskStatus(
                    item.getItemId() == R.id.navigation_pending ?
                            Task.TaskStatus.NOT_STARTED : Task.TaskStatus.COMPLETED);
            return true;
        }
    };
}
