# Auto Charging Car - V1 engineering assumptions and estimates

This is a planning estimate for the user-specified low-voltage DIY setup. The exact N20 gearmotor, generator SKU, battery capacity, finished mass, and friction have not been measured. Speed and power are therefore estimates, not validated real-world performance. The game does not charge its battery from the generator in this version; generator power goes to a test load.

## V1 component specification

- Battery: 4 x 1.2 V AA NiMH cells, 4.8 V nominal, assumed 2.0 Ah (9.6 Wh nominal). An 80% usable fraction gives 7.68 Wh. Use matched cells and an appropriate NiMH charger.
- Drive: 2 x 6 V N20 geared DC motors, nominal 200-300 rpm. The estimate uses a representative 250 rpm motor, voltage-scaled to 200 rpm at 4.8 V. Torque and current are also voltage-scaled from unverified representative values: 0.5 kgf-cm and 0.8 A stall at 6 V. N20 variants differ substantially; replace this proxy with the purchased motor datasheet.
- Motor controller: TB6612FNG dual H-bridge. A 4.8 V pack can supply substantial startup current; use a master switch rated at least 3 A DC and a provisional 2 A time-delay fuse only after checking measured startup/stall current and wire rating; do not route motor current through a breadboard.
- Generator: one 3-6 V brushed motor. The belt remains attached; begin with its circuit open, then measure unloaded voltage, then connect a small test load. No generator output is connected back to the AA pack in V1.
- Measurements: two INA219 modules for battery-side and generator-side V/I, with a 5 V Nano-compatible controller and 0.96 inch I2C OLED. The model assumes 0.15 W combined controller/display/sensor/regulator overhead; this must be measured.
- Wiring and suppression: 22 AWG stranded motor-power wiring, signal jumpers only on a breadboard, 100 nF ceramic motor capacitors, and 470-1000 uF at 10 V or higher across the supply.
- Mechanical assumptions retained: 0.35 kg all-up mass, 64 mm wheel diameter, 0.03 rolling coefficient, 0.15 N equivalent bearing drag, 90% drivetrain efficiency and 90% belt efficiency. These are unverified estimates.
- Generator load proxy: constant 0.000955 N-m opposing torque per generator, corresponding to 30 mW mechanical input at 300 rpm before efficiency losses. It is only a placeholder for a measured test-load curve.

## Steady speed and power estimate

| Generators under test load | Speed | Wheel rpm | Generator rpm | Drive torque L/R | Battery input | Generator electrical output | Battery return | Net battery drain | Ideal runtime* |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 0 | 2.14 km/h | 178 | 710 | 4.50 / 4.50 mN-m | 1.486 W | 0.000 W | 0 W | 1.486 W | 5.2 h |
| 1 | 2.01 km/h | 167 | 625 | 8.74 / 4.50 mN-m | 1.814 W | 0.034 W | 0 W | 1.814 W | 4.2 h |
| 2 | 1.89 km/h | 156 | 540 | 12.99 / 4.50 mN-m | 2.142 W | 0.059 W | 0 W | 2.142 W | 3.6 h |
| 3 | 1.76 km/h | 146 | 455 | 17.23 / 4.50 mN-m | 2.470 W | 0.075 W | 0 W | 2.470 W | 3.1 h |
| 4 | 1.63 km/h | 135 | 371 | 21.47 / 4.50 mN-m | 2.798 W | 0.082 W | 0 W | 2.798 W | 2.7 h |

*Uses 7.68 Wh at constant estimated power. Actual runtime depends on AA cell chemistry/capacity, voltage sag, starts, surface, steering, temperature and motor duty cycle.

### Case A - generator unloaded/disconnected electrically

At full throttle on a level smooth floor, the estimate is **2.14 km/h** (178 wheel rpm), **1.49 W** battery input, and **5.2 hours idealized**. This does not include measured cell voltage sag or starting cycles.

### Case B - generator connected to a test load

At the assumed full test load, the estimate is **2.01 km/h**, **625 generator rpm**, **1.81 W** battery input, and **34.4 mW** at the generator terminals. The output is dissipated in the test load: **0 W returns to the battery**. Drive current rises and speed falls because generator torque is reflected through the 4:1 pulley and belt.

These values are model outputs from generic motor assumptions. A 3-6 V high-speed motor may need far more than 700-800 rpm to produce useful voltage under load, so measure open-circuit voltage and loaded voltage/current before selecting the test resistor or converter.

## What to measure on the real car

1. Measure battery pack voltage and current during startup and steady driving; compute `P_battery = V x I`.
2. With the belt connected and generator electrically open, log generator RPM and open-circuit voltage.
3. Add a resistor load, then measure generator terminal voltage/current and compute `P_generator = V x I`.
4. Compare the loaded run with the unloaded run. Do not connect raw generator output directly to the NiMH battery; V1 intentionally uses a test load only.
5. Enter the exact motor vendor data and measured mass/friction/load curve into `physics_model.json` before treating simulated speed or runtime as real.

## Mechanical notes and conclusion

- The 40:10 pulleys give a 4:1 speed increase and an ideal 1:4 torque reduction; they do not multiply power. The model geometry animates the belt kinematically and does not solve tooth contact, belt tension, or mount stress.
- A 200 rpm wheel speed with 64 mm diameter corresponds to about 2.4 km/h at no load. Loaded speed should be lower; this estimate is about the speed scale, not a promise of an exact 3 km/h real run.
- The drive motors should move a 0.35 kg car on a smooth floor if the purchased motors meet the assumed output torque and the cells can supply startup current. Confirm with an ammeter and a floor test.
- Generator output should be measurable if the selected motor produces adequate voltage at the achieved RPM, but the 4:1 ratio only brings the shaft to roughly 500-800 rpm in these cases. A high-RPM motor may produce only a small loaded output.
- The 1-4 generator cases assume identical generator torque per unit and do not establish that 2-4 units physically fit. Start with one generator as specified.
- Do not expect continuous generation to extend runtime. With a test load, the generator output is dissipated and its added mechanical resistance makes the battery drain faster. Regenerative braking or downhill travel can recover external vehicle energy; steady self-charging cannot.
