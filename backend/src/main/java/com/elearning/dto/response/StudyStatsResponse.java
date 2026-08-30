package com.elearning.dto.response;

/**
 * Response DTO providing learner study statistics for the SRS dashboard.
 */
public class StudyStatsResponse {

    private long cardsDue;
    private long reviewsToday;
    private int newCardsLimit;
    private int maxReviewLimit;
    private long newCardsToday;

    public StudyStatsResponse() {
    }

    public StudyStatsResponse(long cardsDue, long reviewsToday, int newCardsLimit, int maxReviewLimit) {
        this(cardsDue, reviewsToday, newCardsLimit, maxReviewLimit, 0L);
    }

    public StudyStatsResponse(long cardsDue, long reviewsToday, int newCardsLimit, int maxReviewLimit, long newCardsToday) {
        this.cardsDue = cardsDue;
        this.reviewsToday = reviewsToday;
        this.newCardsLimit = newCardsLimit;
        this.maxReviewLimit = maxReviewLimit;
        this.newCardsToday = newCardsToday;
    }

    public long getCardsDue() {
        return cardsDue;
    }

    public void setCardsDue(long cardsDue) {
        this.cardsDue = cardsDue;
    }

    public long getReviewsToday() {
        return reviewsToday;
    }

    public void setReviewsToday(long reviewsToday) {
        this.reviewsToday = reviewsToday;
    }

    public int getNewCardsLimit() {
        return newCardsLimit;
    }

    public void setNewCardsLimit(int newCardsLimit) {
        this.newCardsLimit = newCardsLimit;
    }

    public int getMaxReviewLimit() {
        return maxReviewLimit;
    }

    public void setMaxReviewLimit(int maxReviewLimit) {
        this.maxReviewLimit = maxReviewLimit;
    }

    public long getNewCardsToday() {
        return newCardsToday;
    }

    public void setNewCardsToday(long newCardsToday) {
        this.newCardsToday = newCardsToday;
    }
}
