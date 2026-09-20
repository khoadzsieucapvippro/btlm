package com.elearning.service.srs;

/**
 * Enumeration representing user recall evaluation ratings in the Spaced Repetition System (SRS).
 * Follows the project's 4-grade rating scale:
 * <ul>
 *   <li>1 = Again: Failed recall (quên hoàn toàn / học lại) -> resets repetitions to 0</li>
 *   <li>2 = Hard: Successful but difficult recall (nhớ nhưng vất vả) -> increments repetitions, decreases EF</li>
 *   <li>3 = Good: Normal successful recall (nhớ chuẩn) -> increments repetitions, preserves EF</li>
 *   <li>4 = Easy: Perfect/effortless recall (nhớ tức thì / rất dễ) -> increments repetitions, increases EF</li>
 * </ul>
 */
public enum ReviewRating {
    AGAIN(1, "Again", "Học lại / quên hoàn toàn"),
    HARD(2, "Hard", "Khó / nhớ nhưng vất vả"),
    GOOD(3, "Good", "Tốt / nhớ chuẩn"),
    EASY(4, "Easy", "Dễ / nhớ tức thì");

    private final int value;
    private final String label;
    private final String description;

    ReviewRating(int value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves a numeric integer rating into its corresponding {@link ReviewRating} enum.
     *
     * @param value numeric rating (must be 1, 2, 3, or 4)
     * @return corresponding ReviewRating enum
     * @throws IllegalArgumentException if rating value is outside 1..4 range
     */
    public static ReviewRating fromValue(int value) {
        for (ReviewRating rating : values()) {
            if (rating.value == value) {
                return rating;
            }
        }
        throw new IllegalArgumentException("Giá trị rating không hợp lệ: " + value + ". Giá trị hợp lệ: 1 (Again), 2 (Hard), 3 (Good), 4 (Easy)");
    }
}
