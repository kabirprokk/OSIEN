import bpy
for s in bpy.data.scenes:
    print('SCENE',s.name)
    def rec(lc,d=0):
        print(' '*d,lc.collection.name,'excluded=',lc.exclude,'objs=',len(lc.collection.objects))
        for x in lc.children: rec(x,d+1)
    rec(s.view_layers[0].layer_collection)
