package org.clubplus.clubplusbackend.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomepageStatsDTO {
    private long clubCount;
    private long eventCount;
    private long memberCount;


    public HomepageStatsDTO(long clubCount, long eventCount, long memberCount) {
        this.clubCount = clubCount;
        this.eventCount = eventCount;
        this.memberCount = memberCount;
    }
}
