// KDS Studio demo interactions
const nav = document.getElementById('nav');
addEventListener('scroll', () => nav.classList.toggle('scrolled', scrollY > 30), {passive:true});

const menuBtn = document.getElementById('menuBtn');
const mobileMenu = document.getElementById('mobileMenu');
menuBtn.onclick = () => mobileMenu.classList.toggle('open');
mobileMenu.querySelectorAll('a').forEach(a => a.onclick = () => mobileMenu.classList.remove('open'));

// Filters
document.querySelectorAll('.filters button').forEach(btn => {
  btn.onclick = () => {
    document.querySelectorAll('.filters button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const f = btn.dataset.filter;
    document.querySelectorAll('#gallery .card').forEach(card => {
      card.style.display = (f === 'all' || card.dataset.cat === f) ? '' : 'none';
    });
  };
});

// Lightbox — fast: loads 1000px only on click, preloads neighbour
const cards = [...document.querySelectorAll('#gallery .card')];
const lb = document.getElementById('lightbox');
const lbImg = document.getElementById('lbImg');
const lbCap = document.getElementById('lbCap');
let idx = 0;
function visibleCards(){ return cards.filter(c => c.style.display !== 'none'); }
function fullSrc(img){ return img.dataset.full || img.currentSrc || img.src; }
function preload(url){ const i = new Image(); i.decoding = 'async'; i.src = url; }
function openLb(i){
  const list = visibleCards();
  idx = (i + list.length) % list.length;
  const img = list[idx].querySelector('img');
  lbImg.decoding = 'async';
  lbImg.src = fullSrc(img);
  lbCap.textContent = list[idx].querySelector('figcaption span').textContent;
  lb.classList.add('open');
  document.body.style.overflow = 'hidden';
  // preload next/prev silently so arrows feel instant, no upfront cost
  const nxt = list[(idx+1)%list.length].querySelector('img');
  const prv = list[(idx-1+list.length)%list.length].querySelector('img');
  setTimeout(()=>{ preload(fullSrc(nxt)); preload(fullSrc(prv)); }, 250);
}
function closeLb(){ lb.classList.remove('open'); document.body.style.overflow=''; }
cards.forEach(c => c.onclick = () => openLb(visibleCards().indexOf(c)));
document.getElementById('lbClose').onclick = closeLb;
document.getElementById('lbPrev').onclick = e => { e.stopPropagation(); openLb(idx-1); };
document.getElementById('lbNext').onclick = e => { e.stopPropagation(); openLb(idx+1); };
lb.onclick = e => { if(e.target === lb) closeLb(); };
addEventListener('keydown', e => {
  if(!lb.classList.contains('open')) return;
  if(e.key === 'Escape') closeLb();
  if(e.key === 'ArrowRight') openLb(idx+1);
  if(e.key === 'ArrowLeft') openLb(idx-1);
});

// VIDEOS — no-lag pattern: do NOT autoplay all. Use:
// <video preload="none" poster="images/work-1-480.webp" muted loop playsinline controls data-src="videos/film.mp4"></video>
// + IntersectionObserver below auto-loads only when visible, pauses offscreen.
const lazyVideos = [...document.querySelectorAll('video[data-src]')];
if('IntersectionObserver' in window && lazyVideos.length){
  const vo = new IntersectionObserver(es => es.forEach(en => {
    const v = en.target;
    if(en.isIntersecting && !v.src){ v.src = v.dataset.src; v.load(); }
    if(!en.isIntersecting && !v.paused){ v.pause(); }
  }), {rootMargin:'200px'});
  lazyVideos.forEach(v => vo.observe(v));
}

// Demo enquiry -> WhatsApp
const form = document.getElementById('enquiryForm');
const note = document.getElementById('formNote');
const waBtn = document.getElementById('waBtn');
// TODO: replace with client's real number, e.g. 919999999999
const WHATSAPP_NUMBER = '910000000000';
function waLink(data){
  const text = `Hello KDS Studio! I'm ${data.get('name')} (${data.get('phone')}). Date: ${data.get('date') || '-'}, Service: ${data.get('service')}. ${data.get('msg') || ''}`;
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(text)}`;
}
waBtn.href = `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent('Hello KDS Studio! I want to enquire about a shoot.')}`;
const waBtn2 = document.getElementById('waBtn2');
if(waBtn2) waBtn2.href = waBtn.href;
form.onsubmit = e => {
  e.preventDefault();
  const data = new FormData(form);
  window.open(waLink(data), '_blank');
  note.textContent = 'Opening WhatsApp… we reply within 24 hrs. Thank you! 🤍';
};
