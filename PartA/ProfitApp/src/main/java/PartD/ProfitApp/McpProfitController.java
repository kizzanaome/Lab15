package PartD.ProfitApp;


import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/mcp")
public class McpProfitController {

    private final ProfitRepository repo;

    public McpProfitController(ProfitRepository repo) {
        this.repo = repo;
    }

    // Lists tools available
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return Map.of(
                "tools", List.of(
                        Map.of(
                                "name", "getMonthlyProfit",
                                "description", "Get company profit for the given month in USD",
                                "parameters", Map.of("month", "String in format YYYY-MM")
                        )
                )
        );
    }

    // Executes a tool call
    @PostMapping("/call")
    public Map<String, Object> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        Map<String, Object> params = (Map<String, Object>) request.get("parameters");

        if ("getMonthlyProfit".equals(toolName)) {
            String month = (String) params.get("month");
            Profit p = repo.findById(month).orElseThrow();
            return Map.of("result", Map.of(
                    "month", p.getMonth(),
                    "amountUsd", p.getAmount()
            ));
        }
        return Map.of("error", "Unknown tool");
    }
}

