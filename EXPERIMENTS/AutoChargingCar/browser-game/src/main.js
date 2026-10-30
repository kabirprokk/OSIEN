import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';

const $ = (id) => document.getElementById(id);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.22;
renderer.setClearColor('#151a19');
document.querySelector('#viewport').appendChild(renderer.domElement);

const scene = new THREE.Scene();
scene.background = new THREE.Color('#151a19');
const camera = new THREE.PerspectiveCamera(52, 1, 0.02, 500);
const cameraTarget = new THREE.Vector3();
let cameraDistance=6.4;
let freeOrbit=false,orbitYaw=-1.348,orbitPitch=.26,orbitPointer=null;

scene.add(new THREE.HemisphereLight('#e4ece8', '#343a37', 2.25));
const key = new THREE.DirectionalLight('#fff0d2', 3.0); key.position.set(-30, 60, 22); key.castShadow = true; key.shadow.mapSize.set(2048,2048); key.shadow.camera.left=-45; key.shadow.camera.right=45; key.shadow.camera.top=45; key.shadow.camera.bottom=-45; scene.add(key);
const fill = new THREE.DirectionalLight('#d7dedb', 1.0); fill.position.set(35,24,-35); scene.add(fill);
const studioFloor = new THREE.Mesh(new THREE.PlaneGeometry(2000,2000),new THREE.MeshStandardMaterial({color:'#252a28',roughness:.94}));
studioFloor.rotation.x=-Math.PI/2;studioFloor.position.y=-.012;studioFloor.receiveShadow=true;scene.add(studioFloor);

let P, templateCar, motionRoot, modelLoaded=false;
let inspecting=false, explodedRoot=null, partItems=[], selectedPart=null, inspectionFocus=null, inspectionCenter=new THREE.Vector3(), inspectionWasPaused=false, savedView=null;
const partRaycaster=new THREE.Raycaster(), partPointer=new THREE.Vector2();
const loader=new GLTFLoader();

const stateFactory = () => ({x:0,z:0,yaw:0,wL:0,wR:0,angleL:0,angleR:0,angleFL:0,angleFR:0,lastSpeed:0,longitudinalAcceleration:0,energyWh:0.0,usedWh:0,recoveredWh:0,beltLossWh:0,paused:false,connected:true,balanced:false,load:.65,count:1,grade:0,steer:0,throttle:0,reverse:false,brake:0,demoAt:null,elapsed:0,tauL:0,tauR:0});

function findNodes(root, prefix){const a=[];root.traverse(o=>{if(o.name===prefix||o.name.startsWith(prefix+'.'))a.push(o);});return a;}
function convertPoint(p){return new THREE.Vector3(p[0],p[1],p[2]);}
let beltPath=[], beltSegments=[], beltLength=0;
function preparePath(raw){beltPath=raw.points.map(convertPoint);beltSegments=[];beltLength=0;for(let i=0;i<beltPath.length-1;i++){const len=beltPath[i].distanceTo(beltPath[i+1]);beltSegments.push({start:beltLength,len,a:beltPath[i],b:beltPath[i+1]});beltLength+=len;}}
function pathAt(d){if(!beltLength)return new THREE.Vector3();d=((d%beltLength)+beltLength)%beltLength;let s=beltSegments.find(q=>d<=q.start+q.len);if(!s)s=beltSegments[beltSegments.length-1];return s.a.clone().lerp(s.b,THREE.MathUtils.clamp((d-s.start)/Math.max(s.len,1e-8),0,1));}

function readablePartName(name){return name.replace(/\.\d{3}$/,'').replace(/__/g,' ').replace(/_/g,' ').replace(/\s+/g,' ').trim();}
function explainPart(name){
  const n=name.toLowerCase();
  if(n.includes('battery'))return ['Stores the electrical energy that powers the car.','The battery supplies the motor controller; its terminals and pads provide the positive and negative connections.'];
  if(n.includes('controller')||n.includes('pad'))return ['Routes and controls electrical current between the battery and drive motors.','The controller switches motor current to set direction and speed; the exposed pads are solder or wire connection points.'];
  if(n.includes('rectifier')||n.includes('regulator')||n.includes('charge'))return ['Conditions generator output before it reaches the battery.','Rectification and voltage/current regulation are needed because raw motor output varies with speed and load.'];
  if(n.includes('generator'))return ['Converts belt-driven shaft rotation into electrical output.','Its electrical load creates opposing torque, so the drive system must supply the recovered energy plus conversion losses.'];
  if(n.includes('pulley')||n.includes('tooth')||n.includes('belt'))return ['Transfers rotation from the rear drivetrain to the generator.','The large-to-small pulley ratio raises generator speed while reducing available shaft torque; belt tension carries the load.'];
  if(n.includes('motor')&&n.includes('terminal'))return ['Provides the electrical connection to a drive motor.','The two terminals feed the motor windings; reversing polarity reverses a simple brushed DC motor.'];
  if(n.includes('gearbox'))return ['Reduces motor speed and increases wheel torque.','Small internal gears trade rotational speed for useful axle torque.'];
  if(n.includes('motor')&&n.includes('housing'))return ['Contains the drive motor that turns the wheels.','Current through the motor windings creates torque; the gearbox couples that torque to the axle.'];
  if(n.includes('motor')&&n.includes('bracket'))return ['Holds a drive motor in alignment with its axle.','The bracket transfers motor reaction torque into the chassis and keeps the gearbox output engaged.'];
  if(n.includes('wheel')||n.includes('tread')||n.includes('hub')||n.includes('rim'))return ['Supports the car and turns axle rotation into forward motion.','The tire provides grip; the hub and rim center the wheel on the axle. Individual tread blocks are separate model pieces.'];
  if(n.includes('axle')||n.includes('bearing')||n.includes('bushing')||n.includes('hanger')||n.includes('shaft')||n.includes('coupling'))return ['Supports or transmits rotation between the drivetrain and wheels.','Bearings or bushings let the axle turn while keeping it aligned; couplings transfer torque between shaft sections.'];
  if(n.includes('chassis')||n.includes('cardboard')||n.includes('deck')||n.includes('spacer')||n.includes('rail'))return ['Forms the lightweight structure that carries the car’s components.','The cardboard layers and spacers distribute component loads; keep heavy parts near the center and reinforce motor mounts.'];
  if(n.includes('switch')||n.includes('rocker'))return ['Manually connects or disconnects an electrical circuit.','The rocker operates contacts in series with the circuit; use a switch rated for the motor current.'];
  if(n.includes('resistor'))return ['Dissipates excess electrical energy as heat in the prototype model.','A real resistor needs an appropriate power rating and airflow; it does not return that energy to the battery.'];
  if(n.includes('steer')||n.includes('tie rod')||n.includes('kingpin')||n.includes('servo')||n.includes('fork'))return ['Steers the front wheels and keeps them linked.','The servo moves a horn and pushrod; the tie rod turns both steering arms around their kingpins.'];
  if(n.includes('terminal'))return ['Provides an electrical connection point.','A wire or solder joint connects this terminal to the corresponding circuit.'];
  return ['A distinct piece of the assembled prototype.','Its position and size are taken from the Blender model; inspect neighboring parts to see how it fits into the car.'];
}
function formatPartSize(mm){return mm.map(v=>v>=10?v.toFixed(1):v>=1?v.toFixed(2):v.toFixed(3)).join(' × ')+' mm';}

function buildPartsExplorer(){
  if(!templateCar||explodedRoot)return;
  templateCar.updateWorldMatrix(true,true);scene.updateMatrixWorld(true);
  const meshes=[];templateCar.traverse(o=>{if(o.isMesh&&o.visible&&!o.name.startsWith('Belt_Motion_Marker'))meshes.push(o);});
  const parts=meshes.map(mesh=>{
    mesh.geometry.computeBoundingBox();const localBox=mesh.geometry.boundingBox.clone(),localSize=localBox.getSize(new THREE.Vector3());
    const worldMatrix=mesh.matrixWorld.clone(),worldPosition=new THREE.Vector3(),worldQuat=new THREE.Quaternion(),worldScale=new THREE.Vector3();worldMatrix.decompose(worldPosition,worldQuat,worldScale);
    const size=localSize.clone().multiply(worldScale).multiplyScalar(250);
    const localCenter=localBox.getCenter(new THREE.Vector3()),centerWorld=mesh.localToWorld(localCenter.clone());
    const radius=Math.max(.001,localSize.clone().multiply(worldScale).length()*.5);
    return {mesh,name:mesh.name,size,volume:size.x*size.y*size.z,radius,worldQuat,worldScale,localCenter,centerWorld};
  }).sort((a,b)=>a.volume-b.volume||a.name.localeCompare(b.name));
  const columns=10,spacing=1.12,rows=Math.ceil(parts.length/columns);explodedRoot=new THREE.Group();explodedRoot.name='Selectable_Size_Ordered_Exploded_Parts';
  parts.forEach((part,index)=>{
    const row=Math.floor(index/columns),col=index%columns;
    const clone=part.mesh.clone(false);clone.name=`Inspection_${part.name}`;clone.geometry=part.mesh.geometry.clone();clone.geometry.translate(-part.localCenter.x,-part.localCenter.y,-part.localCenter.z);
    const materialCopy=mat=>{const m=mat.clone();if(m.emissive)m.emissive.set('#000000');return m;};
    clone.material=Array.isArray(part.mesh.material)?part.mesh.material.map(materialCopy):materialCopy(part.mesh.material);
    const fit=THREE.MathUtils.clamp(.30/part.radius,.65,18);clone.position.set((col-(columns-1)/2)*spacing,part.radius*fit-.02,(row-(rows-1)/2)*spacing);clone.quaternion.copy(part.worldQuat);clone.scale.copy(part.worldScale).multiplyScalar(fit);clone.castShadow=true;clone.receiveShadow=true;
    clone.userData.partIndex=index;explodedRoot.add(clone);
    part.clone=clone;part.label=readablePartName(part.name);part.index=index+1;
  });
  partItems=parts;scene.add(explodedRoot);$('partsCount').textContent=`${parts.length} individual model pieces · sorted by size`;
  renderPartList();
}
function renderPartList(){
  const list=$('partsList'),query=$('partsSearch').value.trim().toLowerCase();list.replaceChildren();
  for(const part of partItems){if(query&&!part.label.toLowerCase().includes(query))continue;const b=document.createElement('button');b.type='button';b.className='part-row'+(part===selectedPart?' selected':'');b.setAttribute('role','option');b.setAttribute('aria-selected',String(part===selectedPart));b.innerHTML=`<span class="part-rank">${String(part.index).padStart(3,'0')}</span><span class="part-row-main"><strong></strong><small></small></span>`;b.querySelector('strong').textContent=part.label;b.querySelector('small').textContent=formatPartSize([part.size.x,part.size.y,part.size.z]);b.addEventListener('click',()=>selectPart(part,true));list.append(b);}
}
function selectPart(part,focus){
  if(selectedPart?.clone){const restore=m=>{if(m.emissive)m.emissive.set('#000000');};if(Array.isArray(selectedPart.clone.material))selectedPart.clone.material.forEach(restore);else restore(selectedPart.clone.material);}
  selectedPart=part;const glow=m=>{if(m.emissive)m.emissive.set('#6e9b40');};if(Array.isArray(part.clone.material))part.clone.material.forEach(glow);else glow(part.clone.material);
  const [why,how]=explainPart(part.name),info=$('partInfo');info.replaceChildren();const badge=document.createElement('span');badge.className='eyebrow';badge.textContent=`PART ${String(part.index).padStart(3,'0')} · ${formatPartSize([part.size.x,part.size.y,part.size.z])}`;const title=document.createElement('h3');title.textContent=part.label;const p1=document.createElement('p');p1.innerHTML=`<b>Why it is here</b><br>${why}`;const p2=document.createElement('p');p2.innerHTML=`<b>How it works</b><br>${how}`;info.append(badge,title,p1,p2);
  if(focus){inspectionFocus=part.clone.position.clone();cameraDistance=2.7;orbitPitch=.38;freeOrbit=true;}
  renderPartList();const row=[...$('partsList').children].find(x=>x.querySelector('strong')?.textContent===part.label);row?.scrollIntoView({block:'nearest'});
}
function openPartsExplorer(){
  if(!modelLoaded)return;buildPartsExplorer();savedView={position:camera.position.clone(),target:cameraTarget.clone(),distance:cameraDistance,yaw:orbitYaw,pitch:orbitPitch,free:freeOrbit,paused:state.paused};inspectionWasPaused=state.paused;state.paused=true;inspecting=true;inspectionFocus=null;inspectionCenter.set(0,.2,0);templateCar.visible=false;document.body.classList.add('inspecting');$('partsPanel').hidden=false;$('partsOpen').textContent='Close parts';
  const rect=$('viewport').getBoundingClientRect(),tan=Math.tan(THREE.MathUtils.degToRad(camera.fov/2)),gridW=10*1.12,gridH=Math.ceil(partItems.length/10)*1.12;cameraDistance=Math.max(8,gridW/(2*tan*camera.aspect),gridH*.62/(2*tan))*1.16;orbitYaw=-.22;orbitPitch=.72;freeOrbit=true;selectedPart=null;renderPartList();
}
function closePartsExplorer(){
  if(!inspecting)return;inspecting=false;document.body.classList.remove('inspecting');$('partsPanel').hidden=true;$('partsOpen').textContent='Inspect parts';templateCar.visible=true;if(explodedRoot){explodedRoot.traverse(o=>{if(o.isMesh){o.geometry.dispose();if(Array.isArray(o.material))o.material.forEach(m=>m.dispose());else o.material.dispose();}});scene.remove(explodedRoot);explodedRoot=null;}partItems=[];selectedPart=null;inspectionFocus=null;state.paused=inspectionWasPaused;if(savedView){camera.position.copy(savedView.position);cameraTarget.copy(savedView.target);cameraDistance=savedView.distance;orbitYaw=savedView.yaw;orbitPitch=savedView.pitch;freeOrbit=savedView.free;}
}
function pickExplodedPart(event){
  const rect=canvas.getBoundingClientRect();partPointer.set((event.clientX-rect.left)/rect.width*2-1,-((event.clientY-rect.top)/rect.height)*2+1);partRaycaster.setFromCamera(partPointer,camera);const hit=partRaycaster.intersectObjects(explodedRoot?.children||[],false)[0];if(hit){const part=partItems[hit.object.userData.partIndex];if(part)selectPart(part,true);}
}

function createV1Electronics(root){
  for(const name of ['Battery_6V_2000mAh','Battery_Holder','Generator_Rectifier_Regulator','Load_Dump_Resistor']){const old=root.getObjectByName(name);if(old)old.visible=false;}
  for(const name of ['Main_Switch','Switch_Red_Rocker']){const o=root.getObjectByName(name);if(o)o.visible=false;}
  const controller=root.getObjectByName('Motor_Controller');if(controller)controller.name='TB6612FNG_Motor_Driver_Board';
  const box=(name,size,pos,color,rough=.58)=>{const m=new THREE.Mesh(new THREE.BoxGeometry(...size),new THREE.MeshStandardMaterial({color,roughness:rough,metalness:.08}));m.name=name;m.position.set(...pos);m.castShadow=true;m.receiveShadow=true;root.add(m);return m;};
  const cellMat=new THREE.MeshStandardMaterial({color:'#315d49',roughness:.38,metalness:.15}),capMat=new THREE.MeshStandardMaterial({color:'#b9c3bd',roughness:.28,metalness:.72});
  box('Battery_Holder_4xAA_NiMH',[.108,.006,.030],[0,.071,-.030],'#202724');
  box('Battery_Pack_End_Retainer',[.108,.001,.030],[0,.074,-.030],'#303a34');
  for(const x of [-.026,.026])for(const z of [-.0057,.0057]){
    const cell=new THREE.Mesh(new THREE.CylinderGeometry(.0052,.0052,.050,28),cellMat);cell.name='AA_NiMH_1.2V_Cell';cell.rotation.z=Math.PI/2;cell.position.set(x,.080,-.030+z);cell.castShadow=true;root.add(cell);
    for(const end of [-1,1]){const cap=new THREE.Mesh(new THREE.CylinderGeometry(.0053,.0053,.0012,24),capMat);cap.name='AA_Cell_Terminal';cap.rotation.z=Math.PI/2;cap.position.set(x+end*.0256,.080,-.030+z);cap.castShadow=true;root.add(cap);}
  }
  const batteryPositive=root.getObjectByName('Battery_Positive_Terminal'),batteryNegative=root.getObjectByName('Battery_Negative_Terminal');
  if(batteryPositive){batteryPositive.position.set(-.052,.0858,-.0243);batteryPositive.name='Battery_Positive_Terminal';}
  if(batteryNegative){batteryNegative.position.set(-.052,.0858,-.0357);batteryNegative.name='Battery_Negative_Terminal';}
  box('Battery_Series_Link_Row_1',[.003,.001,.001],[0,.080,-.0243],'#bbc5bf');
  box('Battery_Series_Link_Row_2',[.003,.001,.001],[0,.080,-.0357],'#bbc5bf');
  box('Battery_Series_Link_Row_Crossover',[.001,.001,.013],[.052,.080,-.030],'#bbc5bf');
  box('TB6612FNG_Driver_IC',[.010,.003,.009],[.056,.077,0],'#171c1a');
  box('INA219_Battery_Sensor_Board',[.012,.0016,.009],[.068,.0765,-.009],'#24724a');
  box('INA219_Battery_Sensor_IC',[.004,.0015,.004],[.068,.078,-.009],'#171c1a');
  box('Arduino_Nano_Compatible_5V',[.045,.0016,.018],[.082,.069,-.039],'#23764a');
  box('Arduino_Nano_Main_IC',[.010,.002,.008],[.082,.071,-.039],'#171c1a');
  box('OLED_0.96in_I2C_Module',[.027,.002,.024],[.082,.075,-.039],'#151918');
  box('OLED_Display_Glass',[.022,.001,.018],[.082,.0765,-.039],'#8bb7a5');
  box('INA219_Generator_Sensor_Board',[.026,.003,.020],[.045,.069,.045],'#24724a');
  box('INA219_Generator_Sensor_IC',[.005,.0015,.005],[.045,.071,.045],'#171c1a');
  box('Generator_Schottky_1N5819',[.006,.003,.003],[.063,.072,.045],'#202522');
  box('Adjustable_DC_DC_Test_Module',[.025,.004,.020],[.083,.069,.045],'#28764d');
  box('Generator_Test_Load_Resistor',[.018,.006,.010],[.111,.070,.045],'#c79734');
  box('Master_Switch_SPST',[.016,.009,.014],[.112,.070,.020],'#242a27');
  box('Switch_Red_Rocker_V1',[.009,.003,.007],[.112,.076,.020],'#a72d22');
  const fuse=box('Main_Fuse_2A',[.012,.005,.005],[.096,.071,-.005],'#bac4be');fuse.rotation.z=Math.PI/2;
  for(const [x,z] of [[-.081,-.034],[-.081,.034],[.052,0]]){const cap=new THREE.Mesh(new THREE.CylinderGeometry(.002,.002,.004,20),new THREE.MeshStandardMaterial({color:'#d7b54b',roughness:.38}));cap.name='Motor_Noise_Suppression_Capacitor_100nF';cap.position.set(x,.069,z);cap.castShadow=true;root.add(cap);}
  const bulk=new THREE.Mesh(new THREE.CylinderGeometry(.004,.004,.009,24),new THREE.MeshStandardMaterial({color:'#353c39',roughness:.42}));bulk.name='Supply_Capacitor_1000uF_10V';bulk.position.set(.045,.074,-.008);bulk.castShadow=true;root.add(bulk);
}

async function loadModel(){
  try{
    const [gltf,raw,physics]=await Promise.all([
      loader.loadAsync('/auto_charging_car.glb?inspector=20261016-size-ordered'),fetch('/belt_path.json').then(r=>r.json()),fetch('/physics_model.json').then(r=>r.json())
    ]);
    P=physics; preparePath(raw); templateCar=gltf.scene; templateCar.name='Blender_Exported_Auto_Charging_Car'; templateCar.scale.setScalar(4); templateCar.position.set(0,0,0);createV1Electronics(templateCar);modelLoaded=true;
    templateCar.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;if(o.material){o.material.side=THREE.DoubleSide;}}});
    templateCar.visible=false;
    scene.add(templateCar);
    $('connection').textContent='Blender drivetrain + V1 electronics · '+(gltf.animations[0]?.name||'live physical rig');
    $('motorSpec').textContent=`${P.motor_model} · datasheet proxy`;
    const a=steadyEstimate(0,0,1),b=steadyEstimate(.65,1,1);
    $('caseA').textContent=`${(a.speed*3.6).toFixed(2)} km/h · ${a.drive.toFixed(2)} W · ${(P.usable_energy_Wh/a.netPower).toFixed(1)} h ideal`;
    $('caseB').textContent=`${(b.speed*3.6).toFixed(2)} km/h · ${(b.generated*1000).toFixed(1)} mW output · ${(P.usable_energy_Wh/b.netPower).toFixed(1)} h ideal`;
    $('connection').title=`Imported ${gltf.animations.length} baked Blender animation clip(s).`;
    initModes();
  }catch(err){console.error(err);$('connection').textContent='Could not load Blender export';$('notice').textContent='Run the project launcher so the local model server can serve the exported Blender file.';}
}

const R=()=>P?.wheel_radius_m??.032;
function motorTorque(w,duty){const d=Math.abs(duty);if(d<1e-4)return 0;const w0=P.motor_no_load_rpm*2*Math.PI/60;return Math.sign(duty)*P.motor_stall_torque_Nm*d*Math.max(0,1-Math.abs(w)/(w0*d));}
function currentForTorque(tau,duty){const d=Math.abs(duty);if(d<1e-4)return 0;const f=THREE.MathUtils.clamp(Math.abs(tau)/(P.motor_stall_torque_Nm*d),0,1);return d*(P.motor_no_load_A+(P.motor_stall_A-P.motor_no_load_A)*f);}
function loadTorque(st){return st.connected?P.generator_torque_Nm*P.pulley_ratio/P.belt_efficiency*st.load*st.count:0;}
function roadTorque(w){const sign=Math.abs(w)>.002?Math.sign(w):0;return sign*(P.rolling_resistance_coefficient*P.mass_kg*9.80665+P.bearing_drag_N)*R()/2;}
function steadyEstimate(load,connected,count){
  const omega0=P.motor_no_load_rpm*2*Math.PI/60, road=(P.rolling_resistance_coefficient*P.mass_kg*9.80665+P.bearing_drag_N)*R()/(2*P.driveline_efficiency);
  const tauGen=connected?P.generator_torque_Nm*P.pulley_ratio/P.belt_efficiency*load*count:0;
  const wl=Math.max(0,omega0*(1-(road+tauGen)/P.motor_stall_torque_Nm));
  const wr=Math.max(0,omega0*(1-road/P.motor_stall_torque_Nm));
  const tl=road+tauGen,tr=road;
  const il=P.motor_no_load_A+(P.motor_stall_A-P.motor_no_load_A)*THREE.MathUtils.clamp(tl/P.motor_stall_torque_Nm,0,1);
  const ir=P.motor_no_load_A+(P.motor_stall_A-P.motor_no_load_A)*THREE.MathUtils.clamp(tr/P.motor_stall_torque_Nm,0,1);
  const drive=P.battery_voltage_V*(il+ir)/P.controller_efficiency+P.electronics_idle_W;
  const pg=connected?P.generator_torque_Nm*(4*wl)*load*count:0;
  const generated=pg*P.generator_efficiency;
  const returned=0;
  const net=Math.max(.001,drive);
  return {wl,wr,speed:R()*(wl+wr)/2,rpm:(wl+wr)*30/Math.PI,genRpm:wl*60/2/Math.PI*4,drive,pg,generated,returned,net,tl,tr,il,ir};
}

// ---------- Track ----------
const TRACK_A=18, TRACK_B=10;
function trackPos(s){return new THREE.Vector3(TRACK_A*Math.cos(s),0,TRACK_B*Math.sin(s));}
function trackDeriv(s){return new THREE.Vector3(-TRACK_A*Math.sin(s),0,TRACK_B*Math.cos(s));}
function trackYaw(s){const d=trackDeriv(s);return Math.atan2(-d.z,d.x);}
function progressOf(x,z){let s=Math.atan2(z/TRACK_B, x/TRACK_A); if(s<0)s+=Math.PI*2; return s;}
function lateralOffset(x,z,s){const c=trackPos(s);const t=trackDeriv(s);const mag=Math.hypot(t.x,t.z);const nx=-t.z/mag, nz=t.x/mag; return (x-c.x)*nx+(z-c.z)*nz;}
function normalizeAngle(a){while(a>Math.PI)a-=Math.PI*2;while(a<-Math.PI)a+=Math.PI*2;return a;}

// ---------- Cars ----------
const CAR_COLORS=[0x6e9b40,0xc79734,0x4a90d9,0xd94a4a,0x9b6ed9,0xd9a44a,0x4ad9b8];
let cars=[]; let userCar=null; let mode='free';
let state=null;
let trackLine=null, startLine=null;

function tintCar(root,color){
  root.traverse(o=>{
    if(o.isMesh&&o.material){
      const apply=m=>{const c=m.clone();c.color=new THREE.Color(color);if(c.emissive)c.emissive.set(0x000000);return c;};
      o.material=Array.isArray(o.material)?o.material.map(apply):apply(o.material);
    }
  });
}
function findCarParts(root){
  return {
    leftWheelNodes:findNodes(root,'Rear_Left_Wheel_Rotation'),
    rightWheelNodes:findNodes(root,'Rear_Right_Wheel_Rotation'),
    frontWheelRotations:[root.getObjectByName('Front_Left_Wheel_Rotation'),root.getObjectByName('Front_Right_Wheel_Rotation')].filter(Boolean),
    frontPivots:[root.getObjectByName('Front_Left_Steering_Pivot'),root.getObjectByName('Front_Right_Steering_Pivot')].filter(Boolean),
    leftAxle:root.getObjectByName('Rear_Left_Axle_Rotation'),
    rightAxle:root.getObjectByName('Rear_Right_Axle_Rotation'),
    generatorRot:root.getObjectByName('Generator_Pulley_Rotation'),
    tieRodControl:root.getObjectByName('Front_Tie_Rod_Control'),
    servoHornRot:root.getObjectByName('Front_Servo_Horn_Rotation'),
    markers:findNodes(root,'Belt_Motion_Marker'),
  };
}
function createCar(color,isUser){
  const root=templateCar.clone(true);
  root.visible=true;
  tintCar(root,color);
  root.scale.setScalar(4); root.position.set(0,0,0);
  const parts=findCarParts(root);
  const motion=new THREE.Group(); motion.add(root); scene.add(motion);
  const st=stateFactory();
  if(isUser){ st.energyWh=P.usable_energy_Wh; }
  const car={color,isUser,root,motion,state:st,parts, ai:{targetSpeed:0.5+Math.random()*0.3, seed:Math.random()}, lap:0, progress:0, prevProgress:0, finished:false, finishTime:null, lastThrottle:0, lastSteer:0};
  return car;
}

function updateCarModel(car,vL,vR){
  const root=car.root, st=car.state, parts=car.parts;
  if(!root)return;
  car.motion.position.set(st.x*4,0,st.z*4);car.motion.rotation.y=st.yaw;
  const speed=Math.abs((vL+vR)*.5),accel=st.longitudinalAcceleration??0;
  root.rotation.z=THREE.MathUtils.clamp(accel*.025,-.045,.045);
  root.rotation.x=-Math.sign(st.steer)*speed*speed*Math.abs(Math.tan(st.steer))*.025;
  const leftAngle=st.angleL,rightAngle=st.angleR;
  for(const n of parts.leftWheelNodes)n.rotation.z=leftAngle;
  for(const n of parts.rightWheelNodes)n.rotation.z=rightAngle;
  if(parts.frontWheelRotations[0])parts.frontWheelRotations[0].rotation.z=st.angleFL;
  if(parts.frontWheelRotations[1])parts.frontWheelRotations[1].rotation.z=st.angleFR;
  if(parts.leftAxle)parts.leftAxle.rotation.z=leftAngle;
  if(parts.rightAxle)parts.rightAxle.rotation.z=rightAngle;
  if(parts.generatorRot)parts.generatorRot.rotation.z=leftAngle*4;
  for(const p of parts.frontPivots)p.rotation.y=-st.steer;
  if(parts.tieRodControl){parts.tieRodControl.position.x=.080-.010*Math.cos(st.steer);parts.tieRodControl.rotation.y=-Math.atan(.403846*Math.tan(st.steer));}
  if(parts.servoHornRot)parts.servoHornRot.rotation.y=-st.steer;
  const distance=.020*leftAngle;
  parts.markers.forEach((m,i)=>{const p=pathAt((i/Math.max(1,parts.markers.length))*beltLength+distance);m.position.copy(p);});
}

function physicsStep(car, dt, inp){
  const st=car.state;
  st.elapsed+=dt;
  if(inp) { st.load=st.load; }
  st.load=Number($('load')?.value||65)/100;
  st.count=Number($('generators')?.value||1);
  st.grade=Number($('grade')?.value||0)/100;
  st.connected = (mode!=='free') ? false : st.connected; // traffic/race: generator load off by default? keep user toggle
  // keep connected state from toggle for user car
  if(car.isUser){ st.connected = generatorOn; }
  if(st.energyWh<=1e-5){inp.throttle=0;inp.driveTh=0;}
  let dutyL=inp.throttle,dutyR=inp.throttle;
  const burden=loadTorque(st)/P.motor_stall_torque_Nm;
  if(st.balanced&&st.connected&&st.load>0){
    const w0=P.motor_no_load_rpm*2*Math.PI/60;
    const correction=Math.sign(dutyL||1)*(Math.min(.35,burden)+THREE.MathUtils.clamp((st.wR-st.wL)/w0*.65,-.20,.20));
    dutyL=THREE.MathUtils.clamp(dutyL+correction,-1,1);
  }
  let tl=motorTorque(st.wL,dutyL),tr=motorTorque(st.wR,dutyR);
  const traction=P.mass_kg*9.80665*.72*R()/2;
  const requestedL=tl,requestedR=tr;tl=THREE.MathUtils.clamp(tl,-traction,traction);tr=THREE.MathUtils.clamp(tr,-traction,traction);
  st.tauL=requestedL;st.tauR=requestedR;
  const slope=P.mass_kg*9.80665*st.grade*R()/2;
  const gen=loadTorque(st),genL=st.wL===0?0:Math.sign(st.wL)*gen;
  const roadL=roadTorque(st.wL),roadR=roadTorque(st.wR);
  const brakeTau=st.brake*.035;
  const inertia=P.mass_kg*R()*R()/2+0.000012;
  const aL=(tl-roadL-genL+slope-Math.sign(st.wL||inp.throttle)*brakeTau)/inertia;
  const aR=(tr-roadR+slope-Math.sign(st.wR||inp.throttle)*brakeTau)/inertia;
  let nextWL=st.wL+aL*dt,nextWR=st.wR+aR*dt;
  const unpoweredLevel=Math.abs(inp.throttle)<.01&&Math.abs(st.grade)<.01;
  if(unpoweredLevel){
    if(st.wL!==0&&st.wL*nextWL<=0)nextWL=0;
    if(st.wR!==0&&st.wR*nextWR<=0)nextWR=0;
    if(Math.abs(nextWL)<.4)nextWL=0;
    if(Math.abs(nextWR)<.4)nextWR=0;
  }
  st.wL=nextWL;st.wR=nextWR;
  st.wL=THREE.MathUtils.clamp(st.wL,-P.motor_no_load_rpm*2*Math.PI/60*1.25,P.motor_no_load_rpm*2*Math.PI/60*1.25);
  st.wR=THREE.MathUtils.clamp(st.wR,-P.motor_no_load_rpm*2*Math.PI/60*1.25,P.motor_no_load_rpm*2*Math.PI/60*1.25);
  const vL=st.wL*R(),vR=st.wR*R(),v=(vL+vR)/2;
  st.longitudinalAcceleration=(v-st.lastSpeed)/Math.max(dt,1e-4);st.lastSpeed=v;
  if(unpoweredLevel&&Math.abs(st.wL)<.8&&Math.abs(st.wR)<.8){st.wL=st.wR=st.lastSpeed=st.longitudinalAcceleration=0;}
  const wheelbase=.170;
  const steerYaw=v*Math.tan(st.steer)/wheelbase;
  const imbalanceYaw=(vR-vL)/P.track_m*.32;
  const yawRate=-steerYaw+imbalanceYaw;
  st.yaw+=yawRate*dt;
  st.x+=Math.cos(st.yaw)*v*dt;st.z-=Math.sin(st.yaw)*v*dt;
  st.angleL+=st.wL*dt;st.angleR+=st.wR*dt;
  st.angleFL+=(v+yawRate*P.track_m/2)/R()*dt;
  st.angleFR+=(v-yawRate*P.track_m/2)/R()*dt;
  const curL=currentForTorque(requestedL,dutyL),curR=currentForTorque(requestedR,dutyR);
  const driveW=P.battery_voltage_V*(curL+curR)/P.controller_efficiency+(Math.abs(inp.throttle)>.01?P.electronics_idle_W:0.004);
  const genOmega=Math.abs(st.wL)*P.pulley_ratio;
  const genMechanical=st.connected?P.generator_torque_Nm*st.load*genOmega*st.count:0;
  const beltInput=genMechanical/P.belt_efficiency;
  const beltLoss=Math.max(0,beltInput-genMechanical);
  const genElectrical=genMechanical*P.generator_efficiency;
  const testLoadOutput=genElectrical;
  const chargerOutput=0;
  const netW=driveW;
  if(car.isUser){
    st.usedWh+=driveW*dt/3600;
    st.recoveredWh+=testLoadOutput*dt/3600;
    st.beltLossWh+=beltLoss*dt/3600;
    st.energyWh=THREE.MathUtils.clamp(st.energyWh-netW*dt/3600,0,P.usable_energy_Wh);
  }
  updateCarModel(car,vL,vR);
  if(car.isUser) updateUI({v,curL,curR,driveW,netW,genMechanical,genElectrical,chargerOutput,beltLoss});
  return {v,curL,curR};
}

let generatorOn=true;
let keys=new Set();

function getUserInputs(){
  let th=Number($('throttle').value)/100;
  const forwardKey=keys.has('w')||keys.has('arrowup'),reverseKey=keys.has('s')||keys.has('arrowdown');
  if(forwardKey||reverseKey||state.reverse)th=1;
  if(state.demoAt!==null){const t=state.elapsed-state.demoAt; if(t<2)th=0;else if(t<5)th=(t-2)/3;else if(t<8)th=1;else if(t<10)th=0;else{state.demoAt=null;$('demoButton').classList.remove('running');}}
  if(P&&state.energyWh<=1e-5)th=0;
  const reverse=state.reverse||reverseKey;
  if(keys.has('a')||keys.has('arrowleft'))state.steer=-.35;else if(keys.has('d')||keys.has('arrowright'))state.steer=.35;else state.steer=Number($('steer').value)/100*.42;
  if(keys.has(' '))state.brake=1;else if(state.brake<0.1)state.brake=0;
  const inp={throttle:reverse?-th:th,driveTh:th,reverse};
  state.lastUserThrottle = inp.throttle;
  state.lastUserSteer = state.steer;
  return inp;
}

// ---------- AI / Traffic ----------
let samples=[]; // {f:[], a:[throttle,steer]}
function loadSamples(){try{samples=JSON.parse(localStorage.getItem('ac_ai_samples')||'[]');}catch(e){samples=[];}}
function persistSamples(){try{localStorage.setItem('ac_ai_samples',JSON.stringify(samples.slice(-2000)));}catch(e){}}
function buildFeature(car){
  const st=car.state;
  const s=progressOf(st.x,st.z);
  const speed=Math.abs((st.wL+st.wR)/2*R());
  const off=lateralOffset(st.x,st.z,s);
  const headErr=normalizeAngle(trackYaw(s)-st.yaw);
  // nearest car ahead distance
  let ahead=20;
  for(const c of cars){ if(c===car)continue; const ds=progressOf(c.state.x,c.state.z)-s; let d=ds; if(d<0)d+=Math.PI*2; const dist=d*14; if(dist<ahead)ahead=dist; }
  return [s/(Math.PI*2), speed/1.0, off/3, headErr, ahead/20];
}
function euclid(a,b){let s=0;for(let i=0;i<a.length;i++){const d=a[i]-b[i];s+=d*d;}return Math.sqrt(s);}
function knnPredict(feat){
  if(samples.length<20)return null;
  const dists=samples.map(s=>({d:euclid(s.f,feat),a:s.a})).sort((a,b)=>a.d-b.d);
  const k=Math.min(7,dists.length);
  let tw=0,tt=0,ts=0;
  for(let i=0;i<k;i++){const w=1/(dists[i].d+1e-3);tw+=w;tt+=w*dists[i].a[0];ts+=w*dists[i].a[1];}
  return {throttle:tt/tw, steer:ts/tw};
}
function recordSample(car){
  if(!car.isUser)return;
  const st=car.state;
  const speed=Math.abs((st.wL+st.wR)/2*R());
  const th=state.lastUserThrottle||0;
  if(Math.abs(th)<0.01 && speed<0.05)return;
  if(mode==='free')return; // only learn on track
  const feat=buildFeature(car);
  const action=[th, state.lastUserSteer||0];
  samples.push({f:feat,a:action});
  if(samples.length>2000)samples.shift();
  persistSamples();
  $('aiSamples').textContent=samples.length;
}

function aiInputs(car){
  const st=car.state;
  const s=progressOf(st.x,st.z);
  car.progress=s;
  const speed=Math.abs((st.wL+st.wR)/2*R());
  // heuristic
  let target=car.ai.targetSpeed;
  // avoidance: slow if car ahead close
  let aheadDist=99;
  for(const c of cars){ if(c===car)continue; const ds=progressOf(c.state.x,c.state.z)-s; let d=ds; if(d<0)d+=Math.PI*2; const dist=d*14; if(dist<aheadDist)aheadDist=dist; }
  if(aheadDist<4) target=Math.max(0.2, target*0.5);
  const sLook=s+0.4;
  const yawLook=trackYaw(sLook);
  const headErr=normalizeAngle(yawLook-st.yaw);
  const off=lateralOffset(st.x,st.z,s);
  let steer=headErr*1.6 - off*0.5;
  steer=THREE.MathUtils.clamp(steer,-0.38,0.38);
  let throttle=0, brake=0;
  if(speed<target-0.05) throttle=1;
  else if(speed>target+0.1){ throttle=0; brake=0.2; }
  else throttle=0.35;
  // learned override
  if(samples.length>=20){
    const feat=buildFeature(car);
    const pred=knnPredict(feat);
    if(pred){ throttle=THREE.MathUtils.clamp(pred.throttle,-1,1); steer=THREE.MathUtils.clamp(pred.steer,-0.38,0.38); }
  }
  st.steer=steer;
  car.lastThrottle=throttle; car.lastSteer=steer;
  return {throttle, driveTh:Math.abs(throttle), reverse:false, brake};
}

function trafficInputs(car){
  return aiInputs(car);
}

// ---------- Modes ----------
function clearCars(){
  for(const c of cars){ scene.remove(c.motion); }
  cars=[];
  userCar=null;
}
function makeTrackVisuals(){
  if(trackLine){scene.remove(trackLine);trackLine=null;}
  if(startLine){scene.remove(startLine);startLine=null;}
  const pts=Array.from({length:121},(_,i)=>trackPos(i/120*Math.PI*2));
  const geo=new THREE.BufferGeometry().setFromPoints(pts);
  trackLine=new THREE.Line(geo,new THREE.LineBasicMaterial({color:'#d4ff85',opacity:.6,transparent:true}));
  trackLine.position.y=.01; scene.add(trackLine);
  // start line
  const a=trackPos(0), b=trackPos(0.001);
  const t=trackDeriv(0); const n=new THREE.Vector3(-t.z,0,t.x); n.normalize();
  const w=4;
  const sg=new THREE.BufferGeometry().setFromPoints([a.clone().addScaledVector(n,-w), a.clone().addScaledVector(n,w)]);
  startLine=new THREE.Line(sg,new THREE.LineBasicMaterial({color:'#ffffff'}));
  startLine.position.y=.02; scene.add(startLine);
}
function hideTrackVisuals(){
  if(trackLine){scene.remove(trackLine);trackLine=null;}
  if(startLine){scene.remove(startLine);startLine=null;}
}
function initModes(){
  // mode buttons
  document.querySelectorAll('.mode-btn').forEach(btn=>{
    btn.addEventListener('click',()=>setMode(btn.dataset.mode));
  });
  setMode('free');
}
function setMode(m){
  mode=m;
  document.querySelectorAll('.mode-btn').forEach(b=>{const on=b.dataset.mode===m;b.classList.toggle('active',on);b.setAttribute('aria-selected',String(on));});
  clearCars();
  if(m==='free'){
    hideTrackVisuals();
    const c=createCar(CAR_COLORS[0],true); cars.push(c); userCar=c;
    state=c.state; state.energyWh=P.usable_energy_Wh; state.paused=false;
    $('raceHud').hidden=true;
  } else if(m==='traffic'){
    makeTrackVisuals();
    const c=createCar(CAR_COLORS[0],true); cars.push(c); userCar=c;
    state=c.state; state.energyWh=P.usable_energy_Wh; state.paused=false;
    // spawn traffic
    for(let i=0;i<6;i++){
      const tc=createCar(CAR_COLORS[1+i% (CAR_COLORS.length-1)],false);
      tc.ai.targetSpeed=0.35+Math.random()*0.45;
      // place along track
      const s=(i/6)*Math.PI*2 + Math.random()*0.3;
      const p=trackPos(s);
      tc.state.x=p.x; tc.state.z=p.z; tc.state.yaw=trackYaw(s);
      tc.progress=s;
      cars.push(tc);
    }
    $('raceHud').hidden=true;
  } else if(m==='race'){
    makeTrackVisuals();
    const c=createCar(CAR_COLORS[0],true); cars.push(c); userCar=c;
    state=c.state; state.energyWh=P.usable_energy_Wh; state.paused=false;
    const ai=createCar(CAR_COLORS[2],false);
    ai.ai.targetSpeed=0.7;
    const s=Math.PI; // opposite side
    const p=trackPos(s);
    ai.state.x=p.x; ai.state.z=p.z; ai.state.yaw=trackYaw(s);
    ai.progress=s;
    cars.push(ai);
    raceState.active=true; raceState.startTime=null; raceState.userLaps=0; raceState.aiLaps=0; raceState.finished=false; raceState.result='';
    $('raceHud').hidden=false;
    updateRaceHud();
  }
  // camera
  freeOrbit=false;cameraDistance=6.4;orbitYaw=-1.348;orbitPitch=.26;
}

// ---------- Race state ----------
const raceState={active:false,startTime:null,userLaps:0,aiLaps:0,finished:false,result:''};
function updateRaceHud(){
  $('raceLap').textContent=raceState.userLaps;
  $('aiLap').textContent=raceState.aiLaps;
  if(raceState.startTime){ $('raceTime').textContent=((performance.now()-raceState.startTime)/1000).toFixed(1); }
  $('raceResult').textContent=raceState.result;
}

// lap detection
function updateLaps(car){
  if(mode!=='race')return;
  const s=car.progress;
  // detect wrap forward
  if(car.prevProgress > Math.PI*1.5 && s < Math.PI*0.5 && car.state.lastSpeed>0.1){
    car.lap+=1;
    if(car===userCar) raceState.userLaps=car.lap;
    else raceState.aiLaps=car.lap;
    if(!raceState.finished && car.lap>=3){
      raceState.finished=true;
      raceState.result = (car===userCar)?'You win!':'AI wins!';
      if(car===userCar) raceState.result+=' 🏁';
    }
  }
  car.prevProgress=s;
}

// ---------- Camera ----------
function updateChaseCamera(dt){
  if(inspecting){
    const look=inspectionFocus||inspectionCenter;cameraTarget.lerp(look,1-Math.exp(-Math.max(dt,.001)*7));
    const distance=cameraDistance;const desired=new THREE.Vector3(look.x+Math.sin(orbitYaw)*Math.cos(orbitPitch)*distance,look.y+Math.sin(orbitPitch)*distance,look.z+Math.cos(orbitYaw)*Math.cos(orbitPitch)*distance);
    camera.position.lerp(desired,1-Math.exp(-Math.max(dt,.001)*5));camera.lookAt(cameraTarget);return;
  }
  const car=userCar; if(!car){ camera.lookAt(0,0,0); return; }
  const st=car.state;
  const px=st.x*4,pz=st.z*4,yaw=st.yaw,fx=Math.cos(yaw),fz=-Math.sin(yaw),rx=Math.sin(yaw),rz=Math.cos(yaw);
  const look=new THREE.Vector3(px+fx*2.5,.40,pz+fz*2.5);
  const desired=freeOrbit
    ?new THREE.Vector3(look.x+Math.sin(orbitYaw)*Math.cos(orbitPitch)*cameraDistance,look.y+Math.sin(orbitPitch)*cameraDistance,look.z+Math.cos(orbitYaw)*Math.cos(orbitPitch)*cameraDistance)
    :new THREE.Vector3(px-fx*cameraDistance+rx*1.45,2.15+Math.min(.35,Math.abs((st.wL+st.wR)*.5)*.025),pz-fz*cameraDistance+rz*1.45);
  const blend=1-Math.exp(-Math.max(dt,.001)*4.2);
  camera.position.lerp(desired,blend);cameraTarget.lerp(look,1-Math.exp(-Math.max(dt,.001)*6));camera.lookAt(cameraTarget);
}
function resize(){const r=document.querySelector('#viewport').getBoundingClientRect();renderer.setSize(r.width,r.height,false);camera.aspect=r.width/r.height;camera.updateProjectionMatrix();}
new ResizeObserver(resize).observe(document.querySelector('#viewport')); resize();

// ---------- UI ----------
function updateUI(t={}){
  const car=userCar; if(!car)return;
  const st=car.state;
  const v=t.v??(R()*(st.wL+st.wR)/2),kmh=Math.abs(v)*3.6;
  $('speed').textContent=kmh.toFixed(2);$('speedFill').style.width=`${Math.min(100,kmh/2*100)}%`;
  $('leftRpm').textContent=(st.wL*60/(2*Math.PI)).toFixed(0);$('rightRpm').textContent=(st.wR*60/(2*Math.PI)).toFixed(0);
  $('genRpm').textContent=(Math.abs(st.wL)*60/(2*Math.PI)*4).toFixed(0);$('beltSpeed').textContent=`Belt ${(Math.abs(st.wL)*.020).toFixed(3)} m/s · 4:1 speed-up`;
  $('motorTorque').textContent=`${(Math.abs(st.tauL??0)*1000).toFixed(2)} / ${(Math.abs(st.tauR??0)*1000).toFixed(2)}`;
  $('motorCurrent').textContent=`${(t.curL??0).toFixed(3)} A left · ${(t.curR??0).toFixed(3)} A right`;
  $('batteryUse').textContent=(st.usedWh*1000).toFixed(1);
  $('batteryPower').textContent=`${(t.netW??0).toFixed(2)} W net draw`;
  $('energyRecovered').textContent=(st.recoveredWh*1000).toFixed(1);
  $('returnedPower').textContent=`${((t.genElectrical??0)*1000).toFixed(1)} mW into test load`;
  $('beltLossEnergy').textContent=(st.beltLossWh*1000).toFixed(1);
  $('beltLossPower').textContent=`${((t.beltLoss??0)*1000).toFixed(1)} mW dissipated`;
  $('batteryCurrent').textContent=`${((t.driveW??0)/P.battery_voltage_V).toFixed(3)} A · ${P.battery_voltage_V} V nominal`;
  $('losses').textContent=`Motor/controller ${Math.max(0,(t.driveW??0)-((t.curL??0)+(t.curR??0))*P.battery_voltage_V).toFixed(2)} W · generator ${(Math.max(0,(t.genMechanical??0)-(t.genElectrical??0))).toFixed(3)} W`;
  const soc=100*st.energyWh/P.usable_energy_Wh;$('soc').textContent=soc.toFixed(1);$('batteryFill').style.width=`${soc}%`;
  const estimate=steadyEstimate(st.load,st.connected,st.count);const hours=st.energyWh/estimate.net;
  $('runtime').textContent=`${hours.toFixed(1)} h at estimated cruise · ideal constant use`;
  $('gearState').textContent=Math.abs(v)>.02?(st.connected&&st.load>0?'BELT + GENERATOR LOADED':'DRIVE TRAIN MOVING'):'MOTORS OFF';
  $('notice').textContent=st.connected&&st.load>0
    ?'Generator torque is applied to the left rear axle. Output is measured at a test load and is not returned to the AA pack. The model may arc; speed balancing represents encoder feedback.'
    :(st.grade>0.005?'Downhill gravity is an external energy input; returned power is limited by generator and charger losses.':'The 3D model uses the Blender geometry. Motion follows the live motor curve estimate; exact speed and runtime require your actual motor and battery measurements.');
  $('throttleValue').textContent=`${Math.round(Math.abs(inputs().throttle)*100)}%`;$('steerValue').textContent=`${Math.round(st.steer/0.42*100)}%`;
  $('aiSamples').textContent=samples.length;
}

// inputs() needs to operate on user car state
function inputs(){ 
  // keep compatibility: use userCar.state
  if(userCar){ state=userCar.state; }
  return getUserInputs(); 
}

// ---------- Events ----------
document.addEventListener('keydown',e=>{if(e.target instanceof HTMLInputElement)return;if(['ArrowUp','ArrowDown','ArrowLeft','ArrowRight',' '].includes(e.key))e.preventDefault();keys.add(e.key.toLowerCase());if(e.key.toLowerCase()==='g'&&!e.repeat){generatorOn=!generatorOn;setToggle($('generatorSwitch'),generatorOn); if(userCar)userCar.state.connected=generatorOn;} });
document.addEventListener('keyup',e=>{keys.delete(e.key.toLowerCase());if(e.key===' ')state.brake=0;});
window.addEventListener('blur',()=>{keys.clear();if(state){state.brake=0;state.reverse=false;}$('throttle').value=0;$('driveButton').classList.remove('active');$('reverseButton').classList.remove('active');});
function setToggle(el,on){el.classList.toggle('on',on);el.setAttribute('aria-pressed',String(on));el.querySelector('span').textContent=on?'ON':'OFF';}
$('throttle').addEventListener('input',()=>{$('throttleValue').textContent=$('throttle').value+'%';});
$('steer').addEventListener('input',()=>{$('steerValue').textContent=$('steer').value+'%';});
$('load').addEventListener('input',()=>{$('loadValue').textContent=$('load').value+'%';});
$('generators').addEventListener('input',()=>{$('generatorCount').textContent=$('generators').value+' unit'+(Number($('generators').value)>1?'s':'');});
$('grade').addEventListener('input',()=>{$('gradeValue').textContent=`${$('grade').value}% ${Number($('grade').value)>0?'downhill':Number($('grade').value)<0?'uphill':''}`;});
$('generatorSwitch').addEventListener('click',()=>{generatorOn=!generatorOn;setToggle($('generatorSwitch'),generatorOn); if(userCar)userCar.state.connected=generatorOn;});
$('balanceSwitch').addEventListener('click',()=>{state.balanced=!state.balanced;setToggle($('balanceSwitch'),state.balanced);});
$('driveButton').addEventListener('pointerdown',e=>{e.currentTarget.setPointerCapture(e.pointerId);$('throttle').value=100;$('driveButton').classList.add('active');});['pointerup','pointercancel'].forEach(e=>$('driveButton').addEventListener(e,()=>{$('throttle').value=0;$('driveButton').classList.remove('active');}));
$('reverseButton').addEventListener('pointerdown',e=>{e.currentTarget.setPointerCapture(e.pointerId);state.reverse=true;$('reverseButton').classList.add('active');});['pointerup','pointercancel'].forEach(e=>$('reverseButton').addEventListener(e,()=>{state.reverse=false;$('reverseButton').classList.remove('active');}));
$('demoButton').addEventListener('click',()=>{state.demoAt=state.elapsed;state.x=state.z=state.yaw=state.wL=state.wR=state.angleL=state.angleR=state.angleFL=state.angleFR=state.lastSpeed=state.longitudinalAcceleration=0;state.energyWh=P.usable_energy_Wh;state.usedWh=state.recoveredWh=state.beltLossWh=0;$('demoButton').classList.add('running');$('throttle').value=0;});
$('pauseButton').addEventListener('click',()=>{state.paused=!state.paused;$('pauseButton').textContent=state.paused?'Resume':'Pause';});
$('resetView').addEventListener('click',()=>{freeOrbit=false;cameraDistance=6.4;orbitYaw=-1.348;orbitPitch=.26;});
$('nerdyOpen').addEventListener('click',()=>$('nerdyDialog').showModal());
$('nerdyClose').addEventListener('click',()=>$('nerdyDialog').close());
$('nerdyDialog').addEventListener('click',e=>{if(e.target===$('nerdyDialog'))$('nerdyDialog').close();});
$('partsOpen').addEventListener('click',()=>inspecting?closePartsExplorer():openPartsExplorer());
$('partsClose').addEventListener('click',closePartsExplorer);
$('partsSearch').addEventListener('input',renderPartList);

document.querySelector('#viewport').addEventListener('wheel',e=>{cameraDistance=THREE.MathUtils.clamp(cameraDistance+Math.sign(e.deltaY)*.45,inspecting?1.15:4.2,inspecting?40:11);},{passive:true});
const canvas=renderer.domElement;
canvas.addEventListener('pointerdown',e=>{
  if(e.button!==0)return;
  freeOrbit=true;
  const offset=camera.position.clone().sub(cameraTarget),flat=Math.hypot(offset.x,offset.z);
  if(offset.lengthSq()>.001){cameraDistance=offset.length();orbitYaw=Math.atan2(offset.x,offset.z);orbitPitch=Math.atan2(offset.y,flat);}
  orbitPointer={id:e.pointerId,x:e.clientX,y:e.clientY,startX:e.clientX,startY:e.clientY,moved:false};canvas.setPointerCapture(e.pointerId);e.preventDefault();
});
canvas.addEventListener('pointermove',e=>{
  if(!orbitPointer||orbitPointer.id!==e.pointerId)return;
  if(Math.hypot(e.clientX-orbitPointer.startX,e.clientY-orbitPointer.startY)>5)orbitPointer.moved=true;
  const dx=e.clientX-orbitPointer.x,dy=e.clientY-orbitPointer.y;orbitPointer.x=e.clientX;orbitPointer.y=e.clientY;
  orbitYaw-=dx*.006;orbitPitch=THREE.MathUtils.clamp(orbitPitch+dy*.005,-.15,1.35);
});
const stopOrbitDrag=e=>{if(orbitPointer?.id===e.pointerId){const click=!orbitPointer.moved;orbitPointer=null;if(click&&inspecting)pickExplodedPart(e);}};
canvas.addEventListener('pointerup',stopOrbitDrag);canvas.addEventListener('pointercancel',stopOrbitDrag);canvas.addEventListener('lostpointercapture',stopOrbitDrag);

// ---------- Main loop ----------
const PHYSICS_STEP=1/120;let last=performance.now(),physicsAccumulator=0;
function animate(now){
  requestAnimationFrame(animate);
  const frameDt=Math.min(.1,Math.max(0,(now-last)/1000));last=now;
  if(state && !state.paused && P && modelLoaded){
    // user inputs
    const userInp = userCar ? getUserInputs() : {throttle:0,driveTh:0,reverse:false,brake:0};
    // record sample before step
    if(userCar) recordSample(userCar);
    physicsAccumulator+=frameDt;
    while(physicsAccumulator>=PHYSICS_STEP){
      // step all cars
      for(const car of cars){
        let inp;
        if(car===userCar){ inp=userInp; }
        else if(mode==='traffic'){ inp=trafficInputs(car); }
        else if(mode==='race'){ inp=aiInputs(car); }
        else { inp={throttle:0,driveTh:0,reverse:false,brake:0}; }
        physicsStep(car,PHYSICS_STEP,inp);
        car.progress=progressOf(car.state.x,car.state.z);
        updateLaps(car);
      }
      physicsAccumulator-=PHYSICS_STEP;
    }
    // race timer
    if(mode==='race' && raceState.active){
      // start timer on first user throttle
      if(!raceState.startTime && Math.abs(userCar.state.lastSpeed)>0.05){ raceState.startTime=performance.now(); }
      updateRaceHud();
    }
  } else physicsAccumulator=0;
  updateChaseCamera(frameDt);renderer.render(scene,camera);
}
requestAnimationFrame(animate);

loadSamples();
$('aiSamples').textContent=samples.length;
loadModel();