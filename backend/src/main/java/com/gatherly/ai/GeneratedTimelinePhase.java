package com.gatherly.ai;

import java.util.List;

public record GeneratedTimelinePhase(
    String phaseName,
    String timeframe,
    List<String> keyMilestones
) {}
