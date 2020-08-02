package com.example.mooddiary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.maps.model.LatLng;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * This is an activity where user adds mood event
 */
public class AddMoodEventActivity extends AppCompatActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, AdapterView.OnItemSelectedListener {
    private static final SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss");

    private Button addButton;
    private ImageButton cancelButton;
    private ImageButton photoFromCameraButton;
    private ImageButton photoFromAlbumButton;
    private TextView dateText;
    private TextView timeText;
    private Spinner moodSpinner;
    private Spinner socialSituationSpinner;
    private AutocompleteSupportFragment locationAutoComplete;
    private EditText reasonEdit;
    private ImageView photoImage;
    private ProgressBar loadingImage;
    private int moodNamePosition = -1 ; // if moodevent is null

    private boolean successFlag;
    private boolean photoChangeFlag;

    // record the social situation which is chosen from social situation spinner
    private String socialSituationSpinnerResult = "alone";
    private String moodSpinnerResult = "happy";
    private String dateResult = "";
    private String timeResult = "";
    private String preciseTimeResult = "";
    private String locationResult = "";
    private LatLng locationLatLngResult = new LatLng(100,200);
    private String reasonResult = "";
    private String photoResult = "";

    private static final int TAKE_PHOTO = 1;
    private static final  int CHOOSE_PHOTO = 2;

    private Uri imageUri;

    private boolean two_selected = false;
    private ArrayList<MoodBean> mData = null;
    private MsAdapter msAdapter = null;

    private boolean isFromView = false;
    private MoodEvent moodEventFromView;

    private Calendar current;

    private String placesAPIKey;


    /**
     * This creates the view of add activity
     * @param savedInstanceState
     *      If the activity is being re-initialized after previously being shut down
     *      then this Bundle contains the data it most recently supplied in.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood_event);

        // find all views
        addButton = findViewById(R.id.add_mood_event_button);
        cancelButton = findViewById(R.id.cancel_add_mood_event_button);
        dateText = findViewById(R.id.add_date_text);
        timeText = findViewById(R.id.add_time_text);
        moodSpinner = findViewById(R.id.add_mood_spinner);
        socialSituationSpinner = findViewById(R.id.add_social_situation_spinner);
        locationAutoComplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.add_location_autoComplete);
        reasonEdit = findViewById(R.id.add_textual_reason_edit);
        photoImage = findViewById(R.id.add_image_reason);
        photoFromCameraButton = findViewById(R.id.add_photo_camera);
        photoFromAlbumButton = findViewById(R.id.add_photo_album);
        loadingImage = findViewById(R.id.add_downloading_progress);

        photoChangeFlag = false;
        placesAPIKey = getString(R.string.google_maps_key);
        if(!Places.isInitialized()) { Places.initialize(getApplicationContext(), placesAPIKey); }
        locationAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        locationAutoComplete.setHint(getString(R.string.chooseLocation));

        /*
          get intent from either Main Activity(Home Fragment) or View Activity
         */
        Intent intentFrom = getIntent();
        /*
          check where it comes from
         */
        isFromView = !intentFrom.getBooleanExtra("action_add", false);

        if (isFromView) {
            /*
              from View Activity
             */

            moodEventFromView = (MoodEvent) intentFrom.getExtras().getSerializable("mood_event_edit");

            // initialize return results in Add Activity
            dateResult = moodEventFromView.getDate();
            timeResult = moodEventFromView.getTime();
            preciseTimeResult = moodEventFromView.getPreciseTime();
            moodSpinnerResult = moodEventFromView.getMood().getMood();
            socialSituationSpinnerResult = moodEventFromView.getSocialSituation();
            locationResult = moodEventFromView.getLocation();
            locationLatLngResult = new LatLng(moodEventFromView.getLatitude(), moodEventFromView.getLongitude());
            reasonResult = moodEventFromView.getReason();
            photoResult = moodEventFromView.getPhoto();
            loadingImage.setVisibility(View.VISIBLE);
            // initialize views in Add Activity
            dateText.setText(moodEventFromView.getDate());
            timeText.setText(moodEventFromView.getTime());
            locationAutoComplete.setText(moodEventFromView.getLocation());
            reasonEdit.setText(moodEventFromView.getReason());
            String socialSituation = moodEventFromView.getSocialSituation();
            Mood mood = moodEventFromView.getMood();
            if (!moodEventFromView.getPhoto().equals("")) {
                StorageReference imageRef = Database.storageRef.child(moodEventFromView.getPhoto());
                try{
                    final File tempFile = File.createTempFile("tempPhoto","png");
                    imageRef.getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            if(!photoChangeFlag) {
                                loadingImage.setVisibility(View.INVISIBLE);
                                Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                                photoImage.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (Exception e) {}

//                Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir("photo") + "/" + moodEventFromView.getPhoto());
//                photoImage.setImageBitmap(bitmap);
            } else {
                loadingImage.setVisibility(View.INVISIBLE);
            }

            String moodName = mood.getMood();
            switch (moodName) {
                case "happy":
                    moodNamePosition = 0;
                    break;
                case "angry":
                    moodNamePosition = 1;
                    break;
                case "content":
                    moodNamePosition = 2;
                    break;
                case "meh":
                    moodNamePosition = 3;
                    break;
                case "sad":
                    moodNamePosition = 4;
                    break;
                case "stressed":
                    moodNamePosition = 5;
                    break;
                default:
                    break;
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.SocialSituation, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            socialSituationSpinner.setAdapter(adapter);
            if (socialSituation != null) {
                int spinnerPosition = adapter.getPosition(socialSituation);
                socialSituationSpinner.setSelection(spinnerPosition);
            }

        }


        // set Date and Time through time and date picker
        dateText.setOnClickListener(this);
        timeText.setOnClickListener(this);

        // choose photo from camera
        photoFromCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File outputImage = new File(getFilesDir(),"out_image.jpg");

                try{
                    if (outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e){
                    e.printStackTrace();
                }

                if (Build.VERSION.SDK_INT >= 24){
                    imageUri = FileProvider.getUriForFile(AddMoodEventActivity.this,"com.example.mooddiary.fileprovider",outputImage);
                }else{
                    imageUri = Uri.fromFile(outputImage);
                }


                // start camera
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
            }
        });

        // choose photo from album
        photoFromAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(AddMoodEventActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(AddMoodEventActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else {
                    openAlbum();
                }
            }
        });

        locationAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                locationResult = place.getName();
                locationAutoComplete.setText(locationResult);
                locationLatLngResult = place.getLatLng();
            }

            @Override
            public void onError(@NonNull Status status) {
            }
        });

        locationAutoComplete.getView().findViewById(R.id.places_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        locationResult = "";
                        locationLatLngResult = new LatLng(100,200);
                        locationAutoComplete.setText("");
                    }
                });

        socialSituationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                String[] socialsituation = getResources().getStringArray(R.array.SocialSituation);
                socialSituationSpinnerResult = socialsituation[pos];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        // initialize mood spinner
        mData = new ArrayList<MoodBean>();
        bindViews();
        if (moodNamePosition != -1) {
            moodSpinner.setSelection(moodNamePosition);
        }

        // finish edit/add mood event
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reasonResult = reasonEdit.getText().toString();
                successFlag = true;
                current = Calendar.getInstance();
                String [] checkNumberOfReasonWords = reasonResult.split(" ");
                if (checkNumberOfReasonWords.length > 3 || reasonResult.length() > 20) {
                    reasonEdit.setError("reason no more than 20 characters or 3 words");
                    successFlag = false;
                }

                if (dateResult.equals("")) {
                    dateText.setError("This field is required");
                    successFlag = false;
                }

                if (timeResult.equals("")) {
                    timeText.setError("This field is required");
                    successFlag = false;
                }

                try{
                    if(photoChangeFlag) {
                        Uri file = Uri.fromFile(new File(getExternalFilesDir("photo") + "/" + photoResult));
                        StorageReference imageRef = Database.storageRef.child(photoResult);
                        UploadTask uploadTask = imageRef.putFile(file);

                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                successFlag = false;
                            }
                        });
                    }
                }catch(Exception e) {}

                if(!successFlag) { return; }

                MoodEvent moodEventResult = new MoodEvent(moodSpinnerResult, dateResult, timeResult,
                        preciseTimeResult, socialSituationSpinnerResult, locationResult,
                        locationLatLngResult.latitude, locationLatLngResult.longitude,
                        reasonResult, photoResult, LoginActivity.userName);

                if(moodEventResult.getNumericDate() > current.getTimeInMillis()) {
                    Toast.makeText(AddMoodEventActivity.this, "You cannot choose future time",Toast.LENGTH_LONG).show();
                    return;
                }

                if (isFromView) {
                    Intent intent = new Intent();
                    intent.putExtra("edited_mood_event", moodEventResult);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("added_mood_event", moodEventResult);
                    setResult(RESULT_OK, intent);
                    finish();
                }


            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
      This deals with photo part
     */
    // function used in choose photo from album
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
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
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    photoImage.setImageURI(imageUri);
                    Bitmap bitmap = ((BitmapDrawable)photoImage.getDrawable()).getBitmap();
                    photoResult = LoginActivity.userName + "_" + simpleDate.format(new Date()) + ".png";
                    try {
                        File addPhoto = new File(getExternalFilesDir("photo").toString() + "/" + photoResult);
                        FileOutputStream out = new FileOutputStream(addPhoto);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                        loadingImage.setVisibility(View.INVISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    photoChangeFlag = true;
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);

                    } else {
                        handleImageBeforeKitKat(data);
                    }
                    photoChangeFlag = true;
                    loadingImage.setVisibility(View.INVISIBLE);
                }
                break;
            default:
                break;
        }
    }

    // This is used for date and time picker
    @Override
    public void onClick(View view) {
        if (view.getId()==R.id.add_date_text){

            Calendar calendar=Calendar.getInstance();
            DatePickerDialog dialog =
                    new DatePickerDialog(this,this,calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        }
        if (view.getId()==R.id.add_time_text){
            Calendar calendar=Calendar.getInstance();
            TimePickerDialog dialog =
                    new TimePickerDialog(this,this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            dialog.show();
        }
    }

    /**
     * This displays the data from date picker
     * @param datePicker
     *      get the data from date picker
     * @param year
     *      get the year from date picker
     * @param month
     *      get the month from date picker
     * @param dayOfMonth
     *      get the day from date picker
     */
    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {


        String desc=String.format("%04d/%02d/%02d",year,month+1,dayOfMonth);
        dateText.setText(desc);
        dateResult = desc;


    }
    /**
     * This displays the data from time picker
     * @param timePicker
     *      This is the date from time picker
     * @param hourOfDay
     *      This is the hour from time picker
     * @param minute
     *      This is the minute from time picker
     */
    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        Calendar calendar=Calendar.getInstance();
            // it's after current
        String desc_precise=String.format("%02d:%02d:%02d",hourOfDay,minute,calendar.get(Calendar.SECOND));
        String desc=String.format("%02d:%02d",hourOfDay,minute);
        timeText.setText(desc);
        preciseTimeResult = desc_precise;
        timeResult = desc;



    }
    /**
     * get require permission from system to deal with opening album
     * @param requestCode
     *      This requestCode from openAlbum
     *
     * @param permissions
     *      This permission from user
     *
     * @param grantResults
     *      This grantResults from user
     *
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else {
                    Toast.makeText(AddMoodEventActivity.this,"YOU DENIED THE PERMISSION",Toast.LENGTH_SHORT).show();
                }
                break;
            default:

        }
    }
    /**
     * This displays the image
     * @param imagePath
     *      This is the image path from album
     */

    private void displayImage(String imagePath){
        if(imagePath != null){
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            photoImage.setImageBitmap(bitmap);
            photoResult = LoginActivity.userName + "_" + simpleDate.format(new Date()) + ".png";
            File editPhoto = new File(getExternalFilesDir("photo").toString() + "/" + photoResult);
            try {
                FileOutputStream out = new FileOutputStream(editPhoto);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }else {
//            Toast.makeText(this,"failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * handleImageOnKitKat
     * @param data
     *      This is data from picture
     */

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {

            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {

            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {

            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    /**
     * handleImageOnKitKat for version lower than 4.0
     * @param data
     *      This is data from picture
     */
    private void handleImageBeforeKitKat(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);

    }

    /**
     * This is to get ImagePath through uri
     * @param uri
     *      This is the uri of Image
     * @param selection
     *      This is the Image you select
     * @return
     *      return the Image path
     */

    private String getImagePath(Uri uri, String selection){
        String path = null;
        // get image path through Uri and selection

        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if(cursor !=null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * This is to initialize mood spinner
     */
    private void bindViews() {
        moodSpinner  = findViewById(R.id.add_mood_spinner);

        mData.add(new MoodBean(R.drawable.happy,"happy"));
        mData.add(new MoodBean(R.drawable.angry,"angry"));
        mData.add(new MoodBean(R.drawable.content,"content"));
        mData.add(new MoodBean(R.drawable.meh,"meh"));
        mData.add(new MoodBean(R.drawable.sad,"sad"));
        mData.add(new MoodBean(R.drawable.stressed,"stressed"));

        msAdapter = new MsAdapter<MoodBean>(mData,R.layout.spinner_item) {
            @Override
            public void bindView(MsAdapter.ViewHolder holder, MoodBean obj) {
                holder.setImageResource(R.id.icon,obj.getIcon());
                holder.setText(R.id.name, obj.getName());
            }
        };
        moodSpinner.setAdapter(msAdapter);
        moodSpinner.setOnItemSelectedListener(this);
    }

    /**
     * This to get the mood which you select
     * @param parent
     *      the mood you select
     * @param view
     *      the view you selct
     * @param position
     *      the position of your view
     * @param id
     *      the if of the view
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()){

            case R.id.add_mood_spinner:
                if(two_selected){
                    TextView txt_name = view.findViewById(R.id.name);
                    moodSpinnerResult = txt_name.getText().toString();

                }else
                    two_selected = true;
                break;
        }
    }

    /**
     * Callback method to be invoked when the selection disappears from this view.
     * This deals with nothing selected from mood spinner.
     * @param parent
     *      The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * This detects the change of mood through spinner
     * @param hasCapture
     *      This indicates if there is a change
     */
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
