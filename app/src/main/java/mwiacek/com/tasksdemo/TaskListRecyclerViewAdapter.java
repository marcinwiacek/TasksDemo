package mwiacek.com.tasksdemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter for taskListRecyclerView. It's supporting tasks of type
 * {@link TaskListRecyclerViewAdapter#mSupportedTaskStatus}
 * from {@link TaskListRecyclerViewAdapter#mTasks}
 */
class TaskListRecyclerViewAdapter
        extends RecyclerView.Adapter<TaskListRecyclerViewAdapter.TaskListRecyclerViewHolder> {
    private Context mContext;

    // DataSet and information which entries from DataSet are supported.
    private ArrayList<Task> mTasks;
    private Task.TaskStatus mSupportedTaskStatus;

    // Callback used after clicking on entry
    private OnItemClicked mOnClick;

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    /**
     * Setting callback for handling click on single task list entry
     * (can be used for starting edit action)
     */
    void setOnClick(OnItemClicked onClick) {
        this.mOnClick = onClick;
    }

    TaskListRecyclerViewAdapter(Context context, ArrayList<Task> tasks) {
        mContext = context;
        mTasks = tasks;

        setSupportedTaskStatus(Task.TaskStatus.NOT_STARTED);
    }

    /**
     * Specify which tasks from {@link TaskListRecyclerViewAdapter#mTasks} are selected
     */
    void setSupportedTaskStatus(Task.TaskStatus supportedTaskStatus) {
        this.mSupportedTaskStatus = supportedTaskStatus;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskListRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new TaskListRecyclerViewHolder(
                LayoutInflater.from(viewGroup.getContext()).inflate(
                        R.layout.task_list_item, viewGroup, false));
    }

    /**
     * Bind task Name (first layout line) and
     * (task Description or information about task execution len) (second layout line)
     */
    @Override
    public void onBindViewHolder(@NonNull TaskListRecyclerViewHolder viewHolder, int position) {
        Task task = (Task) getItem(position);
        viewHolder.taskTitleTextView.setText(task.getName());
        viewHolder.taskDescriptionTextView.setText(
                task.getTaskStatus() == Task.TaskStatus.COMPLETED ?
                        String.format(mContext.getResources().getString(
                                R.string.task_list_entry_time_info),
                                task.getTaskExecutionLengthInMilis()) :
                        task.getDescription());
    }

    /**
     * Get number of items of supported task type
     */
    @Override
    public int getItemCount() {
        int retSize = 0;
        for (Task t : mTasks) {
            if (t.getTaskStatus() == mSupportedTaskStatus) {
                retSize++;
            }
        }
        return retSize;
    }

    /**
     * Get task of supported type
     * @param position - task position (numerated from 0)
     * @return - task
     */
    Object getItem(int position) {
        int pos = -1;
        for (Task t : mTasks) {
            if (t.getTaskStatus() == mSupportedTaskStatus) {
                pos++;
            }
            if (pos == position) {
                return t;
            }
        }
        return null;
    }

    class TaskListRecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitleTextView;
        TextView taskDescriptionTextView;

        TaskListRecyclerViewHolder(View view) {
            super(view);

            taskTitleTextView = view.findViewById(R.id.taskTitle);
            taskDescriptionTextView = view.findViewById(R.id.taskDescription);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClick.onItemClick(getAdapterPosition());
                }
            });
        }
    }
}
