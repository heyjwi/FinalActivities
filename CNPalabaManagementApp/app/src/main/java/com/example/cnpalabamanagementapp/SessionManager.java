package com.example.cnpalabamanagementapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_CONTACT = "contact";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PROFILE_IMAGE = "profile_image";
    private static final String KEY_BIRTH_DATE = "birthDate";
    private static final String KEY_EMERGENCY_CONTACT = "emergencyContact";
    private static final String KEY_EMERGENCY_CONTACT_NUMBER = "emergencyContactNumber";
    private static final String KEY_EMERGENCY_CONTACT_EMAIL = "emergencyContactEmail";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // Modified login method that works for both admin and user
    public void login(String username, String role, String userId, String fullName,
                      String email, String contact, String branch, String address,
                      String profileImage) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_CONTACT, contact);
        editor.putString(KEY_BRANCH, branch);
        editor.putString(KEY_ADDRESS, address);
        editor.putString(KEY_PROFILE_IMAGE, profileImage);
        editor.apply();
    }

    // Additional method for admin with extra fields
    public void adminLogin(String username, String role, String userId, String fullName,
                           String email, String contact, String branch, String address,
                           String profileImage, String birthDate, String emergencyContact,
                           String emergencyContactNumber, String emergencyContactEmail) {

        Log.d("SessionDebug", "Storing - BirthDate: " + birthDate +
                ", EmergencyContact: " + emergencyContact);

        // ... similar for other fields
        login(username, role, userId, fullName, email, contact, branch, address, profileImage);
        editor.putString(KEY_BIRTH_DATE, birthDate);
        editor.putString(KEY_EMERGENCY_CONTACT, emergencyContact);
        editor.putString(KEY_EMERGENCY_CONTACT_NUMBER, emergencyContactNumber);
        editor.putString(KEY_EMERGENCY_CONTACT_EMAIL, emergencyContactEmail);
        editor.apply();
    }


    public void logout() {
        editor.clear();
        editor.apply();
    }

    public void updateProfileImage(String imageUrl) {
        editor.putString(KEY_PROFILE_IMAGE, imageUrl);
        editor.apply();
    }


    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "user"); // default to "user"
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getContact() {
        return prefs.getString(KEY_CONTACT, "");
    }

    public String getBranch() {
        return prefs.getString(KEY_BRANCH, "");
    }

    public String getAddress() {
        return prefs.getString(KEY_ADDRESS, "");
    }

    public String getProfileImage() {
        return prefs.getString(KEY_PROFILE_IMAGE, "");
    }

    public String getBirthDate() {
        return prefs.getString(KEY_BIRTH_DATE, "");
    }

    public String getEmergencyContact() {
        return prefs.getString(KEY_EMERGENCY_CONTACT, "");
    }

    public String getEmergencyContactNumber() {
        return prefs.getString(KEY_EMERGENCY_CONTACT_NUMBER, "");
    }

    public String getEmergencyContactEmail() {
        return prefs.getString(KEY_EMERGENCY_CONTACT_EMAIL, "");
    }
}
