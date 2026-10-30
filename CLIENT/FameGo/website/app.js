// FameGo site — vanilla interaction engine. No framework, no build.
// Every module guards its own DOM: safe on all pages, calm on reduced-motion.
(function () {
  "use strict";

  // Failsafe first: never trap a visitor behind the loader or hidden content.
  window.addEventListener("error", function () {
    try {
      document.body.classList.remove("loading");
      var l = document.getElementById("loader");
      if (l) l.style.display = "none";
      document.querySelectorAll(".rv").forEach(function (e) { e.classList.add("in"); });
    } catch (_) { /* stay visible */ }
  });

  var fine = window.matchMedia("(pointer: fine)").matches;
  var calm = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var wide = window.matchMedia("(min-width: 1021px)").matches;

  // Slate preloader — snap, count, lift. Failsafe included.
  (function loader() {
    var l = document.getElementById("loader");
    if (!l) { document.body.classList.remove("loading"); return; }
    if (calm) { l.style.display = "none"; document.body.classList.remove("loading"); return; }
    var n = document.getElementById("lcount"), bar = document.getElementById("lbar"), done = false;
    function finish() {
      if (done) return; done = true;
      l.classList.add("done");
      document.body.classList.remove("loading");
      setTimeout(function () { l.style.display = "none"; }, 900);
    }
    var t0 = null;
    function frame(t) {
      if (done) return;
      if (!t0) t0 = t;
      var p = Math.min(1, (t - t0) / 900);
      if (n) n.textContent = p < 0.34 ? "ROLLING…" : p < 0.67 ? "CHECKING CREW…" : "CALL SHEET UP";
      if (bar) bar.style.width = (p * 100).toFixed(1) + "%";
      if (p < 1) requestAnimationFrame(frame); else setTimeout(finish, 150);
    }
    requestAnimationFrame(frame);
    setTimeout(finish, 2600);
  })();

  // Footer year
  var y = document.getElementById("year");
  if (y) y.textContent = String(new Date().getFullYear());

  // Nav: shrink on scroll + full-screen mobile menu (button stays reachable).
  // Sub-pages reuse the classic dropdown instead.
  var topbar = document.getElementById("topbar");
  var menuBtn = document.getElementById("menuBtn");
  var mmenu = document.getElementById("mmenu");
  var siteNav = document.getElementById("siteNav");
  if (menuBtn && !mmenu && siteNav) {
    menuBtn.addEventListener("click", function () {
      var open = siteNav.classList.toggle("open");
      menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
      document.body.classList.toggle("menu-open", open);
    });
  }
  if (menuBtn && mmenu) {
    var links = mmenu.querySelectorAll("a");
    links.forEach(function (a, i) { a.style.transitionDelay = (0.06 * i + 0.1) + "s"; });
    menuBtn.addEventListener("click", function () {
      var open = mmenu.classList.toggle("open");
      menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
      menuBtn.setAttribute("aria-label", open ? "Close menu" : "Open menu");
      document.body.classList.toggle("menu-open", open);
    });
    links.forEach(function (a) {
      a.addEventListener("click", function () {
        mmenu.classList.remove("open");
        menuBtn.setAttribute("aria-expanded", "false");
        document.body.classList.remove("menu-open");
      });
    });
  }

  // Rise-on-scroll
  var els = document.querySelectorAll(".rv");
  if ("IntersectionObserver" in window && els.length) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); }
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -6% 0px" });
    els.forEach(function (el, i) {
      el.style.transitionDelay = Math.min(i % 3, 2) * 70 + "ms";
      io.observe(el);
    });
  } else {
    els.forEach(function (el) { el.classList.add("in"); });
  }

  // Masked line reveals for display headings
  (function linereveal() {
    var heads = document.querySelectorAll("h2.disp");
    if (!heads.length) return;
    heads.forEach(function (h) {
      var parts = h.innerHTML.split(/<br\s*\/?>/i);
      if (parts.length < 2) return;
      h.classList.add("rl");
      h.innerHTML = parts.map(function (p) {
        return '<span class="rl-line"><span>' + p + "</span></span>";
      }).join("");
      h.querySelectorAll(".rl-line > span").forEach(function (s, i) {
        s.style.transitionDelay = (i * 0.1) + "s";
      });
    });
    var lines = document.querySelectorAll("h2.disp.rl");
    if (calm || !("IntersectionObserver" in window)) {
      lines.forEach(function (h) { h.classList.add("in"); });
      return;
    }
    var o = new IntersectionObserver(function (es) {
      es.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add("in"); o.unobserve(en.target); }
      });
    }, { threshold: 0.4 });
    lines.forEach(function (h) { o.observe(h); });
  })();

  // Sample-booking playback (clearly a demo, loops while visible)
  (function desk() {
    var box = document.getElementById("deskStatus");
    if (!box) return;
    var lines = Array.prototype.slice.call(box.querySelectorAll("div"));
    var bar = document.getElementById("deskBar");
    if (calm || !("IntersectionObserver" in window)) {
      lines.forEach(function (d) { d.classList.add("show"); });
      if (bar) bar.style.width = "100%";
      return;
    }
    function play() {
      lines.forEach(function (d) { d.classList.remove("show"); });
      if (bar) bar.style.width = "0";
      var i = 0;
      (function next() {
        if (i >= lines.length) { setTimeout(play, 3600); return; }
        lines[i].classList.add("show");
        if (bar) bar.style.width = ((i + 1) / lines.length * 100) + "%";
        i++;
        setTimeout(next, i === lines.length ? 2200 : 950);
      })();
    }
    var o = new IntersectionObserver(function (es) {
      es.forEach(function (en) { if (en.isIntersecting) { o.disconnect(); play(); } });
    }, { threshold: 0.3 });
    o.observe(box);
  })();

  // Interactive demo: 4-question booking sim (demo content, no real data)
  (function demo() {
    var body = document.getElementById("demoBody");
    if (!body) return;
    var stepEl = document.getElementById("demoStep"), dots = document.getElementById("demoDots"),
        back = document.getElementById("demoBack");
    var state = {}, order = ["what", "when", "where", "who"], at = 0;
    var QS = {
      what: { q: "What are you shooting?", opts: [["Fashion", "editorial · lookbook"], ["Food", "menu · café"], ["Product", "e-com · brand"], ["Reel", "vertical video"], ["Event", "live coverage"], ["Corporate", "interview · office"]] },
      when: { q: "When's call time?", opts: [["Today", "crew within hours"], ["Tomorrow", "morning slots"], ["This weekend", "prime time"], ["Pick a date", "plan ahead"]] },
      where: { q: "Where are we shooting?", opts: [["Bandra West"], ["Andheri"], ["Colaba"], ["Juhu"]] },
      who: { q: "Who do you need?", opts: [["Videographer", "video + gimbal"], ["Photographer", "stills + lights"], ["Cinematographer", "cinema kit"], ["Drone operator", "aerials"], ["Editor", "post-production"], ["Assistant", "set support"]] }
    };
    function paintDots() {
      if (!dots) return;
      Array.prototype.forEach.call(dots.children, function (d, i) { d.classList.toggle("on", i <= Math.min(at, 3)); });
    }
    function ask() {
      var k = order[at], q = QS[k];
      if (stepEl) stepEl.textContent = "STEP " + (at + 1) + " OF 4";
      if (back) back.hidden = at === 0;
      paintDots();
      var h = '<div class="demo-q">' + q.q + "</div><div class='demo-opts'>";
      q.opts.forEach(function (o, i) {
        h += "<button class='opt' data-i='" + i + "'>" + o[0] + (o[1] ? "<small>" + o[1] + "</small>" : "") + "</button>";
      });
      body.innerHTML = h + "</div>";
      Array.prototype.forEach.call(body.querySelectorAll(".opt"), function (b) {
        b.addEventListener("click", function () {
          b.classList.add("picked");
          state[k] = q.opts[parseInt(b.getAttribute("data-i"), 10)][0];
          setTimeout(function () { at++; at < 4 ? ask() : searching(); }, 220);
        });
      });
    }
    function searching() {
      if (stepEl) stepEl.textContent = "MATCHING · DEMO";
      if (back) back.hidden = true;
      paintDots();
      body.innerHTML = "<div class='demo-q'>Finding verified crew…</div><div class='demo-opts'><div class='desk-status' style='min-height:0'>" +
        "<div class='show'>Checking availability…</div><div class='show gold'>Matching the brief…</div>" +
        "<div class='show ok'>Verified profiles only</div></div></div>";
      setTimeout(found, calm ? 100 : 1600);
    }
    function found() {
      body.innerHTML = "<div class='demo-result show'><div class='found'>Crew found <span>✓</span></div>" +
        "<p style='color:var(--muted);margin-top:10px'>" + state.what + " · " + state.when + " · " + state.where + " · " + state.who + "</p>" +
        "<div class='demo-crew'><div class='avatar'>V</div><div><h4>" + state.who + " <small>· VERIFIED</small></h4>" +
        "<p>Verified profile · gear checked · in-app rates · chat unlocks on accept</p></div></div>" +
        "<div class='cta-row' style='margin-top:24px'><a class='btn' href='#download'>Get the app to book</a>" +
        "<button class='btn ghost' id='demoAgain'>Run it back</button></div></div>";
      var ag = document.getElementById("demoAgain");
      if (ag) ag.addEventListener("click", function () { at = 0; state = {}; ask(); });
    }
    if (back) back.addEventListener("click", function () { if (at > 0) { at--; ask(); } });
    ask();
  })();

  // Sticky phone journey: swap screens as steps pass
  (function journey() {
    var steps = document.getElementById("jSteps"), scr = document.getElementById("jScreen");
    if (!steps || !scr || !("IntersectionObserver" in window)) return;
    var panes = Array.prototype.slice.call(scr.querySelectorAll(".scr"));
    function show(n) {
      panes.forEach(function (p) { p.classList.toggle("on", p.getAttribute("data-s") === String(n)); });
    }
    var o = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) show(en.target.getAttribute("data-s"));
      });
    }, { threshold: 0.55 });
    steps.querySelectorAll(".j-step").forEach(function (s) { o.observe(s); });
  })();

  // Scroll-driven 3D phone — banks, straightens and rises as the journey scrolls
  (function phone3d() {
    var scene = document.getElementById("p3d");
    if (!scene || calm) return;
    var journey = scene.closest ? (scene.closest(".journey") || scene.parentElement) : scene.parentElement;
    function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
    var crx = 10, cry = -26, cdy = 0, mrx = 0, mry = 0, hidden = false;
    document.addEventListener("visibilitychange", function () { hidden = document.hidden; });
    if (fine) {
      document.addEventListener("pointermove", function (e) {
        var r = scene.getBoundingClientRect();
        if (!r.width) return;
        mry = clamp((e.clientX - (r.left + r.width / 2)) / r.width, -1, 1) * 5;
        mrx = clamp(-(e.clientY - (r.top + r.height / 2)) / r.height, -1, 1) * 4;
      }, { passive: true });
    }
    (function frame() {
      requestAnimationFrame(frame);
      if (hidden) return;
      var r = journey.getBoundingClientRect(), vh = window.innerHeight;
      var p = clamp((vh * 0.72 - r.top) / (r.height || 1), 0, 1);
      var trx = 10 - 15 * p;
      var tryy = -26 + 46 * p;
      var tdy = (0.5 - p) * -70;
      crx += (trx + mrx - crx) * 0.08;
      cry += (tryy + mry - cry) * 0.08;
      cdy += (tdy - cdy) * 0.08;
      scene.style.transform = "translate3d(0," + cdy.toFixed(1) + "px,0) rotateX(" +
        crx.toFixed(2) + "deg) rotateY(" + cry.toFixed(2) + "deg)";
    })();
  })();

  // Split converge: halves meet at MATCHED (desktop pin)
  var splitWrap = document.getElementById("split");
  var halfL = document.getElementById("halfL"), halfR = document.getElementById("halfR"),
      badge = document.getElementById("matchBadge");
  var splitPinned = false;
  if (splitWrap && halfL && wide && fine && !calm) {
    splitWrap.classList.add("pinned");
    splitPinned = true;
  } else if (badge) {
    badge.style.position = "static"; badge.style.transform = "none";
    badge.style.opacity = "1"; badge.style.margin = "24px auto 0"; badge.style.display = "block";
    badge.style.width = "max-content";
  }

  // Matching sequence: process steps, then the confirmed role
  (function mapmatch() {
    var log = document.getElementById("mapLog");
    if (!log) return;
    var lines = Array.prototype.slice.call(log.querySelectorAll("div"));
    var dots = Array.prototype.slice.call(document.querySelectorAll("#mapSvg .dotc"));
    var card = document.getElementById("matchCard"), played = false;
    function play() {
      if (played) return; played = true;
      var i = 0;
      (function next() {
        if (i < lines.length) {
          lines[i].classList.add("show");
          if (dots[i - 1]) dots[i - 1].classList.add("here");
          i++;
          setTimeout(next, 700);
        } else if (card) card.classList.add("show");
      })();
    }
    if (calm) {
      lines.forEach(function (d) { d.classList.add("show"); });
      dots.forEach(function (d) { d.classList.add("here"); });
      if (card) card.classList.add("show");
      return;
    }
    if (!("IntersectionObserver" in window)) { play(); return; }
    var o = new IntersectionObserver(function (es) {
      es.forEach(function (en) { if (en.isIntersecting) { o.disconnect(); play(); } });
    }, { threshold: 0.35 });
    o.observe(log);
  })();

  // Verification stamps
  (function stamps() {
    var box = document.getElementById("stamps");
    if (!box) return;
    var items = Array.prototype.slice.call(box.querySelectorAll(".stamp"));
    if (calm || !("IntersectionObserver" in window)) {
      items.forEach(function (s) { s.classList.add("stamped"); });
      return;
    }
    var o = new IntersectionObserver(function (es) {
      es.forEach(function (en) {
        if (!en.isIntersecting) return;
        o.disconnect();
        items.forEach(function (s, i) { setTimeout(function () { s.classList.add("stamped"); }, i * 260); });
      });
    }, { threshold: 0.35 });
    o.observe(box);
  })();

  // Gallery shutter flash on first view
  (function shutter() {
    var sheet = document.getElementById("sheet"), flash = document.getElementById("flash");
    if (!sheet || !flash || calm || !("IntersectionObserver" in window)) return;
    var o = new IntersectionObserver(function (es) {
      es.forEach(function (en) {
        if (!en.isIntersecting) return;
        o.disconnect();
        flash.classList.remove("go");
        void flash.offsetWidth;
        flash.classList.add("go");
      });
    }, { threshold: 0.3 });
    o.observe(sheet);
  })();

  if (!fine || calm) return; // hover-only feedback below is for desktop pointers

  // Rising motes — the water of this set. Slow, gold, endless.
  (function sea() {
    var cv = document.getElementById("sea");
    if (!cv || !cv.getContext) return;
    var ctx = cv.getContext("2d"), W = 0, H = 0, P = [], hidden = false;
    function size() {
      W = cv.width = window.innerWidth; H = cv.height = window.innerHeight;
      var n = Math.min(70, Math.floor(W * H / 30000));
      P = [];
      for (var i = 0; i < n; i++) P.push({
        x: Math.random() * W, y: Math.random() * H,
        r: 0.7 + Math.random() * 1.6, s: 0.15 + Math.random() * 0.4,
        a: 0.1 + Math.random() * 0.3, ph: Math.random() * Math.PI * 2, wob: 0.3 + Math.random() * 0.7
      });
    }
    size();
    window.addEventListener("resize", size);
    document.addEventListener("visibilitychange", function () { hidden = document.hidden; });
    var t = 0;
    (function frame() {
      requestAnimationFrame(frame);
      if (hidden) return;
      t += 0.008;
      ctx.clearRect(0, 0, W, H);
      for (var i = 0; i < P.length; i++) {
        var p = P[i];
        p.y -= p.s;
        p.x += Math.sin(t * 1.5 + p.ph) * 0.12 * p.wob;
        if (p.y < -8) { p.y = H + 8; p.x = Math.random() * W; }
        var tw = p.a * (0.6 + 0.4 * Math.sin(t * 2 + p.ph));
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, 6.283);
        ctx.fillStyle = "rgba(246,185,65," + tw.toFixed(3) + ")";
        ctx.fill();
      }
    })();
  })();

  // Chasing companion: trails your cursor across the whole page like a curious
  // fish — buttery slow pursuit with a swim wobble, banking into every turn.
  // Changes state with scroll depth; hands off to the journey phone on stage.
  (function companion() {
    var fp = document.getElementById("floatphone");
    if (!fp) return;
    document.body.classList.add("companion-live");
    var states = Array.prototype.slice.call(fp.querySelectorAll(".cscr"));
    var journeySec = document.getElementById("how");
    var demoSec = document.getElementById("demo");
    var matchSec = document.getElementById("matching");
    var cur = -1, hidden = false;
    document.addEventListener("visibilitychange", function () { hidden = document.hidden; });
    function setState(n) {
      if (n === cur) return; cur = n;
      states.forEach(function (s) { s.classList.toggle("on", s.getAttribute("data-c") === String(n)); });
    }
    // Handoff: fade out while the journey's own phone is on stage
    if (journeySec && "IntersectionObserver" in window) {
      var ho = new IntersectionObserver(function (es) {
        es.forEach(function (en) { fp.classList.toggle("away", en.isIntersecting); });
      }, { threshold: 0.12 });
      ho.observe(journeySec);
    }
    var mx = window.innerWidth * 0.72, my = window.innerHeight * 0.38;
    document.addEventListener("pointermove", function (e) { mx = e.clientX; my = e.clientY; }, { passive: true });
    var demoTop = 0, matchTop = 0;
    function measure() {
      demoTop = demoSec ? demoSec.offsetTop : 0;
      matchTop = matchSec ? matchSec.offsetTop : 0;
    }
    measure();
    window.addEventListener("resize", measure);
    window.addEventListener("load", measure);
    function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
    var t = Math.random() * 100, px = 0, py = 0, rot = 0, started = false;
    (function frame() {
      requestAnimationFrame(frame);
      if (hidden) return;
      t += 0.016;
      var W = window.innerWidth, H = window.innerHeight;
      var x0 = 20, x1 = Math.max(x0 + 1, W - 208);
      var y0 = 76, y1 = Math.max(y0 + 1, H - 456);
      // swim wobble so it feels alive even when the cursor rests
      var wx = Math.sin(t * 0.9) * 26 + Math.sin(t * 0.31 + 2) * 14;
      var wy = Math.cos(t * 0.7 + 1) * 22 + Math.sin(t * 0.23) * 12;
      // chase a point trailing below-right of the cursor — never covering it,
      // so buttons and text under the pointer stay visible and clickable
      var gx = clamp(mx + 34 + wx, x0, x1);
      var gy = clamp(my + 48 + wy, y0, y1);
      if (!started) { px = gx; py = gy; started = true; }
      var vx = gx - px, vy = gy - py;
      px += vx * 0.045;
      py += vy * 0.045;
      var targetRot = clamp(vx * 0.3, -12, 12);
      rot += (targetRot - rot) * 0.07;
      var y = window.scrollY;
      setState(y < demoTop - 300 ? 0 : y < matchTop - 300 ? 1 : 2);
      fp.style.transform = "translate3d(" + px.toFixed(1) + "px," + py.toFixed(1) + "px,0) rotate(" + rot.toFixed(2) + "deg)";
    })();
  })();

  // Magnetic primary CTAs — small pull, tactile release
  document.querySelectorAll(".btn.sheen").forEach(function (b) {
    b.addEventListener("pointermove", function (e) {
      var r = b.getBoundingClientRect();
      var x = (e.clientX - (r.left + r.width / 2)) / r.width;
      var y = (e.clientY - (r.top + r.height / 2)) / r.height;
      b.style.transform = "translate(" + (x * 6).toFixed(1) + "px," + (y * 5).toFixed(1) + "px)";
    });
    b.addEventListener("pointerleave", function () { b.style.transform = ""; });
  });

  // Viewfinder cursor — gold lens ring chasing a center dot (index only)
  (function cursor() {
    var dot = document.getElementById("curDot"), ring = document.getElementById("curRing");
    if (!dot || !ring) return;
    var cx = -100, cy = -100, rx = cx, ry = cy, on = false, hidden = false;
    document.addEventListener("visibilitychange", function () { hidden = document.hidden; });
    document.addEventListener("pointermove", function (e) {
      cx = e.clientX; cy = e.clientY;
      if (!on) { on = true; document.body.classList.add("cur-on"); }
      dot.style.transform = "translate(" + cx + "px," + cy + "px)";
    }, { passive: true });
    (function loop() {
      requestAnimationFrame(loop);
      if (hidden) return;
      rx += (cx - rx) * 0.2; ry += (cy - ry) * 0.2;
      ring.style.transform = "translate(" + rx.toFixed(1) + "px," + ry.toFixed(1) + "px)";
    })();
    document.addEventListener("mouseover", function (e) {
      if (!e.target.closest) return;
      ring.classList.toggle("big", !!e.target.closest("a,button,summary,.opt,.shot,.tier"));
    });
  })();

  // Interactive dot-grid — cells ignite gold and lean away near the cursor
  (function grid() {
    var cv = document.getElementById("grid");
    if (!cv || !cv.getContext) return;
    var ctx = cv.getContext("2d"), W = 0, H = 0, cols = 0, rows = 0, SP = 46;
    var mx = -9999, my = -9999, sx = mx, sy = my, hidden = false;
    function size() {
      W = cv.width = window.innerWidth; H = cv.height = window.innerHeight;
      cols = Math.ceil(W / SP) + 1; rows = Math.ceil(H / SP) + 1;
    }
    size();
    window.addEventListener("resize", size);
    document.addEventListener("pointermove", function (e) { mx = e.clientX; my = e.clientY; }, { passive: true });
    document.addEventListener("visibilitychange", function () { hidden = document.hidden; });
    var R = 150; // cursor influence radius
    (function frame() {
      requestAnimationFrame(frame);
      if (hidden) return;
      sx += (mx - sx) * 0.12; sy += (my - sy) * 0.12;
      ctx.clearRect(0, 0, W, H);
      for (var j = 0; j < rows; j++) {
        var by = j * SP;
        for (var i = 0; i < cols; i++) {
          var bx = i * SP;
          var dx = bx - sx, dy = by - sy;
          var d = Math.sqrt(dx * dx + dy * dy);
          var heat = d < R ? 1 - d / R : 0;
          var push = heat * heat * 11;
          var ox = d ? dx / d * push : 0, oy = d ? dy / d * push : 0;
          var a = 0.05 + heat * 0.4;
          ctx.beginPath();
          ctx.arc(bx + ox, by + oy, 1.1 + heat * 0.9, 0, 6.283);
          ctx.fillStyle = heat > 0.02
            ? "rgba(246,185,65," + a.toFixed(3) + ")"
            : "rgba(255,255,255," + a.toFixed(3) + ")";
          ctx.fill();
        }
      }
    })();
  })();

  // Card spotlight — soft light follows the cursor over interactive surfaces
  document.addEventListener("pointermove", function (e) {
    var t = e.target.closest ? e.target.closest(".card,.phone,.tier,.shot,.opt,.stamp") : null;
    if (!t) return;
    var r = t.getBoundingClientRect();
    t.style.setProperty("--mx", (e.clientX - r.left).toFixed(1) + "px");
    t.style.setProperty("--my", (e.clientY - r.top).toFixed(1) + "px");
  }, { passive: true });

  // Scroll engine: progress beam, gentle parallax, split converge, cue fade
  var layers = Array.prototype.slice.call(document.querySelectorAll("[data-plx],[data-plx-x]"));
  var sfill = document.getElementById("scrollbarFill");
  var cue = document.getElementById("scrollCue");
  var ticking = false;
  function render() {
    ticking = false;
    var vh = window.innerHeight, y = window.scrollY;
    if (topbar) topbar.classList.toggle("scrolled", y > 40);
    if (cue) cue.classList.toggle("hide", y > 120);
    if (sfill) {
      var max = document.documentElement.scrollHeight - vh;
      sfill.style.transform = "scaleX(" + (max > 0 ? Math.max(0, Math.min(1, y / max)).toFixed(3) : 0) + ")";
    }
    for (var i = 0; i < layers.length; i++) {
      var el = layers[i], r = el.getBoundingClientRect();
      if (r.bottom < -200 || r.top > vh + 200) continue;
      var c = r.top + r.height / 2 - vh / 2;
      var sy = parseFloat(el.getAttribute("data-plx")) || 0;
      var sx = parseFloat(el.getAttribute("data-plx-x")) || 0;
      el.style.translate = (sx ? (-c * sx).toFixed(1) + "px" : "0px") + " " + (sy ? (-c * sy).toFixed(1) + "px" : "0px");
    }
    if (splitPinned) {
      var wr = splitWrap.getBoundingClientRect();
      var total = splitWrap.offsetHeight - vh;
      var p = Math.max(0, Math.min(1, -wr.top / total));
      var shift = Math.min(1, p * 1.6);
      halfL.style.transform = "translateX(" + (shift * 22).toFixed(1) + "%)";
      halfR.style.transform = "translateX(" + (-shift * 22).toFixed(1) + "%)";
      var b = Math.max(0, Math.min(1, (p - 0.55) / 0.3));
      badge.style.setProperty("--mb", b.toFixed(3));
      badge.style.setProperty("--ms", (0.4 + b * 0.6).toFixed(3));
    }
  }
  function onScroll() {
    if (!ticking) { ticking = true; requestAnimationFrame(render); }
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  window.addEventListener("resize", onScroll);
  render();

  // Deep-link to app
  window.openFameGoApp = function () {
    window.location.href = "famego://auth/callback";
  };
})();
