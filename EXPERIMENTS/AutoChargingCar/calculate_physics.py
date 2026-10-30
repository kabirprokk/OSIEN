"""Transparent steady-state estimate using a published 6 V geared motor curve."""
import json, math, os
OUT=os.path.dirname(os.path.abspath(__file__))
V=4.8
CAPACITY_AH=2.0
USABLE_FRACTION=0.80
ENERGY_WH=V*CAPACITY_AH*USABLE_FRACTION
MASS=0.35
R_WHEEL=0.032
TRACK=0.146
CRR=0.03
BEARING_DRAG_N=0.15
ETA_DRIVELINE=0.90
ETA_BELT=0.90
ETA_GENERATOR=0.55
ETA_CHARGER=0.0 # version 1 sends generator output to a test load, never to the battery
CONTROLLER_EFF=0.90
ELECTRONICS_IDLE_W=0.150 # assumed 5 V Nano + OLED + two sensors and regulator
NOLOAD_RPM=200.0 # 250 rpm at 6 V, linearly scaled to 4.8 V
NOLOAD_A=0.060 # approximate 6 V no-load current scaled to 4.8 V
STALL_TORQUE=0.050*(V/6.0) # representative 0.5 kgf-cm 6 V N20 proxy, voltage-scaled
STALL_A=0.800*(V/6.0) # representative 0.8 A 6 V stall proxy, voltage-scaled
GEN_MECH_W_EACH=0.030 # reference mechanical power at 300 generator rpm
GEN_NOMINAL_RPM=300.0
GEN_TORQUE=GEN_MECH_W_EACH/(GEN_NOMINAL_RPM*2*math.pi/60)
GEN_AXLE_TORQUE=GEN_TORQUE*4/ETA_BELT
G=9.80665
F_ROAD=CRR*MASS*G+BEARING_DRAG_N
TAU_ROAD=F_ROAD*R_WHEEL/ETA_DRIVELINE
OMEGA0=NOLOAD_RPM*2*math.pi/60
I_SLOPE=STALL_A-NOLOAD_A

def solve(count):
    # Generator loads the left rear axle only. Its resisting torque is rated at
    # 300 gen rpm and remains finite at standstill; right/left speeds differ slightly.
    left_tau=TAU_ROAD/2+count*GEN_AXLE_TORQUE
    right_tau=TAU_ROAD/2
    w_l=max(0.0,OMEGA0*(1-left_tau/STALL_TORQUE))
    w_r=max(0.0,OMEGA0*(1-right_tau/STALL_TORQUE))
    w=(w_l+w_r)/2
    def motor_current(tau): return NOLOAD_A+I_SLOPE*min(1,max(0,tau/STALL_TORQUE))
    i_l=motor_current(left_tau); i_r=motor_current(right_tau)
    motor_electrical=V*(i_l+i_r)/CONTROLLER_EFF+ELECTRONICS_IDLE_W
    gen_mech=count*GEN_TORQUE*(4*w_l)
    gen_electrical=gen_mech*ETA_GENERATOR
    returned=min(max(0,motor_electrical),gen_electrical*ETA_CHARGER)
    net=max(0,motor_electrical-returned)
    rpm=w*60/(2*math.pi)
    left_rpm=w_l*60/(2*math.pi)
    speed=w*R_WHEEL
    gen_rpm=left_rpm*4
    belt_speed=(0.020*w_l)
    return {
      'generators':count,'speed_m_s':speed,'speed_km_h':speed*3.6,'wheel_rpm':rpm,
      'generator_rpm_each':gen_rpm,'belt_speed_m_s':belt_speed,
      'left_wheel_rpm':left_rpm,'right_wheel_rpm':w_r*60/(2*math.pi),
      'wheel_torque_total_Nm':left_tau+right_tau,'motor_torque_each_Nm':(left_tau+right_tau)/2,
      'motor_torque_left_Nm':left_tau,'motor_torque_right_Nm':right_tau,
      'motor_current_left_A':i_l,'motor_current_right_A':i_r,'motor_current_each_A':(i_l+i_r)/2,'drive_battery_power_W':motor_electrical,
      'drive_motor_electrical_input_W':V*(i_l+i_r),'mechanical_drive_output_W':left_tau*w_l+right_tau*w_r,
      'driveline_loss_W':max(0,(left_tau*w_l+right_tau*w_r)-F_ROAD*speed-gen_mech/ETA_BELT),
      'generator_mechanical_input_W':gen_mech,'belt_loss_W':gen_mech*(1/ETA_BELT-1),
      'generator_electrical_output_W':gen_electrical,'test_load_dissipation_W':gen_electrical,'charger_loss_W':0.0,
      'returned_power_W':returned,'net_battery_power_W':net,
      'runtime_hours_ideal':ENERGY_WH/net if net>0 else None,
      'road_force_N':F_ROAD,'road_mechanical_power_W':F_ROAD*speed,
      'generator_torque_each_Nm':GEN_TORQUE if count else 0,
    }

cases=[solve(n) for n in range(5)]
params={
 'battery_voltage_V':V,'battery_capacity_Ah':CAPACITY_AH,'battery_nominal_Wh':V*CAPACITY_AH,
 'usable_fraction':USABLE_FRACTION,'usable_energy_Wh':ENERGY_WH,'mass_kg':MASS,
 'wheel_radius_m':R_WHEEL,'track_m':TRACK,'rolling_resistance_coefficient':CRR,
 'bearing_drag_N':BEARING_DRAG_N,'driveline_efficiency':ETA_DRIVELINE,'belt_efficiency':ETA_BELT,
 'generator_efficiency':ETA_GENERATOR,'charger_efficiency':ETA_CHARGER,
 'controller_efficiency':CONTROLLER_EFF,'electronics_idle_W':ELECTRONICS_IDLE_W,
 'motor_model':'N20 6 V 250 rpm nominal proxy; speed/current/torque voltage-scaled to 4.8 V (vendor SKU unconfirmed)',
 'motor_rated_voltage_V':6.0,'motor_rated_no_load_rpm':250.0,
 'motor_no_load_rpm':NOLOAD_RPM,'motor_no_load_A':NOLOAD_A,'motor_stall_torque_Nm':STALL_TORQUE,
 'motor_stall_A':STALL_A,'generator_mechanical_W_each':GEN_MECH_W_EACH,
 'pulley_ratio':4.0,'generator_nominal_rpm':GEN_NOMINAL_RPM,'generator_torque_Nm':GEN_TORQUE,
 'generator_output_mode':'test_load_only','battery_charge_enabled':False,
 'prototype_electronics':{
   'battery':'4 x AA NiMH, 1.2 V nominal each, 4.8 V pack, assumed 2.0 Ah',
   'drive_motors':'2 x N20 brushed geared DC, 6 V, 200-300 rpm rated; proxy curve assumes 250 rpm at 6 V',
   'motor_driver':'TB6612FNG dual H-bridge',
   'protection':'Master switch rated at least 3 A DC; initial 2 A time-delay fuse is provisional and must be checked against measured startup/stall current and wire rating',
   'generator':'1 x 3-6 V brushed DC motor; belt connected, initially unloaded then tested into resistor/load',
   'generator_measurement':'INA219 voltage/current sensor on generator side; test load only, no battery charging in version 1',
   'controller':'5 V Arduino Nano-compatible board, two INA219 sensors, 0.96 inch I2C OLED',
   'noise_suppression':'100 nF ceramic capacitors at motors; 470-1000 uF supply capacitor, >=10 V',
   'wiring':'22 AWG stranded wire for motor power; breadboard/jumper wires for signals only'
 },
 'cases':cases,
 'equations':'P=V*I; P_mech=tau*omega; N20 proxy speed/current and torque linearly interpolated between voltage-scaled no-load and stall endpoints; generator electrical output is dissipated/measured at a test load; battery return is zero.'
}
with open(os.path.join(OUT,'physics_model.json'),'w',encoding='utf-8') as f: json.dump(params,f,indent=2)
base,one=cases[0],cases[1]
lines=['# Auto Charging Car - V1 engineering assumptions and estimates','',
 'This is a planning estimate for the user-specified low-voltage DIY setup. The exact N20 gearmotor, generator SKU, battery capacity, finished mass, and friction have not been measured. Speed and power are therefore estimates, not validated real-world performance. The game does not charge its battery from the generator in this version; generator power goes to a test load.', '',
 '## V1 component specification','',
 '- Battery: 4 x 1.2 V AA NiMH cells, 4.8 V nominal, assumed 2.0 Ah (9.6 Wh nominal). An 80% usable fraction gives 7.68 Wh. Use matched cells and an appropriate NiMH charger.',
 '- Drive: 2 x 6 V N20 geared DC motors, nominal 200-300 rpm. The estimate uses a representative 250 rpm motor, voltage-scaled to 200 rpm at 4.8 V. Torque and current are also voltage-scaled from unverified representative values: 0.5 kgf-cm and 0.8 A stall at 6 V. N20 variants differ substantially; replace this proxy with the purchased motor datasheet.',
 '- Motor controller: TB6612FNG dual H-bridge. A 4.8 V pack can supply substantial startup current; use a master switch rated at least 3 A DC and a provisional 2 A time-delay fuse only after checking measured startup/stall current and wire rating; do not route motor current through a breadboard.',
 '- Generator: one 3-6 V brushed motor. The belt remains attached; begin with its circuit open, then measure unloaded voltage, then connect a small test load. No generator output is connected back to the AA pack in V1.',
 '- Measurements: two INA219 modules for battery-side and generator-side V/I, with a 5 V Nano-compatible controller and 0.96 inch I2C OLED. The model assumes 0.15 W combined controller/display/sensor/regulator overhead; this must be measured.',
 '- Wiring and suppression: 22 AWG stranded motor-power wiring, signal jumpers only on a breadboard, 100 nF ceramic motor capacitors, and 470-1000 uF at 10 V or higher across the supply.',
 '- Mechanical assumptions retained: 0.35 kg all-up mass, 64 mm wheel diameter, 0.03 rolling coefficient, 0.15 N equivalent bearing drag, 90% drivetrain efficiency and 90% belt efficiency. These are unverified estimates.',
 '- Generator load proxy: constant 0.000955 N-m opposing torque per generator, corresponding to 30 mW mechanical input at 300 rpm before efficiency losses. It is only a placeholder for a measured test-load curve.', '',
 '## Steady speed and power estimate','',
 '| Generators under test load | Speed | Wheel rpm | Generator rpm | Drive torque L/R | Battery input | Generator electrical output | Battery return | Net battery drain | Ideal runtime* |',
 '|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|']
for c in cases:
    lines.append(f"| {c['generators']} | {c['speed_km_h']:.2f} km/h | {c['wheel_rpm']:.0f} | {c['generator_rpm_each']:.0f} | {c['motor_torque_left_Nm']*1000:.2f} / {c['motor_torque_right_Nm']*1000:.2f} mN-m | {c['drive_battery_power_W']:.3f} W | {c['generator_electrical_output_W']:.3f} W | 0 W | {c['net_battery_power_W']:.3f} W | {c['runtime_hours_ideal']:.1f} h |")
lines += ['', '*Uses 7.68 Wh at constant estimated power. Actual runtime depends on AA cell chemistry/capacity, voltage sag, starts, surface, steering, temperature and motor duty cycle.', '',
 '### Case A - generator unloaded/disconnected electrically','',
 f"At full throttle on a level smooth floor, the estimate is **{base['speed_km_h']:.2f} km/h** ({base['wheel_rpm']:.0f} wheel rpm), **{base['drive_battery_power_W']:.2f} W** battery input, and **{base['runtime_hours_ideal']:.1f} hours idealized**. This does not include measured cell voltage sag or starting cycles.", '',
 '### Case B - generator connected to a test load','',
 f"At the assumed full test load, the estimate is **{one['speed_km_h']:.2f} km/h**, **{one['generator_rpm_each']:.0f} generator rpm**, **{one['drive_battery_power_W']:.2f} W** battery input, and **{one['generator_electrical_output_W']*1000:.1f} mW** at the generator terminals. The output is dissipated in the test load: **0 W returns to the battery**. Drive current rises and speed falls because generator torque is reflected through the 4:1 pulley and belt.", '',
 'These values are model outputs from generic motor assumptions. A 3-6 V high-speed motor may need far more than 700-800 rpm to produce useful voltage under load, so measure open-circuit voltage and loaded voltage/current before selecting the test resistor or converter.', '',
 '## What to measure on the real car','',
 '1. Measure battery pack voltage and current during startup and steady driving; compute `P_battery = V x I`.',
 '2. With the belt connected and generator electrically open, log generator RPM and open-circuit voltage.',
 '3. Add a resistor load, then measure generator terminal voltage/current and compute `P_generator = V x I`.',
 '4. Compare the loaded run with the unloaded run. Do not connect raw generator output directly to the NiMH battery; V1 intentionally uses a test load only.',
 '5. Enter the exact motor vendor data and measured mass/friction/load curve into `physics_model.json` before treating simulated speed or runtime as real.', '',
 '## Mechanical notes and conclusion','',
 '- The 40:10 pulleys give a 4:1 speed increase and an ideal 1:4 torque reduction; they do not multiply power. The model geometry animates the belt kinematically and does not solve tooth contact, belt tension, or mount stress.',
 '- A 200 rpm wheel speed with 64 mm diameter corresponds to about 2.4 km/h at no load. Loaded speed should be lower; this estimate is about the speed scale, not a promise of an exact 3 km/h real run.',
 '- The drive motors should move a 0.35 kg car on a smooth floor if the purchased motors meet the assumed output torque and the cells can supply startup current. Confirm with an ammeter and a floor test.',
 '- Generator output should be measurable if the selected motor produces adequate voltage at the achieved RPM, but the 4:1 ratio only brings the shaft to roughly 500-800 rpm in these cases. A high-RPM motor may produce only a small loaded output.',
 '- The 1-4 generator cases assume identical generator torque per unit and do not establish that 2-4 units physically fit. Start with one generator as specified.',
 '- Do not expect continuous generation to extend runtime. With a test load, the generator output is dissipated and its added mechanical resistance makes the battery drain faster. Regenerative braking or downhill travel can recover external vehicle energy; steady self-charging cannot.' ,'']
with open(os.path.join(OUT,'ENGINEERING_REPORT.md'),'w',encoding='utf-8') as f:f.write('\n'.join(lines))
print(json.dumps(cases,indent=2))
