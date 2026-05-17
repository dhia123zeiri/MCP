package com.doctor_office.streams.model;

/**
 * Running average aggregator. Used by requirements #11 and #12.
 *
 * Note: Streams aggregators must produce a NEW object on each step when
 * stored in a state store with caching — mutating in place is safe in
 * Kafka Streams' contract, but we keep a small `with(...)` API for clarity.
 */
public class Avg {

    public long   count;
    public double sum;
    public double avg;

    public Avg() {}

    public Avg(long count, double sum) {
        this.count = count;
        this.sum   = sum;
        this.avg   = count == 0 ? 0.0 : sum / count;
    }

    public Avg add(double value) {
        long   newCount = this.count + 1;
        double newSum   = this.sum + value;
        return new Avg(newCount, newSum);
    }
}
