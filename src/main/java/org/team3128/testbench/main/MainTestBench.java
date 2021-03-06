package org.team3128.testbench.main;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import org.team3128.common.NarwhalRobot;
import org.team3128.common.drive.SRXTankDrive;
import org.team3128.common.listener.ListenerManager;
import org.team3128.common.listener.controllers.ControllerExtreme3D;
import org.team3128.common.util.units.Length;
import org.team3128.common.util.limelight.CalculatedData;
import org.team3128.common.util.limelight.Limelight;
import org.team3128.common.util.limelight.LimelightData;
import org.team3128.testbench.autonomous.*;

import edu.wpi.first.wpilibj.Joystick;

import org.team3128.common.NarwhalRobot;
import org.team3128.common.drive.SRXTankDrive;
import org.team3128.common.hardware.misc.Piston;
import org.team3128.common.hardware.misc.TwoSpeedGearshift;
import org.team3128.common.listener.ListenerManager;
import org.team3128.common.listener.POVValue;
import org.team3128.common.listener.controllers.ControllerExtreme3D;
import org.team3128.common.listener.controltypes.Button;
import org.team3128.common.listener.controltypes.POV;
import org.team3128.common.narwhaldashboard.NarwhalDashboard;
import org.team3128.common.util.Constants;
import org.team3128.common.util.Log;
import org.team3128.common.util.enums.Direction;
import org.team3128.common.util.units.Angle;
import org.team3128.common.util.units.Length;

import java.io.IOException;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.command.CommandGroup;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainTestBench extends NarwhalRobot {

    public TalonSRX boi1, boi2;
    public ListenerManager listenerLeft, listenerRight;
    public Joystick leftJoystick, rightJoystick;
    public double wheelCirc;
    public int lowGearMaxSpeed;
    public SRXTankDrive drive;
    public NetworkTable table;


    public File f;
    public File ftemp;
    BufferedWriter bw;
    BufferedReader br;
    FileWriter fw;
    FileReader fr;
    FileWriter fwtemp;
    FileReader frtemp;
    BufferedReader brtemp;
    BufferedWriter bwtemp;
    String currentLine;
    String newLine;

    private int errorCase = 0;
    private int counter = 0;

    public double camAngle = 24.5;
    public double camHeight = 10.25;
    public double hatchHeight = 28.5;
    public double w = 14.5;

    public int ledToggle = 0;

    Limelight limelight;

    public double[] limelightVals;
    public double[] calculatedVals;

	@Override
	protected void constructHardware()
	{
        newLine = "";
        limelight = new Limelight(camAngle, camHeight, hatchHeight, w, true);
        newLine = "tx,ty,ts,ta,thoriz,tvert,tshort,tlong,deltax,deltay,theta,calculatedDist,theta0,theta1,d0,d1\n";
        NarwhalDashboard.put("counter", String.valueOf(counter));
        boi1 = new TalonSRX(1);
        boi2 = new TalonSRX(2);

        SRXTankDrive.initialize(boi1, boi2, wheelCirc, 1, 25.25 * Length.in, lowGearMaxSpeed);
        drive = SRXTankDrive.getInstance();
        
        leftJoystick = new Joystick(0);
        rightJoystick = new Joystick(1);

        listenerLeft = new ListenerManager(leftJoystick);
        listenerRight = new ListenerManager(rightJoystick);

        addListenerManager(listenerLeft);
        addListenerManager(listenerRight);

    }

    @Override
    protected void setupListeners() {

        listenerRight.nameControl(ControllerExtreme3D.JOYY, "MoveForwards");
		listenerRight.nameControl(ControllerExtreme3D.TWIST, "MoveTurn");
		listenerRight.nameControl(ControllerExtreme3D.THROTTLE, "Throttle");

		listenerRight.addMultiListener(() ->
		{
			double x = listenerRight.getAxis("MoveForwards");
			double y = listenerRight.getAxis("MoveTurn");
			double t = listenerRight.getAxis("Throttle") * -1;
			drive.arcadeDrive(x, y, t, true);
        }, "MoveForwards", "MoveTurn", "Throttle");
        
        listenerRight.nameControl(new Button(2), "LightOn");
		listenerRight.addButtonDownListener("LightOn", () -> {
            if(ledToggle == 0){
                limelight.ledOn();
                ledToggle = 1;
            } else {
                limelight.ledOff();
                ledToggle = 0;
            }
        });
        /*listenerRight.nameControl(new Button(2), "LightOff");
		listenerRight.addButtonUpListener("LightOff", () -> {
		    table.getEntry("ledMode").setNumber(1);
        });*/
        listenerRight.nameControl(new Button(12), "ResetCounter");
        listenerRight.addButtonDownListener("ResetCounter", () -> {
            System.out.print(newLine);
            newLine = "";
        });

		listenerRight.nameControl(ControllerExtreme3D.TRIGGER, "limelightVals");
		listenerRight.addButtonDownListener("limelightVals", () -> {
            limelight.ledOn();

            LimelightData data = limelight.getValues(1000);
            CalculatedData calcData = limelight.doMath(data);

            newLine += data.tx() + ",";
            newLine += data.ty() + ",";
            newLine += data.shear() + ",";
            newLine += data.area() + ",";
            newLine += data.boxWidth() + ",";
            newLine += data.boxHeight() + ",";
            newLine += data.fittedShort() + ",";
            newLine += data.fittedLong() + ",";

            newLine += calcData.dX + ",";
            newLine += calcData.dY + ",";
            newLine += calcData.theta + ",";
            newLine += calcData.d + ",";
            
            newLine += calcData.theta0 + ",";
            newLine += calcData.theta1 + ",";
            newLine += calcData.d0 + ",";
            newLine += calcData.d1 + ",";

            Log.info("MainTestBench", "Datums recorded.");
            
            counter++;
            newLine += "\n";
        });
        
        listenerRight.nameControl(new Button(9), "deleteLastLine");
        listenerRight.addButtonDownListener("deleteLastLine", () -> {
        });
        listenerRight.nameControl(new Button(10), "closeFile");
        listenerRight.addButtonDownListener("closeFile", () -> {
            try {
                bw.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        });
        listenerRight.nameControl(new Button(5), "pipeline_0");
        listenerRight.addButtonDownListener("pipeline_0", () -> {
            table.getEntry("pipeline").setDouble(0.0);
        });
        listenerRight.nameControl(new Button(3), "pipeline_1");
        listenerRight.addButtonDownListener("pipeline_1", () -> {
            table.getEntry("pipeline").setDouble(1.0);
        });
        listenerRight.nameControl(new Button(4), "pipeline_2");
        listenerRight.addButtonDownListener("pipeline_2", () -> {
            table.getEntry("pipeline").setDouble(2.0);
        });
        listenerRight.nameControl(new Button(7), "CamMode");
        listenerRight.addButtonDownListener("CamMode", () -> {
            table.getEntry("camMode").setNumber(0);
  
        });

        listenerRight.nameControl(new Button(8), "DriveMode");
        listenerRight.addButtonDownListener("DriveMode", () -> {
            table.getEntry("camMode").setNumber(1);
  
        });
    }

    @Override
    protected void constructAutoPrograms() {
        NarwhalDashboard.addAuto("Test", new TestBenchTest(drive));
    }

    @Override
    protected void teleopInit() {

    }

    @Override
    protected void teleopPeriodic() {


    }

    @Override
    protected void autonomousInit() {
    }

    @Override
    protected void disabledInit() {
        limelight.ledOff();
    }

    @Override
    protected void updateDashboard() {
        NarwhalDashboard.put("ts", String.valueOf(table.getEntry("ts").getDouble(0.0)));

    }

    public static void main(String[] args) {
        RobotBase.startRobot(MainTestBench::new);
    }
}