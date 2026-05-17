package com.doctor_office.streams.model;

/**
 * A "purchase" — supplies (vaccines, food, medication) bought from a supplier
 * for a given pet type. Maps to the assignment's Purchases topic.
 */
public class Supply {

    public String supplyId;
    public long   petTypeId;
    public String supplierCountry;
    public double amount;
    public int    units;
    public long   timestamp;

    public Supply() {}

    public Supply(String supplyId, long petTypeId, String supplierCountry,
                  double amount, int units, long timestamp) {
        this.supplyId        = supplyId;
        this.petTypeId       = petTypeId;
        this.supplierCountry = supplierCountry;
        this.amount          = amount;
        this.units           = units;
        this.timestamp       = timestamp;
    }

    public double total() { return amount * units; }
}
