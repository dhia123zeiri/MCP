package com.doctor_office.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(PetTypeListener.class)
public class PetType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_type_id")
    private Long id;

    @Column(name = "species")
    private String species;

    @Column(name = "breed")
    private String breed;

    @Column(name = "price")
    private Double price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
