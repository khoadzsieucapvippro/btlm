package com.elearning.repository;

/**
 * Lightweight projection record containing account authentication & authorization status.
 * Used by {@link com.elearning.security.JwtAuthenticationFilter} to verify account active status
 * and authorization version in a single lightweight indexed query without loading the entire Account entity graph.
 *
 * @param status account status string (e.g. 'Active', 'Inactive', 'Banned')
 * @param authorizationVersion current authorization version of the account
 */
public record AccountAuthSummary(String status, Long authorizationVersion) {
}
