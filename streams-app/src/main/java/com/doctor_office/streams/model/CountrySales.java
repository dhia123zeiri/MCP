package com.doctor_office.streams.model;

/**
 * For each pet type, the country with the highest sales and the amount
 * of those sales (requirement #17). The assignment explicitly says:
 *   "Include the value of such sales."
 */
public class CountrySales {

    public String country_code;
    public double sales_amount;

    public CountrySales() {}

    public CountrySales(String countryCode, double salesAmount) {
        this.country_code = countryCode;
        this.sales_amount = salesAmount;
    }

    public static CountrySales empty() {
        return new CountrySales("", Double.NEGATIVE_INFINITY);
    }
}
