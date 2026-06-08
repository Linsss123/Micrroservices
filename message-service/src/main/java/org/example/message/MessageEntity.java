package org.example.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(length = 100, nullable = false)
    private String senderId;

    @Column(length = 1000, nullable = false)
    private String text;

    @Column(nullable = false)
    private Instant timestamp;

    protected MessageEntity() {}

    public MessageEntity(String id, String senderId, String text, Instant timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public Instant getTimestamp() { return timestamp; }

    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
