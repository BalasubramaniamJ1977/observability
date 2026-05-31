package com.payments.transformer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@SpringBootApplication
@RestController
@Slf4j
public class TransformerApplication {

    private static final Map<String, String> FORMAT_MAP = Map.of(
        "SWIFT", "MT103",
        "SEPA",  "pacs.008",
        "CHAPS", "pacs.008",
        "BACS",  "Bacs-STD-18",
        "FPS",   "ISO20022",
        "RTP",   "pacs.008"
    );

    @Value("${spring.application.name}") private String svc;
    @Value("${next.service.url}")        private String nextUrl;
    @Autowired private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(TransformerApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", svc); }

    @SuppressWarnings("unchecked")
    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> payment) {
        String rail = (String) payment.getOrDefault("routing_rail", "");
        payment.put("message_format", FORMAT_MAP.getOrDefault(rail, "ISO20022"));
        log.info("[{}] INPUT  {}", svc, payment);
        Map<String, Object> result = restTemplate.postForObject(nextUrl + "/process", payment, Map.class);
        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
