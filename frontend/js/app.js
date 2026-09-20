/**
 * =============================================================================
 * CORE APPLICATION SHELL BOOTSTRAP (PRODUCTION HOMEPAGE)
 * Module: frontend/js/app.js
 * 
 * Responsibilities:
 * - Production Shell Initialization
 * - Accessible Responsive Navigation (aria-expanded sync)
 * - Session-Aware Navbar Integration via initNavbarAuth()
 * 
 * Non-Negotiable:
 * - Zero verification harness or test-only logic.
 * - Zero direct DOM mutations for testing.
 * =============================================================================
 */

import { initNavbarAuth } from './ui/nav.js';

/**
 * Initializes the production application shell on DOM ready.
 */
document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initNavbarAuth('navAuthContainer');
});

/**
 * Handles accessible mobile navigation disclosure attributes.
 */
export function initNavbar() {
  const navToggler = document.querySelector('.navbar-toggler');
  const navCollapse = document.getElementById('primaryNavMenu');

  if (navToggler && navCollapse) {
    navCollapse.addEventListener('show.bs.collapse', () => {
      navToggler.setAttribute('aria-expanded', 'true');
    });

    navCollapse.addEventListener('hide.bs.collapse', () => {
      navToggler.setAttribute('aria-expanded', 'false');
    });
  }
}
