package com.hongyuwu.careerchronicle.client;

public enum ManualPage {
    OVERVIEW("careerchronicle.ui.manual_overview", "careerchronicle.ui.manual_overview_title", "careerchronicle.ui.manual_overview_hint"),
    RACES("careerchronicle.ui.manual_races", "careerchronicle.ui.manual_races_title", "careerchronicle.ui.manual_races_hint"),
    CLASSES("careerchronicle.ui.manual_classes", "careerchronicle.ui.manual_classes_title", "careerchronicle.ui.manual_classes_hint"),
    SKILLS("careerchronicle.ui.manual_skills", "careerchronicle.ui.manual_skills_title", "careerchronicle.ui.manual_skills_hint"),
    CLUES("careerchronicle.ui.manual_clues", "careerchronicle.ui.manual_clues_title", "careerchronicle.ui.manual_clues_hint"),
    FUSIONS("careerchronicle.ui.manual_fusions", "careerchronicle.ui.manual_fusions_title", "careerchronicle.ui.manual_fusions_hint");

    public final String labelKey;
    public final String titleKey;
    public final String hintKey;

    ManualPage(String labelKey, String titleKey, String hintKey) {
        this.labelKey = labelKey;
        this.titleKey = titleKey;
        this.hintKey = hintKey;
    }
}
