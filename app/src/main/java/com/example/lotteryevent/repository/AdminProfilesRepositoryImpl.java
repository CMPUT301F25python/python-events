package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminProfilesRepositoryImpl implements IAdminProfilesRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Repo implementation to retrieve all user profiles for admin view
     * Uses firestore to fetch entire collection of profiles
     * @param callback
     */
    @Override
    public void getAllProfiles(ProfilesCallback callback) {
        db.collection("users")
                .get()
                /**
                 * Adds users from the db to a list and calls callback's success
                 * behaviour with this list
                 * @param query snapshot containing all documents returned by the Firestore query
                 */
                .addOnSuccessListener(query -> {
                    List<User> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            list.add(u);
                        }
                    }
                    callback.onSuccess(list);
                })
                /**
                 * Calls the callback's failure behaviour on failure
                 */
                .addOnFailureListener(callback::onFailure);
    }
}
