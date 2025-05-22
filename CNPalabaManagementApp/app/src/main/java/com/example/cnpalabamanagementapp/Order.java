package com.example.cnpalabamanagementapp;


import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public class Order {
    private int orderNumber;
    private String documentId;
    private String customerName;
    private String contactNumber;
    private String email;
    private String serviceType;
    private boolean superwash;
    private boolean dry;
    private boolean fold;
    private int arielQuantity;
    private int downyQuantity;
    private String loadType;
    private Timestamp timestamp;
    private String status;
    private String handlerUserId;
    private int total;
    private String handlerName;
    private String handlerBranch;
    private String orderNote;
    private String documentationUrl;

    public Order() {}

    public Order(String documentId, String customerName, String contactNumber, String email,
                 String serviceType, boolean superwash, boolean dry, boolean fold,
                 int arielQuantity, int downyQuantity, String loadType, Timestamp timestamp,
                 String status, String handlerUserId, int total, String handlerName,
                 String handlerBranch,int orderNumber, String orderNote) {
        this.documentId = documentId;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.serviceType = serviceType;
        this.superwash = superwash;
        this.dry = dry;
        this.fold = fold;
        this.arielQuantity = arielQuantity;
        this.downyQuantity = downyQuantity;
        this.loadType = loadType;
        this.timestamp = timestamp;
        this.status = status;
        this.handlerUserId = handlerUserId;
        this.total = total;
        this.handlerName = handlerName;
        this.handlerBranch = handlerBranch;
        this.orderNumber = orderNumber;
        this.orderNote = orderNote;
    }

    // Getters and Setters

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    // Setter
    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public boolean isSuperwash() {
        return superwash;
    }

    public void setSuperwash(boolean superwash) {
        this.superwash = superwash;
    }

    public boolean isDry() {
        return dry;
    }

    public void setDry(boolean dry) {
        this.dry = dry;
    }

    public boolean isFold() {
        return fold;
    }

    public void setFold(boolean fold) {
        this.fold = fold;
    }

    public int getArielQuantity() {
        return arielQuantity;
    }

    public void setArielQuantity(int arielQuantity) {
        this.arielQuantity = arielQuantity;
    }

    public int getDownyQuantity() {
        return downyQuantity;
    }

    public void setDownyQuantity(int downyQuantity) {
        this.downyQuantity = downyQuantity;
    }

    public String getLoadType() {
        return loadType;
    }

    public void setLoadType(String loadType) {
        this.loadType = loadType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandlerUserId() {
        return handlerUserId;
    }

    public void setHandlerUserId(String handlerUserId) {
        this.handlerUserId = handlerUserId;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getHandlerBranch() {
        return handlerBranch;
    }

    public void setHandlerBranch(String handlerBranch) {
        this.handlerBranch = handlerBranch;
    }

    // Helper method to get selected services as a list
    public List<String> getSelectedServices() {
        List<String> services = new ArrayList<>();
        if (superwash) services.add("Superwash");
        if (dry) services.add("Dry");
        if (fold) services.add("Fold");
        return services;
    }

    // Helper method to get formatted timestamp
    public String getFormattedDate() {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        return "";
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public String getOrderNote() {
        return orderNote;
    }
    public String getFormattedCompletedDate() {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
        return "N/A"; // or return an empty string if preferred
    }
}