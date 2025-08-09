package CompanyService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/company/ask?question=...
 * The model can call CompanyTools.getMonthlyProfit and CompanyTools.convertUsd while answering.
 */
@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final ChatClient chat;
    private final CompanyTools tools;

    public CompanyController(ChatClient.Builder builder, CompanyTools tools) {
        // Important: register tools on the ChatClient you build.
        this.chat = builder.defaultTools(tools).build();
        this.tools = tools;
    }

    @GetMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestParam String question) {
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }

        // Strong system prompt to keep the model on task.
        String system = """
            You are the company's financial assistant.
            - If the user asks about profit for a specific month (YYYY-MM), call the profit tool.
            - If they want the amount in another currency, call the currency tool after fetching USD.
            - Be concise and show both USD and converted amount when conversion is requested.
            - If the month is missing or invalid, ask the user to provide it as YYYY-MM.
            """;

        String answer = chat.prompt()
                .system(system)
                .user(question)
                .call()            // <-- model may call tools here (function-calling)
                .content();        // final text after all tool calls

        Map<String, String> out = new HashMap<>();
        out.put("question", question);
        out.put("answer", answer);
        return ResponseEntity.ok(out);
    }
}
