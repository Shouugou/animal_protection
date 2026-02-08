package com.animalprotection.dto;

import java.util.List;

public class PatrolReportRequest {
    private Long claimId;
    private String summary;
    private Double distanceKm;
    private Integer durationSec;
    private List<PatrolTrackPoint> trackPoints;
    private List<RiskPoint> riskPoints;

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public List<PatrolTrackPoint> getTrackPoints() {
        return trackPoints;
    }

    public void setTrackPoints(List<PatrolTrackPoint> trackPoints) {
        this.trackPoints = trackPoints;
    }

    public List<RiskPoint> getRiskPoints() {
        return riskPoints;
    }

    public void setRiskPoints(List<RiskPoint> riskPoints) {
        this.riskPoints = riskPoints;
    }

    public static class PatrolTrackPoint {
        private Integer seqNo;
        private Double latitude;
        private Double longitude;
        private String pointTime;
        private String address;

        public Integer getSeqNo() {
            return seqNo;
        }

        public void setSeqNo(Integer seqNo) {
            this.seqNo = seqNo;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getPointTime() {
            return pointTime;
        }

        public void setPointTime(String pointTime) {
            this.pointTime = pointTime;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    public static class RiskPoint {
        private String riskType;
        private String description;
        private String address;
        private Double latitude;
        private Double longitude;
        private String foundAt;

        public String getRiskType() {
            return riskType;
        }

        public void setRiskType(String riskType) {
            this.riskType = riskType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getFoundAt() {
            return foundAt;
        }

        public void setFoundAt(String foundAt) {
            this.foundAt = foundAt;
        }
    }
}
