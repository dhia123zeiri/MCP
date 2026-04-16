package com.doctor_office.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long petId;
    @Column(name = "name")
    private String name;
    @Column(name="species")
    private String species;
    @Column(name="breed")
    private String breed;
    @Column(name="age")
    private Integer age;
    @Column(name="weight")
    private float weight;
    @Column(name="medical_history")
    private String medicalHistory;
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

}
