package CurrencyApp;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {

    /**
     * GET /api/currency/usd-to?amount=100&currency=UGX
     *  simple fixed rates .
     */
    @GetMapping("/usd-to")
    public double convertUsd(@RequestParam double amount, @RequestParam String currency) {
        double rate = switch (currency.toUpperCase()) {
            case "EUR" -> 0.92;
            case "GBP" -> 0.78;
            case "UGX" -> 3800.0;
            case "KES" -> 129.0;
            default -> 1.0; // USD fallback
        };
        return amount * rate;
    }
}
