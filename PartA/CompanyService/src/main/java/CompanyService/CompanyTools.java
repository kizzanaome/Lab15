package CompanyService;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.tool.annotation.Tool;  // annotation that makes this method callable by the LLM

import java.util.Map;

/**
 * Tools = functions the model can call when answering questions.
 * - getMonthlyProfit(month) calls the Profit service
 * - convertUsd(amount, currency) calls the Currency service
 */
@Component
public class CompanyTools {

    private final RestTemplate rt;

    public CompanyTools(RestTemplate rt) {
        this.rt = rt;
    }

    /**
     * LLM will call this when it needs profit for a month.
     * @param month ISO "YYYY-MM" (e.g. "2025-07")
     * @return map with month and amount
     */
    @Tool(name = "getMonthlyProfit",
            description = "Get company profit for the given month in USD. month format: YYYY-MM")
    public Map<String, Object> getMonthlyProfit(String month) {
        return callProfitTool(month);
    }

    public Map<String, Object> callProfitTool(String month) {
        String url = "http://localhost:8091/mcp/call";
        Map<String, Object> payload = Map.of(
                "tool", "getMonthlyProfit",
                "parameters", Map.of("month", month)
        );
        return rt.postForObject(url, payload, Map.class);
    }


    /**
     * LLM will call this when it needs currency conversion.
     * @param amount amount in USD
     * @param currency target currency code, e.g. EUR, UGX
     * @return converted amount
     */
    @Tool(name = "convertUsd",
            description = "Convert an amount in USD to another currency, e.g. EUR, UGX, KES, GBP")
    public Map<String, Object> convertUsd(double amount, String currency) {
        String url = "http://localhost:8092/api/currency/usd-to?amount=" + amount + "&currency=" + currency;
        Double converted = rt.getForObject(url, Double.class);
        return Map.of(
                "currency", currency.toUpperCase(),
                "amount", converted
        );
    }
}
