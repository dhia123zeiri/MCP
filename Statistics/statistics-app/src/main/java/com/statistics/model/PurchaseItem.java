package com.statistics.model;

public class PurchaseItem {
    private Long medicationId;
    private String medicationName;
    private Integer amountBought;
    private Double totalPurchasePrice;

    public PurchaseItem() {}

    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public Integer getAmountBought() { return amountBought; }
    public void setAmountBought(Integer amountBought) { this.amountBought = amountBought; }
    public Double getTotalPurchasePrice() { return totalPurchasePrice; }
    public void setTotalPurchasePrice(Double totalPurchasePrice) { this.totalPurchasePrice = totalPurchasePrice; }
}
