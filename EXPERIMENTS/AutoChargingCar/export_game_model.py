"""Export the assembled Blender geometry and matching 10 s demo rig to glTF."""
import bpy, math, os, json, struct
from mathutils import Vector

OUT=os.path.dirname(os.path.abspath(__file__))
src=os.path.join(OUT,'AutoChargingCar_EngineeringPrototype.blend')
if bpy.data.filepath != src: bpy.ops.wm.open_mainfile(filepath=src)
scene=bpy.data.scenes['ASSEMBLED_ENGINEERING_VIEW']
bpy.context.window.scene=scene
if not hasattr(bpy.ops.export_scene,'gltf'):
    bpy.ops.preferences.addon_enable(module='io_scene_gltf2')

# Keep the exported model exactly in the Blender assembly collections.
target=[]
for cn in ['01_CHASSIS','02_WHEELS','03_AXLES','04_DRIVE_MOTORS','05_GENERATOR','06_BELT_SYSTEM','07_BATTERY','08_ELECTRONICS']:
    target.extend(list(bpy.data.collections[cn].objects))
target=[o for o in target if o.type in {'MESH','CURVE','SURFACE'}
        and not o.name.startswith('Energy_Path_')
        and o.name not in {'Front_Left_Wheel_Axle','Front_Right_Wheel_Axle'}]
rig_names=['Front_Left_Wheel_Rotation','Front_Right_Wheel_Rotation','Rear_Left_Wheel_Rotation','Rear_Right_Wheel_Rotation','Front_Left_Steering_Pivot','Front_Right_Steering_Pivot','Front_Tie_Rod_Control','Front_Servo_Horn_Rotation','Rear_Left_Axle_Rotation','Rear_Right_Axle_Rotation','Generator_Pulley_Rotation']
target.extend(bpy.data.objects[n] for n in rig_names)
for o in bpy.data.objects: o.select_set(False)
for o in target: o.select_set(True)
bpy.context.view_layer.objects.active=bpy.data.objects['Rear_Left_Axle_Rotation']

# Create an export-only baked version of the same 2–5 s acceleration / 5–8 s
# steady motion. The native .blend retains its editable custom-property drivers.
frames=[(1,0.0),(50,0.0),(125,45.0),(200,90.0),(250,90.0)]
angle_at={fr:a for fr,a in frames}
def finish_action(obj):
    if obj.animation_data and obj.animation_data.action:
        act=obj.animation_data.action; act.name='Engineering_Demo_'+obj.name
        for layer in act.layers:
            for strip in layer.strips:
                for slot in act.slots:
                    try: bag=strip.channelbag(slot)
                    except Exception: bag=None
                    if bag:
                        for fc in bag.fcurves:
                            for kp in fc.keyframe_points: kp.interpolation='LINEAR'
        # Merge separate object actions into one glTF clip through same-named NLA strips.
        track=obj.animation_data.nla_tracks.new(); track.name='Engineering_Demo'; strip=track.strips.new('Engineering_Demo',1,obj.animation_data.action)
        strip.name='Engineering_Demo'; obj.animation_data.action=None
def anim(obj, prop, values):
    obj.animation_data_clear()
    for frame,value in values:
        setattr(obj,prop,value); obj.keyframe_insert(data_path=prop,frame=frame,group='Engineering Demo')
    finish_action(obj)
def anim_multi(obj, channels):
    obj.animation_data_clear()
    for prop,values in channels.items():
        for frame,value in values:
            setattr(obj,prop,value); obj.keyframe_insert(data_path=prop,frame=frame,group='Engineering Demo')
    finish_action(obj)

for name in rig_names[:4]:
    ob=bpy.data.objects[name]
    anim(ob,'rotation_euler',[(fr,(0.0,-a,0.0)) for fr,a in frames])
for name in ['Rear_Left_Axle_Rotation','Rear_Right_Axle_Rotation']:
    anim(bpy.data.objects[name],'rotation_euler',[(fr,(0.0,-a,0.0)) for fr,a in frames])
for name in ['Front_Left_Steering_Pivot','Front_Right_Steering_Pivot']:
    anim(bpy.data.objects[name],'rotation_euler',[(1,(0,0,0)),(125,(0,0,0)),(175,(0,0,0.16)),(200,(0,0,0.16)),(250,(0,0,0))])
anim(bpy.data.objects['Front_Servo_Horn_Rotation'],'rotation_euler',[(1,(0,0,0)),(125,(0,0,0)),(175,(0,0,.16)),(200,(0,0,.16)),(250,(0,0,0))])
anim_multi(bpy.data.objects['Front_Tie_Rod_Control'],{'location':[(1,(.070,0,.049)),(125,(.070,0,.049)),(175,(.069872,0,.049)),(200,(.069872,0,.049)),(250,(.070,0,.049))],'rotation_euler':[(1,(0,0,0)),(125,(0,0,0)),(175,(0,0,.0646)),(200,(0,0,.0646)),(250,(0,0,0))]})
anim(bpy.data.objects['Generator_Pulley_Rotation'],'rotation_euler',[(fr,(0.0,-4*a,0.0)) for fr,a in frames])

# Sample the actual Blender belt spline and animate its existing marker pieces along it.
curve=bpy.data.objects['Belt_Continuous_40_to_10']
sp=curve.data.splines[0]
path=[Vector((p.co.x,p.co.y,p.co.z)) for p in sp.points]
if path[0] != path[-1]: path.append(path[0])
segments=[]; total=0.0
for a,b in zip(path,path[1:]):
    length=(b-a).length; segments.append((total,length,a,b)); total+=length
def at_distance(distance):
    d=distance%total
    for start,length,a,b in segments:
        if start<=d<=start+length:
            return a.lerp(b,(d-start)/length)
    return path[0]
markers=sorted([o for o in bpy.data.collections['06_BELT_SYSTEM'].objects if o.name.startswith('Belt_Motion_Marker')],key=lambda o:o.name)
for i,ob in enumerate(markers):
    origin=i*total/max(1,len(markers))
    vals=[]
    for fr,drive_angle in frames:
        # Linear interpolation of authored angle between the five animation keys.
        before=max((item for item in frames if item[0]<=fr),key=lambda z:z[0])
        after=min((item for item in frames if item[0]>=fr),key=lambda z:z[0])
        if after[0]==before[0]: a=before[1]
        else: a=before[1]+(after[1]-before[1])*(fr-before[0])/(after[0]-before[0])
        loc=at_distance(origin+0.020*a)
        vals.append((fr,(loc.x,-0.0015,loc.z)))
    # Animate local location; markers have no parent in the native assembly.
    ob.animation_data_clear()
    for fr,loc in vals:
        ob.location=loc; ob.keyframe_insert(data_path='location',frame=fr,group='Engineering Demo')
    if ob.animation_data and ob.animation_data.action:
        act=ob.animation_data.action; act.name='Engineering_Demo_'+ob.name
        for layer in act.layers:
            for strip in layer.strips:
                for slot in act.slots:
                    try: bag=strip.channelbag(slot)
                    except Exception: bag=None
                    if bag:
                        for fc in bag.fcurves:
                            for kp in fc.keyframe_points: kp.interpolation='LINEAR'
        tr=ob.animation_data.nla_tracks.new(); tr.name='Engineering_Demo'; st=tr.strips.new('Engineering_Demo',1,ob.animation_data.action); st.name='Engineering_Demo'; ob.animation_data.action=None

# Convert procedural curves to mesh in this export-only process, retaining names/materials.
for o in target:
    if o.type=='CURVE': o.select_set(True)
bpy.context.view_layer.objects.active=curve
bpy.ops.object.convert(target='MESH')
for o in bpy.context.selected_objects: o.select_set(False)
for o in target:
    if o.name in bpy.data.objects: o.select_set(True)
bpy.context.view_layer.objects.active=bpy.data.objects['Rear_Left_Axle_Rotation']

path_json=[]
for p in path:
    # glTF Y-up coordinate mapping: Blender (x,y,z) -> (x,z,-y).
    path_json.append([p.x,p.z,0.0015])
with open(os.path.join(OUT,'belt_path.json'),'w') as f: json.dump({'length_m':total,'points':path_json},f)

props=bpy.ops.export_scene.gltf.get_rna_type().properties
kwargs={'filepath':os.path.join(OUT,'browser-game','public','auto_charging_car.glb'),
        'export_format':'GLB','use_selection':True,'export_animations':True,
        'export_frame_range':True,'export_force_sampling':True,
        'export_nla_strips':True,'export_yup':True,'export_apply':False,
        'export_extras':True}
if 'export_nla_strips_merged_animation_name' in props:
    kwargs['export_nla_strips_merged_animation_name']='Engineering_Demo'
kwargs={k:v for k,v in kwargs.items() if k in props}
bpy.ops.export_scene.gltf(**kwargs)
# Blender exports one NLA clip per animated object. All strips share the same
# frame range, so join their samplers/channels into one directly playable clip.
glb=kwargs['filepath']; raw=open(glb,'rb').read(); magic,version,total=struct.unpack_from('<4sII',raw,0)
json_len,json_type=struct.unpack_from('<I4s',raw,12); doc=json.loads(raw[20:20+json_len]); binary=raw[20+json_len:]
clips=doc.get('animations',[])
if clips:
    samplers=[]; channels=[]
    for clip in clips:
        offset=len(samplers); samplers.extend(clip['samplers'])
        for channel in clip['channels']:
            channel=dict(channel); channel['sampler']+=offset; channels.append(channel)
    doc['animations']=[{'name':'Engineering_Demo','samplers':samplers,'channels':channels}]
    encoded=json.dumps(doc,separators=(',',':')).encode('utf-8'); encoded+=b' '*((-len(encoded))%4)
    chunk=struct.pack('<I4s',len(encoded),b'JSON')+encoded+binary
    with open(glb,'wb') as f:f.write(struct.pack('<4sII',magic,version,12+len(chunk))+chunk)
    print('Merged',len(clips),'object tracks into one Engineering_Demo clip')
print('Exported Blender assembly:',kwargs['filepath'])
print('Animation options:', {k:str(props[k].default) for k in ['export_animations','export_nla_strips'] if k in props})
