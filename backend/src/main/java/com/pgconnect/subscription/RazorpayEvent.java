package com.pgconnect.subscription;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("razorpay_events")
public class RazorpayEvent {
    @Id private String id; @Indexed(unique=true) private String eventId; private String event; private Instant receivedAt=Instant.now();
    public String getId(){return id;} public String getEventId(){return eventId;} public String getEvent(){return event;}
    public void setEventId(String v){eventId=v;} public void setEvent(String v){event=v;}
}
