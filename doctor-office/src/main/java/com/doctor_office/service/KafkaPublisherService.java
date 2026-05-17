package com.doctor_office.service;

import com.doctor_office.entity.Country;
import com.doctor_office.entity.PetType;
import com.doctor_office.repository.CountryRepository;
import com.doctor_office.repository.PetTypeRepository;
import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Backup mechanism that publishes DBInfo from the Spring Boot app itself.
 * In production, Kafka Connect (JDBC source) is the authoritative path
 * and runs continuously; this is a startup convenience for first-run
 * developer setups where Connect may not yet be configured.
 */
@Service
public class KafkaPublisherService {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisherService.class);

    private static final String PETTYPES_TOPIC  = "dbinfo.pettypes";
    private static final String COUNTRIES_TOPIC = "dbinfo.countries";

    private final KafkaProducer<String, String> kafkaProducer;
    private final PetTypeRepository petTypeRepository;
    private final CountryRepository countryRepository;
    private final Gson gson;

    @Autowired
    public KafkaPublisherService(KafkaProducer<String, String> kafkaProducer,
                                 PetTypeRepository petTypeRepository,
                                 CountryRepository countryRepository) {
        this.kafkaProducer     = kafkaProducer;
        this.petTypeRepository = petTypeRepository;
        this.countryRepository = countryRepository;
        this.gson              = new Gson();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishOnStartup() {
        log.info("Publishing initial DBInfo to Kafka (one row per record, keyed)...");
        publishPetTypes();
        publishCountries();
    }

    public void publishPetTypes() {
        try {
            for (PetType pt : petTypeRepository.findAll()) {
                String payload = gson.toJson(pt);
                ProducerRecord<String, String> rec = new ProducerRecord<>(
                    PETTYPES_TOPIC, String.valueOf(pt.getId()), payload);
                kafkaProducer.send(rec, (md, ex) -> {
                    if (ex != null) log.error("PetType publish failed", ex);
                });
            }
            kafkaProducer.flush();
            log.info("PetType bootstrap published.");
        } catch (Exception e) {
            log.error("PetType bootstrap publish failed: {}", e.toString());
        }
    }

    public void publishCountries() {
        try {
            for (Country c : countryRepository.findAll()) {
                String payload = gson.toJson(c);
                ProducerRecord<String, String> rec = new ProducerRecord<>(
                    COUNTRIES_TOPIC, c.getCode(), payload);
                kafkaProducer.send(rec, (md, ex) -> {
                    if (ex != null) log.error("Country publish failed", ex);
                });
            }
            kafkaProducer.flush();
            log.info("Country bootstrap published.");
        } catch (Exception e) {
            log.error("Country bootstrap publish failed: {}", e.toString());
        }
    }
}
