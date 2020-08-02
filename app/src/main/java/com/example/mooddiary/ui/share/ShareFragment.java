package com.example.mooddiary.ui.share;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mooddiary.Database;
import com.example.mooddiary.FollowAdapter;
import com.example.mooddiary.FollowerAdapter;
import com.example.mooddiary.LoginActivity;
import com.example.mooddiary.R;
import com.example.mooddiary.Request;
import com.example.mooddiary.RequestAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is ShareFragment showing a list of user's friends
 */
public class ShareFragment extends Fragment {

    private ListView shareFollowListView;
    private ListView shareRequestListView;
    private ListView shareFollowerListView;
    private RequestAdapter shareRequestAdapter;
    private FollowAdapter shareFollowAdapter;
    private FollowerAdapter shareFollowerAdapter;
    private ArrayList<String> shareFollowerList = new ArrayList<>(); // people follows user
    private ArrayList<String> shareFollowList = new ArrayList<>(); // user follows these people
    private ArrayList<Request> shareReceivedRequestList = new ArrayList<>(); // new request received by user

    /**
     * This creates the view for the user's friend list.
     * @param inflater
     *      This is a LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container
     *      This can be null. If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState
     *      If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return
     *      Return the view for the fragment UI
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_share, container, false);
        shareRequestListView = root.findViewById(R.id.share_new_request_list_view);
        shareFollowListView = root.findViewById(R.id.share_follow_list_view);
        shareFollowerListView = root.findViewById(R.id.share_follower_list_view);

        shareFollowAdapter = new FollowAdapter(getActivity(), R.layout.follow_list_item, shareFollowList);
        shareFollowerAdapter = new FollowerAdapter(getActivity(), R.layout.follower_list_item, shareFollowerList);
        shareRequestAdapter = new RequestAdapter(getActivity(), R.layout.request_list_item, shareReceivedRequestList);

        shareFollowListView.setAdapter(shareFollowAdapter);
        shareFollowerListView.setAdapter(shareFollowerAdapter);
        shareRequestListView.setAdapter(shareRequestAdapter);

        shareFollowListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final String friendUsername = (String) shareFollowListView.getItemAtPosition(i);
                final BottomSheetDialog followBottomSheetDialog = new BottomSheetDialog(getActivity());
                View sheetView = getActivity().getLayoutInflater().inflate(R.layout.follow_expand_bottom, null);
                followBottomSheetDialog.setContentView(sheetView);
                LinearLayout shareUnfollowLinearLayout = sheetView.findViewById(R.id.share_cancel_following);
                shareUnfollowLinearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // remove friend's name from user's following list
                        shareFollowList.remove(friendUsername);
                        DocumentReference userFollowRef = Database.getUserFollowList(LoginActivity.userName);
                        HashMap<String, Object> followData = new HashMap<>();
                        followData.put("FollowList", shareFollowList);
                        userFollowRef.set(followData);
                        // remove user's name from friend's follower list
                        DocumentReference friendFollowerRef = Database.getUserFollowerList(friendUsername);
                        friendFollowerRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null) {
                                        ArrayList<String> friendFollowerList = (ArrayList<String>) document.get("FollowerList");
                                        friendFollowerList.remove(LoginActivity.userName);
                                        HashMap<String, Object> friendFollowerData = new HashMap<>();
                                        friendFollowerData.put("FollowerList", friendFollowerList);
                                        Database.getUserFollowerList(friendUsername).set(friendFollowerData);
                                    }
                                }
                            }
                        });
                        followBottomSheetDialog.dismiss();
                    }
                });
                followBottomSheetDialog.show();
            }
        });

        shareFollowerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final String friendUsername = (String) shareFollowerListView.getItemAtPosition(i);
                final BottomSheetDialog followerBottomSheetDialog = new BottomSheetDialog(getActivity());
                View sheetView = getActivity().getLayoutInflater().inflate(R.layout.follower_expand_bottom, null);
                followerBottomSheetDialog.setContentView(sheetView);
                LinearLayout shareDeleteFollowerLinearlayout = sheetView.findViewById(R.id.share_delete_follower);
                shareDeleteFollowerLinearlayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // delete friend from user's follower list
                        shareFollowerList.remove(friendUsername);
                        DocumentReference userFollowerRef = Database.getUserFollowerList(LoginActivity.userName);
                        HashMap<String, Object> followerData = new HashMap<>();
                        followerData.put("FollowerList", shareFollowerList);
                        userFollowerRef.set(followerData);
                        // delete user from user's follow list
                        DocumentReference friendFollowRef = Database.getUserFollowList(friendUsername);
                        friendFollowRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document != null) {
                                        ArrayList<String> friendFollowList = (ArrayList<String>) document.get("FollowList");
                                        friendFollowList.remove(LoginActivity.userName);
                                        HashMap<String, Object> friendFollowData = new HashMap<>();
                                        friendFollowData.put("FollowList", friendFollowList);
                                        Database.getUserFollowList(friendUsername).set(friendFollowData);
                                    }
                                }
                            }
                        });
                        followerBottomSheetDialog.dismiss();
                    }
                });

                if (shareFollowList.contains(friendUsername)) {
                    TextView shareAlreadyFollowedText = sheetView.findViewById(R.id.share_already_followed_text);
                    shareAlreadyFollowedText.setVisibility(View.VISIBLE);
                } else {
                    LinearLayout shareFollowBackLinearLayout = sheetView.findViewById(R.id.share_follow_back);
                    shareFollowBackLinearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Request request = new Request(LoginActivity.userName, friendUsername);
                            Request.send(request);
                            followerBottomSheetDialog.dismiss();
                            Toast.makeText(getActivity(), "request sent", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                followerBottomSheetDialog.show();
            }
        });

        DocumentReference followRef = Database.getUserFollowList(LoginActivity.userName);
        followRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot == null) { return; }
                ArrayList<String> followList = (ArrayList<String>) documentSnapshot.get("FollowList");
                shareFollowList.clear();
                for (String follow : followList) {
                    shareFollowList.add(follow);
                }
                shareFollowAdapter.notifyDataSetChanged();
                setDynamicHeight(shareRequestListView);
                setDynamicHeight(shareFollowListView);
                setDynamicHeight(shareFollowerListView);
            }
        });

        DocumentReference followerRef = Database.getUserFollowerList(LoginActivity.userName);
        followerRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null) {
                    ArrayList<String> followerList = (ArrayList<String>) documentSnapshot.get("FollowerList");
                    shareFollowerList.clear();
                    for (String follower : followerList) {
                        shareFollowerList.add(follower);
                    }
                    shareFollowerAdapter.notifyDataSetChanged();
                    setDynamicHeight(shareRequestListView);
                    setDynamicHeight(shareFollowListView);
                    setDynamicHeight(shareFollowerListView);
                }
            }
        });

        Query receivedRequestListQuery = Database.getRequestListByReceiver(LoginActivity.userName);
        receivedRequestListQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if(queryDocumentSnapshots == null) { return; }
                shareReceivedRequestList.clear();
                for (QueryDocumentSnapshot document: queryDocumentSnapshots) {
                    final Request request = document.toObject(Request.class);
                    if (!request.getConfirmed()) {
                        shareReceivedRequestList.add(request);
                    } else {
                        if (!request.getDeclined()) {
                            shareFollowerList.add(request.getSender());
                            HashMap<String, Object> followerData = new HashMap<>();
                            followerData.put("FollowerList", shareFollowerList);
                            Database.getUserFollowerList(LoginActivity.userName).set(followerData);

                            DocumentReference senderFollowRef = Database.getUserFollowList(request.getSender());
                            senderFollowRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null) {
                                            ArrayList<String> senderFollowList = (ArrayList<String>) document.get("FollowList");
                                            senderFollowList.add(request.getReceiver());
                                            HashMap<String, Object> senderFollowData = new HashMap<>();
                                            senderFollowData.put("FollowList", senderFollowList);
                                            Database.getUserFollowList(request.getSender()).set(senderFollowData);
                                        }
                                    }
                                }
                            });
                            Database.getRequest(request.getSender() + request.getReceiver()).delete();
                        } else {
                            Database.getRequest(request.getSender() + request.getReceiver()).delete();
                        }
                    }
                    shareRequestAdapter.notifyDataSetChanged();
                    setDynamicHeight(shareRequestListView);
                    setDynamicHeight(shareFollowListView);
                    setDynamicHeight(shareFollowerListView);
                }
            }
        });

        return root;
    }

    /**
     * This sets the dynamic height for a list view
     * @param mListView
     *      This is the list view to set
     */
    private void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
    }

}
