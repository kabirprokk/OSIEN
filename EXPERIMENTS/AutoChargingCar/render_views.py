import bpy, os
out=os.path.dirname(os.path.abspath(__file__))
assembled=bpy.data.scenes['ASSEMBLED_ENGINEERING_VIEW']
exploded=bpy.data.scenes['EXPLODED_VIEW']
transparent=bpy.data.scenes['TRANSPARENT_DRIVETRAIN_VIEW']
jobs=[(assembled,'CAM_PERSPECTIVE','01_assembled'),(assembled,'CAM_TOP','02_top'),(assembled,'CAM_SIDE','03_side'),(assembled,'CAM_BOTTOM','04_bottom'),(assembled,'CAM_BELT_CLOSEUP','05_belt_closeup'),(exploded,'CAM_EXPLODED','06_exploded'),(transparent,'CAM_TRANSPARENT','07_transparent_drivetrain')]
ground=bpy.data.objects.get('Ground_Reference'); old=ground.hide_render if ground else False
for sc,camname,filename in jobs:
    bpy.context.window.scene=sc
    sc.camera=bpy.data.objects[camname]
    sc.render.resolution_x=1200; sc.render.resolution_y=900; sc.render.resolution_percentage=100
    if ground: ground.hide_render=(camname=='CAM_BOTTOM')
    if filename in ('05_belt_closeup','07_transparent_drivetrain'):
        old_rear={}
        for ob in sc.objects:
            if ob.name in bpy.data.collections['09_WIRING'].objects:
                old_rear[ob]=ob.hide_render; ob.hide_render=True
            if ob.name.startswith(('Rear_Left_','EXPLODED_Rear_Left_','TRANSPARENT_Rear_Left_')) and not ob.name.endswith('Rotation'):
                old_rear[ob]=ob.hide_render; ob.hide_render=True
            if ob.name.startswith('BELT_CLOSEUP_Translucent_Rear_Left_Tire'):
                old_rear[ob]=ob.hide_render; ob.hide_render=True
            if ob.name.startswith('Battery_Positive_to_Controller') or ob.name.startswith('Battery_Negative_to_Controller') or ob.name.startswith('Controller_to_') or ob.name.startswith('Generator_to_') or ob.name.startswith('Charge_Return_to_'):
                old_rear[ob]=ob.hide_render; ob.hide_render=True
    sc.render.filepath=os.path.join(out,filename+'.png')
    bpy.ops.render.render(write_still=True,scene=sc.name)
    if filename in ('05_belt_closeup','07_transparent_drivetrain'):
        for ob,value in old_rear.items(): ob.hide_render=value
if ground: ground.hide_render=old
bpy.context.window.scene=assembled
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(out,'AutoChargingCar_EngineeringPrototype.blend'))
