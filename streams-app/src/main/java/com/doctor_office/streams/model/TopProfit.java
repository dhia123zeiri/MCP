package com.doctor_office.streams.model;

/**
 * Tracks the pet type currently holding the highest all-time profit
 * (requirement #13).
 */
public class TopProfit {

    public long   pet_type_id;
    public double profit;

    public TopProfit() {}

    public TopProfit(long petTypeId, double profit) {
        this.pet_type_id = petTypeId;
        this.profit      = profit;
    }

    public static TopProfit empty() {
        return new TopProfit(-1L, Double.NEGATIVE_INFINITY);
    }
}
