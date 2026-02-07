package com.animalprotection.dto;

public class InventoryItemCreateRequest {
    private String itemName;
    private String productionDate;
    private String expiryDate;
    private Double stockQty;
    private Double lowStockThreshold;
    private Integer warningDays;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(String productionDate) {
        this.productionDate = productionDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Double getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Double lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getWarningDays() {
        return warningDays;
    }

    public void setWarningDays(Integer warningDays) {
        this.warningDays = warningDays;
    }
}
