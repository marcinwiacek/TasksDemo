package mwiacek.com.tasksdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;

/**
 * Activity for creating / editing new tasks
 */
public class AddEditTaskActivity extends AppCompatActivity
        implements TaskKeywordRecyclerViewAdapter.OnItemEditedDeleted {
    // Code for identifying result from file picker activity
    private static final int FILE_PICKER_REQUEST_CODE = 37;

    // Code for identifying result from Firebase Sign UI
    private static final int FIREBASE_SIGN_IN_REQUEST_CODE = 123;

    // edited task
    private Task mTask;

    // location of file for uploading into cloud
    private Uri mLocalFileForCloudStorage;

    private Button mSaveButton;
    private EditText mTaskDescriptionTextEdit;
    private EditText mTaskNameTextEdit;
    private TaskKeywordRecyclerViewAdapter mTaskKeywordListAdapter;
    private ProgressBar mFileUploadProgressBar;
    private RelativeLayout mAddEditTaskRelativeLayout;
    private TextView mFilePickerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        mSaveButton = findViewById(R.id.saveButton);
        mTaskNameTextEdit = findViewById(R.id.taskNameEditText);
        mTaskDescriptionTextEdit = findViewById(R.id.taskDescriptionEditText);
        mFileUploadProgressBar = findViewById(R.id.uploadFileProgressBar);
        mAddEditTaskRelativeLayout = findViewById(R.id.addEditLayout);
        mFilePickerTextView = findViewById(R.id.taskFilePickerTextView);
        RecyclerView taskKeywordListRecyclerView =
                findViewById(R.id.taskKeywordListRecyclerView);
        Button cancelButton = findViewById(R.id.cancelButton);
        FloatingActionButton taskKeywordAddFloatingActionButton =
                findViewById(R.id.taskKeywordAddFloatingActionButton);

        mFilePickerTextView.setWidth(mTaskNameTextEdit.getWidth());

        mTask = null;
        mLocalFileForCloudStorage = Uri.EMPTY;

        // Parse input data from intent
        Intent inputIntent = getIntent();
        if (inputIntent != null && MainActivity.ACTION_EDIT_TASK.equals(inputIntent.getAction())) {
            String taskFileName = inputIntent.getStringExtra(Util.INTENT_TASK_FILE_NAME);
            if (!"".equals(taskFileName)) {
                mTask = Util.readTaskFromInternalStorage(getApplicationContext(),
                        taskFileName);
                if (mTask != null) {
                    setTitle(getString(R.string.add_edit_activity_title_edit));
                    mTaskNameTextEdit.setText(mTask.getName());
                    mTaskDescriptionTextEdit.setText(mTask.getDescription());
                }
            }
        }
        // We assume that all incorrect intent parameters or
        // incorrect reading task from file lead to new task
        if (mTask == null) {
            mTask = new Task();
            setTitle(getString(R.string.add_edit_activity_title_new));
        }

        mTaskKeywordListAdapter = new TaskKeywordRecyclerViewAdapter(mTask.getKeywords());
        mTaskKeywordListAdapter.setOnEditDelete(this);

        taskKeywordListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskKeywordListRecyclerView.setAdapter(mTaskKeywordListAdapter);

        mTaskNameTextEdit.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence c, int start, int before, int count) {
                mSaveButton.setEnabled(c.toString().length() != 0);
            }

            public void beforeTextChanged(CharSequence c, int start, int count, int after) {
            }

            public void afterTextChanged(Editable c) {
            }
        });

        mSaveButton.setEnabled(mTaskNameTextEdit.getText().toString().length() != 0);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return when filename was not selected
                if (mLocalFileForCloudStorage == null ||
                        Uri.EMPTY.equals(mLocalFileForCloudStorage)) {
                    addUpdateTaskOnExit();
                    finish();
                    return;
                }

                // User is logged in and we can upload file
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    uploadFileToFireBase();
                    return;
                }

                // Authenticate user and do file upload
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                                        new AuthUI.IdpConfig.PhoneBuilder().build()))
                                .build(),
                        FIREBASE_SIGN_IN_REQUEST_CODE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        taskKeywordAddFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask.getKeywords().add("");
                mTaskKeywordListAdapter.notifyDataSetChanged();
            }
        });

        mFilePickerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, FILE_PICKER_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == FIREBASE_SIGN_IN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                uploadFileToFireBase();
                return;
            }
            Toast.makeText(getApplicationContext(),
                    getString(R.string.add_edit_activity_login_failed_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode != FILE_PICKER_REQUEST_CODE ||
                resultCode != Activity.RESULT_OK ||
                resultData == null) {
            return;
        }
        mLocalFileForCloudStorage = resultData.getData();
        if (mLocalFileForCloudStorage == null) {
            return;
        }
        mFilePickerTextView.setText(mLocalFileForCloudStorage.toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        AuthUI.getInstance().signOut(getApplicationContext());
    }

    private void addUpdateTaskOnExit() {
        // Update task with new values
        mTask.setName(mTaskNameTextEdit.getText().toString());
        mTask.setDescription(mTaskDescriptionTextEdit.getText().toString());

        Intent returnBroadcastIntent = new Intent();
        Intent inputIntent = getIntent();
        // Once again: we create new task when intent for activity
        // didn't have all correct data
        if (getString(R.string.add_edit_activity_title_edit).equals(getTitle())) {
            returnBroadcastIntent.setAction(MainActivity.ACTION_EDIT_TASK);
            returnBroadcastIntent.putExtra(Util.INTENT_TASK_UNIQUE_POSITION,
                    inputIntent.getIntExtra(Util.INTENT_TASK_UNIQUE_POSITION, -1));
        } else {
            returnBroadcastIntent.setAction(MainActivity.ACTION_ADD_NEW_TASK);
        }
        returnBroadcastIntent.putExtra(Util.INTENT_TASK_FILE_NAME,
                mTask.saveToInternalStorage(getApplicationContext()));

        //TODO: we should show some info when task was not saved correctly
        if (!"".equals(returnBroadcastIntent.getStringExtra(Util.INTENT_TASK_FILE_NAME))) {
            sendBroadcast(returnBroadcastIntent);
        }
    }

    private void uploadFileToFireBase() {
        // Disable (edit) controls and show progress indicator
        for (int i = 0; i < mAddEditTaskRelativeLayout.getChildCount(); i++) {
            mAddEditTaskRelativeLayout.getChildAt(i).setEnabled(false);
        }
        // TODO: disabling Keywords
        mFileUploadProgressBar.setVisibility(View.VISIBLE);
        mFileUploadProgressBar.setProgress(0);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("files/" +
                        mTask.getFileName() +
                        mLocalFileForCloudStorage.getLastPathSegment());
        UploadTask uploadTask = storageRef.putFile(mLocalFileForCloudStorage);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                mFileUploadProgressBar.setProgress((int) (100.0 *
                        taskSnapshot.getBytesTransferred() /
                        taskSnapshot.getTotalByteCount()));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Enable all controls and hide progress bar
                for (int i = 0; i < mAddEditTaskRelativeLayout.getChildCount(); i++) {
                    mAddEditTaskRelativeLayout.getChildAt(i).setEnabled(true);
                }
                // TODO: enabling Keywords
                mFileUploadProgressBar.setVisibility(View.GONE);

                Toast.makeText(getApplicationContext(),
                        getString(R.string.add_edit_activity_upload_file_failed_message),
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                addUpdateTaskOnExit();
                finish();
            }
        });
    }

    /**
     * Callback from TaskKeywordRecyclerViewAdapter
     */
    public void onItemEdit(int position, String newText) {
        mTask.getKeywords().set(position, newText);
    }

    /**
     * Callback from TaskKeywordRecyclerViewAdapter
     */
    public void onItemDelete(int position) {
        mTask.getKeywords().remove(position);
        mTaskKeywordListAdapter.notifyItemRemoved(position);
    }
}
