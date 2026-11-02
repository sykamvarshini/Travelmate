package com.travelmate.dto;

import java.util.List;

public class WeatherResponse {
    private String city;
    private Double currentTemperatureC;
    private String condition;
    private Double rainProbability;
    private Double windSpeedKmH;
    private List<DailyForecast> forecast;
    private Boolean isLive;
    private String warning;

    public static class DailyForecast {
        private String date;
        private Double maxTempC;
        private Double minTempC;
        private String condition;
        private Double rainProbability;

        public DailyForecast() {}
        public DailyForecast(String date, Double maxTempC, Double minTempC, String condition, Double rainProbability) {
            this.date = date;
            this.maxTempC = maxTempC;
            this.minTempC = minTempC;
            this.condition = condition;
            this.rainProbability = rainProbability;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Double getMaxTempC() { return maxTempC; }
        public void setMaxTempC(Double maxTempC) { this.maxTempC = maxTempC; }

        public Double getMinTempC() { return minTempC; }
        public void setMinTempC(Double minTempC) { this.minTempC = minTempC; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public Double getRainProbability() { return rainProbability; }
        public void setRainProbability(Double rainProbability) { this.rainProbability = rainProbability; }
    }

    public WeatherResponse() {}

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getCurrentTemperatureC() { return currentTemperatureC; }
    public void setCurrentTemperatureC(Double currentTemperatureC) { this.currentTemperatureC = currentTemperatureC; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Double getRainProbability() { return rainProbability; }
    public void setRainProbability(Double rainProbability) { this.rainProbability = rainProbability; }

    public Double getWindSpeedKmH() { return windSpeedKmH; }
    public void setWindSpeedKmH(Double windSpeedKmH) { this.windSpeedKmH = windSpeedKmH; }

    public List<DailyForecast> getForecast() { return forecast; }
    public void setForecast(List<DailyForecast> forecast) { this.forecast = forecast; }

    public Boolean getIsLive() { return isLive; }
    public void setIsLive(Boolean isLive) { this.isLive = isLive; }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }
}
