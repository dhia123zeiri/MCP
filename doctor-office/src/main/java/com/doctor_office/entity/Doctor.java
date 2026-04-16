package com.doctor_office.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;


@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Doctor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="doctor_id")
    private Long doctorId;
    @Column(name="name")
    private String name;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name="email")
    private String email;
    @Column(name="years_of_experience")
    private Integer yearsOfExperience;
    @OneToMany(mappedBy = "doctor",cascade = CascadeType.ALL)
    private List<Pet> pets;



}
