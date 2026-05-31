package com.payments.accounting;

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
import java.util.UUID;

@SpringBootApplication
@RestController
@Slf4j
public class AccountingGeneratorApplication {

    @Value("${spring.application.name}") private String svc;
    @Value("${next.service.url}")        private String nextUrl;
    @Autowired private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(AccountingGeneratorApplication.class, args);
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "ok", "service", svc); }

    @SuppressWarnings("unchecked")
    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> payment) {
        String jid = UUID.randomUUID().toString();
        payment.put("journal_id",    jid);
        payment.put("debit_entry",   "DR-" + jid.substring(0, 8).toUpperCase());
        payment.put("credit_entry",  "CR-" + jid.substring(0, 8).toUpperCase());
        log.info("[{}] INPUT  {}", svc, payment);
        Map<String, Object> result = restTemplate.postForObject(nextUrl + "/process", payment, Map.class);
        log.info("[{}] OUTPUT {}", svc, result);
        return result;
    }
}
