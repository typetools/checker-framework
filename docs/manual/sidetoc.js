// Highlights, in the side table of contents, the section that the reader is
// viewing, and keeps that entry visible within the side table of contents.
// lwarp-postprocess inlines this file into each page of the multi-page manual.
document.addEventListener("DOMContentLoaded", function () {
  var container = document.querySelector("div.sidetoccontainer");
  if (!container) {
    return;
  }
  var page = location.pathname.split("/").pop();

  // The sidetoc entries for this page, in document order.  A section's target
  // is the anchor that precedes its heading.
  var sections = [];
  container.querySelectorAll("nav.sidetoc a[href]").forEach(function (link) {
    var href = link.getAttribute("href");
    var hash = href.indexOf("#");
    if (hash === -1 || href.substring(0, hash) !== page) {
      return;
    }
    var target = document.getElementById(href.substring(hash + 1));
    if (target) {
      sections.push({ link: link, target: target });
    }
  });
  if (sections.length === 0) {
    return;
  }

  var current = null;
  function update() {
    // The current section is the last one whose start is above this line.
    var threshold = window.innerHeight / 4;
    var next = sections[0];
    for (var i = 1; i < sections.length; i++) {
      if (sections[i].target.getBoundingClientRect().top > threshold) {
        break;
      }
      next = sections[i];
    }
    if (next === current) {
      return;
    }
    if (current) {
      current.link.classList.remove("sidetoc-current");
    }
    current = next;
    current.link.classList.add("sidetoc-current");

    // Scroll only the sidetoc; scrollIntoView() might also scroll the page.
    var linkBox = current.link.getBoundingClientRect();
    var containerBox = container.getBoundingClientRect();
    if (linkBox.top < containerBox.top || linkBox.bottom > containerBox.bottom) {
      container.scrollTop += linkBox.top - containerBox.top - container.clientHeight / 3;
    }
  }

  var pending = false;
  function onScroll() {
    if (!pending) {
      pending = true;
      requestAnimationFrame(function () {
        pending = false;
        update();
      });
    }
  }
  // Depending on the window size, lwarp's layout scrolls either the window or
  // the main.bodycontainer element.  Scroll events do not bubble, so listen
  // during the capture phase.  Ignore scrolling of the sidetoc itself.
  document.addEventListener(
    "scroll",
    function (event) {
      if (event.target !== container) {
        onScroll();
      }
    },
    true
  );
  window.addEventListener("resize", onScroll);
  window.addEventListener("hashchange", onScroll);
  update();
});
