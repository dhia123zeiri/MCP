package com.doctor_office.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Read-only endpoints over the result tables Kafka Connect populates.
 * Covers display of requirements #5 through #17.
 */
@Tag(name = "Metrics", description = "Stream-computed metrics read back from Postgres")
@RestController
@RequestMapping(path = "/api/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
public class MetricsController {

    @PersistenceContext
    private EntityManager em;

    // ── #5 revenue per item ──────────────────────────────────────────────
    @GetMapping("/revenue-per-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> revenuePerItem() {
        return rows("SELECT pet_type_id, revenue, updated_at FROM res_revenue_per_item ORDER BY pet_type_id");
    }

    // ── #6 expenses per item ─────────────────────────────────────────────
    @GetMapping("/expenses-per-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> expensesPerItem() {
        return rows("SELECT pet_type_id, expenses, updated_at FROM res_expenses_per_item ORDER BY pet_type_id");
    }

    // ── #7 profit per item ───────────────────────────────────────────────
    @GetMapping("/profit-per-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> profitPerItem() {
        return rows("SELECT pet_type_id, profit, updated_at FROM res_profit_per_item ORDER BY pet_type_id");
    }

    // ── #8/#9/#10 totals ─────────────────────────────────────────────────
    @GetMapping("/totals")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> totals() {
        return rows("SELECT scope, amount, updated_at FROM res_totals");
    }

    // ── #11 avg per item ─────────────────────────────────────────────────
    @GetMapping("/avg-per-appointment-by-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> avgByItem() {
        return rows("SELECT pet_type_id, avg_amount, count, updated_at FROM res_avg_per_appointment_by_item ORDER BY pet_type_id");
    }

    // ── #12 avg overall ──────────────────────────────────────────────────
    @GetMapping("/avg-per-appointment-all")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> avgAll() {
        return rows("SELECT scope, avg_amount, count, updated_at FROM res_avg_per_appointment_all");
    }

    // ── #13 top-profit item ──────────────────────────────────────────────
    @GetMapping("/top-profit-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> topProfitItem() {
        return rows("SELECT scope, pet_type_id, profit, updated_at FROM res_top_profit_item");
    }

    // ── #14/#15/#16 windowed metrics ─────────────────────────────────────
    @GetMapping("/windowed")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> windowed(@RequestParam(required = false) String metric) {
        // Whitelist allowed values to avoid SQL injection
        String table;
        switch (metric == null ? "" : metric) {
            case "revenue":  table = "res_windowed_revenue";  break;
            case "expenses": table = "res_windowed_expenses"; break;
            case "profit":   table = "res_windowed_profit";   break;
            default:
                // Combined view: union all three
                return rows(
                    "SELECT window_key, metric, amount, updated_at FROM res_windowed_revenue "
                  + "UNION ALL SELECT window_key, metric, amount, updated_at FROM res_windowed_expenses "
                  + "UNION ALL SELECT window_key, metric, amount, updated_at FROM res_windowed_profit "
                  + "ORDER BY updated_at DESC LIMIT 60");
        }
        return rows("SELECT window_key, metric, amount, updated_at FROM " + table
                  + " ORDER BY updated_at DESC LIMIT 50");
    }

    // ── #17 top country per item ─────────────────────────────────────────
    @GetMapping("/top-country-per-item")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> topCountryPerItem() {
        return rows("SELECT pet_type_id, country_code, sales_amount, updated_at FROM res_top_country_per_item ORDER BY pet_type_id");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(String sql) {
        var q = em.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        List<jakarta.persistence.Tuple> tuples = q.getResultList();
        List<Map<String, Object>> out = new ArrayList<>(tuples.size());
        for (var t : tuples) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (var el : t.getElements()) {
                row.put(el.getAlias(), t.get(el.getAlias()));
            }
            out.add(row);
        }
        return out;
    }
}
