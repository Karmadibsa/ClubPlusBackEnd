package org.clubplus.clubplusbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data // Génère getters, setters, toString, equals, hashCode
@Builder // Permet une construction facile
public class DashboardSummaryDto {
    private long totalEvents;
    private long upcomingEventsCount30d;
    private double averageEventOccupancyRate;
    private long totalActiveMembers;
    private long totalParticipations;
    private List<Map<String, Object>> monthlyRegistrations;
    private Map<String, Double> averageEventRatings;

}
