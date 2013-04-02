package org.rhq.core.domain.search;

import java.io.Serializable;

public class SearchSuggestion implements Serializable, Comparable<SearchSuggestion> {

    private static final long serialVersionUID = 1L;

    public enum Kind {
        Unknown(""), //
        InstructionalTextComment(""), //
        GlobalSavedSearch("GLOBAL"), //
        UserSavedSearch("SAVED"), //
        AdvancedOperator("SYNTAX"), //
        Advanced("QUERY"), //
        Simple("TEXT");

        private String displayName;

        private Kind(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getName() {
            return name();
        }
    }

    private Kind kind;
    private String value;
    private String optional; // optional data based on kind.
    private String label;
    private int startIndex;
    private int endIndex;

    public SearchSuggestion() {
        // public ctor for GWT
    }

    public SearchSuggestion(Kind kind, String value) {
        this(kind, value, value, null, 0, 0);
    }

    public SearchSuggestion(Kind kind, String value, int startIndex, int length) {
        this(kind, value, value, null, startIndex, length);
    }

    public SearchSuggestion(Kind kind, String label, String value, int startIndex, int length) {
        this(kind, label, value, null, startIndex, length);
    }

    public SearchSuggestion(Kind kind, String label, String value, String optional, int startIndex, int length) {
        this.kind = kind;
        this.label = label;
        this.value = value;
        this.optional = optional;
        this.startIndex = startIndex;
        this.endIndex = startIndex + length;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getOptional() {
        return optional;
    }

    public void setOptional(String optional) {
        this.optional = optional;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int compareTo(SearchSuggestion other) {
        int kindComparision = (this.kind.ordinal() - other.kind.ordinal());
        if (kindComparision != 0) {
            return kindComparision;
        }
        return toLowerStripQuotes(label).compareTo(toLowerStripQuotes(other.label));
    }

    private static String toLowerStripQuotes(String data) {
        if (data.length() < 2) {
            return data;
        }
        char firstChar = data.charAt(0);
        char lastChar = data.charAt(data.length() - 1);
        if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
            return data.substring(1, data.length() - 1);
        }
        return data;
    }

    public String toString() {
        String before = label.substring(0, startIndex);
        String highlight = label.substring(startIndex, endIndex);
        String after = label.substring(endIndex);
        return before + "(" + highlight + ")" + after + "->" + value;
    }
}
