// Client-side only: ticking checklist items is never sent to the server or stored.
(function () {
  document.querySelectorAll('[data-checklist]').forEach(function (list) {
    var boxes = list.querySelectorAll('input[type="checkbox"]');
    var progress = list.querySelector('[data-progress]');
    if (!boxes.length || !progress) return;
    function update() {
      var done = 0;
      boxes.forEach(function (b) {
        b.closest('li').classList.toggle('done', b.checked);
        if (b.checked) done++;
      });
      progress.textContent = done === boxes.length
        ? 'All ' + boxes.length + ' ready'
        : done + ' of ' + boxes.length + ' ready';
    }
    boxes.forEach(function (b) { b.addEventListener('change', update); });
    update();
  });
})();
