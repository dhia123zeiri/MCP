package com.appointment_processor.processor;

import com.appointment_processor.model.AppointmentRequest;
import com.appointment_processor.model.PetType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AppointmentRequestProcessor implements Processor<String, String, String, String> {

    private static final Logger log = LoggerFactory.getLogger(AppointmentRequestProcessor.class);

    private ProcessorContext<String, String> context;
    private final Gson gson = new Gson();
    private final Random random = new Random();
    
    // Buffer the latest petTypes record to only process the latest state
    private Record<String, String> latestRecord = null;

    // Only process DBInfo messages produced at or after this app started
    private final long startupTime = System.currentTimeMillis();

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        // Schedule a periodic task every 1 second to process the latest buffered petTypes update
        this.context.schedule(Duration.ofMillis(1000), PunctuationType.WALL_CLOCK_TIME, this::punctuate);
        log.info("AppointmentRequestProcessor initialized with 1-second buffering punctuator.");
    }

    @Override
    public void process(Record<String, String> record) {
        String key = record.key();
        if ("petTypes".equals(key) || key == null) {
            // Ignore historical records published before this app started
            if (record.timestamp() < startupTime) {
                log.info("Skipping historical petTypes record (timestamp: {} < startupTime: {}).", record.timestamp(), startupTime);
                return;
            }
            // Buffer the record (overwriting any older ones) to ensure only the latest state is evaluated
            this.latestRecord = record;
        }
    }

    private void punctuate(long timestamp) {
        if (latestRecord == null) {
            return;
        }

        Record<String, String> recordToProcess = this.latestRecord;
        this.latestRecord = null; // Clear immediately to avoid reprocessing

        log.info("Punctuator processing latest petTypes state update...");
        try {
            Type listType = new TypeToken<ArrayList<PetType>>() {}.getType();
            List<PetType> newPetTypes = gson.fromJson(recordToProcess.value(), listType);
            if (newPetTypes != null && !newPetTypes.isEmpty()) {
                log.info("Received {} available pet types. Generating appointments immediately.", newPetTypes.size());
                
                // Generate up to 3 appointments immediately based on the available types received
                int requestCount = Math.min(3, newPetTypes.size());
                for (int i = 0; i < requestCount; i++) {
                    PetType selected = newPetTypes.get(random.nextInt(newPetTypes.size()));
                    AppointmentRequest request = new AppointmentRequest(
                            UUID.randomUUID().toString(),
                            selected.getSpecies(),
                            selected.getBreed(),
                            selected.getPrice()
                    );

                    Record<String, String> outRecord = new Record<>(request.getAppointmentId(), gson.toJson(request), System.currentTimeMillis());
                    context.forward(outRecord);
                    log.info("Generated and forwarded new appointment request: {}", request);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse/process petTypes record: {}", recordToProcess.value(), e);
        }
    }

    @Override
    public void close() {
        log.info("Closing AppointmentRequestProcessor.");
    }
}
