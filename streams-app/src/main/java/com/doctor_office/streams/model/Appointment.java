package com.doctor_office.streams.model;

/**
 * A "sale" — a vet appointment for a pet. Maps to the assignment's Sales topic.
 *
 * Each appointment carries a price, units (e.g. number of treatments) and a
 * country (where the pet owner is from) so we can compute requirement #17.
 */
public class Appointment {

    public String appointmentId;
    public long   petTypeId;
    public long   doctorId;
    public String countryCode;
    public double amount;        // price per unit
    public int    units;         // e.g. number of treatments
    public long   timestamp;     // epoch millis

    public Appointment() {}

    public Appointment(String appointmentId, long petTypeId, long doctorId,
                       String countryCode, double amount, int units, long timestamp) {
        this.appointmentId = appointmentId;
        this.petTypeId     = petTypeId;
        this.doctorId      = doctorId;
        this.countryCode   = countryCode;
        this.amount        = amount;
        this.units         = units;
        this.timestamp     = timestamp;
    }

    public double total() { return amount * units; }
}
