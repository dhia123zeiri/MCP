package com.doctor_office.service;

import com.doctor_office.entity.PetType;
import com.doctor_office.repository.PetTypeRepository;
import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaPublisherService {

    private static final String TOPIC = "DBInfo";

    private final KafkaProducer<String, String> kafkaProducer;
    private final PetTypeRepository petTypeRepository;
    private final Gson gson;

    @Autowired
    public KafkaPublisherService(KafkaProducer<String, String> kafkaProducer, PetTypeRepository petTypeRepository) {
        this.kafkaProducer = kafkaProducer;
        this.petTypeRepository = petTypeRepository;
        this.gson = new Gson();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishOnStartup() {
        System.out.println("Application started. Publishing DBInfo to Kafka...");
        publishPetTypes();
    }

    public void publishPetTypes() {
        try {
            List<PetType> petTypes = petTypeRepository.findAll();
            String jsonPayload = gson.toJson(petTypes);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "petTypes", jsonPayload);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Successfully published petTypes to topic " + metadata.topic());
                } else {
                    System.err.println("Failed to publish to Kafka: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error publishing pet types: " + e.getMessage());
        }
    }
}
