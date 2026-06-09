package com.appointment_processor.model;

public class PetType {
    private Long id;
    private String species;
    private String breed;
    private Double price;

    public PetType() {}

    public PetType(Long id, String species, String breed, Double price) {
        this.id = id;
        this.species = species;
        this.breed = breed;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
