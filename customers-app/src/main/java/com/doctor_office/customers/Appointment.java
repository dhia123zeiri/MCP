package com.doctor_office.customers;

/**
 * Same shape as streams-app's Appointment POJO. Defined here to keep the
 * customers-app independently deployable with no shared module.
 */
public final class Appointment {

    public String appointmentId;
    public long   petTypeId;
    public long   doctorId;
    public String countryCode;
    public double amount;
    public int    units;
    public long   timestamp;

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
}
