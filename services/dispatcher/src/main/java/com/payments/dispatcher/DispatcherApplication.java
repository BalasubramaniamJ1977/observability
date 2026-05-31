package com.payments.dispatcher;

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
public class DispatcherApplication {

    private static final Map<String, String> ENDPOINTS = Map.of(
        "SWIFT", "swift.network.internal:9000",
        "SEPA",  "sepa.clearing.internal:9001",
        "CHAPS", "chaps.boe.internal:9002",
        "BACS",  "bacs.clearing.internal:9003",
        "FPS",   "fps.link.internal:9004",
        "RTP",   "rtp.network.internal:9005"
    );

    @Value("${spring.application.name}") private String svc;
    @Value("${next.service.url}")        private String nextUrl;
    @Autowired private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(DispatcherApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", svc); }

    @SuppressWarnings("unchecked")
    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> payment) {
        String rail    = (String) payment.getOrDefault("routing_rail", "");
        String postRef = (String) payment.getOrDefault("posting_ref", payment.getOrDefault("uetr", "UNKNOWN").toString().substring(0, 8).toUpperCase());
        payment.put("network_endpoint", ENDPOINTS.getOrDefault(rail, "unknown.network:9999"));
        payment.put("dispatch_ref",     "DSP-" + postRef);
        log.info("[{}] INPUT  {}", svc, payment);
        Map<String, Object> result = restTemplate.postForObject(nextUrl + "/process", payment, Map.class);
        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
