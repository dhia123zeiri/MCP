package com.doctor_office.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Country reference data. Per assignment requirement #1:
 *   "countries cannot be deleted and optionally not changed".
 * We enforce that at the service layer (no delete endpoint, no update).
 */
@Entity
@Table(name = "country")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country {

    @Id
    @Column(name = "code", length = 3)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;
}
