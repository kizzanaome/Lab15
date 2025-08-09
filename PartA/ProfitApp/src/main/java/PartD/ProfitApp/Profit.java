package PartD.ProfitApp;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Stores profit per month. Primary key is the ISO month string, e.g. "2025-07". */
@Entity
public class Profit {
    @Id
    private String month;
    private double amount;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
