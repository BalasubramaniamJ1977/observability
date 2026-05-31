package com.payments.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@RestController
@Slf4j
public class PaymentProcessorApplication {

    @Value("${spring.application.name}") private String svc;
    @Value("${next.service.url}")        private String nextUrl;
    @Autowired private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessorApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", svc); }

    @SuppressWarnings("unchecked")
    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> payment) {
        // Gaussian fraud score centred on 0.1, std-dev 0.08, clamped to [0,1]
        double raw   = ThreadLocalRandom.current().nextGaussian() * 0.08 + 0.1;
        double score = Math.round(Math.max(0.0, Math.min(1.0, raw)) * 10_000.0) / 10_000.0;
        String status = score < 0.7 ? "CLEARED" : "REVIEW";
        payment.put("fraud_score",        score);
        payment.put("compliance_status",  status);
        log.info("[{}] INPUT  {}", svc, payment);

        if ("REVIEW".equals(status)) {
            log.warn("[{}] Payment flagged fraud_score={}", svc, score);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Payment " + payment.get("uetr") + " requires compliance review (fraud_score=" + score + ")");
        }

        Map<String, Object> result = restTemplate.postForObject(nextUrl + "/process", payment, Map.class);
        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
