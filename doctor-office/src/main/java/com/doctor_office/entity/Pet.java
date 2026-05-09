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
    @ManyToOne
    @JoinColumn(name = "pet_type_id")
    private PetType petType;
    @Column(name="age")
    private Integer age;
    @Column(name="weight")
    private float weight;
    @Column(name="medical_history")
    private String medicalHistory;
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public PetType getPetType() { return petType; }
    public void setPetType(PetType petType) { this.petType = petType; }

}
