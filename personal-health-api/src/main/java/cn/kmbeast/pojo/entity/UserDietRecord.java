package cn.kmbeast.pojo.entity;

import java.time.LocalDateTime;

public class UserDietRecord {
    private Integer id;
    private Integer userId;
    private String foodName;
    private String calories;
    private String protein;
    private String fat;
    private String carbs;
    private LocalDateTime createTime;

    // Getter 和 Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getCalories() { return calories; }
    public void setCalories(String calories) { this.calories = calories; }
    public String getProtein() { return protein; }
    public void setProtein(String protein) { this.protein = protein; }
    public String getFat() { return fat; }
    public void setFat(String fat) { this.fat = fat; }
    public String getCarbs() { return carbs; }
    public void setCarbs(String carbs) { this.carbs = carbs; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}