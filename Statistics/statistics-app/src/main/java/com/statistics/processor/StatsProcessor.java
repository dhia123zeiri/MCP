package com.statistics.processor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.statistics.model.AppointmentRequest;
import com.statistics.model.PurchaseItem;
import com.statistics.model.StatisticsResult;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsProcessor implements Processor<String, String, String, String> {

    private static final Logger log = LoggerFactory.getLogger(StatsProcessor.class);

    private ProcessorContext<String, String> context;
    private final Gson gson = new Gson();

    private final java.util.Set<String> processedReceiptIds = new java.util.HashSet<>();
    private boolean initialStateForwarded = false;

    // Global in-memory state for the statistics
    private List<AppointmentRequest> allAppointments = new ArrayList<>();
    private List<PurchaseItem> allPurchases = new ArrayList<>();
    private double totalRevenue = 0.0;
    private double totalExpenses = 0.0;

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        log.info("StatsProcessor initialized.");
    }

    @Override
    public void process(Record<String, String> record) {
        String topic = context.recordMetadata().get().topic();
        boolean updated = false;
        List<AppointmentRequest> newAppointments = new ArrayList<>();
        List<PurchaseItem> newPurchases = new ArrayList<>();

        try {
            if ("Sales".equals(topic)) {
                AppointmentRequest appointment = gson.fromJson(record.value(), AppointmentRequest.class);
                if (appointment != null) {
                    allAppointments.add(appointment);
                    newAppointments.add(appointment);
                    if (appointment.getPrice() != null) {
                        totalRevenue += appointment.getPrice();
                    }
                    updated = true;
                    log.info("Processed Appointment: {}", appointment.getAppointmentId());
                }
            } else if ("Purchases".equals(topic)) {
                String receiptId = record.key();
                if (receiptId != null && !processedReceiptIds.contains(receiptId)) {
                    processedReceiptIds.add(receiptId);
                    
                    Type listType = new TypeToken<ArrayList<PurchaseItem>>() {}.getType();
                    List<PurchaseItem> items = gson.fromJson(record.value(), listType);
                    if (items != null && !items.isEmpty()) {
                        for (PurchaseItem item : items) {
                            allPurchases.add(item);
                            newPurchases.add(item);
                            if (item.getTotalPurchasePrice() != null) {
                                totalExpenses += item.getTotalPurchasePrice();
                            }
                        }
                        updated = true;
                        log.info("Processed new Purchase Receipt {} with {} items.", receiptId, items.size());
                    }
                } else {
                    log.info("Duplicate or already processed Purchase Receipt {} ignored.", receiptId);
                }
            }

            if (updated) {
                double profit = totalRevenue - totalExpenses;
                StatisticsResult result;
                if (!initialStateForwarded) {
                    result = new StatisticsResult(allAppointments, allPurchases, totalRevenue, totalExpenses, profit);
                    initialStateForwarded = true;
                    log.info("Forwarding initial full state to Results topic.");
                } else {
                    result = new StatisticsResult(newAppointments, newPurchases, totalRevenue, totalExpenses, profit);
                    log.info("Forwarding delta StatisticsResult: {} appointments, {} purchases.", newAppointments.size(), newPurchases.size());
                }
                String jsonResult = gson.toJson(result);
                
                Record<String, String> outRecord = new Record<>(UUID.randomUUID().toString(), jsonResult, record.timestamp());
                context.forward(outRecord);
                log.info("Forwarded updated StatisticsResult: Rev={}, Exp={}, Profit={}", totalRevenue, totalExpenses, profit);
            }
        } catch (Exception e) {
            log.error("Failed to process record from topic {}: {}", topic, record.value(), e);
        }
    }

    @Override
    public void close() {
        log.info("Closing StatsProcessor.");
    }
}
