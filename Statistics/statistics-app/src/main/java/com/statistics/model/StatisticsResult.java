package com.statistics.model;

import java.util.List;

public class StatisticsResult {
    private List<AppointmentRequest> appointments;
    private List<PurchaseItem> purchases;
    private double revenue;
    private double expenses;
    private double profit;

    public StatisticsResult() {}

    public StatisticsResult(List<AppointmentRequest> appointments, List<PurchaseItem> purchases, double revenue, double expenses, double profit) {
        this.appointments = appointments;
        this.purchases = purchases;
        this.revenue = revenue;
        this.expenses = expenses;
        this.profit = profit;
    }

    public List<AppointmentRequest> getAppointments() { return appointments; }
    public void setAppointments(List<AppointmentRequest> appointments) { this.appointments = appointments; }
    public List<PurchaseItem> getPurchases() { return purchases; }
    public void setPurchases(List<PurchaseItem> purchases) { this.purchases = purchases; }
    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
    public double getExpenses() { return expenses; }
    public void setExpenses(double expenses) { this.expenses = expenses; }
    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
}
