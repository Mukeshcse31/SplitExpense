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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.app.splitwise_clone.model.InstantMessage;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

public class MainActivity extends AppCompatActivity {

    // Constants
    public static final String SPLIT_PREFS = "SplitPrefs";
    public static final String DISPLAY_NAME_KEY = "username";
    static final String TAG = "Registration";

    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mUsernameView;
    private TextInputLayout mUserNameLayout;
    private TextInputLayout mlabel_confirm_password;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
private Button mloginbutton, msignUp, mgetLogin, mgetSignUp;
    // Firebase instance variables
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
        String displayName = prefs.getString(DISPLAY_NAME_KEY, "");
        if (TextUtils.isEmpty(displayName)) {
            setContentView(R.layout.activity_register);
            mEmailView = (AutoCompleteTextView) findViewById(R.id.register_email);
            mPasswordView = (EditText) findViewById(R.id.register_password);
            mConfirmPasswordView = (EditText) findViewById(R.id.register_confirm_password);
            mUsernameView = (AutoCompleteTextView) findViewById(R.id.register_username);
mloginbutton = findViewById(R.id.loginbutton);
msignUp = findViewById(R.id.register_sign_up_button);
mgetLogin = findViewById(R.id.getLogin);
mgetSignUp = findViewById(R.id.getSignUp);

            mUserNameLayout = findViewById(R.id.label_userName);
            mlabel_confirm_password = findViewById(R.id.label_confirm_password);

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
            mAuth = FirebaseAuth.getInstance();
        } else
            gotoNextPage();

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
        } else if (!isEmailValid(email)) {
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

    private boolean isEmailValid(String email) {
        // You can add more checking logic here.
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        String confirmPassword = mConfirmPasswordView.getText().toString();
        return confirmPassword.equals(password) && password.length() > getResources().getInteger(R.integer.password_length);
    }

    private void createFirebaseUser() {

        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUser onComplete: " + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.d(TAG, "user creation failed", task.getException());
                            showErrorDialog(getString(R.string.error_registration) + "\n" + task.getException().getMessage());
                        } else {
                            saveDisplayName();
                            gotoNextPage();
                        }
                    }
                });
    }


    // TODO: Save the display name to Shared Preferences
    private void saveDisplayName() {
        String displayName = mUsernameView.getText().toString();
        if(TextUtils.isEmpty(displayName)) displayName = "Mukesh"; //TODO parameterize when login

        SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
        prefs.edit().putString(DISPLAY_NAME_KEY, displayName).apply();
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

    private void gotoNextPage(){
        Intent intent = new Intent(MainActivity.this, Expenses.class);
        intent = new Intent(MainActivity.this, Friends.class);
        finish();
        startActivity(intent);
    }
    public void login(View v){

        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, "User Login onComplete: " + task.isSuccessful());

                if (!task.isSuccessful()) {
                    Log.d(TAG, "user Login failed", task.getException());
                    showErrorDialog(getString(R.string.error_Login) + "\n" + task.getException().getMessage());
                } else {
                    saveDisplayName();//TODO get the display name from DB
                    gotoNextPage();
                }
            }
        });
    }
    //TODO Login and setTitle
}
