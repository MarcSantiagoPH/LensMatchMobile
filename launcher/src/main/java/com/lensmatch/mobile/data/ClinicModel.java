package com.lensmatch.mobile.data;

import java.util.Map;

public class ClinicModel {
    private final String clinicName;
    private final String contactNumber;
    private final String email;
    private final String address;
    private final String businessHours;

    public ClinicModel(String clinicName, String contactNumber, String email, String address, String businessHours) {
        this.clinicName = clinicName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.businessHours = businessHours;
    }

    public static ClinicModel fromMap(Map<String, Object> data) {
        if (data == null) return new ClinicModel("Franselle Optical Clinic", "", "", "", "");

        String name = data.containsKey("clinicName") && data.get("clinicName") != null ? String.valueOf(data.get("clinicName"))
                : (data.containsKey("name") && data.get("name") != null ? String.valueOf(data.get("name")) : "Franselle Optical Clinic");

        String phone = data.containsKey("contactNumber") && data.get("contactNumber") != null ? String.valueOf(data.get("contactNumber"))
                : (data.containsKey("phone") && data.get("phone") != null ? String.valueOf(data.get("phone")) : "");

        String emailStr = data.containsKey("email") && data.get("email") != null ? String.valueOf(data.get("email")) : "";

        String addr = data.containsKey("address") && data.get("address") != null ? String.valueOf(data.get("address")) : "";

        String hours = data.containsKey("businessHours") && data.get("businessHours") != null ? String.valueOf(data.get("businessHours"))
                : (data.containsKey("hours") && data.get("hours") != null ? String.valueOf(data.get("hours")) : "");

        return new ClinicModel(name, phone, emailStr, addr, hours);
    }

    public String getClinicName() { return clinicName != null && !clinicName.trim().isEmpty() ? clinicName : "Franselle Optical Clinic"; }
    public String getContactNumber() { return contactNumber != null ? contactNumber.trim() : ""; }
    public String getEmail() { return email != null ? email.trim() : ""; }
    public String getAddress() { return address != null ? address.trim() : ""; }
    public String getBusinessHours() { return businessHours != null ? businessHours.trim() : ""; }
}
