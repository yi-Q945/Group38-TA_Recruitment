(function () {
  var prefersReducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function formatFileSize(bytes) {
    if (!bytes && bytes !== 0) {
      return '';
    }
    if (bytes < 1024) {
      return bytes + ' B';
    }
    if (bytes < 1024 * 1024) {
      return (bytes / 1024).toFixed(1) + ' KB';
    }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  function enhanceRevealMotion() {
    var selector = '.hero-card, .panel, .kpi-card, .job-card, .spotlight-card, .feature-card, .journey-step, .signal-card';
    var items = document.querySelectorAll(selector);

    if (!items.length) {
      return;
    }

    items.forEach(function (item, index) {
      if (!item.classList.contains('reveal-ready')) {
        item.classList.add('reveal-ready');
        item.style.setProperty('--reveal-delay', Math.min(index * 45, 260) + 'ms');
      }
    });

    if (prefersReducedMotion || !('IntersectionObserver' in window)) {
      items.forEach(function (item) {
        item.classList.add('is-visible');
      });
      return;
    }

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    }, {
      threshold: 0.12,
      rootMargin: '0px 0px -24px 0px'
    });

    items.forEach(function (item) {
      observer.observe(item);
    });
  }

  function enhanceAlerts() {
    var alerts = document.querySelectorAll('.alert');
    alerts.forEach(function (alert) {
      window.setTimeout(function () {
        alert.classList.add('alert-exit');
      }, 5200);
      window.setTimeout(function () {
        if (alert.parentNode) {
          alert.parentNode.removeChild(alert);
        }
      }, 5800);
    });
  }

  function enhanceUploadZones() {
    var zones = document.querySelectorAll('[data-upload-zone]');
    zones.forEach(function (zone) {
      var input = zone.querySelector('input[type="file"]');
      var fileName = zone.querySelector('[data-upload-filename]');
      var hint = zone.querySelector('[data-upload-hint]');

      if (!input) {
        return;
      }

      var maxMb = Number(input.getAttribute('data-max-mb') || '5');
      var defaultName = fileName ? fileName.textContent : '';
      var defaultHint = hint ? hint.textContent : '';

      function renderFileState(file, droppedWithoutBinding) {
        zone.classList.toggle('has-file', !!file);
        zone.classList.remove('is-invalid');

        if (!file) {
          if (fileName) {
            fileName.textContent = defaultName;
          }
          if (hint) {
            hint.textContent = defaultHint;
          }
          return;
        }

        if (fileName) {
          fileName.textContent = file.name;
        }

        if (hint) {
          hint.textContent = formatFileSize(file.size) + (droppedWithoutBinding ? ' · Drop detected, then click choose if your browser blocks auto-attach.' : ' · Ready to upload');
        }

        if (file.size > maxMb * 1024 * 1024) {
          zone.classList.add('is-invalid');
          if (hint) {
            hint.textContent = formatFileSize(file.size) + ' · File exceeds the ' + maxMb + 'MB limit';
          }
        }
      }

      input.addEventListener('change', function () {
        renderFileState(input.files && input.files[0] ? input.files[0] : null, false);
      });

      ['dragenter', 'dragover'].forEach(function (eventName) {
        zone.addEventListener(eventName, function (event) {
          event.preventDefault();
          zone.classList.add('is-dragging');
        });
      });

      ['dragleave', 'dragend', 'drop'].forEach(function (eventName) {
        zone.addEventListener(eventName, function (event) {
          event.preventDefault();
          zone.classList.remove('is-dragging');
        });
      });

      zone.addEventListener('drop', function (event) {
        var files = event.dataTransfer && event.dataTransfer.files;
        if (!files || !files.length) {
          return;
        }

        var file = files[0];
        try {
          input.files = files;
          input.dispatchEvent(new Event('change', { bubbles: true }));
        } catch (error) {
          renderFileState(file, true);
        }
      });

      renderFileState(input.files && input.files[0] ? input.files[0] : null, false);
    });
  }

  function enhanceFormSubmissions() {
    document.addEventListener('submit', function (event) {
      var form = event.target;
      if (!(form instanceof HTMLFormElement)) {
        return;
      }

      if (typeof form.checkValidity === 'function' && !form.checkValidity()) {
        return;
      }

      // GET forms (filters, search) should NOT lock the button — the page navigates away anyway
      if (form.method.toUpperCase() === 'GET') {
        return;
      }

      var submitter = event.submitter || form.querySelector('button[type="submit"], input[type="submit"]');
      if (!submitter || submitter.dataset.loadingApplied === 'true') {
        return;
      }

      submitter.dataset.loadingApplied = 'true';
      submitter.dataset.originalText = submitter.textContent;
      submitter.classList.add('is-loading');
      submitter.disabled = true;
      submitter.textContent = submitter.getAttribute('data-loading-text') || (submitter.textContent.replace(/\.\.\.$/, '') + '...');

      // Safety net: restore button after 8 seconds in case redirect doesn't happen
      window.setTimeout(function () {
        if (submitter.dataset.loadingApplied === 'true') {
          submitter.disabled = false;
          submitter.classList.remove('is-loading');
          submitter.textContent = submitter.dataset.originalText || 'Submit';
          delete submitter.dataset.loadingApplied;
        }
      }, 8000);
    }, true);
  }

  function enhanceLoginQuickFill() {
    var usernameInput = document.querySelector('[data-login-username]');
    var passwordInput = document.querySelector('[data-login-password]');
    if (!usernameInput || !passwordInput) {
      return;
    }

    var loginCard = document.querySelector('.login-card');
    var buttons = document.querySelectorAll('[data-fill-login]');
    buttons.forEach(function (button) {
      button.addEventListener('click', function () {
        usernameInput.value = button.getAttribute('data-username') || '';
        passwordInput.value = button.getAttribute('data-password') || '';
        usernameInput.dispatchEvent(new Event('input', { bubbles: true }));
        passwordInput.dispatchEvent(new Event('input', { bubbles: true }));
        passwordInput.focus();

        if (loginCard) {
          loginCard.classList.remove('flash-card');
          void loginCard.offsetWidth;
          loginCard.classList.add('flash-card');
        }
      });
    });
  }

  function enhanceAutoRefresh() {
    var refreshSeconds = Number(document.body.getAttribute('data-auto-refresh-seconds') || '0');
    if (!refreshSeconds) {
      return;
    }

    var indicators = document.querySelectorAll('[data-refresh-countdown]');
    var remaining = refreshSeconds;
    var reloading = false;

    function isEditing() {
      var active = document.activeElement;
      return !!(active && active.matches && active.matches('input, textarea, select, [contenteditable="true"]'));
    }

    function render(paused) {
      indicators.forEach(function (indicator) {
        if (reloading) {
          indicator.textContent = 'Refreshing...';
        } else if (paused) {
          indicator.textContent = 'Auto-refresh paused while you edit';
        } else {
          indicator.textContent = 'Refresh in ' + remaining + 's';
        }
      });
    }

    render(false);

    var intervalId = window.setInterval(function () {
      if (reloading) {
        return;
      }

      var paused = document.hidden || isEditing();
      if (paused) {
        remaining = refreshSeconds;
        render(true);
        return;
      }

      remaining -= 1;
      if (remaining <= 0) {
        reloading = true;
        window.clearInterval(intervalId);
        render(false);
        window.location.reload();
        return;
      }

      render(false);
    }, 1000);
  }

  document.addEventListener('DOMContentLoaded', function () {
    enhanceRevealMotion();
    enhanceAlerts();
    enhanceUploadZones();
    enhanceFormSubmissions();
    enhanceLoginQuickFill();
    enhanceAutoRefresh();
  });
})();
