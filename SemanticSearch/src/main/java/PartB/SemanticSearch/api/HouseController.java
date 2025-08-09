package PartB.SemanticSearch.api;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rentalsearch.model.RentalHouse;
import rentalsearch.service.RedisVectorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/houses")
public class HouseController {

    private final RedisVectorService service;

    public HouseController(RedisVectorService service) {
        this.service = service;
    }

    /** Add a house; we embed and index it. */
    @PostMapping
    public ResponseEntity<Map<String, String>> add(@RequestBody RentalHouse h) {
        if (h.getDescription() == null || h.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "description is required"));
        }
        String id = service.addHouse(h);
        return ResponseEntity.ok(Map.of("id", id));
    }

    /** Semantic search: returns top-k closest listings by meaning. */
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam @NotBlank String prompt,
            @RequestParam(defaultValue = "3") @Min(1) int k) {
        return service.search(prompt, k);
    }
}
