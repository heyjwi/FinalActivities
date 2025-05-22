package com.example.cnpalabamanagementapp;

public class FirestoreCounter {
    private String id;
    private int count;

    public FirestoreCounter() {}

    public FirestoreCounter(String id, int count) {
        this.id = id;
        this.count = count;
    }

    // Getters and setters
    public String getId() { return id; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
