package org.firstinspires.ftc.teamcode.auton;

import android.annotation.SuppressLint;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.drive.SampleTankDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.openftc.apriltag.AprilTagDetection;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

import java.util.ArrayList;


// Credit: OpenFTC for a lot
@Config
@Autonomous(name="1+5 LEFT SIDE TANK")
public class AprilTagLeftTank extends LinearOpMode
{
    public static double encoderMultiplier = 30.71283;
    public static double low = 1 * encoderMultiplier;
    public static double medium = 11 * encoderMultiplier;
    public static double high = 21 * encoderMultiplier;
    public static double stack1 = 4 * encoderMultiplier;
    public static double stack2 = 3 * encoderMultiplier;
    public static double stack3 = 2 * encoderMultiplier;
    public static double stack4 = 1 * encoderMultiplier;
    public static double stack5 = 0 * encoderMultiplier;
    public static double stackLift = 5 * encoderMultiplier;
    public static double stackTime1 = 0.25;
    public static double stackTime2 = 0.25;
    public static double stackTime3 = 0.25;
    public static double scoreTime1 = 0.25;
    public static double scoreTime2 = 0.25;
    public static double open = 0.8;
    public static double closed = 1.0;
    public static double intake = 0;
    public static double deposit = 250;
    public static double extended = 90;
    public static double retracted = 0;

    DcMotorEx liftMotorLeft;
    DcMotorEx liftMotorRight;

    FtcDashboard dashboard = FtcDashboard.getInstance();
    Telemetry dashboardTelemetry = dashboard.getTelemetry();

    OpenCvCamera camera;
    AprilTagDetectionPipeline aprilTagDetectionPipeline;
    PoleObserverPipeline poleObserverPipeline;

    static final double FEET_PER_METER = 3.28084;

    // Lens intrinsics
    // UNITS ARE PIXELS
    // NOTE: this calibration is for the C920 webcam at 800x448.
    // You will need to do your own calibration for other configurations!
    double fx = 578.272;
    double fy = 578.272;
    double cx = 402.145;
    double cy = 221.506;

    // UNITS ARE METERS
    double tagsize = 0.166;

    // Tag ID 1,2,3 from the 36h11 family
    int LEFT = 1;
    int MIDDLE = 2;
    int RIGHT = 3;

    AprilTagDetection tagOfInterest = null;

    @Override
    public void runOpMode()
    {
        telemetry.setAutoClear(true);
        TelemetryPacket packet = new TelemetryPacket();
        FtcDashboard dashboard = FtcDashboard.getInstance();

        SampleTankDrive drive = new SampleTankDrive(hardwareMap);
        Pose2d startPose = new Pose2d(-36, -63, Math.toRadians(90)); // Set start pose to center of the field, facing north
        drive.setPoseEstimate(startPose);

        // Servo
        Servo gripServo = hardwareMap.servo.get("manipulator");
        Servo leftV4B = hardwareMap.servo.get("leftV4B");
        Servo rightV4B = hardwareMap.servo.get("rightV4B");
        Servo leftGuide = hardwareMap.servo.get("leftGuide");
        Servo rightGuide = hardwareMap.servo.get("rightGuide");

        leftV4B.scaleRange(0, 300);
        rightV4B.scaleRange(0, 300);
        leftGuide.scaleRange(0, 300);
        rightGuide.scaleRange(0, 300);

        rightV4B.setDirection(Servo.Direction.REVERSE);
        rightGuide.setDirection(Servo.Direction.REVERSE);

        // Declare our motors
        // Make sure your ID's match your configuration
        liftMotorLeft = hardwareMap.get(DcMotorEx.class, "liftMotorLeft");
        liftMotorRight = hardwareMap.get(DcMotorEx.class, "liftMotorRight");

        liftMotorLeft.setTargetPosition(0);
        liftMotorRight.setTargetPosition(0);

        liftMotorLeft.setMode(DcMotorEx.RunMode.RUN_TO_POSITION);
        liftMotorRight.setMode(DcMotorEx.RunMode.RUN_TO_POSITION);

        liftMotorLeft.setDirection(DcMotorEx.Direction.REVERSE);

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        camera = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId);
        aprilTagDetectionPipeline = new AprilTagDetectionPipeline(tagsize, fx, fy, cx, cy);

        poleObserverPipeline = new PoleObserverPipeline(telemetry);

        camera.setPipeline(aprilTagDetectionPipeline);
        camera.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener()
        {
            @Override
            public void onOpened()
            {
                camera.startStreaming(1280,720, OpenCvCameraRotation.UPRIGHT);
                FtcDashboard.getInstance().startCameraStream(camera,5);
            }

            @Override
            public void onError(int errorCode)
            {

            }
        });

        telemetry.setMsTransmissionInterval(50);

        TrajectorySequence parkingOne = drive.trajectorySequenceBuilder(startPose)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score preload on low
                .splineTo(new Vector2d(-30, -54), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack1);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack1);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 1st cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-36, -36), Math.toRadians(90))
                .splineTo(new Vector2d(-36, -24), Math.toRadians(90))
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 1st cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack2);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack2);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 2nd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 2nd cone off stack on low
                .setReversed(false)
                .splineTo(new Vector2d(-54, -18), Math.toRadians(305))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack3);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack3);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 3rd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 3rd cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack4);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack4);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 4th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) medium);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) medium);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 4th cone off stack on medium
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack5);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack5);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 5th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 5th cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -6), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition(0);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition(0);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to park
                .setReversed(true)
                .splineTo(new Vector2d(-36, -24), Math.toRadians(270))

                // Parking spot 1
                .splineTo(new Vector2d(-48, -36), Math.toRadians(180))
                .splineTo(new Vector2d(-60, -36), Math.toRadians(270))
                .build();

        TrajectorySequence parkingTwo = drive.trajectorySequenceBuilder(startPose)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score preload on low
                .splineTo(new Vector2d(-30, -54), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack1);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack1);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 1st cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-36, -36), Math.toRadians(90))
                .splineTo(new Vector2d(-36, -24), Math.toRadians(90))
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 1st cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack2);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack2);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 2nd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 2nd cone off stack on low
                .setReversed(false)
                .splineTo(new Vector2d(-54, -18), Math.toRadians(305))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack3);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack3);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 3rd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 3rd cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack4);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack4);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 4th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) medium);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) medium);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 4th cone off stack on medium
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack5);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack5);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 5th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 5th cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -6), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition(0);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition(0);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to park
                .setReversed(true)
                .splineTo(new Vector2d(-36, -24), Math.toRadians(270))

                // Parking spot 2
                .splineTo(new Vector2d(-36, -36), Math.toRadians(270))
                .build();

        TrajectorySequence parkingThree = drive.trajectorySequenceBuilder(startPose)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score preload on low
                .splineTo(new Vector2d(-30, -54), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack1);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack1);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 1st cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-36, -36), Math.toRadians(90))
                .splineTo(new Vector2d(-36, -24), Math.toRadians(90))
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack1 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 1st cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack2);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack2);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 2nd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack2 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) low);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) low);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 2nd cone off stack on low
                .setReversed(false)
                .splineTo(new Vector2d(-54, -18), Math.toRadians(305))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack3);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack3);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 3rd cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack3 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 3rd cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-6, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack4);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack4);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 4th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-24, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) (stack4 + stackLift));
                    liftMotorRight.setPower(1.0);
                })
                .waitSeconds(stackTime3)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) medium);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) medium);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 4th cone off stack on medium
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -18), Math.toRadians(315))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) stack5);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) stack5);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to pick 5th cone off stack
                .setReversed(true)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(180))
                .splineTo(new Vector2d(-62, -12), Math.toRadians(180))
                .waitSeconds(stackTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(closed);
                })
                .waitSeconds(stackTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition((int) high);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition((int) high);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(deposit);
                    rightV4B.setPosition(deposit);
                    leftGuide.setPosition(extended);
                    rightGuide.setPosition(extended);
                })

                // Go to score 5th cone off stack on high
                .setReversed(false)
                .splineTo(new Vector2d(-48, -12), Math.toRadians(0))
                .splineTo(new Vector2d(-30, -6), Math.toRadians(45))
                .waitSeconds(scoreTime1)
                .addTemporalMarker(() -> {
                    gripServo.setPosition(open);
                })
                .waitSeconds(scoreTime2)
                .addTemporalMarker(() -> {
                    liftMotorLeft.setTargetPosition(0);
                    liftMotorLeft.setPower(1.0);
                    liftMotorRight.setTargetPosition(0);
                    liftMotorRight.setPower(1.0);
                    leftV4B.setPosition(intake);
                    rightV4B.setPosition(intake);
                    leftGuide.setPosition(retracted);
                    rightGuide.setPosition(retracted);
                })

                // Go to park
                .setReversed(true)
                .splineTo(new Vector2d(-36, -24), Math.toRadians(270))

                // Parking spot 3
                .splineTo(new Vector2d(-24, -36), Math.toRadians(0))
                .splineTo(new Vector2d(-12, -36), Math.toRadians(270))
                .build();

        //drive.followTrajectorySequenceAsync(trajSeq);

        /*
         * The INIT-loop:
         * This REPLACES waitForStart!
         */
        while (!isStarted() && !isStopRequested())
        {
            ArrayList<AprilTagDetection> currentDetections = aprilTagDetectionPipeline.getLatestDetections();

            if(currentDetections.size() != 0)
            {
                boolean tagFound = false;

                for(AprilTagDetection tag : currentDetections)
                {
                    if(tag.id == LEFT || tag.id == MIDDLE || tag.id == RIGHT)
                    {
                        tagOfInterest = tag;
                        tagFound = true;
                        break;
                    }
                }

                if(tagFound)
                {
                    telemetry.addLine("Tag of interest is in sight!\n\nLocation data:");
                    tagToTelemetry(tagOfInterest);
                    tagToPacket(tagOfInterest, packet);
                }
                else
                {
                    telemetry.addLine("Don't see tag of interest :(");

                    if(tagOfInterest == null)
                    {
                        telemetry.addLine("(The tag has never been seen)");
                    }
                    else
                    {
                        telemetry.addLine("\nBut we HAVE seen the tag before; last seen at:");
                        tagToTelemetry(tagOfInterest);
                    }
                }

            }
            else
            {
                telemetry.addLine("Don't see tag of interest :(");

                if(tagOfInterest == null)
                {
                    telemetry.addLine("(The tag has never been seen)");
                }
                else
                {
                    telemetry.addLine("\nBut we HAVE seen the tag before; last seen at:");
                    tagToTelemetry(tagOfInterest);
                }

            }

            dashboard.sendTelemetryPacket(packet);

            telemetry.update();
            sleep(20);
        }

        /*
         * The START command just came in: now work off the latest snapshot acquired
         * during the init loop.
         */
        camera.setPipeline(poleObserverPipeline);

        /* Update the telemetry */
        if(tagOfInterest != null)
        {
            telemetry.addLine("Tag snapshot:\n");
            tagToTelemetry(tagOfInterest);
            telemetry.update();
        }
        else
        {
            telemetry.addLine("No tag snapshot available, it was never sighted during the init loop :(");
            telemetry.update();
        }

        /* Actually do something useful */
        //region MOVEMENT
        // speed 0.4 is pretty good

        if(tagOfInterest == null || tagOfInterest.id == LEFT){
            drive.followTrajectorySequenceAsync(parkingOne);
        }else if(tagOfInterest.id == MIDDLE){
            drive.followTrajectorySequenceAsync(parkingTwo);
        }else{
            drive.followTrajectorySequenceAsync(parkingThree);
        }

        while(opModeIsActive()) {
            drive.update();
        }
        //endregion


        /* You wouldn't have this in your autonomous, this is just to prevent the sample from ending */
        //while (opModeIsActive()) {sleep(20);}
    }

    @SuppressLint("DefaultLocale")
    void tagToTelemetry(AprilTagDetection detection)
    {
        telemetry.addLine(String.format("\nDetected tag ID=%d", detection.id));
        telemetry.addLine(String.format("Translation X: %.2f feet", detection.pose.x*FEET_PER_METER));
        telemetry.addLine(String.format("Translation Y: %.2f feet", detection.pose.y*FEET_PER_METER));
        telemetry.addLine(String.format("Translation Z: %.2f feet", detection.pose.z*FEET_PER_METER));
        telemetry.addLine(String.format("Rotation Yaw: %.2f degrees", Math.toDegrees(detection.pose.yaw)));
        telemetry.addLine(String.format("Rotation Pitch: %.2f degrees", Math.toDegrees(detection.pose.pitch)));
        telemetry.addLine(String.format("Rotation Roll: %.2f degrees", Math.toDegrees(detection.pose.roll)));
    }

    @SuppressLint("DefaultLocale")
    void tagToPacket(AprilTagDetection detection, TelemetryPacket packet)
    {
        packet.put("Detected Tag", String.format("Detected tag ID=%d", detection.id));
        packet.put("Translation X", String.format("Translation X: %.2f feet", detection.pose.x*FEET_PER_METER));
        packet.put("Translation Y", String.format("Translation Y: %.2f feet", detection.pose.y*FEET_PER_METER));
        packet.put("Translation Z", String.format("Translation Z: %.2f feet", detection.pose.z*FEET_PER_METER));
    }

}