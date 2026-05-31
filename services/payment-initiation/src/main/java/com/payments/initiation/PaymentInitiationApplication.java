package com.payments.initiation;

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
public class PaymentInitiationApplication {

    @Value("${spring.application.name}") private String svc;
    @Value("${next.service.url}")        private String nextUrl;
    @Autowired private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(PaymentInitiationApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", svc);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/initiate")
    public Map<String, Object> initiate(@RequestBody Map<String, Object> payment) {
        log.info("[{}] INPUT  {}", svc, payment);
        Map<String, Object> result = restTemplate.postForObject(nextUrl + "/process", payment, Map.class);
        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
