package com.webapp.cognitodemo.entity.peer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PeerSessionRespondRequest {

    @NotNull(message = "accept is required")
    private Boolean accept;

    /* Optional — teacher can paste a meeting link (e.g. Google Meet) when accepting. */
    private String meetingLink;
}
