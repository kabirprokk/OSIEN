// OGS Engine | browser-based scene runner
// Runs Neon Pulse-style flip arenas, Orbit Sling-style gravity puzzles and
// top-down scenes 1:1 with the Studio canvas (960x540 world, 0,0 = center).
#include "editor/browser_game.h"

#include <iomanip>
#include <sstream>

namespace ogs::editor {
namespace {

const char* kind_name(const ObjectKind kind) {
    switch (kind) {
        case ObjectKind::Orb: return "orb";
        case ObjectKind::Platform: return "platform";
        case ObjectKind::Planet: return "planet";
        case ObjectKind::Hazard: return "hazard";
        case ObjectKind::Goal: return "goal";
        case ObjectKind::Coin: return "coin";
        case ObjectKind::Mote: return "mote";
        case ObjectKind::Pad: return "pad";
        case ObjectKind::Gate: return "gate";
    }
    return "orb";
}

const char* game_type_name(const GameType type) {
    switch (type) {
        case GameType::Platformer: return "platformer";
        case GameType::TopDown: return "top_down";
        case GameType::OrbitPuzzle: return "orbit_puzzle";
        case GameType::SideScroller: return "side_scroller";
    }
    return "platformer";
}

std::string color_hex(const std::uint32_t rgb) {
    std::ostringstream color;
    color << '#' << std::hex << std::setw(6) << std::setfill('0') << rgb;
    return color.str();
}

std::string json_string(const std::string& value) {
    std::ostringstream output;
    output << '"';
    for (const unsigned char ch : value) {
        switch (ch) {
            case '"': output << "\\\""; break;
            case '\\': output << "\\\\"; break;
            case '\b': output << "\\b"; break;
            case '\f': output << "\\f"; break;
            case '\n': output << "\\n"; break;
            case '\r': output << "\\r"; break;
            case '\t': output << "\\t"; break;
            case '<': output << "\\u003c"; break;
            case '>': output << "\\u003e"; break;
            case '&': output << "\\u0026"; break;
            default:
                if (ch < 0x20) {
                    output << "\\u00" << std::hex << std::setw(2) << std::setfill('0') << static_cast<unsigned int>(ch) << std::dec;
                } else {
                    output << static_cast<char>(ch);
                }
        }
    }
    output << '"';
    return output.str();
}

} // namespace

std::string make_browser_game_html(const Document& document) {
    std::ostringstream entities;
    entities << std::setprecision(9) << '[';
    bool first = true;
    for (const SceneObject& object : document.objects()) {
        if (!first) entities << ',';
        first = false;
        entities << "{\"id\":" << object.id << ",\"kind\":" << json_string(kind_name(object.kind))
                 << ",\"name\":" << json_string(object.name)
                 << ",\"x\":" << object.position.x
                 << ",\"y\":" << object.position.y
                 << ",\"scale\":" << object.scale
                 << ",\"mass\":" << object.mass
                 << ",\"orbit_rad\":" << object.orbit_rad
                 << ",\"orbit_spd\":" << object.orbit_spd
                 << ",\"mover_amp\":" << object.mover_amp
                 << ",\"mover_spd\":" << object.mover_spd
                 << ",\"value\":" << object.value << '}';
    }
    entities << ']';

    std::ostringstream html;
    html << R"OGS(<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>OSEVA Game Preview</title><style>
*{box-sizing:border-box}body{margin:0;background:#08090c;color:#f4f4f2;font:14px 'Segoe UI',sans-serif;display:grid;min-height:100vh;place-items:center}
main{width:min(100vw,1280px);padding:18px}header{display:flex;justify-content:space-between;align-items:center;padding:0 2px 12px;color:#c8c8c2}
canvas{display:block;width:100%;aspect-ratio:16/9;background:)OGS" << color_hex(document.background_color()) << R"OGS(;border:1px solid #34343a;border-radius:8px;touch-action:none}small{color:#96969a}button{background:#eee;color:#171719;border:0;border-radius:6px;padding:8px 14px;font-weight:600;cursor:pointer}
</style></head><body><main><header><strong>OSEVA · GAME PREVIEW</strong><span><small id="help">Tap / Space: flip · R: restart</small>　<button id="restart">Restart</button></span></header>
<canvas id="game" width="960" height="540" aria-label="Playable game preview"></canvas></main>
<script>
"use strict";
const scene=)OGS" << entities.str() << R"OGS(;
const gameType=)OGS" << json_string(game_type_name(document.game_type())) << R"OGS(;
const spawnId=)OGS" << document.player_spawn_id() << R"OGS(;
const background=)OGS" << json_string(color_hex(document.background_color())) << R"OGS(;
const levelPar=)OGS" << document.par() << R"OGS(;
const levelHint=)OGS" << json_string(document.hint()) << R"OGS(;
const canvas=document.getElementById('game'),ctx=canvas.getContext('2d');
const NEON=(gameType==='platformer'||gameType==='side_scroller'),SLING=(gameType==='orbit_puzzle'),TOP=(gameType==='top_down');
document.getElementById('help').textContent=SLING?'Drag the orb back · release to sling · R: retry':(TOP?'Move: WASD / arrows · R: restart':'Tap / Space / W: flip gravity · A/D: steer · R: restart');
const clamp=(v,a,b)=>v<a?a:(v>b?b:v);
const wells=scene.filter(o=>o.kind==='planet').map(o=>({o,bx:o.x,by:o.y,ph:Math.random()*6.28,m:o.mass>0?o.mass:420}));
const movers=scene.filter(o=>o.kind==='platform'&&o.mover_amp>0).map(o=>({o,baseY:o.y,ph:Math.random()*6.28}));
const plats=scene.filter(o=>o.kind==='platform'),hazards=scene.filter(o=>o.kind==='hazard');
const coins=scene.filter(o=>o.kind==='coin'),motes=scene.filter(o=>o.kind==='mote');
const goals=scene.filter(o=>o.kind==='goal'),gates=scene.filter(o=>o.kind==='gate');
const padObj=scene.find(o=>o.kind==='pad'),playerSource=scene.find(o=>o.id===spawnId&&o.kind==='orb');
const padX=padObj?.x??playerSource?.x??-320,padY=padObj?.y??playerSource?.y??0;
let player,won=false,last=0,time=0,shield=0,score=0,coinCount=0,taken=new Set(),shots=0,stars=0;
let parts=[];for(let i=0;i<160;i++)parts.push({on:false,x:0,y:0,vx:0,vy:0,life:0,max:1,size:2,color:'#00F0FF'});
let floats=[];
function burst(x,y,n,color,spd){let c=0;for(const p of parts){if(p.on)continue;const a=Math.random()*6.283,s=spd*(.35+Math.random()*.85);p.on=true;p.x=x;p.y=y;p.vx=Math.cos(a)*s;p.vy=Math.sin(a)*s;p.life=p.max=.35+Math.random()*.5;p.size=1.5+Math.random()*3;p.color=color;if(++c>=n)break;}}
function float(text,x,y,color){floats.push({text,x,y,color:color||'#FFE45E',life:.9});if(floats.length>12)floats.shift();}
let AC=null,master=null;
function beep(f0,f1,dur,type){try{if(!AC)return;if(master==null)return;const o=AC.createOscillator(),g=AC.createGain(),t=AC.currentTime;o.type=type||'sine';o.frequency.setValueAtTime(Math.max(30,f0),t);o.frequency.exponentialRampToValueAtTime(Math.max(30,f1),t+dur);g.gain.setValueAtTime(.5,t);g.gain.exponentialRampToValueAtTime(.001,t+dur);o.connect(g);g.connect(master);o.start(t);o.stop(t+dur+.02);}catch(e){}}
function audio(){try{if(AC)return;const C=window.AudioContext||window.webkitAudioContext;if(!C)return;AC=new C();master=AC.createGain();master.gain.value=.2;master.connect(AC.destination);}catch(e){}}
addEventListener('pointerdown',audio,{once:true});addEventListener('keydown',audio,{once:true});
function reset(){
 player=SLING?{x:padX,y:padY,vx:0,vy:0,r:13,state:'aim',flyT:0,stillT:0}:{x:playerSource?.x??0,y:playerSource?.y??0,vx:0,vy:0,grav:1,r:14,wob:0};
 won=false;shield=NEON?1.2:0;score=0;coinCount=0;taken=new Set();shots=0;stars=0;drag=null;trail.length=0;savedBest=false;
 for(const p of parts)p.on=false;floats=[];
 for(const w of wells){w.o.x=w.bx;w.o.y=w.by;w.ph=Math.random()*6.28;}
 for(const m of movers){m.o.y=m.baseY;}
}
function neonFlip(){if(!NEON||won)return;player.grav*=-1;player.vy=player.grav*220;player.wob=1;beep(player.grav>0?520:660,player.grav>0?340:880,.07,'square');}
const keys=new Set();
addEventListener('keydown',e=>{const k=e.key.toLowerCase();if(['arrowup','arrowdown','arrowleft','arrowright',' '].includes(k))e.preventDefault();if(e.repeat)return;keys.add(k);if(!started){started=true;if(k!=='r')return;}if(k==='r')reset();if((k===' '||k==='w'||k==='arrowup')&&NEON)neonFlip();});
addEventListener('keyup',e=>keys.delete(e.key.toLowerCase()));
document.getElementById('restart').addEventListener('click',()=>{started=true;reset();});
// ---- sling drag (pointer events ONLY: no double-fire on touch) ----
let drag=null,pid=null;const trail=[];
let started=false,savedBest=false,best=null;
try{best=JSON.parse(localStorage.getItem('oseva-preview-best')||'null');}catch(e){best=null;}
function saveBest(){try{
 if(SLING){const rec={stars,shots};if(!best||rec.stars>(best.stars||0)||(rec.stars===(best.stars||0)&&(best.shots||99)>rec.shots))best=rec;}
 else{const rec={score:Math.floor(score),coins:coinCount};if(!best||rec.score>(best.score||0))best=rec;}
 localStorage.setItem('oseva-preview-best',JSON.stringify(best));}catch(e){}}
function canvasPos(e){const r=canvas.getBoundingClientRect();return{x:(e.clientX-r.left)*(960/r.width)-480,y:270-(e.clientY-r.top)*(540/r.height)};}
canvas.addEventListener('pointerdown',e=>{
 if(!started){started=true;e.preventDefault();return;}
 if(SLING){if(drag||player.state!=='aim'||won)return;const p=canvasPos(e);if(Math.hypot(p.x-player.x,p.y-player.y)>110)return;pid=e.pointerId;drag={sx:p.x,sy:p.y,dx:0,dy:0};e.preventDefault();}
 else if(NEON){if(e.target===canvas)neonFlip();}
});
addEventListener('pointermove',e=>{if(!drag||e.pointerId!==pid)return;const p=canvasPos(e);drag.dx=clamp(p.x-drag.sx,-230,230);drag.dy=clamp(p.y-drag.sy,-230,230);player.x=clamp(padX+drag.dx*.45,-460,460);player.y=clamp(padY+drag.dy*.45,-260,260);if(e.cancelable)e.preventDefault();});
function slingRelease(dx,dy){
 const POWER=5.2,MAXV=1150;let vx=-dx*POWER,vy=-dy*POWER;const sp=Math.hypot(vx,vy);
 if(sp<90){player.x=padX;player.y=padY;return;}
 if(sp>MAXV){vx*=MAXV/sp;vy*=MAXV/sp;}
 player.vx=vx;player.vy=vy;player.state='fly';player.flyT=0;player.stillT=0;trail.length=0;shots++;beep(300,700,.12,'square');
}
addEventListener('pointerup',e=>{if(!drag||e.pointerId!==pid)return;pid=null;const d=drag;drag=null;player.x=padX;player.y=padY;slingRelease(d.dx,d.dy);});
addEventListener('pointercancel',()=>{if(drag){drag=null;pid=null;player.x=padX;player.y=padY;}});
if(!window.PointerEvent){
 canvas.addEventListener('touchstart',e=>{if(SLING&&player.state==='aim'&&!won&&e.touches[0]){const t=e.touches[0],p=canvasPos(t);if(Math.hypot(p.x-player.x,p.y-player.y)<=110){drag={sx:p.x,sy:p.y,dx:0,dy:0};e.preventDefault();}else if(NEON)neonFlip();}},{passive:false});
}
function wellForce(x,y,out){let ax=0,ay=0;for(const w of wells){const dx=w.o.x-x,dy=w.o.y-y;let d2=dx*dx+dy*dy;const mind=(w.o.r||24)*(w.o.r||24);if(d2<900)d2=900;let f=(95000*(w.m||420))/d2;if(f>1900)f=1900;const d=Math.sqrt(d2);ax+=dx/d*f;ay+=dy/d*f;}out.x=ax;out.y=ay;return out;}
const _a={x:0,y:0};
function backToPad(silent){player.x=padX;player.y=padY;player.vx=0;player.vy=0;player.state='aim';player.flyT=0;player.stillT=0;trail.length=0;drag=null;}
function coinVal(c){if(c.value>0)return c.value;return c.scale>1.5?5:1;}
function stepNeon(dt){
 const p=player;
 p.vy+=2250*p.grav*dt;p.vy=clamp(p.vy,-800,800);
 p.x+=(((keys.has('a')||keys.has('arrowleft'))?-260:0)+((keys.has('d')||keys.has('arrowright'))?260:0))*dt;
 p.x=clamp(p.x,-466,466);p.y+=p.vy*dt;
 if(p.wob>0)p.wob=Math.max(0,p.wob-dt*4);
 score+=10*dt;
 const touchSlab=plats.some(s=>Math.abs(p.x-s.x)<36*s.scale+10&&Math.abs(p.y-s.y)<9*s.scale+10);
 const touchHaz=hazards.some(h=>Math.hypot(p.x-h.x,p.y-h.y)<14+12*h.scale);
 const outTop=p.y>262,outBot=p.y<-262;
 if((touchSlab||touchHaz||outTop||outBot)&&shield<=0){burst(p.x,p.y,30,'#FF2BD6',360);beep(220,55,.35,'sawtooth');reset();float('OUCH',0,60,'#FF2B2B');return;}
 if(shield>0&&(touchSlab||touchHaz)){/* shield passes through */}
 for(const c of coins){if(taken.has(c.id))continue;if(Math.hypot(p.x-c.x,p.y-c.y)<14+8*c.scale){taken.add(c.id);const v=coinVal(c);coinCount+=v;score+=v*12;burst(c.x,c.y,10,'#FFE45E',240);float('+'+v+' ◉',c.x,c.y);beep(988,1568,.09,'triangle');}}
 for(const m of motes){if(taken.has(m.id))continue;if(Math.hypot(p.x-m.x,p.y-m.y)<14+9){taken.add(m.id);score+=25;burst(m.x,m.y,10,'#00F0FF',240);float('+10',m.x,m.y,'#00F0FF');beep(880,1180,.09,'sine');}}
 for(const g of goals){if(Math.hypot(p.x-g.x,p.y-g.y)<34*g.scale){won=true;burst(g.x,g.y,60,'#FFE45E',420);beep(660,1320,.35,'triangle');return;}}
 for(const g of gates){if(Math.abs(p.x-g.x)<32*g.scale+14&&Math.abs(p.y-g.y)<150){won=true;burst(g.x,g.y,60,'#FFE45E',420);beep(660,1320,.35,'triangle');return;}}
}
function stepSling(dt){
 const p=player;
 if(p.state!=='fly')return;
 wellForce(p.x,p.y,_a);p.vx+=_a.x*dt;p.vy+=_a.y*dt;
 const sp=Math.hypot(p.vx,p.vy);if(sp>1500){p.vx*=1500/sp;p.vy*=1500/sp;}
 p.x+=p.vx*dt;p.y+=p.vy*dt;p.flyT+=dt;
 trail.push({x:p.x,y:p.y});if(trail.length>46)trail.shift();
 if(p.x<-467){p.x=-467;p.vx=Math.abs(p.vx)*.72;}if(p.x>467){p.x=467;p.vx=-Math.abs(p.vx)*.72;}
 if(p.y<-257){p.y=-257;p.vy=Math.abs(p.vy)*.72;}if(p.y>257){p.y=257;p.vy=-Math.abs(p.vy)*.72;}
 for(const w of wells){const dx=p.x-w.o.x,dy=p.y-w.o.y,d=Math.hypot(dx,dy),minD=(w.o.scale*18||24)+13;if(d<minD&&d>.01){const nx=dx/d,ny=dy/d;p.x=w.o.x+nx*minD;p.y=w.o.y+ny*minD;const vn=p.vx*nx+p.vy*ny;if(vn<0){p.vx-=1.8*vn*nx;p.vy-=1.8*vn*ny;burst(p.x,p.y,8,'#7B2FFF',200);beep(660,520,.06,'triangle');}}}
 for(const s of plats){const hw=36*s.scale,hh=9*s.scale;if(Math.abs(p.x-s.x)<hw+13&&Math.abs(p.y-s.y)<hh+13){if(hw>=hh){p.vy=-p.vy*.72;p.y=s.y+(p.y>s.y?hh+13:-hh-13);}else{p.vx=-p.vx*.72;p.x=s.x+(p.x>s.x?hw+13:-hw-13);}beep(500,420,.05,'square');}}
 for(const h of hazards){if(Math.hypot(p.x-h.x,p.y-h.y)<h.scale*12+9){burst(p.x,p.y,24,'#FF2B2B',320);beep(220,55,.3,'sawtooth');float('SPIKED',p.x,p.y,'#FF2B2B');backToPad();return;}}
 for(const c of coins){if(taken.has(c.id))continue;if(Math.hypot(p.x-c.x,p.y-c.y)<13+8*c.scale){taken.add(c.id);const v=coinVal(c);coinCount+=v;burst(c.x,c.y,12,'#FFE45E',260);float('+'+v+' ◉',c.x,c.y);beep(988,1568,.1,'triangle');}}
 for(const m of motes){if(taken.has(m.id))continue;if(Math.hypot(p.x-m.x,p.y-m.y)<13+9){taken.add(m.id);coinCount+=2;burst(m.x,m.y,10,'#00F0FF',240);float('+2 ◉',m.x,m.y,'#00F0FF');}}
 for(const g of goals){if(Math.hypot(p.x-g.x,p.y-g.y)<(g.scale*26||32)){won=true;stars=shots<=levelPar?3:(shots<=levelPar+2?2:1);coinCount+=stars*6+(stars>=3?4:0);burst(g.x,g.y,60,'#FFE45E',420);beep(523,1046,.3,'triangle');return;}}
 const spd=Math.hypot(p.vx,p.vy);
 if(spd<26){p.stillT+=dt;if(p.stillT>.8){backToPad();return;}}else p.stillT=0;
 if(p.flyT>14)backToPad();
}
function stepTop(dt){
 const p=player,ox=p.x,oy=p.y;
 p.vx=((keys.has('d')||keys.has('arrowright'))?1:0)-((keys.has('a')||keys.has('arrowleft'))?1:0);
 p.vy=((keys.has('w')||keys.has('arrowup'))?1:0)-((keys.has('s')||keys.has('arrowdown'))?1:0);
 p.x=clamp(p.x+p.vx*250*dt,-466,466);p.y=clamp(p.y+p.vy*250*dt,-256,256);
 for(const s of plats){if(Math.abs(p.x-s.x)<36*s.scale+14&&Math.abs(p.y-s.y)<9*s.scale+14){p.x=ox;p.y=oy;break;}}
 for(const h of hazards){if(Math.hypot(p.x-h.x,p.y-h.y)<28*h.scale){reset();return;}}
 for(const c of coins){if(taken.has(c.id))continue;if(Math.hypot(p.x-c.x,p.y-c.y)<14+8*c.scale){taken.add(c.id);coinCount+=coinVal(c);beep(988,1568,.09,'triangle');}}
 for(const m of motes){if(taken.has(m.id))continue;if(Math.hypot(p.x-m.x,p.y-m.y)<14+9){taken.add(m.id);score+=10;beep(880,1180,.09,'sine');}}
 for(const g of goals.concat(gates)){if(Math.hypot(p.x-g.x,p.y-g.y)<34*(g.scale||1)){won=true;beep(660,1320,.3,'triangle');return;}}
}
function frame(now){
 const dt=Math.min((now-last)/1000||0,1/30);last=now;time+=dt;
 for(const m of movers){m.o.y=m.baseY+Math.sin(time*(m.o.mover_spd||1.2))*(m.o.mover_amp||30);}
 for(const w of wells){if(w.o.orbit_rad>0){w.o.x=clamp(w.bx+Math.cos(w.ph)*(w.o.orbit_rad||48),-440,440);w.o.y=clamp(w.by+Math.sin(w.ph)*(w.o.orbit_spd||.6)*40,-240,240);w.ph+=dt*Math.abs(w.o.orbit_spd||.6);}}
 if(shield>0)shield-=dt;
 for(const p of parts){if(!p.on)continue;p.life-=dt;if(p.life<=0){p.on=false;continue;}p.x+=p.vx*dt;p.y+=p.vy*dt;p.vx*=(1-1.6*dt);p.vy*=(1-1.6*dt);}
 for(let i=floats.length-1;i>=0;i--){floats[i].life-=dt;floats[i].y+=30*dt;if(floats[i].life<=0)floats.splice(i,1);}
 if(!won){if(NEON)stepNeon(dt);else if(SLING)stepSling(dt);else stepTop(dt);}
 else if(!savedBest){savedBest=true;saveBest();}
 draw();requestAnimationFrame(frame);
}
function draw(){
 ctx.clearRect(0,0,960,540);ctx.fillStyle=background;ctx.fillRect(0,0,960,540);
 ctx.save();ctx.translate(480,270);ctx.scale(1,-1);
 ctx.strokeStyle='rgba(160,180,210,.08)';ctx.lineWidth=1;
 for(let x=-480;x<=480;x+=40){ctx.beginPath();ctx.moveTo(x,-270);ctx.lineTo(x,270);ctx.stroke();}
 for(let y=-260;y<=260;y+=40){ctx.beginPath();ctx.moveTo(-480,y);ctx.lineTo(480,y);ctx.stroke();}
 const t=time;
 for(const w of wells){const r=(w.o.scale||1)*18;ctx.save();ctx.shadowColor='#7B2FFF';ctx.shadowBlur=22;const g=ctx.createRadialGradient(w.o.x-4,w.o.y-4,2,w.o.x,w.o.y,r+4);g.addColorStop(0,'#E9DDFF');g.addColorStop(.4,'#7B2FFF');g.addColorStop(1,'rgba(20,8,50,0)');ctx.fillStyle=g;ctx.beginPath();ctx.arc(w.o.x,w.o.y,r+4,0,6.2832);ctx.fill();ctx.shadowBlur=0;ctx.fillStyle='#0D0620';ctx.beginPath();ctx.arc(w.o.x,w.o.y,r,0,6.2832);ctx.fill();ctx.strokeStyle='rgba(179,136,255,.9)';ctx.lineWidth=2;ctx.beginPath();ctx.arc(w.o.x,w.o.y,r-1,t*.6,t*.6+4.2);ctx.stroke();ctx.restore();}
 for(const s of plats){const r=18*s.scale;ctx.save();ctx.fillStyle='#141B45';ctx.fillRect(s.x-r*2,s.y-r/2,r*4,r);ctx.strokeStyle='#00F0FF';ctx.shadowColor='#00F0FF';ctx.shadowBlur=10;ctx.lineWidth=2;ctx.beginPath();ctx.moveTo(s.x-r*2+4,s.y+r/2);ctx.lineTo(s.x+r*2-4,s.y+r/2);ctx.stroke();ctx.restore();}
 for(const h of hazards){const r=14*h.scale;ctx.save();ctx.translate(h.x,h.y);ctx.rotate(t*.8);ctx.shadowColor='#FF2B2B';ctx.shadowBlur=14;ctx.fillStyle='#FF2B2B';ctx.beginPath();for(let k=0;k<16;k++){const rr=k%2?r*.45:r,aa=k/16*6.2832;if(k)ctx.lineTo(Math.cos(aa)*rr,Math.sin(aa)*rr);else ctx.moveTo(Math.cos(aa)*rr,Math.sin(aa)*rr);}ctx.closePath();ctx.fill();ctx.shadowBlur=0;ctx.fillStyle='#3A0A0A';ctx.beginPath();ctx.arc(0,0,r*.3,0,6.2832);ctx.fill();ctx.restore();}
 for(const g of goals){const r=(g.scale||1)*26;ctx.save();ctx.shadowColor='#FFE45E';ctx.shadowBlur=20;ctx.strokeStyle='#FFE45E';ctx.lineWidth=4;ctx.beginPath();ctx.arc(g.x,g.y,r+Math.sin(t*3.4)*2,0,6.2832);ctx.stroke();ctx.shadowBlur=0;ctx.restore();}
 for(const g of gates){const w=32*(g.scale||1);ctx.save();ctx.fillStyle='#8A6A00';ctx.fillRect(g.x-w,g.y-150,w*2,300);ctx.fillStyle='#0A0A0A';for(let cr=0;cr<6;cr++){ctx.fillRect(g.x-w,g.y-150+cr*50,cr%2?w:w,50);}ctx.fillStyle='#F5F5F5';for(let cr=0;cr<6;cr++){ctx.fillRect(g.x+(cr%2?-w:0),g.y-150+cr*50,w,50);}ctx.strokeStyle='#FFE45E';ctx.shadowColor='#FFE45E';ctx.shadowBlur=16;ctx.lineWidth=3;ctx.strokeRect(g.x-w,g.y-150,w*2,300);ctx.restore();}
 for(const c of coins){if(taken.has(c.id))continue;const r=8*c.scale,sq=Math.abs(Math.cos(t*3+c.x));ctx.save();ctx.shadowColor='#FFE45E';ctx.shadowBlur=12;ctx.fillStyle='#FFD166';ctx.beginPath();ctx.ellipse(c.x,c.y,Math.max(2,r*(.35+.65*sq)),r,0,0,6.2832);ctx.fill();ctx.restore();}
 for(const m of motes){if(taken.has(m.id))continue;ctx.save();ctx.shadowColor='#00F0FF';ctx.shadowBlur=14;ctx.fillStyle='#00F0FF';ctx.beginPath();ctx.arc(m.x,m.y+Math.sin(t*4+m.x)*3,7,0,6.2832);ctx.fill();ctx.restore();}
 if(SLING){ctx.save();ctx.strokeStyle='rgba(0,240,255,.5)';ctx.lineWidth=2;ctx.setLineDash([8,8]);ctx.beginPath();ctx.arc(padX,padY,26+Math.sin(t*3)*2,0,6.2832);ctx.stroke();ctx.setLineDash([]);ctx.restore();}
 for(const o of scene){if(o.kind==='orb'&&o.id!==spawnId){ctx.fillStyle='#6b8cff';ctx.beginPath();ctx.arc(o.x,o.y,14*o.scale,0,6.2832);ctx.fill();}}
 for(const tr of trail){ctx.fillStyle='rgba(0,240,255,.5)';ctx.fillRect(tr.x-2,tr.y-2,4,4);}
 if(SLING&&player.state==='aim'&&!won&&!drag){ctx.save();ctx.strokeStyle='rgba(179,136,255,.35)';ctx.lineWidth=1.5;ctx.setLineDash([6,8]);ctx.beginPath();ctx.arc(player.x,player.y,110,0,6.2832);ctx.stroke();ctx.setLineDash([]);ctx.restore();}
 if(SLING&&player.state==='aim'&&drag){
  const pull=Math.hypot(drag.dx,drag.dy),power=clamp(pull/230,0,1);
  ctx.save();ctx.strokeStyle='rgba(179,136,255,.30)';ctx.lineWidth=1.5;ctx.setLineDash([6,8]);ctx.beginPath();ctx.arc(padX,padY,104,0,6.2832);ctx.stroke();ctx.setLineDash([]);ctx.restore();
  let sx=padX,sy=padY,svx=-drag.dx*5.2,svy=-drag.dy*5.2;const ssp=Math.hypot(svx,svy);if(ssp>1150){svx*=1150/ssp;svy*=1150/ssp;}
  ctx.save();ctx.fillStyle='rgba(234,242,255,.8)';
  for(let q=0;q<100;q++){let axx=0,ayy=0;for(const w of wells){const dx=w.o.x-sx,dy=w.o.y-sy;let d2=dx*dx+dy*dy;if(d2<900)d2=900;let f=(95000*(w.m||420))/d2;if(f>1900)f=1900;const d=Math.sqrt(d2);axx+=dx/d*f;ayy+=dy/d*f;}svx+=axx*.016;svy+=ayy*.016;sx+=svx*.016;sy+=svy*.016;if(q%4===0){ctx.beginPath();ctx.arc(sx,sy,2.4,0,6.2832);ctx.fill();}if(sx<-480||sx>480||sy<-270||sy>270)break;const gl=goals[0];if(gl&&Math.hypot(sx-gl.x,sy-gl.y)<(gl.scale*26||32)){ctx.strokeStyle='#3DFFA6';ctx.lineWidth=3;ctx.beginPath();ctx.arc(gl.x,gl.y,(gl.scale*26||32)+5,0,6.2832);ctx.stroke();break;}}
  ctx.restore();
  const band=power>.75?'rgba(255,43,43,.9)':(power>.4?'rgba(255,217,100,.85)':'rgba(0,240,255,.7)');
  ctx.save();ctx.strokeStyle=band;ctx.lineWidth=2+power*3;ctx.beginPath();ctx.moveTo(padX-8,padY);ctx.lineTo(player.x,player.y);ctx.stroke();ctx.beginPath();ctx.moveTo(padX+8,padY);ctx.lineTo(player.x,player.y);ctx.stroke();ctx.restore();
 }
 if(!won){
  const br=1+Math.sin(t*3.2)*.04+(player.wob>0?player.wob*.12:0),rr=player.r*br;
  ctx.save();ctx.shadowColor=SLING?'#B388FF':'#00e4ef';ctx.shadowBlur=24;const pg=ctx.createRadialGradient(player.x-3,player.y-3,1,player.x,player.y,rr+5);pg.addColorStop(0,'#FFFFFF');pg.addColorStop(.5,SLING?'#B388FF':'#00e4ef');pg.addColorStop(1,'rgba(0,0,0,0)');ctx.fillStyle=pg;ctx.beginPath();ctx.arc(player.x,player.y,rr+5,0,6.2832);ctx.fill();ctx.shadowBlur=0;ctx.fillStyle='#05060F';ctx.beginPath();ctx.arc(player.x,player.y,rr,0,6.2832);ctx.fill();ctx.fillStyle=SLING?'#B388FF':'#00e4ef';ctx.beginPath();ctx.arc(player.x,player.y,rr*.5,0,6.2832);ctx.fill();ctx.strokeStyle='#EEF2FF';ctx.lineWidth=1.6;ctx.beginPath();ctx.arc(player.x,player.y,rr-.5,0,6.2832);ctx.stroke();
  if(shield>0){ctx.strokeStyle='rgba(0,240,255,'+(.35+.35*Math.sin(t*10)).toFixed(3)+')';ctx.lineWidth=2.5;ctx.setLineDash([8,6]);ctx.beginPath();ctx.arc(player.x,player.y,rr+13,0,6.2832);ctx.stroke();ctx.setLineDash([]);}
  ctx.restore();
 }
 for(const p of parts){if(!p.on)continue;ctx.globalAlpha=clamp(p.life/p.max,0,1);ctx.fillStyle=p.color;ctx.fillRect(p.x-p.size/2,p.y-p.size/2,p.size,p.size);}
 ctx.globalAlpha=1;
 ctx.save();ctx.scale(1,-1);
 for(const f of floats){ctx.globalAlpha=clamp(f.life/.9,0,1);ctx.fillStyle=f.color;ctx.font='700 16px Segoe UI';ctx.textAlign='center';ctx.fillText(f.text,f.x,-f.y);}
 ctx.restore();ctx.globalAlpha=1;
 ctx.restore();
 const labels={platformer:'NEON · FLIP',top_down:'TOP-DOWN',orbit_puzzle:'ORBIT SLING',side_scroller:'NEON · FLIP'};
 ctx.fillStyle='#f3f3ef';ctx.font='600 14px Segoe UI';
 if(SLING)ctx.fillText(won?('COMPLETE · '+'★'.repeat(stars)+'☆'.repeat(3-stars)):('SHOT '+shots+'/'+levelPar+' · '+levelHint),24,32);
 else ctx.fillText(won?'LEVEL COMPLETE · score '+Math.floor(score):(labels[gameType]+' · score '+Math.floor(score)+' · ◉ '+coinCount),24,32);
 if(SLING&&player.state==='aim'&&!won&&!drag)ctx.fillText(levelHint,24,54);
 if(!started){
  ctx.fillStyle='rgba(4,4,8,.74)';ctx.fillRect(0,0,960,540);
  ctx.textAlign='center';ctx.fillStyle='#f3f3ef';ctx.font='800 46px Segoe UI';
  ctx.fillText(SLING?'ORBIT SLING':(TOP?'TOP-DOWN':'NEON FLIP'),480,238);
  ctx.font='400 17px Segoe UI';ctx.fillStyle='#c8c8c2';ctx.fillText(levelHint,480,276);
  ctx.fillStyle='#00e4ef';ctx.font='700 20px Segoe UI';
  ctx.fillText('CLICK OR TAP TO PLAY',480,322-(SLING?0:0));ctx.textAlign='left';
 }else if(won){
  ctx.fillStyle='rgba(4,4,8,.62)';ctx.fillRect(0,0,960,540);
  ctx.textAlign='center';ctx.fillStyle='#FFE45E';ctx.font='800 40px Segoe UI';
  ctx.fillText(SLING?('★'.repeat(stars)+'☆'.repeat(3-stars)):'LEVEL COMPLETE',480,238);
  ctx.fillStyle='#f3f3ef';ctx.font='600 19px Segoe UI';
  ctx.fillText(SLING?('Goal in '+shots+' shots (par '+levelPar+') · +'+(stars*6+(stars>=3?4:0))+' ◉'):('Score '+Math.floor(score)+' · ◉ '+coinCount),480,276);
  if(best)ctx.fillText(SLING?('Best: '+'★'.repeat(best.stars||0)+' in '+(best.shots||0)+' shots'):('Best: '+best.score+' pts'),480,304);
  ctx.fillStyle='#00e4ef';ctx.font='700 18px Segoe UI';ctx.fillText('Press R to play again',480,340);ctx.textAlign='left';
 }
}
reset();
requestAnimationFrame(frame);
</script></body></html>)OGS";
    return html.str();
}

} // namespace ogs::editor
