package PartD.ProfitApp;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/** Inserts some months on startup so we can query immediately. */
@Configuration
public class DataLoader {
    @Bean
    CommandLineRunner init(ProfitRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                Profit p1 = new Profit(); p1.setMonth("2025-06"); p1.setAmount(17850.00);
                Profit p2 = new Profit(); p2.setMonth("2025-07"); p2.setAmount(22100.50);
                Profit p3 = new Profit(); p3.setMonth("2025-08"); p3.setAmount(19999.99);
                repo.save(p1); repo.save(p2); repo.save(p3);
            }
        };
    }
}
