package mwiacek.com.tasksdemo;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;

public class TaskKeywordRecyclerViewAdapter
        extends RecyclerView.Adapter<TaskKeywordRecyclerViewAdapter.CustomViewHolder> {
    // dataSet
    private ArrayList<String> mKeywords;
    // Callback for editing and deleting entry
    private OnItemEditedDeleted mOnEditDelete;

    public interface OnItemEditedDeleted {
        void onItemEdit(int position, String newText);

        void onItemDelete(int position);
    }

    void setOnEditDelete(OnItemEditedDeleted onEditDelete) {
        this.mOnEditDelete = onEditDelete;
    }

    TaskKeywordRecyclerViewAdapter(ArrayList<String> keywords) {
        mKeywords = keywords;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CustomViewHolder(
                LayoutInflater.from(viewGroup.getContext()).inflate(
                        R.layout.task_keyword_list_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder viewHolder, int position) {
        viewHolder.taskKeywordEditText.setText(mKeywords.get(position));
    }

    @Override
    public int getItemCount() {
        return mKeywords.size();
    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        EditText taskKeywordEditText;

        CustomViewHolder(View view) {
            super(view);

            ImageView deleteImageView = view.findViewById(R.id.minusKeywordImageView);
            deleteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnEditDelete.onItemDelete(getAdapterPosition());
                }
            });

            taskKeywordEditText = view.findViewById(R.id.taskKeywordEditText);
            taskKeywordEditText.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence c, int start, int before, int count) {
                    mOnEditDelete.onItemEdit(getAdapterPosition(), c.toString());
                }

                public void beforeTextChanged(CharSequence c, int start, int count, int after) {
                }

                public void afterTextChanged(Editable c) {
                }
            });
        }
    }
}
