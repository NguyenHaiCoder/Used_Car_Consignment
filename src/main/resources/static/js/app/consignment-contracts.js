/* Hà Lệ Quyên: ký tên bằng chuột / touch trên canvas hợp đồng. */
(() => {
  'use strict';

  const root = document.getElementById('contract-create');
  if (!root) return;

  function setupPad(pad) {
    const canvas = pad.querySelector('canvas.sig-canvas');
    const block = pad.closest('.sig-block');
    const clearBtn = block?.querySelector('.sig-clear');
    const key = pad.getAttribute('data-sig');
    const preview = root.querySelector(`[data-sig-preview="${key}"]`);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let drawing = false;
    let hasInk = false;
    let lastX = 0;
    let lastY = 0;

    function resize() {
      const ratio = window.devicePixelRatio || 1;
      const rect = pad.getBoundingClientRect();
      const w = Math.max(1, Math.round(rect.width));
      const h = Math.max(1, Math.round(rect.height));
      const snapshot = hasInk ? canvas.toDataURL() : null;
      canvas.width = Math.round(w * ratio);
      canvas.height = Math.round(h * ratio);
      canvas.style.width = `${w}px`;
      canvas.style.height = `${h}px`;
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = '#0b1f3a';
      ctx.lineWidth = 2.2;
      if (snapshot) {
        const img = new Image();
        img.onload = () => {
          ctx.clearRect(0, 0, w, h);
          ctx.drawImage(img, 0, 0, w, h);
        };
        img.src = snapshot;
      } else {
        ctx.clearRect(0, 0, w, h);
      }
    }

    function point(event) {
      const rect = canvas.getBoundingClientRect();
      const src = event.touches ? event.touches[0] : event;
      return {
        x: src.clientX - rect.left,
        y: src.clientY - rect.top,
      };
    }

    function syncPreview() {
      if (!preview) return;
      if (!hasInk) {
        preview.classList.remove('is-signed');
        preview.innerHTML = '<span>Chưa ký</span>';
        return;
      }
      preview.classList.add('is-signed');
      preview.innerHTML = `<img alt="Chữ ký" src="${canvas.toDataURL('image/png')}">`;
    }

    function markInk() {
      if (hasInk) return;
      hasInk = true;
      pad.classList.add('has-ink');
      if (clearBtn) clearBtn.hidden = false;
    }

    function start(event) {
      event.preventDefault();
      drawing = true;
      pad.classList.add('is-drawing');
      const p = point(event);
      lastX = p.x;
      lastY = p.y;
      ctx.beginPath();
      ctx.moveTo(lastX, lastY);
      ctx.lineTo(lastX + 0.01, lastY + 0.01);
      ctx.stroke();
      markInk();
    }

    function move(event) {
      if (!drawing) return;
      event.preventDefault();
      const p = point(event);
      ctx.beginPath();
      ctx.moveTo(lastX, lastY);
      ctx.lineTo(p.x, p.y);
      ctx.stroke();
      lastX = p.x;
      lastY = p.y;
      markInk();
    }

    function end() {
      if (!drawing) return;
      drawing = false;
      pad.classList.remove('is-drawing');
      syncPreview();
    }

    function clear() {
      const ratio = window.devicePixelRatio || 1;
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = '#0b1f3a';
      ctx.lineWidth = 2.2;
      hasInk = false;
      pad.classList.remove('has-ink', 'is-drawing');
      if (clearBtn) clearBtn.hidden = true;
      syncPreview();
    }

    canvas.addEventListener('mousedown', start);
    canvas.addEventListener('mousemove', move);
    window.addEventListener('mouseup', end);
    canvas.addEventListener('mouseleave', end);

    canvas.addEventListener('touchstart', start, { passive: false });
    canvas.addEventListener('touchmove', move, { passive: false });
    canvas.addEventListener('touchend', end);
    canvas.addEventListener('touchcancel', end);

    clearBtn?.addEventListener('click', (event) => {
      event.preventDefault();
      clear();
    });

    resize();
    window.addEventListener('resize', resize);
  }

  root.querySelectorAll('.sig-pad[data-sig]').forEach(setupPad);
})();
