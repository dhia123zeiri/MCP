package com.doctor_office.entity;

import com.doctor_office.service.KafkaPublisherService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PetTypeListener {

    private KafkaPublisherService kafkaPublisherService;

    @Autowired
    public void setKafkaPublisherService(@Lazy KafkaPublisherService kafkaPublisherService) {
        this.kafkaPublisherService = kafkaPublisherService;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void onPetTypeChange(PetType petType) {
        if (kafkaPublisherService != null) {
            System.out.println("PetType table updated. Publishing changes to Kafka...");
            kafkaPublisherService.publishPetTypes();
        }
    }
}
