package com.statistics.model;

public class AppointmentRequest {
    private String appointmentId;
    private String species;
    private String breed;
    private Double price;

    public AppointmentRequest() {}

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
