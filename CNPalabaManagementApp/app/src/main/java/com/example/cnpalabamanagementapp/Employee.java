package com.example.cnpalabamanagementapp;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

public class Employee implements Parcelable {
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String profileImageUrl;
    private Timestamp createdAt;
    private String role;
    private String email;
    private String contactNumber;
    private String branch; // lowercase
    private String address;
    private String birthDate;
    private String emergencyContact;
    private String emergencyContactNumber;
    private String emergencyContactEmail;
    private String gender;
    private String startDate;


    // Parcelable implementation
    protected Employee(Parcel in) {
        firstName = in.readString();
        middleName = in.readString();
        lastName = in.readString();
        profileImageUrl = in.readString();
        createdAt = in.readParcelable(Timestamp.class.getClassLoader());
        role = in.readString();
        email = in.readString();
        contactNumber = in.readString();
        branch = in.readString();
        address = in.readString();
        birthDate = in.readString();
        emergencyContact = in.readString();
        emergencyContactNumber = in.readString();
        emergencyContactEmail = in.readString();
        gender = in.readString();
        startDate = in.readString();
        username = in.readString();


    }

    public static final Creator<Employee> CREATOR = new Creator<Employee>() {
        @Override
        public Employee createFromParcel(Parcel in) { return new Employee(in); }
        @Override
        public Employee[] newArray(int size) { return new Employee[size]; }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(middleName);
        dest.writeString(lastName);
        dest.writeString(profileImageUrl);
        dest.writeParcelable(createdAt, flags);
        dest.writeString(role);
        dest.writeString(email);
        dest.writeString(contactNumber);
        dest.writeString(branch);
        dest.writeString(address);
        dest.writeString(birthDate);
        dest.writeString(emergencyContact);
        dest.writeString(emergencyContactNumber);
        dest.writeString(emergencyContactEmail);
        dest.writeString(gender);
        dest.writeString(startDate);
        dest.writeString(username);


    }


    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }


    // Empty constructor REQUIRED for Firestore
    public Employee() {}

    // Add all getters and setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return firstName + " " +
                (middleName == null || middleName.isEmpty() ? "" : middleName + " ") +
                lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return this.contactNumber;
    }

    // Add these if not present
    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBranch() {
        return this.branch;
    }

    public String getAddress() {
        return this.address;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyContactEmail() {
        return emergencyContactEmail;
    }

    public void setEmergencyContactEmail(String emergencyContactEmail) {
        this.emergencyContactEmail = emergencyContactEmail;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}