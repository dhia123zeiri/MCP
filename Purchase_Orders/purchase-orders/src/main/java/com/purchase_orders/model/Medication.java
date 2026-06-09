package com.purchase_orders.model;

public class Medication {
    private Long id;
    private String name;
    private Double price;
    private Integer stockAmount;

    public Medication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStockAmount() { return stockAmount; }
    public void setStockAmount(Integer stockAmount) { this.stockAmount = stockAmount; }
}
