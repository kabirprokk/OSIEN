# Auto Charging Car — local 3D driving game

The game loads `public/auto_charging_car.glb`, exported from the assembled Blender scene. The car geometry, belt loop, wire routing, split rear axles, steering pivots, motor parts, and electronics come from that file. Wheel/axle/generator rotations are driven at runtime by the same 40:10 ratio and by the physics model in `public/physics_model.json`.

## Run it

1. Install Node.js 20.19+ (or 22.12+) once.
2. In this folder run `npm install`.
3. Run `npm run dev` and open the localhost URL it prints (normally `http://127.0.0.1:5173`).

The repository root also has `Launch_Auto_Charging_Car_Game.bat` for Windows.

## Controls

- W / Up: drive; S / Down: reverse; A/D or Left/Right: steer; Space: friction brake.
- G or the switch: connect/disconnect the generator electrical load. The belt and generator shaft still spin while mechanically connected to the moving left rear axle.
- Generator load slider: adjust resisting torque. Generator count slider: compare the 1–4 equivalent load cases; the GLB intentionally contains the requested one physical generator.
- Motor speed balancing compensates the generator load pulling on the left rear axle, modeling the encoder feedback needed to avoid an unwanted arc.
- Road grade slider: positive is downhill. Gravity can power the generator; the model does not treat a level road as an external energy source.
- Mouse drag / wheel: orbit and zoom. The 10 s demo follows the Blender scene timing (stationary, accelerate, drive, coast).

## Physics scope

The game integrates two independent motor torque curves, road and bearing drag, generator torque reflected through belt efficiency, differential wheel speed, grade force, charge conversion loss, and battery energy. It estimates a flat-road full-throttle cruise speed and runtime. It is not a tire finite-element model or a substitute for measured motor, controller, battery, belt, and chassis data. The exact motor SKU and battery discharge rating are unknown. Assumptions, formulas, buildability findings, and manufacturer links are in `../ENGINEERING_REPORT.md`.
