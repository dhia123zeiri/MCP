package com.appointment_processor.model;

public class AppointmentRequest {
    private String appointmentId;
    private String species;
    private String breed;
    private Double price;

    public AppointmentRequest() {}

    public AppointmentRequest(String appointmentId, String species, String breed, Double price) {
        this.appointmentId = appointmentId;
        this.species = species;
        this.breed = breed;
        this.price = price;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Override
    public String toString() {
        return "AppointmentRequest{" +
                "appointmentId='" + appointmentId + '\'' +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", price=" + price +
                '}';
    }
}
