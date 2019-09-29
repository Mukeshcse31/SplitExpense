package com.google.app.splitwise_clone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.app.splitwise_clone.model.User;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class SignIn extends AppCompatActivity {

    // Constants
    public static final String SPLIT_PREFS = "SplitPrefs";
    public static final String DISPLAY_NAME_KEY = "displayName";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    private DatabaseReference mDatabaseReference;
    static final String TAG = "Registration";
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUsernameView;
    private TextInputLayout mUserNameLayout;
    private TextInputLayout mlabel_confirm_password;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private Button mloginbutton, msignUp, mgetLogin, mgetSignUp;
    private AppCompatImageView offline_iv;
    // Firebase instance variables
    private FirebaseAuth mAuth;

//TODO when sign out, the sharedPreferences must be cleared

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

//        https://stackoverflow.com/questions/40081539/default-firebaseapp-is-not-initialized
//        FirebaseApp.initializeApp(getBaseContext());
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
        mDatabaseReference = AppUtils.getDBReference();
        String displayName = prefs.getString(DISPLAY_NAME_KEY, "");
        String email = prefs.getString(USERNAME_KEY, "");
        String password = prefs.getString(PASSWORD_KEY, "");
        offline_iv = findViewById(R.id.offline_iv);

        mEmailView = findViewById(R.id.register_email);
        mPasswordView = findViewById(R.id.register_password);
        mConfirmPasswordView = findViewById(R.id.register_confirm_password);
        mUsernameView = findViewById(R.id.register_username);
        mloginbutton = findViewById(R.id.login_bn);
        msignUp = findViewById(R.id.register_sign_up_button);
        mgetLogin = findViewById(R.id.getLogin);
        mgetSignUp = findViewById(R.id.getSignUp);

        mUserNameLayout = findViewById(R.id.label_userName);
        mlabel_confirm_password = findViewById(R.id.label_confirm_password);

        if (!AppUtils.isOnline(this)) {
            offline_iv.setVisibility(View.VISIBLE);
            mEmailView.setVisibility(View.GONE);
            mPasswordView.setVisibility(View.GONE);
            mConfirmPasswordView.setVisibility(View.GONE);
            mUsernameView.setVisibility(View.GONE);
            mloginbutton.setVisibility(View.GONE);
            msignUp.setVisibility(View.GONE);
            mgetLogin.setVisibility(View.GONE);
            mgetSignUp.setVisibility(View.GONE);
            mUserNameLayout.setVisibility(View.GONE);
            mlabel_confirm_password.setVisibility(View.GONE);
            return;
        } else {
            offline_iv.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(email)) {

            // Keyboard sign in action
            mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.integer.register_form_finished || id == EditorInfo.IME_NULL) {
                        attemptRegistration();
                        return true;
                    }
                    return false;
                }
            });

        } else {
            signInWithCredentials(email, password);
        }


    }

    // Executed when Sign Up button is pressed.
    public void signUp(View v) {
        attemptRegistration();
    }

    private void attemptRegistration() {

        // Reset errors displayed in the form.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        Log.d(TAG, "TextUtils.isEmpty(password): " + TextUtils.isEmpty(password));
        Log.d(TAG, "TextUtils.isEmpty(password) && !isPasswordValid(password): " + (TextUtils.isEmpty(password) && !isPasswordValid(password)));


        //Check the userName
        String displayName = mUsernameView.getText().toString().trim();
        if(!AppUtils.checkUserName(displayName)){
            focusView = mUsernameView;
            mUsernameView.setError(getString(R.string.error_username));
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            Log.d(TAG, "Password Invalid");
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!AppUtils.checkEmail(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            createFirebaseUser();

        }
    }

    private boolean isPasswordValid(String password) {
        String confirmPassword = mConfirmPasswordView.getText().toString();
        return confirmPassword.equals(password) && password.length() > getResources().getInteger(R.integer.password_length);
    }

    private void createFirebaseUser() {

        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUser onComplete: " + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.d(TAG, "user creation failed", task.getException());
                            showErrorDialog(getString(R.string.error_registration) + "\n" + task.getException().getMessage());
                        } else {

                            final String displayName = mUsernameView.getText().toString();
                            //update the user's profile for the display Name
//                            https://firebase.google.com/docs/auth/android/manage-users
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
//                                    .setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg"))
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                saveUserCredentials(email, password);
                                                gotoSummaryPage();
                                                Log.d(TAG, "User profile updated.");
                                            }
                                        }
                                    });

                            //TODO check if the user is already added by other user.
                            //if already added, don't add new user record so as to save the friend's link
                            Query query = mDatabaseReference.child("users/" + displayName);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Log.i(TAG, displayName + " is already added by another user");
                                    } else {
                                        User friend = new User(displayName, email);
                                        mDatabaseReference.child("users/" + displayName).setValue(friend, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {

                                                if (databaseError != null) {
                                                    Log.e(TAG, databaseError.getDetails());
                                                }
                                                if (databaseError != null)
                                                    Log.i(TAG, databaseError.getDetails());
                                                showSnackBar(getString(R.string.signup_success));
                                                finish();
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                });
    }


    // TODO: Save the display name to Shared Preferences
    private void saveUserCredentials(String email, String password) {

        SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
        prefs.edit().putString(USERNAME_KEY, email).apply();
        prefs.edit().putString(PASSWORD_KEY, password).apply();
        prefs.edit().putString(DISPLAY_NAME_KEY, FirebaseUtils.getUserName()).apply();
    }

    public void showSnackBar(String message) {

//https://stackoverflow.com/questions/30729312/how-to-dismiss-a-snackbar-using-its-own-action-button
        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackBar.setAction(getString(R.string.close), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call your action method here
                snackBar.dismiss();
            }
        });
        snackBar.show();
    }

    // TODO: Create an alert dialog to show in case registration failed
    private void showErrorDialog(String message) {

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_title))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    public void getLogin(View v) {

        mUserNameLayout.setVisibility(View.INVISIBLE);
        mlabel_confirm_password.setVisibility(View.INVISIBLE);
        msignUp.setVisibility(View.INVISIBLE);
        mgetLogin.setVisibility(View.INVISIBLE);

        mloginbutton.setVisibility(View.VISIBLE);
        mgetSignUp.setVisibility(View.VISIBLE);

    }


    public void getSignUp(View v) {

        mUserNameLayout.setVisibility(View.VISIBLE);
        mlabel_confirm_password.setVisibility(View.VISIBLE);
        msignUp.setVisibility(View.VISIBLE);
        mgetLogin.setVisibility(View.VISIBLE);

        mloginbutton.setVisibility(View.INVISIBLE);
        mgetSignUp.setVisibility(View.INVISIBLE);

    }

    private void gotoSummaryPage() {
//        Intent intent = new Intent(SignIn.this, FriendsList.class);
        Intent intent = new Intent(SignIn.this, SummaryActivity.class);

        finish();
        startActivity(intent);
    }

    public void login(View v) {

        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            return;
        }
        signInWithCredentials(email, password);

    }
    //TODO Login and setTitle

    private void signInWithCredentials(final String email, final String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "User Login onComplete: " + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.d(TAG, "user Login failed", task.getException());
                            showErrorDialog(getString(R.string.error_Login) + "\n" + task.getException().getMessage());
                        } else {

                            String userName = FirebaseUtils.getUserName();
                            showSnackBar(userName + " signed in");
                            saveUserCredentials(email, password);
                            gotoSummaryPage();
                            FirebaseMessaging.getInstance().subscribeToTopic(userName);
                        }
                    }
                });
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i(TAG, "on resume");
    }
}