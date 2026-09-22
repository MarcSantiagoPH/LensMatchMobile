package com.lensmatch.mobile.ui.guidelines;

public class GuidelineSlideModel {
    private final int iconResId;
    private final String stepTag;
    private final String title;
    private final String subtitle;
    private final String description;
    private final String tip;

    public GuidelineSlideModel(int iconResId, String stepTag, String title, String subtitle, String description, String tip) {
        this.iconResId = iconResId;
        this.stepTag = stepTag;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.tip = tip;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getStepTag() {
        return stepTag;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public String getTip() {
        return tip;
    }
}
