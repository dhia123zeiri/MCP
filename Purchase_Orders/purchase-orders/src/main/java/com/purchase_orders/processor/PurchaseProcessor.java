package com.purchase_orders.processor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.purchase_orders.model.Medication;
import com.purchase_orders.model.PurchaseItem;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PurchaseProcessor implements Processor<String, String, String, String> {

    private static final Logger log = LoggerFactory.getLogger(PurchaseProcessor.class);
    private static final Duration PURCHASE_INTERVAL = Duration.ofMillis(700);

    private ProcessorContext<String, String> context;
    private final Gson gson = new Gson();

    // Only process DBInfo messages produced at or after this app started
    private final long startupTime = System.currentTimeMillis();
    private List<Medication> latestMedications;

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
        log.info("PurchaseProcessor initialized. Will ignore DBInfo messages older than startup time ({}).", startupTime);

        context.schedule(PURCHASE_INTERVAL, PunctuationType.WALL_CLOCK_TIME, this::onPunctuate);
    }

    @Override
    public void process(Record<String, String> record) {
        if (!"medications".equals(record.key())) {
            return;
        }

        if (record.timestamp() < startupTime) {
            log.info("Skipping historical medications record (timestamp: {} < startupTime: {}).", record.timestamp(), startupTime);
            return;
        }

        log.info("Received live medications update (timestamp: {}). Saving latest medications snapshot.", record.timestamp());

        try {
            Type listType = new TypeToken<ArrayList<Medication>>() {}.getType();
            List<Medication> medications = gson.fromJson(record.value(), listType);

            if (medications == null || medications.isEmpty()) {
                log.info("No medications in update, skipping.");
                return;
            }

            latestMedications = medications;
        } catch (Exception e) {
            log.error("Failed to parse medications record: {}", record.value(), e);
        }
    }

    private void onPunctuate(long timestamp) {
        if (latestMedications == null || latestMedications.isEmpty()) {
            return;
        }

        Medication lowestStock = latestMedications.stream()
                .filter(m -> m.getId() != null && m.getStockAmount() != null)
                .min(Comparator.comparingInt(Medication::getStockAmount))
                .orElse(null);

        if (lowestStock == null) {
            log.info("No valid medications found in latest snapshot, skipping scheduled purchase.");
            return;
        }

        try {
            double price = lowestStock.getPrice() != null ? lowestStock.getPrice() : 0.0;
            PurchaseItem item = new PurchaseItem(lowestStock.getId(), lowestStock.getName(), 1, price);
            List<PurchaseItem> purchaseList = new ArrayList<>();
            purchaseList.add(item);

            String receiptId = UUID.randomUUID().toString();
            String jsonReceipt = gson.toJson(purchaseList);
            Record<String, String> outRecord = new Record<>(receiptId, jsonReceipt, System.currentTimeMillis());
            context.forward(outRecord);

            log.info("Scheduled purchase at {}: bought 1 unit of '{}' (stock {}). Receipt: {}",
                    timestamp, lowestStock.getName(), lowestStock.getStockAmount(), receiptId);
        } catch (Exception e) {
            log.error("Failed to execute scheduled purchase.", e);
        }
    }

    @Override
    public void close() {
        log.info("Closing PurchaseProcessor.");
    }
}
