/**
 * =============================================================================
 * SESSION-AWARE NAVIGATION COMPONENT (TASK 9B.1)
 * Module: frontend/js/ui/nav.js
 * 
 * Responsibilities:
 * - Single authoritative navbar session rendering across the application.
 * - Renders Guest state (Đăng nhập, Bắt đầu học) vs Authenticated state
 *   (User Name, Role Badge, Hồ sơ link, Đăng xuất button).
 * - Reacts automatically to auth events: 'auth:login', 'auth:logout',
 *   'auth:expired', 'auth:update', and multi-tab synchronization.
 * - Safe DOM construction (zero unsafe innerHTML) defending against XSS.
 * - Accessible keyboard navigation and WCAG 2.2 AA compliant focus states.
 * =============================================================================
 */

import { authManager } from '../auth/auth-state.js';
import { createSafeElement, clearContainer } from './security.js';
import { showToast } from './ui.js';

/**
 * Maps backend JSON role string to scholarly Vietnamese display label.
 * Contract: Backend roles are ["Learner"], ["Creator"], ["Moderator"], ["Admin"].
 * 
 * @param {string} role 
 * @returns {string}
 */
export function getRoleLabel(role) {
  switch (role) {
    case 'Admin': return 'Quản trị';
    case 'Moderator': return 'Kiểm duyệt';
    case 'Creator': return 'Tác giả';
    case 'Learner':
    default:
      return 'Học viên';
  }
}

/**
 * Maps backend role to CSS badge classes defined in style.css.
 * 
 * @param {string} role 
 * @returns {string}
 */
export function getRoleBadgeClass(role) {
  switch (role) {
    case 'Admin':
      return 'badge-status badge-status-approved';
    case 'Moderator':
      return 'badge-status badge-status-active';
    case 'Creator':
      return 'badge-status badge-status-pending';
    case 'Learner':
    default:
      return 'badge-status badge-status-draft';
  }
}

/**
 * Resolves the primary/highest-privileged role from an array of roles.
 * Priority hierarchy: Admin > Moderator > Creator > Learner.
 * 
 * @param {string[]} roles 
 * @returns {string}
 */
export function getPrimaryRole(roles) {
  if (!Array.isArray(roles) || roles.length === 0) return 'Learner';
  if (roles.includes('Admin')) return 'Admin';
  if (roles.includes('Moderator')) return 'Moderator';
  if (roles.includes('Creator')) return 'Creator';
  return 'Learner';
}

/**
 * Authoritatively renders the authentication actions inside the navbar container.
 * 
 * @param {HTMLElement} container 
 */
export function renderNavbarAuth(container) {
  if (!container) return;

  clearContainer(container);

  if (!authManager.isAuthenticated()) {
    // --- GUEST STATE ---
    const loginLink = createSafeElement('a', {
      className: 'btn-chinese-secondary py-2 px-3 text-decoration-none',
      text: 'Đăng nhập',
      attrs: {
        href: 'login.html',
        id: 'navLoginBtn'
      }
    });

    const registerLink = createSafeElement('a', {
      className: 'btn-chinese-primary py-2 px-3 text-decoration-none',
      text: 'Bắt đầu học',
      attrs: {
        href: 'register.html',
        id: 'navRegisterBtn'
      }
    });

    container.appendChild(loginLink);
    container.appendChild(registerLink);
  } else {
    // --- AUTHENTICATED STATE ---
    const user = authManager.getUser();
    const displayName = user?.fullName?.trim() || user?.emailOrPhone || 'Học viên';
    const roles = authManager.getRoles();
    const primaryRole = getPrimaryRole(roles);

    const userWrapper = createSafeElement('div', {
      className: 'd-flex align-items-center flex-wrap gap-2',
      attrs: { id: 'navAuthenticatedSection' },
      children: [
        // Role Badge (Decorative UX metadata; backend is security boundary)
        createSafeElement('span', {
          className: getRoleBadgeClass(primaryRole),
          text: getRoleLabel(primaryRole),
          attrs: {
            id: 'navRoleBadge',
            'aria-label': `Vai trò: ${getRoleLabel(primaryRole)}`
          }
        }),

        // User Profile Link
        createSafeElement('a', {
          className: 'nav-link-custom py-1 px-2 fw-semibold text-dark text-decoration-none d-inline-flex align-items-center',
          text: displayName,
          attrs: {
            href: 'profile.html',
            id: 'navUserNameLink',
            title: `Xem thông tin hồ sơ của ${displayName}`
          }
        }),

        // Admin Management link (for Admin)
        ...(roles.includes('Admin') ? [
          createSafeElement('a', {
            className: 'btn-chinese-outline py-1 px-3 text-decoration-none btn-sm',
            text: 'Quản trị',
            attrs: {
              href: 'admin-lessons.html',
              id: 'navAdminBtn',
              'aria-label': 'Khu vực quản trị & giám sát hệ thống'
            }
          })
        ] : []),

        // Moderator Queue link (for Moderator or Admin)
        ...(roles.includes('Moderator') || roles.includes('Admin') ? [
          createSafeElement('a', {
            className: 'btn-chinese-outline py-1 px-3 text-decoration-none btn-sm',
            text: 'Kiểm duyệt',
            attrs: {
              href: 'moderator-queue.html',
              id: 'navModeratorBtn',
              'aria-label': 'Hàng đợi kiểm duyệt bài học'
            }
          })
        ] : []),

        // Creator Studio link (for Creator or Admin)
        ...(roles.includes('Creator') || roles.includes('Admin') ? [
          createSafeElement('a', {
            className: 'btn-chinese-outline py-1 px-3 text-decoration-none btn-sm',
            text: 'Studio',
            attrs: {
              href: 'creator-lessons.html',
              id: 'navCreatorStudioBtn',
              'aria-label': 'Creator Lesson Studio — Quản lý bài học'
            }
          })
        ] : []),

        // Profile navigation action button
        createSafeElement('a', {
          className: 'btn-chinese-outline py-1 px-3 text-decoration-none btn-sm',
          text: 'Hồ sơ',
          attrs: {
            href: 'profile.html',
            id: 'navProfileBtn',
            'aria-label': 'Trang quản lý hồ sơ cá nhân'
          }
        }),

        // Logout button
        createSafeElement('button', {
          className: 'btn-chinese-secondary py-1 px-3 btn-sm',
          text: 'Đăng xuất',
          attrs: {
            type: 'button',
            id: 'navLogoutBtn',
            'aria-label': 'Đăng xuất khỏi phiên làm việc'
          }
        })
      ]
    });

    // Bind logout handler
    const logoutBtn = userWrapper.querySelector('#navLogoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        const isProtectedPage = typeof window !== 'undefined' && 
          (window.location.pathname.endsWith('profile.html') ||
           window.location.pathname.endsWith('srs-dashboard.html') ||
           window.location.pathname.endsWith('srs-review.html') ||
           window.location.pathname.endsWith('creator-lessons.html') ||
           window.location.pathname.endsWith('creator-lesson-editor.html') ||
           window.location.pathname.endsWith('creator-import.html') ||
           window.location.pathname.endsWith('moderator-queue.html') ||
           window.location.pathname.endsWith('moderator-review.html') ||
           window.location.pathname.endsWith('moderator-history.html') ||
           window.location.pathname.endsWith('admin-accounts.html') ||
           window.location.pathname.endsWith('admin-roles.html') ||
           window.location.pathname.endsWith('admin-radicals.html') ||
           window.location.pathname.endsWith('admin-vocabulary.html') ||
           window.location.pathname.endsWith('admin-lessons.html'));

        authManager.logout(false);

        if (isProtectedPage) {
          window.location.href = 'index.html';
        } else {
          try {
            showToast({
              type: 'info',
              title: 'Phiên làm việc',
              message: 'Bạn đã đăng xuất thành công.'
            });
          } catch (_) {
            // Toast notification optional in standalone environments
          }
        }
      });
    }

    container.appendChild(userWrapper);
  }
}

/**
 * Initializes the navbar authentication watcher and binds auth lifecycle events.
 * 
 * @param {string|HTMLElement} containerOrId - Container ID or HTMLElement instance
 * @returns {() => void} Cleanup function to unbind listeners
 */
export function initNavbarAuth(containerOrId = 'navAuthContainer') {
  const container = typeof containerOrId === 'string'
    ? document.getElementById(containerOrId)
    : containerOrId;

  if (!container) return () => {};

  renderNavbarAuth(container);

  const handleAuthChange = () => {
    renderNavbarAuth(container);
  };

  if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
    window.addEventListener('auth:login', handleAuthChange);
    window.addEventListener('auth:logout', handleAuthChange);
    window.addEventListener('auth:expired', handleAuthChange);
    window.addEventListener('auth:update', handleAuthChange);
  }

  return () => {
    if (typeof window !== 'undefined' && typeof window.removeEventListener === 'function') {
      window.removeEventListener('auth:login', handleAuthChange);
      window.removeEventListener('auth:logout', handleAuthChange);
      window.removeEventListener('auth:expired', handleAuthChange);
      window.removeEventListener('auth:update', handleAuthChange);
    }
  };
}
