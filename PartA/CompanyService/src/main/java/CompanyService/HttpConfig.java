package CompanyService;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Provides a RestTemplate with an interceptor that logs requests and responses.
 * This lets you see Company -> Profit/Currency traffic in the console.
 */
@Configuration
public class HttpConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.getInterceptors().add((request, body, execution) -> {
            System.out.println("---- OUTGOING REQUEST ----");
            System.out.println(request.getMethod() + " " + request.getURI());
            request.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
            if (body != null && body.length > 0) {
                System.out.println("Body: " + new String(body, StandardCharsets.UTF_8));
            }
            ClientHttpResponse response = execution.execute(request, body);
            System.out.println("---- INCOMING RESPONSE ----");
            System.out.println("Status: " + response.getStatusCode());
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line; StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) sb.append(line);
                System.out.println("Body: " + sb);
            }
            return response;
        });
        return rt;
    }
}
