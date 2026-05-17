package com.doctor_office.suppliers;

/** Mirror of streams-app's Supply POJO. Kept local for independent deployment. */
public final class Supply {

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
}
