import bpy, math, os, json
from mathutils import Vector

OUT = os.path.dirname(os.path.abspath(__file__))
bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)
for c in list(bpy.data.collections):
    if c.name != 'Collection': bpy.data.collections.remove(c)
root=bpy.context.scene.collection
for c in list(root.children): root.children.unlink(c)
names=['00_REFERENCE','01_CHASSIS','02_WHEELS','03_AXLES','04_DRIVE_MOTORS','05_GENERATOR','06_BELT_SYSTEM','07_BATTERY','08_ELECTRONICS','09_WIRING','10_RIG','11_MEASUREMENTS','12_EXPLODED_VIEW']
COL={}
for n in names:
    c=bpy.data.collections.new(n); root.children.link(c); COL[n]=c
scene=bpy.context.scene
scene.unit_settings.system='METRIC'; scene.unit_settings.length_unit='MILLIMETERS'; scene.unit_settings.scale_length=1.0
scene.render.engine='BLENDER_EEVEE'; scene.render.resolution_x=1400; scene.render.resolution_y=1000; scene.render.resolution_percentage=100
scene.frame_start=1; scene.frame_end=250; scene.render.fps=25

def mat(name,color,metal=0,rough=.55,alpha=1):
    m=bpy.data.materials.new(name); m.diffuse_color=(*color,alpha); m.use_nodes=True
    p=m.node_tree.nodes.get('Principled BSDF'); p.inputs['Base Color'].default_value=(*color,alpha); p.inputs['Alpha'].default_value=alpha; p.inputs['Metallic'].default_value=metal; p.inputs['Roughness'].default_value=rough
    if alpha<1: m.surface_render_method='DITHERED'
    return m
CARDBOARD=mat('Kraft cardboard',(0.52,.31,.14)); RUBBER=mat('Tire rubber',(.035,.045,.05)); METAL=mat('Satin steel',(.35,.4,.43),.75,.28); ALUM=mat('Motor aluminum',(.55,.58,.6),.65,.3); BLACK=mat('Polymer black',(.04,.06,.07),.1,.35); RED=mat('Positive wire',(.8,.055,.035)); BLUE=mat('Negative wire',(.03,.16,.65)); GREEN=mat('Power path green',(.02,.75,.28),.1,.3); YELLOW=mat('Warning amber',(.95,.5,.02),.1,.32); PCB=mat('PCB green',(.035,.22,.12),.1,.4); LIPO=mat('Battery blue',(.08,.22,.48),.25,.3); WHITE=mat('Label white',(.92,.92,.85)); BELT=mat('Belt dark polyurethane',(.06,.08,.075),.1,.4)
BELT2=mat('Belt motion markers',(.95,.55,.04),.1,.3)

def link_obj(o,coll,name,material=None):
    o.name=name
    if o.data and hasattr(o.data,'name'): o.data.name=name+'_Mesh'
    for c in list(o.users_collection): c.objects.unlink(o)
    COL[coll].objects.link(o)
    if material: o.data.materials.append(material)
    return o
def cube(name,loc,dim,coll,material,bevel=0):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc); o=bpy.context.object; o.dimensions=dim; bpy.ops.object.transform_apply(location=False,rotation=False,scale=True); link_obj(o,coll,name,material)
    if bevel:
        mod=o.modifiers.new('Soft manufactured edges','BEVEL'); mod.width=bevel; mod.segments=3
        o.modifiers.new('Weighted corner normals','WEIGHTED_NORMAL')
    return o
def cyl(name,loc,radius,depth,axis,coll,material,verts=48):
    bpy.ops.mesh.primitive_cylinder_add(vertices=verts, radius=radius, depth=depth, location=loc); o=bpy.context.object
    if axis=='Y': o.rotation_euler[0]=math.pi/2
    if axis=='X': o.rotation_euler[1]=math.pi/2
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True); return link_obj(o,coll,name,material)
def uv_sphere(name,loc,scale,coll,material):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=12, location=loc); o=bpy.context.object; o.scale=scale; bpy.ops.object.transform_apply(location=False,rotation=False,scale=True); return link_obj(o,coll,name,material)
def rod(name,a,b,r,coll,material):
    a,b=Vector(a),Vector(b); d=b-a; o=cyl(name,(a+b)/2,r,d.length,'Z',coll,material,24); o.rotation_mode='QUATERNION'; o.rotation_quaternion=Vector((0,0,1)).rotation_difference(d.normalized()); return o
def text(name,body,loc,size,coll,material,rot=(0,0,0)):
    cu=bpy.data.curves.new(name,'FONT'); cu.body=body; cu.size=size; cu.extrude=.00015; o=bpy.data.objects.new(name,cu); COL[coll].objects.link(o); o.location=loc; o.rotation_euler=rot; cu.materials.append(material); return o
def set_parent(o,p):
    world_location=o.location.copy(); o.parent=p; o.matrix_parent_inverse.identity(); o.location=world_location-p.location

# Chassis: two 2 mm cardboard decks with spacer rails and folded-edge reinforcement.
lower_deck=cube('Chassis_Lower_Deck_250x120x2mm',(0,0,.052),(.250,.120,.002),'01_CHASSIS',CARDBOARD,.001)
# Rear-axle pulley relief slot: the 40 mm drive pulley crosses the lower deck plane by 1 mm.
cutter=cube('Rear_Pulley_Clearance_Cutter',(-.055,0,.056),(.070,.012,.020),'00_REFERENCE',BLACK)
cutter.hide_render=True; cutter.hide_viewport=True
bo=lower_deck.modifiers.new('Rear pulley clearance slot','BOOLEAN'); bo.operation='DIFFERENCE'; bo.object=cutter
upper_deck=cube('Chassis_Upper_Deck_250x120x2mm',(0,0,.060),(.250,.120,.002),'01_CHASSIS',CARDBOARD,.001)
bo2=upper_deck.modifiers.new('Belt tunnel clearance','BOOLEAN'); bo2.operation='DIFFERENCE'; bo2.object=cutter
for x in [-.095,.095]:
    for y in [-.047,.047]: cube('Deck_Spacer_Block',(x,y,.056),(.010,.010,.006),'01_CHASSIS',CARDBOARD,.0008)
for y in [-.057,.057]: cube('Cardboard_Edge_Rail',(0,y,.055),(.238,.004,.009),'01_CHASSIS',CARDBOARD,.001)

# Reference datum / ground plane.
cube('Ground_Reference',(0,0,-.004),(.55,.38,.003),'00_REFERENCE',mat('Ground',(.13,.15,.17),0,.8))

# Main rotation controller. Angle is the single editable drivetrain control.
ctrl=bpy.data.objects.new('Drivetrain_Control',None); COL['10_RIG'].objects.link(ctrl); ctrl.empty_display_type='ARROWS'; ctrl.empty_display_size=.025; ctrl.location=(-.085,0,.032); ctrl['Drive_Angle']=0.0; ctrl['Generator_Load']=0.65; ctrl['Steering_Angle']=0.0; ctrl['Generator_Load_min']=0.0; ctrl['Generator_Load_max']=1.0; ctrl['description']='Drive_Angle is forward wheel travel angle. Steering_Angle moves front kingpins. Load 0..1 represents electrical extraction and added resisting torque.'
ctrl.id_properties_ui('Drive_Angle').update(description='Single drivetrain rotation parameter in radians',default=0.0)
ctrl.id_properties_ui('Generator_Load').update(min=0.0,max=1.0,description='0 disconnected; 1 high electrical load / stronger mechanical resistance')
ctrl.id_properties_ui('Steering_Angle').update(min=-0.45,max=0.45,description='Front kingpin angle, radians')

# Wheels, hubs and treads, each rotating about transverse Y axle via driver.
wheel_objs=[]
for axle,x in [('Front',.085),('Rear',-.085)]:
  for side,y in [('Left',-.073),('Right',.073)]:
    nm=f'{axle}_{side}_Wheel' if axle=='Front' else f'Rear_{side}_Wheel'
    tire=cyl(nm,(x,y,.032),.032,.018,'Y','02_WHEELS',RUBBER,64); tire['outer_diameter_mm']=64; tire['width_mm']=18; tire['axle_hole_diameter_mm']=4
    hub=cyl(nm.replace('Wheel','_Hub'),(x,y,.032),.012,.020,'Y','02_WHEELS',METAL,40)
    ring=cyl(nm.replace('Wheel','_Hub_Rim'),(x,y+(-.010 if y<0 else .010),.032),.008,.001,'Y','02_WHEELS',ALUM,40)
    for k in range(12):
      th=2*math.pi*k/12
      # subtle tread blocks on circumference
      ob=cube(nm.replace('Wheel','_Tread'),(x+.0315*math.cos(th),y,.032+.0315*math.sin(th)),(.004,.019,.006),'02_WHEELS',RUBBER,.0007); ob.rotation_euler[1]=-th
    wheel_objs.append(tire)
    # wheel parented to hidden axle rotation empty at center.
    ax=bpy.data.objects.new(nm+'_Rotation',None); COL['10_RIG'].objects.link(ax); ax.location=(x,y,.032); ax.empty_display_type='CIRCLE'; ax.empty_display_size=.011
    set_parent(tire,ax); set_parent(hub,ax); set_parent(ring,ax)
    for ob in list(COL['02_WHEELS'].objects):
      if ob.name.startswith(nm.replace('Wheel','_Tread')): set_parent(ob,ax)
    f=ax.driver_add('rotation_euler',1); f.driver.type='SCRIPTED'; f.driver.expression='-a'; v=f.driver.variables.new(); v.name='a'; v.type='SINGLE_PROP'; v.targets[0].id=ctrl; v.targets[0].data_path='["Drive_Angle"]'
    ax['rotation_axis']='Y'; ax['driven_by']='Drivetrain_Control[Drive_Angle]'
    if axle=='Front':
      steer=bpy.data.objects.new(f'Front_{side}_Steering_Pivot',None); COL['10_RIG'].objects.link(steer); steer.location=(x-.005,y,.032); steer.empty_display_type='SPHERE'; steer.empty_display_size=.004
      set_parent(ax,steer)
      sf=steer.driver_add('rotation_euler',2); sf.driver.type='SCRIPTED'; sf.driver.expression='s'; sv=sf.driver.variables.new(); sv.name='s'; sv.type='SINGLE_PROP'; sv.targets[0].id=ctrl; sv.targets[0].data_path='["Steering_Angle"]'

# A hidden-by-default translucent tire duplicate is enabled only in belt/cutaway renders.
ghost_mat=mat('Cutaway tire translucent',(.32,.48,.56),.05,.45,.22)
ghost=bpy.data.objects['Rear_Left_Wheel'].copy(); ghost.data=bpy.data.objects['Rear_Left_Wheel'].data.copy(); ghost.name='BELT_CLOSEUP_Translucent_Rear_Left_Tire'; COL['11_MEASUREMENTS'].objects.link(ghost); ghost.data.materials.clear(); ghost.data.materials.append(ghost_mat); ghost.hide_render=True; ghost.hide_viewport=True

# Axles and straw/bushing bearings.
for x,label in [(.085,'Front'),(-.085,'Rear')]:
    if label=='Front':
      # Each front wheel pivots on its own kingpin; no rigid cross axle blocks steering.
      for side,yy in [('Left',-.073),('Right',.073)]:
        rod(f'Front_{side}_Kingpin',(x-.005,yy,.029),(x-.005,yy,.053),.0018,'03_AXLES',METAL)
        fork=rod(f'Front_{side}_Steering_Fork',(x-.005,yy,.032),(x,yy,.032),.0015,'03_AXLES',METAL)
        set_parent(fork,bpy.data.objects[f'Front_{side}_Steering_Pivot'])
        arm=rod(f'Front_{side}_Steering_Arm',(x-.005,yy,.032),(x-.015,(-.052 if side=='Left' else .052),.049),.0012,'03_AXLES',METAL)
        set_parent(arm,bpy.data.objects[f'Front_{side}_Steering_Pivot'])
        # The front wheel pivot carries the rotation; hide the exposed decorative pin
        # so no short gray axle stub protrudes from the outer side of the tire.
        set_parent(shaft,bpy.data.objects[f'Front_{side}_Wheel_Rotation'])
        bpy.ops.mesh.primitive_torus_add(major_radius=.003,minor_radius=.001,major_segments=24,minor_segments=8,location=(x-.005,yy,.051)); link_obj(bpy.context.object,'03_AXLES',f'Front_{side}_Kingpin_Bearing',BLACK)
      cube('Front_Steering_Servo_Mount',(.104,0,.050),(.028,.030,.004),'04_DRIVE_MOTORS',METAL,.001)
      cube('Front_Steering_Servo',(.104,0,.041),(.022,.020,.014),'04_DRIVE_MOTORS',BLACK,.002)['nominal_voltage_V']=6
      horn=cube('Front_Steering_Servo_Horn',(.090,0,.048),(.012,.004,.002),'04_DRIVE_MOTORS',YELLOW,.0005)
      hornrot=bpy.data.objects.new('Front_Servo_Horn_Rotation',None); COL['10_RIG'].objects.link(hornrot); hornrot.location=(.104,0,.048); set_parent(horn,hornrot)
      hf=hornrot.driver_add('rotation_euler',2); hf.driver.type='SCRIPTED'; hf.driver.expression='s'; hv=hf.driver.variables.new(); hv.name='s'; hv.type='SINGLE_PROP'; hv.targets[0].id=ctrl; hv.targets[0].data_path='["Steering_Angle"]'
      tieg=bpy.data.objects.new('Front_Tie_Rod_Control',None); COL['10_RIG'].objects.link(tieg); tieg.location=(.070,0,.049)
      tf=tieg.driver_add('location',0); tf.driver.type='SCRIPTED'; tf.driver.expression='0.080-0.010*cos(s)'; tv=tf.driver.variables.new(); tv.name='s'; tv.type='SINGLE_PROP'; tv.targets[0].id=ctrl; tv.targets[0].data_path='["Steering_Angle"]'
      tf=tieg.driver_add('rotation_euler',2); tf.driver.type='SCRIPTED'; tf.driver.expression='atan(0.403846*tan(s))'; tv=tf.driver.variables.new(); tv.name='s'; tv.type='SINGLE_PROP'; tv.targets[0].id=ctrl; tv.targets[0].data_path='["Steering_Angle"]'
      tie=rod('Front_Tie_Rod',(x-.015,-.052,.049),(x-.015,.052,.049),.001,'03_AXLES',METAL)
      set_parent(tie,tieg)
      tie['purpose']='Cross link joins steering arms; servo commands both kingpins'
      rod('Front_Servo_Pushrod',(.090,0,.048),(x-.015,0,.049),.0008,'03_AXLES',METAL)
    else:
      # Two independent rear stub axles let the left/right motors steer by speed difference.
      axle_l=cyl('Rear_Left_Axle_Shaft',(x,-.0345,.032),.002,.077,'Y','03_AXLES',METAL,32); axle_l['diameter_mm']=4
      axle_r=cyl('Rear_Right_Axle_Shaft',(x,.0385,.032),.002,.069,'Y','03_AXLES',METAL,32); axle_r['diameter_mm']=4
    if label=='Rear':
      # Reinforced hanger blocks bridge the 19 mm chassis-to-axle drop.
      for y in [-.052,.052]:
        hanger=cube('Rear_Axle_Hanger',(x,y,.041),(.010,.014,.024),'03_AXLES',CARDBOARD,.001)
        hcut=cyl('Rear_Hanger_Bore_Cutter',(x,y,.032),.004,.018,'Y','03_AXLES',BLACK,32); link_obj(hcut,'00_REFERENCE',hcut.name); hcut.hide_render=True; hcut.hide_viewport=True
        hm=hanger.modifiers.new('4 mm axle clearance bore','BOOLEAN'); hm.operation='DIFFERENCE'; hm.object=hcut
        tube=cyl('Rear_Straw_Bearing',(x,y,.032),.0035,.013,'Y','03_AXLES',WHITE,32)
        tcut=cyl('Rear_Straw_Bore_Cutter',(x,y,.032),.0021,.014,'Y','03_AXLES',BLACK,32); link_obj(tcut,'00_REFERENCE',tcut.name); tcut.hide_render=True; tcut.hide_viewport=True
        tm=tube.modifiers.new('Hollow straw bearing bore','BOOLEAN'); tm.operation='DIFFERENCE'; tm.object=tcut
        bpy.ops.mesh.primitive_torus_add(major_radius=.003,minor_radius=.001,major_segments=24,minor_segments=8,location=(x,y,.032),rotation=(math.pi/2,0,0)); link_obj(bpy.context.object,'03_AXLES','Rear_Axle_Bushing',BLACK)
    if label=='Rear':
      for side,loc,shaft in [('Left',(x,-.0345,.032),axle_l),('Right',(x,.0385,.032),axle_r)]:
        ar=bpy.data.objects.new(f'Rear_{side}_Axle_Rotation',None); COL['10_RIG'].objects.link(ar); ar.location=loc; ar.empty_display_type='CIRCLE'; ar.empty_display_size=.008
        set_parent(shaft,ar)
        f=ar.driver_add('rotation_euler',1); d=f.driver; d.type='SCRIPTED'; d.expression='-a'; v=d.variables.new(); v.name='a'; v.type='SINGLE_PROP'; v.targets[0].id=ctrl; v.targets[0].data_path='["Drive_Angle"]'

# Rear drive motors (one per side), bracket, can, gearboxes and terminals.
for side,y in [('Left',-.030),('Right',.030)]:
    x=-.085
    cube(side+'_Motor_Mount_Bracket',(x,y,.064),(.048,.030,.003),'04_DRIVE_MOTORS',METAL,.001)
    for dx in [-.016,.016]: cube(side+'_Motor_Bracket_Standoff',(x+dx,y,.06325),(.006,.006,.0045),'04_DRIVE_MOTORS',BLACK,.0005)
    can=cyl(side+'_Drive_Motor_Housing',(x-.016,y,.079),.010,.033,'Y','04_DRIVE_MOTORS',ALUM,48); can['rated_voltage_V']=6; can['stall_torque_Nm_assumed']=.050; can['no_load_rpm_assumed']=250; can['no_load_current_A_assumed']=.075; can['stall_current_A_assumed']=.80; can['model_note']='Representative N20 proxy only; confirm exact vendor curve.'
    cube(side+'_Drive_Gearbox',(x,y,.052),(.022,.026,.038),'04_DRIVE_MOTORS',BLACK,.003)
    sign=-1 if side=='Left' else 1
    cyl(side+'_Gearbox_Output_Shaft',(x,sign*.058,.032),.002,.030,'Y','04_DRIVE_MOTORS',METAL,24)
    cyl(side+'_Axle_Coupling_Collar',(x,sign*.053,.032),.0035,.006,'Y','04_DRIVE_MOTORS',METAL,24)
    set_parent(bpy.data.objects[side+'_Gearbox_Output_Shaft'],bpy.data.objects[f'Rear_{side}_Axle_Rotation'])
    set_parent(bpy.data.objects[side+'_Axle_Coupling_Collar'],bpy.data.objects[f'Rear_{side}_Axle_Rotation'])
    for dy,ma in [(-.006,RED),(.006,BLUE)]: rod(side+'_Motor_Terminal',(x-.005,y+dy,.079),(x,y+dy,.087),.0015,'04_DRIVE_MOTORS',ma)

# Pulley axes are transverse Y, coplanar belt plane y=0. Rear drive pulley center fixed to rear axle.
large=cyl('Drive_Pulley_40mm',(-.085,0,.032),.020,.008,'Y','06_BELT_SYSTEM',METAL,64); large['pitch_diameter_mm']=40; large['ratio_to_generator']=4.0
rear_rot=bpy.data.objects['Rear_Left_Axle_Rotation']
set_parent(large,rear_rot)
for i in range(20):
    th=2*math.pi*i/20
    tooth=cube('Drive_Pulley_Tooth',(-.085+.0205*math.cos(th),0,.032+.0205*math.sin(th)),(.004,.009,.003),'06_BELT_SYSTEM',BLACK,.0005); tooth.rotation_euler[1]=-th
    set_parent(tooth,rear_rot)
gen_rot=bpy.data.objects.new('Generator_Pulley_Rotation',None); COL['10_RIG'].objects.link(gen_rot); gen_rot.location=(-.025,0,.078); gen_rot.empty_display_type='CIRCLE'; gen_rot.empty_display_size=.007
small=cyl('Generator_Pulley_10mm',(-.025,0,.078),.005,.008,'Y','06_BELT_SYSTEM',METAL,48); small['pitch_diameter_mm']=10
set_parent(small,gen_rot)
for i in range(12):
    th=2*math.pi*i/12
    t=cube('Generator_Pulley_Tooth',(-.025+.0054*math.cos(th),0,.078+.0054*math.sin(th)),(.0018,.009,.0018),'06_BELT_SYSTEM',BLACK,.0002); t.rotation_euler[1]=-th
    set_parent(t,gen_rot)

# Belt loop centerline exact common tangent path in XZ plane. Drive pulley radius 20 mm, gen 5 mm.
# Center distance about 76 mm; open belt length ~233 mm. Beveled curve + animated motion texture.
R=.020; r=.005; x1,z1=-.085,.032; x2,z2=-.025,.078
c=math.hypot(x2-x1,z2-z1); beta=math.atan2(z2-z1,x2-x1); alpha=math.asin((R-r)/c)
th_hi=beta+math.acos((R-r)/c); th_lo=beta-math.acos((R-r)/c)
pts=[]
for i in range(96):
    t=i/96; ang=th_hi+(th_lo+2*math.pi-th_hi)*t; pts.append((x1+R*math.cos(ang),0,z1+R*math.sin(ang)))
pts.append((x2+r*math.cos(th_lo),0,z2+r*math.sin(th_lo)))
for i in range(72):
    t=i/72; ang=th_lo+(th_hi+2*math.pi-th_lo)*t; pts.append((x2+r*math.cos(ang),0,z2+r*math.sin(ang)))
cu=bpy.data.curves.new('Belt_Continuous_Loop_Path','CURVE'); cu.dimensions='3D'; cu.resolution_u=16; cu.bevel_depth=.0014; cu.bevel_resolution=4; sp=cu.splines.new('POLY'); sp.points.add(len(pts)-1)
for p,co in zip(sp.points,pts): p.co=(*co,1)
sp.use_cyclic_u=True; bo=bpy.data.objects.new('Belt_Continuous_40_to_10',cu); COL['06_BELT_SYSTEM'].objects.link(bo); cu.materials.append(BELT); bo['center_distance_mm']=round(c*1000,1); bo['approx_length_mm']=233; bo['belt_speed_m_s']='generator pitch radius x generator angular speed'
# Bezier-emulated repeating marker dots provide visible belt travel indication, patterned along the closed path.
for j in range(40):
    t=j/40; idx=int(t*len(pts)); p=pts[idx]; marker=cube('Belt_Motion_Marker',(p[0],-.0015,p[2]),(.003,.0007,.0012),'06_BELT_SYSTEM',BELT2,.0002)
    marker['purpose']='Moving visual belt marker; belt path remains a continuous loop'
# Animate belt shader mapping so a diagonal stripe texture scrolls around the loop.
nt=BELT.node_tree; bs=nt.nodes.get('Principled BSDF'); tex=nt.nodes.new('ShaderNodeTexWave'); tex.wave_type='BANDS'; tex.bands_direction='X'; tex.inputs['Scale'].default_value=32; tex.inputs['Distortion'].default_value=0; tc=nt.nodes.new('ShaderNodeTexCoord'); mapping=nt.nodes.new('ShaderNodeVectorMath'); mapping.operation='ADD'; mapping.inputs[1].default_value=(0,0,0); nt.links.new(tc.outputs['Generated'],mapping.inputs[0]); nt.links.new(mapping.outputs['Vector'],tex.inputs['Vector']); nt.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
# generator motor, shaft/pulley are constrained by ratio 4:1.
cube('Generator_Mount_Plate',(-.025,-.025,.064),(.040,.034,.004),'05_GENERATOR',METAL,.001)
for yy in [-.038,-.012]: cube('Generator_Mount_Standoff',(-.025,yy,.0615),(.006,.006,.001),'05_GENERATOR',BLACK,.0003)
gen=cyl('Generator_Motor',(-.025,-.025,.078),.011,.035,'Y','05_GENERATOR',ALUM,48); gen['voltage_range_V']='3-6'; gen['assumed_efficiency']=.55; gen['is_independent_rotation']=False; gen['electrical_output']='test load only; no battery charging in V1'
for dy,ma in [(-.006,RED),(.006,BLUE)]: rod('Generator_Terminal',(-.014,-.025+dy,.078),(-.006,-.025+dy,.084),.0014,'05_GENERATOR',ma)
shaft=cyl('Generator_Shaft',(-.025,0,.078),.002,.035,'Y','05_GENERATOR',METAL,32)
# The large pulley is parented to the rear axle and inherits its 1:1 motion.
# A second local rotation driver would double its speed.
f=gen_rot.driver_add('rotation_euler',1); d=f.driver; d.type='SCRIPTED'; d.expression='-a*4'; v=d.variables.new(); v.name='a'; v.type='SINGLE_PROP'; v.targets[0].id=ctrl; v.targets[0].data_path='["Drive_Angle"]'
set_parent(shaft,gen_rot)

# Body electrical pieces, placed in footprint with clearance.
cube('Battery_Holder_4xAA_NiMH',(0,.030,.071),(.108,.030,.006),'07_BATTERY',BLACK,.0015)
for cell_x in [-.026,.026]:
    for cell_y in [-.0057,.0057]:
        cell=cyl('AA_NiMH_1.2V_Cell',(cell_x,.030+cell_y,.080),.0052,.050,'X','07_BATTERY',LIPO,48); cell['nominal_voltage_V']=1.2; cell['capacity_mAh_assumed']=2000
# Four cells are wired as a serpentine 4S pack (4.8 V nominal), not 2S2P.
cube('Battery_Pack_End_Retainer',(0,.030,.074),(.108,.030,.001),'07_BATTERY',BLACK,.0004)
for bridge_x,bridge_y,bridge_dims in [(0,.0243,(.003,.001,.001)),(0,.0357,(.003,.001,.001)),(.052,.030,(.001,.013,.001))]:
    cube('Battery_Series_Interconnect', (bridge_x,bridge_y,.080), bridge_dims,'07_BATTERY',METAL,.0002)
cube('TB6612FNG_Motor_Driver_Board',(.056,0,.071),(.040,.030,.009),'08_ELECTRONICS',PCB,.0015)
cube('TB6612FNG_Driver_IC',(.056,0,.077),(.010,.009,.003),'08_ELECTRONICS',BLACK,.0005)
cube('INA219_Battery_Sensor_Board',(.068,.009,.0765),(.012,.009,.0016),'08_ELECTRONICS',PCB,.0004)
cube('INA219_Battery_Sensor_IC',(.068,.009,.078),(.004,.004,.0015),'08_ELECTRONICS',BLACK,.0003)
cube('INA219_Generator_Sensor_Board',(.045,-.045,.069),(.026,.020,.003),'08_ELECTRONICS',PCB,.0005)
cube('INA219_Generator_Sensor_IC',(.045,-.045,.071),(.005,.005,.0015),'08_ELECTRONICS',BLACK,.0003)
cube('Arduino_Nano_Compatible_5V',(.082,.039,.069),(.045,.018,.0016),'08_ELECTRONICS',PCB,.0005)
cube('Arduino_Nano_Main_IC',(.082,.039,.071),(.010,.008,.002),'08_ELECTRONICS',BLACK,.0004)
cube('OLED_0.96in_I2C_Module',(.082,.039,.075),(.027,.024,.002),'08_ELECTRONICS',BLACK,.0005)
cube('OLED_Display_Glass',(.082,.039,.0765),(.022,.018,.001),'08_ELECTRONICS',ALUM,.0002)
cube('Generator_Schottky_1N5819',(.063,-.045,.072),(.006,.003,.003),'08_ELECTRONICS',BLACK,.0005)
cube('Adjustable_DC_DC_Test_Module',(.083,-.045,.069),(.025,.020,.004),'08_ELECTRONICS',PCB,.0005)
cube('Generator_Test_Load_Resistor',(.111,-.045,.070),(.018,.010,.006),'08_ELECTRONICS',YELLOW,.001)
master=cube('Master_Switch_SPST_3A',(.112,-.020,.070),(.016,.014,.009),'08_ELECTRONICS',BLACK,.0015); master['rated_current_A']=3; cube('Switch_Red_Rocker',(.112,-.020,.0755),(.009,.007,.003),'08_ELECTRONICS',RED,.0007)
cyl('Main_Fuse_2A',(.096,.005,.071),.0025,.012,'X','08_ELECTRONICS',METAL,24)
for cap_x,cap_y in [(-.081,-.034),(-.081,.034),(.052,0)]:
    cyl('Noise_Suppression_Capacitor_100nF',(cap_x,cap_y,.069),.002,.004,'Z','08_ELECTRONICS',YELLOW,24)
cyl('Supply_Capacitor_1000uF_10V',(.045,.008,.074),.004,.009,'Z','08_ELECTRONICS',BLACK,32)

# Routed electrical wires as curves, red/blue motor circuits and generator return.
def wire(name,coords,material,radius=.0008):
    # A polyline preserves the designed cable corridor. AUTO Bezier handles
    # overshot corners and made the earlier harnesses loop across components.
    cu=bpy.data.curves.new(name,'CURVE'); cu.dimensions='3D'; cu.resolution_u=2; cu.bevel_depth=radius; cu.bevel_resolution=3; s=cu.splines.new('POLY'); s.points.add(len(coords)-1)
    for bp,co in zip(s.points,coords): bp.co=(*co,1)
    o=bpy.data.objects.new(name,cu); COL['09_WIRING'].objects.link(o); cu.materials.append(material); return o
# Terminated, routed harnesses.  Motor pairs follow opposite chassis edges, above
# the board/deck, and land on the exposed motor pigtails instead of crossing the cargo.
for sign,side in [(-1,'Left'),(1,'Right')]:
    for polarity,dy,ma in [('Positive',-.006,RED),('Negative',.006,BLUE)]:
        end_y=sign*.030+dy
        lane=sign*(.053 if polarity=='Positive' else .058)
        start_y=sign*.010+(0.004 if dy>0 else -0.004)
        cube(f'{side}_{polarity}_Controller_Output_Pad',(.036,start_y,.07625),(.002,.003,.0015),'08_ELECTRONICS',ALUM,.0003)
        wire(f'{side}_{polarity}_Motor_Harness',[(.037,start_y,.079),(.080,sign*.020,.079),(.083,lane,.067),(-.080,lane,.067),(-.090,end_y,.087),(-.085,end_y,.087)],ma,.0007)
# Battery connector and controller input pads; short service loop has visible clearance.
for nm,tx,sy,ey,ma in [('Battery_Positive',-.052,.0243,.008,RED),('Battery_Negative',-.052,.0357,-.008,BLUE)]:
    cube(nm+'_Terminal',(tx,sy,.080),(.003,.004,.002),'07_BATTERY',METAL,.0005)
    cube(nm+'_Controller_Pad',(.076,ey,.07625),(.002,.004,.0015),'08_ELECTRONICS',ALUM,.0003)
    wire(nm+'_Harness',[(tx,sy,.080),(tx,.050 if sy>0 else .010,.086),(.020,.048 if sy>0 else .006,.086),(.070,.024 if sy>0 else -.008,.082),(.076,ey,.079)],ma,.0007)
# Generator output routes through its sensor and test-load electronics only; no battery return path exists.
wire('Generator_Positive_to_INA219_TestLoad',[(-.006,-.031,.084),(.001,-.033,.079),(.012,-.040,.074),(.024,-.045,.073),(.032,-.045,.073)],RED,.0007)
wire('Generator_Negative_to_INA219_TestLoad',[(-.006,-.019,.084),(.002,-.021,.078),(.012,-.030,.074),(.024,-.040,.073),(.032,-.040,.073)],BLUE,.0007)
wire('TestLoad_Positive_to_DC_DC',[(.058,-.045,.073),(.060,-.045,.073),(.066,-.045,.073),(.071,-.045,.072)],RED,.0007)
wire('DC_DC_to_Test_Load_Resistor',[(.095,-.045,.071),(.099,-.045,.073),(.102,-.045,.073)],RED,.0007)
wire('TestLoad_Return',[(.102,-.049,.069),(.094,-.053,.068),(.064,-.053,.068),(.058,-.049,.071)],BLUE,.0007)

# load torque property driver on abstract indicator, documenting physically meaningful sign effect.
load_meter=cube('Generator_Load_Resistance_Indicator',(-.030,.045,.071),(.024,.008,.004),'10_RIG',YELLOW,.001); load_meter['meaning']='Visual load indicator only; real generator load creates opposing torque. Quantified in engineering report.'
f=load_meter.driver_add('scale',1); d=f.driver; d.type='SCRIPTED'; d.expression='1+load'; v=d.variables.new(); v.name='load'; v.type='SINGLE_PROP'; v.targets[0].id=ctrl; v.targets[0].data_path='["Generator_Load"]'

# Rig animation: stationary 0-2 s, accelerate, constant speed to 8 s, hold for readout.
for fr,ang in [(1,0),(50,0),(125,45),(200,90),(250,90)]:
    ctrl['Drive_Angle']=ang; ctrl.keyframe_insert(data_path='["Drive_Angle"]',frame=fr)
if scene.animation_data and scene.animation_data.action:
    for fc in scene.animation_data.action.fcurves:
        for kp in fc.keyframe_points: kp.interpolation='LINEAR'
# map wave offset animate belt travel texture
for fr,val in [(1,0),(250,8)]: mapping.inputs[1].default_value=(val,0,0); mapping.inputs[1].keyframe_insert(data_path='default_value',frame=fr)

# Engineering labels and measurement text block.
data=bpy.data.texts.new('ENGINEERING_README.txt')
data.write('AUTO CHARGING CAR — EDITABLE ENGINEERING PROTOTYPE\n')
data.write('Units: meters internally, Blender display in mm. Overall deck 250 x 120 mm, 2 x 2 mm cardboard layers.\n')
data.write('Rig: select 10_RIG / Drivetrain_Control and change Drive_Angle (radians). Wheel, rear axle and drive pulley ratio 1:1; generator pulley diameter ratio 40/10 = 4:1. Generator shaft follows the belt pulley driver. Generator_Load ranges 0..1; it scales the load indicator and represents opposing torque from extracting current. The rotation rig is kinematic and does not solve torque dynamically.\n')
data.write('Belt is a 3D continuous common-tangent loop, 1.4 mm radius, center distance about 76 mm, approximate pitch length 233 mm. Animated shader bands show travel; this is a kinematic visual, not a tension/contact solver. The two-layer chassis has a 70 x 12 mm centerline belt tunnel and a generator bridge plate.\n')
data.write('Engineering assumptions: 4 x AA NiMH, 4.8 V nominal and assumed 2 Ah; 2 x 6 V N20 motors rated 200-300 rpm; vehicle mass 0.35 kg; wheel radius 0.032 m; rolling coefficient 0.03; bearing drag 0.15 N; drive/controller efficiency 0.90; belt efficiency 0.90; generator efficiency 0.55. Generator power goes to a test load only; battery return is zero.\n')
data.write('At 0.25 m/s, wheel rpm = 74.6; generator rpm = 298. Belt speed = 0.156 m/s (pulley pitch speed). Rolling force 0.103 N; plus axle drag estimate gives 0.253 N, mechanical road power 0.063 W. With motor efficiency 55%, electrical drive input about 0.115 W. Generator mechanical input under load is modeled as 0.030 W; electrical output ~0.0165 W; generator output is dissipated into a test load; battery return is zero. Supplying added generator input through the assumed 55% efficient drive motors and 90% belt takes 0.0606 W extra battery power; subtracting returned charge leaves +0.0474 W net drain. At unloaded disconnect, drive draw ~0.115 W; connected estimate ~0.162 W.\n')
data.write('Drive torque estimate: total wheel torque for rolling+axle drag = 0.0081 Nm. Generator plus belt asks for another ~0.0043 Nm at drive axle (0.0333 W belt input / 7.82 rad/s), for ~0.0124 Nm total. Shared by two rear drive motors, this is ~0.0062 Nm per motor. Assumed 0.012 Nm is stall torque, not confirmed continuous torque; motor datasheets must show at least 6.2 mNm continuous output at required speed. Generator torque at 298 rpm is ~0.00096 Nm. Belt tension difference is about 0.21 N; pretension raises mount load.\n')
data.write('N-generator comparison at equal mechanical input per generator (0.030 W each): N=1: mech 0.030 W, test-load output about 0.03 W, net extra battery drain ~0.0474 W. N=2: mech 0.060 W, returned 0.0264 W, extra drain ~0.0948 W. N=3: 0.090 W, returned 0.0396 W, extra drain ~0.1422 W. N=4: 0.120 W, returned 0.0528 W, extra drain ~0.1896 W. Drive energy for generator input = Pmech/(0.90*0.55); generator and charge stage efficiency = 0.55*0.80=44%. Including drive motor and belt, battery-to-return efficiency is 0.55*0.90*0.55*0.80=21.8%. For all cases returned power is below extra battery power required.\n')
data.write('Buildability notes: the 250x120 mm deck has a 70x12 mm belt tunnel and side edge rails; tires remain outside the deck width. Wheel diameter is 64 mm, chassis underside about 51 mm high and axle centers at 32 mm. Belt clears the two deck layers through the tunnel. Generator bridge and drive-motor brackets need reinforcement with plywood/aluminum doublers; single cardboard fasteners may tear. Chassis may flex at 0.35 kg, especially near the center tunnel; add longitudinal rails/cross braces and keep battery near center. Motor torque suffices only if selected gearmotors deliver at least ~6.2 mNm each continuously at ~75 rpm; verify datasheet.\n')
data.write('Conclusion: A, likely moves on smooth floor if actual geared motors meet torque/speed requirement. B, belt kinematically drives generator, subject to tracking/tension alignment. C, measurable open-circuit voltage is likely at ~300 rpm, but charging a 6 V pack depends on actual motor voltage and a suitable boost charger. D, optimistic return is ~13 mW against 61 mW of added battery draw, increasing net drain ~47 mW. E, continuous generator charging reduces runtime. F, self-charging while driving without external input is impossible; useful recovery sources include braking energy, downhill descent, or external solar. Preserve the design with switchable regenerative braking during deceleration and a small solar panel; use current-limited charging matched to battery chemistry.\n')

# Replace the rough initial placeholders with the motor-curve physics estimate.
with open(os.path.join(OUT,'physics_model.json'),encoding='utf-8') as pf: phys=json.load(pf)
case0,case1=phys['cases'][0],phys['cases'][1]
data.clear()
data.write('AUTO CHARGING CAR — EDITABLE ENGINEERING PROTOTYPE\n')
data.write('Units: meters internally, Blender displays millimeters. Deck 250 x 120 mm, two 2 mm cardboard layers.\n')
data.write('Rig: edit 10_RIG / Drivetrain_Control / Drive_Angle (radians). Wheels and 40 mm pulley rotate -Drive_Angle for forward travel; generator pulley and shaft rotate -4 x Drive_Angle. Generator_Load is a 0..1 visual load indicator; the browser estimates torque/current response from physics_model.json.\n')
data.write('Belt: continuous visible loop around 40 mm and 10 mm pitch pulleys, 4:1. Blender view is kinematic; the browser estimates motor torque/current, wheel speed, belt speed, generator test-load output, and battery use. V1 has no generator-to-battery charging path.\n')
data.write('Motor proxy: N20 6 V 200-300 rpm motor assumption; simulation proxy is 250 rpm and scales to 200 rpm at 4.8 V. Exact N20 vendor torque/current data is unconfirmed. Battery: four AA NiMH cells, 4.8 V nominal, assumed 2 Ah and 7.68 Wh usable. Generator output goes to a test load only; it is not connected to the battery. Vehicle mass: 0.35 kg. See ENGINEERING_REPORT.md for source and limitations.\n')
data.write(f"Generator disconnected: {case0['speed_km_h']:.2f} km/h, {case0['wheel_rpm']:.1f} wheel rpm, {case0['drive_battery_power_W']:.3f} W battery, {case0['runtime_hours_ideal']:.1f} h idealized.\n")
data.write(f"One loaded generator: {case1['speed_km_h']:.2f} km/h, {case1['wheel_rpm']:.1f} wheel rpm, {case1['generator_rpm_each']:.0f} generator rpm, {case1['motor_torque_each_Nm']*1000:.2f} mN m per drive motor, {case1['drive_battery_power_W']:.3f} W drive, {case1['generator_electrical_output_W']:.3f} W generator test-load output, {case1['net_battery_power_W']:.3f} W net, {case1['runtime_hours_ideal']:.1f} h idealized.\n")
data.write('Formulas: P=V*I; P_mech=tau*omega. Generator torque resists the drive and reflected torque includes belt loss. No free energy. 1–4 generator table and buildability caveats are in ENGINEERING_REPORT.md.\n')

# View cameras and lighting.
def aim(o,pt): o.rotation_euler=(Vector(pt)-o.location).to_track_quat('-Z','Y').to_euler()
def camera(name,loc,target,lens=50):
    bpy.ops.object.camera_add(location=loc); o=bpy.context.object; link_obj(o,'00_REFERENCE',name); o.data.lens=lens; aim(o,target); return o
cam=camera('CAM_PERSPECTIVE',(.36,-.42,.34),(0,0,.05),52); scene.camera=cam
camera('CAM_TOP',(0,-.02,.55),(0,0,.035),52)
camera('CAM_SIDE',(-.02,-.52,.07),(0,0,.05),58)
camera('CAM_BOTTOM',(0,-.02,-.30),(0,0,.04),50)
camera('CAM_BELT_CLOSEUP',(-.095,-.19,.16),(-.055,0,.055),70)
camera('CAM_EXPLODED',(.34,-.42,.42),(0,0,.13),52)
camera('CAM_TRANSPARENT',(.035,-.40,.095),(0,0,.055),52)
def area(name,loc,power,size,color):
    bpy.ops.object.light_add(type='AREA',location=loc); o=bpy.context.object; link_obj(o,'00_REFERENCE',name); o.data.energy=power; o.data.shape='DISK'; o.data.size=size; o.data.color=color; aim(o,(0,0,.05))
area('Key_Light',(.2,-.25,.36),8,0.24,(1,.82,.65)); area('Fill_Light',(-.25,-.12,.22),5,.20,(.62,.78,1)); area('Rim_Light',(.0,.25,.28),9,.18,(.55,.78,1))
world=bpy.data.worlds.new('Studio World') if not bpy.data.worlds else bpy.data.worlds[0]; scene.world=world; world.use_nodes=True; world.node_tree.nodes['Background'].inputs[0].default_value=(.045,.06,.08,1); world.node_tree.nodes['Background'].inputs[1].default_value=.45

# Exploded view collection: linked, selectable duplicates with vertical offsets, all named.
# Use colored simple explainer panels as exploded subsystem map.
explode_offsets={'07_BATTERY':.115,'08_ELECTRONICS':.115,'09_WIRING':.100,'05_GENERATOR':.082,'06_BELT_SYSTEM':.060,'04_DRIVE_MOTORS':.040,'03_AXLES':.020,'01_CHASSIS':0,'02_WHEELS':-.025}
for source,offset in explode_offsets.items():
    for o in list(COL[source].objects):
        cp=o.copy(); cp.data=o.data.copy() if o.data else None; cp.name='EXPLODED_'+o.name; COL['12_EXPLODED_VIEW'].objects.link(cp); cp.location=o.location+Vector((0,0,offset)); cp['exploded_offset_mm']=round(offset*1000,1)
for i,(lbl,z) in enumerate([('BATTERY + ELECTRONICS',.210),('GENERATOR',.164),('BELT + PULLEYS',.130),('DRIVE MOTORS',.100),('AXLES',.078),('CHASSIS',.056),('WHEELS',.030)]): text('Exploded_Label_'+str(i),lbl,(.105,-.015,z),.006,'12_EXPLODED_VIEW',WHITE)
# Separate scene controls exploded visibility while keeping the default assembled view clean.
assembled_scene=scene; assembled_scene.name='ASSEMBLED_ENGINEERING_VIEW'
transparent_coll=bpy.data.collections.new('13_TRANSPARENT_VIEW'); root.children.link(transparent_coll)
transmat=mat('Cardboard transparent engineering overlay',(.48,.72,.83),.05,.4,.19)
for ob in list(COL['01_CHASSIS'].objects):
    cp=ob.copy(); cp.data=ob.data.copy() if ob.data else None; cp.name='TRANSPARENT_'+ob.name; transparent_coll.objects.link(cp)
    if cp.data and hasattr(cp.data,'materials'):
        cp.data.materials.clear(); cp.data.materials.append(transmat)
# measurement panel in scene, formatted text.
panel=text('Power_Flow_Information','POWER MODEL / SEE ENGINEERING REPORT + GAME HUD',(.125,-.04,.102),.0035,'11_MEASUREMENTS',WHITE)
panel.hide_render=True
panel['editable_report']='See ENGINEERING_README.txt for assumptions and 1-4 generator comparison.'

# Stage checkpoint before creating exploded and view-specific scene variants.
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(OUT,'Checkpoint_01_Assembled.blend'))

# Transparent engineering materials via alternative viewport and render-friendly transparent overlay copy.
for o in COL['01_CHASSIS'].objects:
    o['transparent_view_alpha']=0.18
# arrow path overlays
def arrow(name,a,b,material):
    rod(name+'_Shaft',a,b,.00045,'11_MEASUREMENTS',material)
    # cone at end
    d=Vector(b)-Vector(a); mid=Vector(b)-d.normalized()*.0025
    bpy.ops.mesh.primitive_cone_add(vertices=24,radius1=.001,radius2=0,depth=.0025,location=mid); ob=bpy.context.object; ob.rotation_mode='QUATERNION'; ob.rotation_quaternion=Vector((0,0,1)).rotation_difference(d.normalized()); link_obj(ob,'11_MEASUREMENTS',name+'_Arrowhead',material)
# The assembled view stays free of measurement arrows; the energy route is explained
# in the report and shown in the dedicated transparent view.

# Separate exploded scene: hide normal source geometry there and reveal exploded copies.
bpy.ops.scene.new(type='FULL_COPY'); exploded_scene=bpy.context.scene; exploded_scene.name='EXPLODED_VIEW'
def find_layer_collection(lc,name):
    if lc.collection.name==name or lc.collection.name.startswith(name+'.'): return lc
    for child in lc.children:
        found=find_layer_collection(child,name)
        if found: return found
    return None
find_layer_collection(assembled_scene.view_layers[0].layer_collection,'12_EXPLODED_VIEW').exclude=True
find_layer_collection(assembled_scene.view_layers[0].layer_collection,'13_TRANSPARENT_VIEW').exclude=True
find_layer_collection(assembled_scene.view_layers[0].layer_collection,'09_WIRING').exclude=True
find_layer_collection(assembled_scene.view_layers[0].layer_collection,'11_MEASUREMENTS').exclude=True
for source in explode_offsets:
    target=find_layer_collection(exploded_scene.view_layers[0].layer_collection,source)
    if target: target.exclude=True
target=find_layer_collection(exploded_scene.view_layers[0].layer_collection,'12_EXPLODED_VIEW')
if target: target.exclude=False
target=find_layer_collection(exploded_scene.view_layers[0].layer_collection,'13_TRANSPARENT_VIEW')
if target: target.exclude=True
exploded_scene.camera=bpy.data.objects.get('CAM_EXPLODED')
# Dedicated transparent chassis scene to expose the motor, axle, pulley and belt path.
bpy.ops.scene.new(type='FULL_COPY'); transparent_scene=bpy.context.scene; transparent_scene.name='TRANSPARENT_DRIVETRAIN_VIEW'
for src in names:
    target=find_layer_collection(transparent_scene.view_layers[0].layer_collection,src)
    if target: target.exclude=(src in ['01_CHASSIS','12_EXPLODED_VIEW'])
target=find_layer_collection(transparent_scene.view_layers[0].layer_collection,'13_TRANSPARENT_VIEW')
if target: target.exclude=False
transparent_scene.camera=bpy.data.objects.get('CAM_TRANSPARENT')
bpy.context.window.scene=assembled_scene
assembled_scene.view_settings.view_transform='AgX'; assembled_scene.view_settings.look='AgX - Medium High Contrast'; assembled_scene.view_settings.exposure=-0.7
# Set default 3D view, select controller, save.
scene.render.image_settings.file_format='PNG'; scene.render.filepath=os.path.join(OUT,'AutoChargingCar_preview.png')
for area0 in bpy.context.screen.areas:
    if area0.type=='VIEW_3D':
        area0.spaces.active.region_3d.view_perspective='PERSP'; area0.spaces.active.region_3d.view_location=(0,0,.045); area0.spaces.active.region_3d.view_distance=.48
bpy.ops.object.select_all(action='DESELECT'); ctrl.select_set(True); bpy.context.view_layer.objects.active=ctrl
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(OUT,'AutoChargingCar_EngineeringPrototype.blend'))
# Render initial assembled preview.
scene.frame_set(140); bpy.ops.render.render(write_still=True)
print('Saved prototype:',os.path.join(OUT,'AutoChargingCar_EngineeringPrototype.blend'))
