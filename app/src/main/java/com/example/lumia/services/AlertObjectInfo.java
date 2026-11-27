package com.example.lumia.services;

import android.graphics.Rect;

public class AlertObjectInfo {

    private final Rect boundingBox;
    private final String label;
    private final float confidence;

    public AlertObjectInfo(Rect boundingBox, String label, float confidence) {
        this.boundingBox = new Rect(boundingBox);
        this.label = label;
        this.confidence = confidence;
    }

    public Rect getBoundingBox() {
        return new Rect(boundingBox);
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }
}