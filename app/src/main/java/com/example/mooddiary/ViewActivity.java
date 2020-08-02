package com.example.mooddiary;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;
import java.io.File;

/**
 * This is an activity where user views details of a mood event from friend
 */
public class ViewActivity extends AppCompatActivity {

    private final int VIEW_TO_ADD_EDIT_REQUEST = 5;

    private TextView viewTimeText;
    private TextView viewDateText;
    private TextView viewReasonText;
    private TextView viewLocationText;
    private TextView viewMoodTypeText;
    private ImageView viewMoodTypeImage;
    private TextView viewSocialSituationText;
    private ImageView viewPhotoImage;
    private Button viewEditButton;
    private MoodEvent moodEvent;
    private MoodEvent editedMoodEvent;
    private ProgressBar viewDownloadingProgress;
    private int position;
    private boolean ifEdited = false;
    private boolean photoChangeFlag;

    /**
     * This creates the view of details of a mood event from friend.
     * @param savedInstanceState
     *      If the activity is being re-initialized after previously being shut down
     *      then this Bundle contains the data it most recently supplied in.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        viewDateText = findViewById(R.id.view_date_text);
        viewTimeText = findViewById(R.id.view_time_text);
        viewSocialSituationText = findViewById(R.id.view_social_situation_text);
        viewLocationText = findViewById(R.id.view_location_text);
        viewMoodTypeText = findViewById(R.id.view_mood_type_text);
        viewMoodTypeImage = findViewById(R.id.view_mood_type_image);
        viewReasonText = findViewById(R.id.view_reason_text);
        viewPhotoImage = findViewById(R.id.view_photo_image);
        viewEditButton = findViewById(R.id.view_edit_button);
        viewDownloadingProgress = findViewById(R.id.view_downloading_progress);

        Intent intent = getIntent();

        moodEvent = (MoodEvent) intent.getSerializableExtra("moodEvent");
        editedMoodEvent = (MoodEvent) intent.getSerializableExtra("moodEvent");
        position = intent.getIntExtra("moodEvent_index", 0);

        viewDateText.setText(editedMoodEvent.getDate());
        viewTimeText.setText(editedMoodEvent.getTime());
        viewReasonText.setText((editedMoodEvent.getReason()));
        viewMoodTypeText.setText(editedMoodEvent.getMood().getMood());
        viewMoodTypeImage.setImageResource(editedMoodEvent.getMood().getMoodImage());
        viewMoodTypeText.setTextColor(Color.parseColor(editedMoodEvent.getMood().getColor()));
        viewLocationText.setText(editedMoodEvent.getLocation());
        viewSocialSituationText.setText(editedMoodEvent.getSocialSituation());

        photoChangeFlag = false;

        if (!editedMoodEvent.getPhoto().equals("")) {
            StorageReference imageRef = Database.storageRef.child(editedMoodEvent.getPhoto());
            try{
                final File tempFile = File.createTempFile("tempPhoto","png");
                imageRef.getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        if(!photoChangeFlag) {
                            viewDownloadingProgress.setVisibility(View.INVISIBLE);
                            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                            viewPhotoImage.setImageBitmap(bitmap);
                        }
                    }
                });
            } catch (Exception e) {}
//            Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir("photo") + "/" + editedMoodEvent.getPhoto());
//            viewPhotoImage.setImageBitmap(bitmap);
        } else {
            viewDownloadingProgress.setVisibility(View.INVISIBLE);
        }

        viewEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_edit = new Intent(ViewActivity.this, AddMoodEventActivity.class);
                intent_edit.putExtra("mood_event_edit", editedMoodEvent);
                startActivityForResult(intent_edit, VIEW_TO_ADD_EDIT_REQUEST);
            }
        });

    }

    /**
     * This deals with the data requested from other activities.
     * @param requestCode
     *      This is originally supplied to startActivityForResult(), allowing to identify who this result came from.
     * @param resultCode
     *      This is returned by the child activity through its setResult().
     * @param data
     *       This is an intent returning result data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case VIEW_TO_ADD_EDIT_REQUEST:
                if (resultCode == RESULT_OK) {
                    ifEdited = true;
                    editedMoodEvent = (MoodEvent) data.getSerializableExtra("edited_mood_event");
                    viewDateText.setText(editedMoodEvent.getDate());
                    viewTimeText.setText(editedMoodEvent.getTime());
                    viewReasonText.setText((editedMoodEvent.getReason()));
                    viewMoodTypeText.setText(editedMoodEvent.getMood().getMood());
                    viewMoodTypeText.setTextColor(Color.parseColor(editedMoodEvent.getMood().getColor()));
                    viewMoodTypeImage.setImageResource(editedMoodEvent.getMood().getMoodImage());
                    viewLocationText.setText(editedMoodEvent.getLocation());
                    viewSocialSituationText.setText(editedMoodEvent.getSocialSituation());
                    if (!editedMoodEvent.getPhoto().equals("")) {
                        if(!moodEvent.getPhoto().equals(editedMoodEvent.getPhoto())) {
                            viewDownloadingProgress.setVisibility(View.INVISIBLE);
                            photoChangeFlag = true;
                        }
                        Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir("photo") + "/" + editedMoodEvent.getPhoto());
                        viewPhotoImage.setImageBitmap(bitmap);
                    } else {
                        viewDownloadingProgress.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            default:
        }
    }

    /**
     * This sends a boolean indicating if edited, original mood event and new mood event
     * back to Home Fragment.
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent = new Intent();
        intent.putExtra("if_edited", ifEdited);
        intent.putExtra("original_mood_event", moodEvent);
        intent.putExtra("edited_mood_event_return", editedMoodEvent);
        setResult(RESULT_OK, intent);
        finish();
    }
}
