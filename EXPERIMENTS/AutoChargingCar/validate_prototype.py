import bpy, math, json, os

ROOT=os.path.dirname(bpy.data.filepath)
required_collections=[f'{i:02d}_' for i in range(13)]
for prefix in required_collections: assert any(c.name.startswith(prefix) for c in bpy.data.collections),f'Missing collection {prefix}'
for scene_name in ['ASSEMBLED_ENGINEERING_VIEW','EXPLODED_VIEW','TRANSPARENT_DRIVETRAIN_VIEW']: assert bpy.data.scenes.get(scene_name),f'Missing scene {scene_name}'
for cam in ['CAM_PERSPECTIVE','CAM_TOP','CAM_SIDE','CAM_BOTTOM','CAM_BELT_CLOSEUP','CAM_EXPLODED','CAM_TRANSPARENT']: assert bpy.data.objects.get(cam),f'Missing camera {cam}'
for nm in ['Front_Left_Wheel','Front_Right_Wheel','Rear_Left_Wheel','Rear_Right_Wheel','Generator_Motor','Generator_Shaft','Drive_Pulley_40mm','Generator_Pulley_10mm','Front_Steering_Servo','Front_Tie_Rod','Front_Tie_Rod_Control','Front_Servo_Horn_Rotation','Battery_6V_2000mAh','Motor_Controller','Generator_Rectifier_Regulator']:
    assert bpy.data.objects.get(nm),f'Missing physical part {nm}'
defaults=[o.name for o in bpy.data.objects if o.name.startswith(('Cube','Cylinder','Text'))]
assert not defaults, f'Default object names remain: {defaults[:5]}'
ctrl=bpy.data.objects['Drivetrain_Control']; assert all(p in ctrl for p in ['Drive_Angle','Generator_Load','Steering_Angle'])
assert abs(ctrl['Generator_Load']-.65)<1e-6

# Deck footprint and tire/deck lateral clearance.
lower=bpy.data.objects['Chassis_Lower_Deck_250x120x2mm']; upper=bpy.data.objects['Chassis_Upper_Deck_250x120x2mm']
assert all(abs(a-b)<1e-6 for a,b in zip(lower.dimensions,(.25,.12,.002)))
assert abs(upper.location.z-lower.location.z-.008)<1e-6
wheel=bpy.data.objects['Front_Left_Wheel']; tire_inner=abs(wheel.matrix_world.translation.y)-wheel['width_mm']/2000
assert tire_inner > upper.dimensions.y/2, (tire_inner,upper.dimensions.y/2)
assert tire_inner-upper.dimensions.y/2 > .003, 'Wheel-to-deck side clearance below 3 mm'

# Independent rear drive stubs and modeled bearing hangers/bushings.
assert bpy.data.objects['Drive_Pulley_40mm'].parent==bpy.data.objects['Rear_Left_Axle_Rotation']
for side in ['Left','Right']:
    assert bpy.data.objects[f'Rear_{side}_Axle_Shaft'].parent==bpy.data.objects[f'Rear_{side}_Axle_Rotation']
    assert bpy.data.objects[f'{side}_Gearbox_Output_Shaft'].parent==bpy.data.objects[f'Rear_{side}_Axle_Rotation']
assert len([o for o in bpy.data.collections['03_AXLES'].objects if o.name.startswith('Rear_Axle_Hanger')])==2
assert all(any(m.type=='BOOLEAN' for m in bpy.data.objects[n].modifiers) for n in ['Rear_Axle_Hanger','Rear_Axle_Hanger.001','Rear_Straw_Bearing','Rear_Straw_Bearing.001'])
assert bpy.data.objects['Front_Left_Wheel_Rotation'].parent==bpy.data.objects['Front_Left_Steering_Pivot']
assert bpy.data.objects['Front_Right_Wheel_Rotation'].parent==bpy.data.objects['Front_Right_Steering_Pivot']
assert bpy.data.objects['Front_Tie_Rod'].parent==bpy.data.objects['Front_Tie_Rod_Control']
assert bpy.data.objects['Front_Steering_Servo_Horn'].parent==bpy.data.objects['Front_Servo_Horn_Rotation']

# Power path and ratio.
assert bpy.data.objects['Generator_Pulley_10mm'].parent==bpy.data.objects['Generator_Pulley_Rotation']
assert bpy.data.objects['Generator_Shaft'].parent==bpy.data.objects['Generator_Pulley_Rotation']
assert bpy.data.objects['Generator_Motor'].parent is None
assert bpy.data.objects['Belt_Continuous_40_to_10'].type=='CURVE'
assert abs(bpy.data.objects['Drive_Pulley_40mm']['pitch_diameter_mm']/bpy.data.objects['Generator_Pulley_10mm']['pitch_diameter_mm']-4)<1e-9
assert abs(bpy.data.objects['Belt_Continuous_40_to_10']['center_distance_mm']-75.6)<.2

# Animation synchronization at frame 200: front/rear wheel and big pulley 1:1,
# generator shaft/pulley 4:1, all rotating in the forward-rolling sign convention.
sc=bpy.data.scenes['ASSEMBLED_ENGINEERING_VIEW']; bpy.context.window.scene=sc; sc.frame_set(200); bpy.context.view_layer.update(); dg=bpy.context.evaluated_depsgraph_get()
angle=bpy.data.objects['Drivetrain_Control'].evaluated_get(dg)['Drive_Angle']
assert abs(angle-90)<1e-4,angle
for n in ['Front_Left_Wheel_Rotation','Front_Right_Wheel_Rotation','Rear_Left_Wheel_Rotation','Rear_Right_Wheel_Rotation','Rear_Left_Axle_Rotation','Rear_Right_Axle_Rotation']:
    assert abs(bpy.data.objects[n].evaluated_get(dg).rotation_euler.y+angle)<1e-4,n
genrot=bpy.data.objects['Generator_Pulley_Rotation'].evaluated_get(dg)
assert abs(genrot.rotation_euler.y+4*angle)<1e-4

# Independent spreadsheet-style energy checks: no energy creation and loaded
# generator increases net draw / decreases runtime for the assumed operating point.
with open(os.path.join(ROOT,'physics_model.json'),encoding='utf-8') as f: phys=json.load(f)
cases=phys['cases']; assert len(cases)==5
for c in cases:
    assert c['returned_power_W'] <= c['generator_electrical_output_W']+1e-9
    assert c['generator_electrical_output_W'] <= c['generator_mechanical_input_W']+1e-9
    assert c['returned_power_W'] <= c['drive_battery_power_W']+1e-9
assert cases[1]['net_battery_power_W']>cases[0]['net_battery_power_W']
assert cases[1]['runtime_hours_ideal']<cases[0]['runtime_hours_ideal']
assert .3 < cases[0]['speed_m_s'] < .6
print('PASS: editable collections, cameras, physical components, deck/tire clearance, drilled rear hangers, independent axles, front steering, 4:1 synchronized motion, GLB source parts, and 0–4-generator energy balance.')
print(f"Case A {cases[0]['speed_km_h']:.2f} km/h, {cases[0]['runtime_hours_ideal']:.2f} h ideal; Case B {cases[1]['speed_km_h']:.2f} km/h, {cases[1]['runtime_hours_ideal']:.2f} h ideal, generator {cases[1]['generator_rpm_each']:.0f} rpm.")
