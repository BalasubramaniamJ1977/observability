package com.payments.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
@RestController
@Slf4j
public class ResponseHandlerApplication {

    @Value("${spring.application.name}") private String svc;

    public static void main(String[] args) {
        SpringApplication.run(ResponseHandlerApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", svc); }

    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> payment) {
        log.info("[{}] INPUT  {}", svc, payment);
        String newUetr = UUID.randomUUID().toString();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status",          "completed");
        result.put("original_uetr",   payment.get("uetr"));
        result.put("new_uetr",        newUetr);
        result.put("routing_rail",    payment.get("routing_rail"));
        result.put("message_format",  payment.get("message_format"));
        result.put("journal_id",      payment.get("journal_id"));
        result.put("posting_ref",     payment.get("posting_ref"));
        result.put("dispatch_ref",    payment.get("dispatch_ref"));
        result.put("amount",          payment.get("amount"));
        result.put("currency",        payment.get("currency"));
        result.put("pipeline_stages", List.of(
            "payment-initiation", "routing", "transformer",
            "payment-processor",  "accounting-generator",
            "posting", "dispatcher", "response-handler"
        ));

        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
