import java.io.*;

public class Features {
    //private static final String dir = "d:/users/brendan/documents/kaggle/axa/drivers/";
    private static final String dir = "../drivers/";

    private static boolean calculateSimilarities = false;

    public static void main(String[] args) throws IOException {
        File[] drivers = new File(dir).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.length() < 5 && new File(dir, name).isDirectory();
            }
        });

        for (File driver : drivers) {
            File file = new File(dir + "/features/" + driver.getName() + "_features.csv");
            if (file.exists()) {
                continue;
            }
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write("drive,v_max,v_avg,a_max,a_avg,d_max,d_avg,jerk_max,jerk_min,"
                    + "cent_max,cent_avg,cent_avg>1,num_cent>1,dist_max,dist_total,moveTime,stopTime,avgStopTime,"
                    + "fractionMoving,stops,stops_per_km,tunnels,highAccelRate,highDecelRate,"
                    + "avgAngle,avgXP,ang<10,ang<20,ang<30,ang<40,ang<50,ang<60,ang<70,ang<80,ang<90,ang<100,ang<110,ang<120,"
                    + "ang<130,ang<140,ang<150,ang<160,ang<170,ang<180,v<2.2,v<3,v<4,v<5,v<6,v<7,v<8,v<9,v<10," +
                    "v<11,v<12,v<13,v<14,v<15,v<16,v<17,v<18,v<19,v<20,v<21,v<22,v<23,v<24,v<25,v<26,v<27,v<28,v<29,v<30,v>30," +
                    "a<.1,a<.2,a<.3,a<.4,a<.5,a<.6,a<.7,a<.8,a<.9,a<1,a<1.25,a<1.5,a<1.75,a<2,a<2.5,a<3,a>3,"
                    + "d<.1,d<.2,d<.3,d<.4,d<.5,d<.6,d<.7,d<.8,d<.9,d<1,d<1.25,d<1.5,d<1.75,d<2,d<2.5,d<3,d>3," +
                    "j<-3,j<-2.5,j<-2,j<-1.5,j<-1,j<-.5,j<0,j<.5,j<1,j<1.5,j<2,j<2.5,j<3,j>3,"
                    + "xp<-40,xp<-30,xp<-20,xp<-10,xp<-5,xp<0,xp<5,xp<10,xp<20,xp<30,xp<40,xp>40,"
                    + "cent<.05,cent<.1,cent<.15,cent<.2,cent<.25,cent<.3,cent<.35,cent<.4,cent<.45,cent>.5,cent>.55,"
                    + "cent<.6,cent<.65,cent<.7,cent<.75,cent<.8,cent<.85,cent<.9,cent<.95,cent<1,cent<2,cent<3,cent>3,"
                    + "vsinang<1,vsinang<2,vsinang<3,vsinang<4,vsinang<5,vsinang<6,vsinang<7,vsinang<8,vsinang>8,vsinang_max,vsinang_avg,\n");

            //hold data for all drives to calculate aggregate features
            float[] agg_vMax = new float[200];
            float[] agg_vAvg = new float[200];
            float[] agg_aMax = new float[200];
            float[] agg_aAvg = new float[200];
            float[] agg_dMax = new float[200];
            float[] agg_dAvg = new float[200];
            float[] agg_jMax = new float[200];
            float[] agg_jMin = new float[200];
            float[] agg_distMax = new float[200];
            float[] agg_distTotal = new float[200];
            float[] agg_movingTime = new float[200];
            float[] agg_fracMoving = new float[200];
            float[] agg_stopsPerKm = new float[200];

            File[] drives = driver.listFiles();

            for (File drive : drives) {
                int driveNumber = Integer.parseInt(drive.getName().split("\\.")[0]);
                BufferedReader data = new BufferedReader(new FileReader(drive));
                data.readLine(); //skip header
                data.readLine(); //skip first 0,0 point - data might start while car is moving

                float vel_max=0, vel_avg=0, accel=0, accel_max=0, accel_avg=0, decel_max=0, decel_avg=0, dist_max=0, dist_total=0;
                float totalVel=0, totalAccel=0, totalDecel=0;
                int movingTime=0, stoppedTime=0, avgStopTime=0, accelTime=0, decelTime=0;
                int numTunnels=0, numStops=0, highAccelEvents=0, highDecelEvents=0;
                float stopsPerKm=0, highAccelRate=0, highDecelRate=0;
                float x_prev, y_prev, v_prev=0, a_prev=-1, delX_prev=0, delY_prev=0, delX_prevMoving=0, delY_prevMoving=0;
                float jerk_max=0, jerk_min=0, fractionMoving=0;
                float cent_accel, cent_max=0, cent_avg=0, cent_avg_gt1=0, total_cent=0, total_cent_gt1=0;
                float total_xp=0, avg_xp=0;
                int num_cent_gt1=0;
                boolean moving = false;
                float angle=0, totalAngle=0, avgAngle=0;
                int[] anglesCounts = new int[18];
                float[] anglePercents = new float[18];
                int numAngles=0;
                float vSinTheta=0, vSinThetaMax=0, vSinThetaAvg=0, total_vSinTheta=0;
                float[] vProfile = new float[30];
                float[] aProfile = new float[17];
                float[] dProfile = new float[17];
                float[] jProfile = new float[14];
                float[] xpProfile = new float[12];
                float[] centProfile = new float[23];
                float[]	vSinThetaProfile = new float[9];


                String[] first_coords = data.readLine().split(",");
                x_prev = Float.parseFloat(first_coords[0]);
                y_prev = Float.parseFloat(first_coords[1]);

                String position;
                while ((position = data.readLine()) != null) {
                    String[] coords = position.split(",");
                    float x = Float.parseFloat(coords[0]);
                    float y = Float.parseFloat(coords[1]);
                    float delX = x - x_prev;
                    float delY = y - y_prev;
                    float dist_vel = (float) Math.sqrt(Math.pow(x - x_prev, 2) + Math.pow(y - y_prev, 2));
                    accel = dist_vel - v_prev;
                    dist_total += dist_vel;
                    float distFromOrigin = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                    if (distFromOrigin > dist_max) dist_max = distFromOrigin;
                    if (dist_vel > 50 || accel > 15) { //check for hyperjump
                        numTunnels += 1;
                        a_prev = -1;
                        moving = true;
                    } else {
                        if (dist_vel > 2) moving = true;
                        if (dist_vel > vel_max) vel_max = dist_vel;
                        if (dist_vel == 0) {
                            stoppedTime += 1;
                            if (moving) numStops++;
                            moving = false;
                        } else {
                            movingTime += 1;
                            if (dist_vel < 2.2) {
                                vProfile[0] += 1;
                            } else if (dist_vel < 3) {
                                vProfile[1] += 1;
                            } else if (dist_vel < 4) {
                                vProfile[2] += 1;
                            } else if (dist_vel < 5) {
                                vProfile[3] += 1;
                            } else if (dist_vel < 6) {
                                vProfile[4] += 1;
                            } else if (dist_vel < 7) {
                                vProfile[5] += 1;
                            } else if (dist_vel < 8) {
                                vProfile[6] += 1;
                            } else if (dist_vel < 9) {
                                vProfile[7] += 1;
                            } else if (dist_vel < 10) {
                                vProfile[8] += 1;
                            } else if (dist_vel < 11) {
                                vProfile[9] += 1;
                            } else if (dist_vel < 12) {
                                vProfile[10] += 1;
                            } else if (dist_vel < 13) {
                                vProfile[11] += 1;
                            } else if (dist_vel < 14) {
                                vProfile[12] += 1;
                            } else if (dist_vel < 15) {
                                vProfile[13] += 1;
                            } else if (dist_vel < 16) {
                                vProfile[14] += 1;
                            } else if (dist_vel < 17) {
                                vProfile[15] += 1;
                            } else if (dist_vel < 18) {
                                vProfile[16] += 1;
                            } else if (dist_vel < 19) {
                                vProfile[17] += 1;
                            } else if (dist_vel < 20) {
                                vProfile[18] += 1;
                            } else if (dist_vel < 21) {
                                vProfile[19] += 1;
                            } else if (dist_vel < 22) {
                                vProfile[20] += 1;
                            } else if (dist_vel < 23) {
                                vProfile[21] += 1;
                            } else if (dist_vel < 24) {
                                vProfile[22] += 1;
                            } else if (dist_vel < 25) {
                                vProfile[23] += 1;
                            } else if (dist_vel < 26) {
                                vProfile[24] += 1;
                            } else if (dist_vel < 27) {
                                vProfile[25] += 1;
                            } else if (dist_vel < 28) {
                                vProfile[26] += 1;
                            } else if (dist_vel < 29) {
                                vProfile[27] += 1;
                            } else if (dist_vel < 30) {
                                vProfile[28] += 1;
                            } else vProfile[29] += 1;
                        }
                        totalVel += dist_vel;


                        if (accel > 0) {
                            accelTime += 1;
                            totalAccel += accel;
                            if (accel > 3) highAccelEvents++;
                            if (accel > accel_max) accel_max = accel;
                            if (accel < .1) {
                                aProfile[0] += 1;
                            } else if (accel < .2) {
                                aProfile[1] += 1;
                            } else if (accel < .3) {
                                aProfile[2] += 1;
                            } else if (accel < .4) {
                                aProfile[3] += 1;
                            } else if (accel < .5) {
                                aProfile[4] += 1;
                            } else if (accel < .6) {
                                aProfile[5] += 1;
                            } else if (accel < .7) {
                                aProfile[6] += 1;
                            } else if (accel < .8) {
                                aProfile[7] += 1;
                            } else if (accel < .9) {
                                aProfile[8] += 1;
                            } else if (accel < 1.0) {
                                aProfile[9] += 1;
                            } else if (accel < 1.25) {
                                aProfile[10] += 1;
                            } else if (accel < 1.5) {
                                aProfile[11] += 1;
                            } else if (accel < 1.75) {
                                aProfile[12] += 1;
                            } else if (accel < 2) {
                                aProfile[13] += 1;
                            } else if (accel < 2.5) {
                                aProfile[14] += 1;
                            } else if (accel < 3) {
                                aProfile[15] += 1;
                            } else aProfile[16] += 1;
                        } else if (accel < 0) {
                            decelTime += 1;
                            totalDecel += accel;
                            if (accel < -3) highDecelEvents++;
                            if (accel < decel_max) decel_max = accel;
                            if (accel > -.1) {
                                dProfile[0] += 1;
                            } else if (accel > -.2) {
                                dProfile[1] += 1;
                            } else if (accel > -.3) {
                                dProfile[2] += 1;
                            } else if (accel > -.4) {
                                dProfile[3] += 1;
                            } else if (accel > -.5) {
                                dProfile[4] += 1;
                            } else if (accel > -.6) {
                                dProfile[5] += 1;
                            } else if (accel > -.7) {
                                dProfile[6] += 1;
                            } else if (accel > -.8) {
                                dProfile[7] += 1;
                            } else if (accel > -.9) {
                                dProfile[8] += 1;
                            } else if (accel > -1.0) {
                                dProfile[9] += 1;
                            } else if (accel > -1.25) {
                                dProfile[10] += 1;
                            } else if (accel > -1.5) {
                                dProfile[11] += 1;
                            } else if (accel > -1.75) {
                                dProfile[12] += 1;
                            } else if (accel > -2) {
                                dProfile[13] += 1;
                            } else if (accel > -2.5) {
                                dProfile[14] += 1;
                            } else if (accel > -3) {
                                dProfile[15] += 1;
                            } else dProfile[16] += 1;
                        }

                        if (a_prev != -1) {
                            float jerk = accel - a_prev;
                            if (jerk > jerk_max) {
                                jerk_max = jerk;
                            } else if (jerk < jerk_min) {
                                jerk_min = jerk;
                            }
                            if (jerk < -3) {
                                jProfile[0] += 1;
                            } else if (jerk < -2.5) {
                                jProfile[1] += 1;
                            } else if (jerk < -2) {
                                jProfile[2] += 1;
                            } else if (jerk < -1.5) {
                                jProfile[3] += 1;
                            } else if (jerk < -1) {
                                jProfile[4] += 1;
                            } else if (jerk < -.5) {
                                jProfile[5] += 1;
                            } else if (jerk < 0) {
                                jProfile[6] += 1;
                            } else if (jerk < .5) {
                                jProfile[7] += 1;
                            } else if (jerk < 1) {
                                jProfile[8] += 1;
                            } else if (jerk < 1.5) {
                                jProfile[9] += 1;
                            } else if (jerk < 2) {
                                jProfile[10] += 1;
                            } else if (jerk < 2.5) {
                                jProfile[11] += 1;
                            } else if (jerk < 3) {
                                jProfile[12] += 1;
                            } else jProfile[13] += 1;
                        }
						/*
						if (dist_vel == 0 || v_prev == 0) {
							cent_accel = 0;
						} else {
							float x2 = (delX - delX_prev) * (delX - delX_prev);
							float y2 = (delY - delY_prev) * (delY - delY_prev);
							cent_accel = (float) Math.sqrt(x2 + y2) / dist_vel;
							if (cent_accel > 1) {
								num_cent_gt1 += 1;
								total_cent_gt1 += cent_accel;
							}
							total_cent += cent_accel;
							if (cent_accel > cent_max) cent_max = cent_accel;
							if (cent_accel < .1) {
								centProfile[0] += 1;
							} else if (cent_accel < .2) {
								centProfile[1] += 1;
							} else if (cent_accel < .3) {
								centProfile[2] += 1;
							} else if (cent_accel < .4) {
								centProfile[3] += 1;
							} else if (cent_accel < .5) {
								centProfile[4] += 1;
							} else if (cent_accel < .75) {
								centProfile[5] += 1;
							} else if (cent_accel < 1) {
								centProfile[6] += 1;
							} else if (cent_accel < 2) {
								centProfile[7] += 1;
							} else if (cent_accel < 3) {
								centProfile[8] += 1;
							} else centProfile[9] += 1;
						}
						*/
                        if (dist_vel == 0 || v_prev == 0) {
                            cent_accel = 0;
                        } else {
                            float crossProduct = delX * delY_prev - delY * delX_prev;
                            cent_accel = crossProduct / v_prev;
                            if (cent_accel > 1) {
                                num_cent_gt1 += 1;
                                total_cent_gt1 += cent_accel;
                            }
                            total_cent += cent_accel;
                            if (cent_accel > cent_max) cent_max = cent_accel;
                            if (cent_accel < .05) {
                                centProfile[0] += 1;
                            } else if (cent_accel < .1) {
                                centProfile[1] += 1;
                            } else if (cent_accel < .15) {
                                centProfile[2] += 1;
                            } else if (cent_accel < .2) {
                                centProfile[3] += 1;
                            } else if (cent_accel < .25) {
                                centProfile[4] += 1;
                            } else if (cent_accel < .3) {
                                centProfile[5] += 1;
                            } else if (cent_accel < .35) {
                                centProfile[6] += 1;
                            } else if (cent_accel < .4) {
                                centProfile[7] += 1;
                            } else if (cent_accel < .45) {
                                centProfile[8] += 1;
                            } else if (cent_accel < .5) {
                                centProfile[9] += 1;
                            } else if (cent_accel < .55) {
                                centProfile[10] += 1;
                            } else if (cent_accel < .6) {
                                centProfile[11] += 1;
                            } else if (cent_accel < .65) {
                                centProfile[12] += 1;
                            } else if (cent_accel < .7) {
                                centProfile[13] += 1;
                            } else if (cent_accel < .75) {
                                centProfile[14] += 1;
                            } else if (cent_accel < .8) {
                                centProfile[15] += 1;
                            } else if (cent_accel < .85) {
                                centProfile[16] += 1;
                            } else if (cent_accel < .9) {
                                centProfile[17] += 1;
                            } else if (cent_accel < .95) {
                                centProfile[18] += 1;
                            } else if (cent_accel < 1) {
                                centProfile[19] += 1;
                            } else if (cent_accel < 2) {
                                centProfile[20] += 1;
                            } else if (cent_accel < 3) {
                                centProfile[21] += 1;
                            } else centProfile[22] += 1;
                        }


                        if (dist_vel > 1) {
                            float dotProduct = delX * delX_prevMoving + delY * delY_prevMoving;
                            float normalization = (float) Math.sqrt((delX * delX + delY * delY) * (delX_prevMoving * delX_prevMoving + delY_prevMoving * delY_prevMoving));
                            if (normalization != 0) {
                                float cosine = dotProduct / normalization;
                                if (cosine > 1) cosine = 1;
                                if (cosine < -1) cosine = -1;
                                angle = (float) (Math.acos(cosine) * 360 / (2 * Math.PI));
                                numAngles++;
                                totalAngle += angle;
                                if (angle == 180) angle = 179.9f;
                                anglesCounts[(int) (angle / 10)]++;

                                vSinTheta = (float) (dist_vel * Math.sin(angle * (2 * Math.PI / 360)));
                                if (vSinTheta > vSinThetaMax) vSinThetaMax = vSinTheta;
                                total_vSinTheta += vSinTheta;
                                if (vSinTheta < 1) {
                                    vSinThetaProfile[0] += 1;
                                } else if (vSinTheta < 2) {
                                    vSinThetaProfile[1] += 1;
                                } else if (vSinTheta < 3) {
                                    vSinThetaProfile[2] += 1;
                                } else if (vSinTheta < 4) {
                                    vSinThetaProfile[3] += 1;
                                } else if (vSinTheta < 5) {
                                    vSinThetaProfile[4] += 1;
                                } else if (vSinTheta < 6) {
                                    vSinThetaProfile[5] += 1;
                                } else if (vSinTheta < 7) {
                                    vSinThetaProfile[6] += 1;
                                } else if (vSinTheta < 8) {
                                    vSinThetaProfile[7] += 1;
                                } else {
                                    vSinThetaProfile[8] += 1;
                                }
                            }

                            delX_prevMoving = delX;
                            delY_prevMoving = delY;
                            //System.out.println(totalAngle);

                        }

                        if (dist_vel > 0) {
                            float crossProduct = delX * delY_prev - delY * delX_prev;
                            total_xp += crossProduct;
                            if (crossProduct < -40) {
                                xpProfile[0] += 1;
                            } else if (crossProduct < -30) {
                                xpProfile[1] += 1;
                            } else if (crossProduct < -20) {
                                xpProfile[2] += 1;
                            } else if (crossProduct < -10) {
                                xpProfile[3] += 1;
                            } else if (crossProduct < -5) {
                                xpProfile[4] += 1;
                            } else if (crossProduct < 0) {
                                xpProfile[5] += 1;
                            } else if (crossProduct < 5) {
                                xpProfile[6] += 1;
                            } else if (crossProduct < 10) {
                                xpProfile[7] += 1;
                            } else if (crossProduct < 20) {
                                xpProfile[8] += 1;
                            } else if (crossProduct < 30) {
                                xpProfile[9] += 1;
                            } else if (crossProduct < 40) {
                                xpProfile[10] += 1;
                            } else xpProfile[11] += 1;

                        }

                        v_prev = dist_vel;
                        a_prev = accel;
                        delX_prev = delX;
                        delY_prev = delY;
                    }

                    x_prev = x;
                    y_prev = y;


                }

                if (movingTime > 0) {
                    vel_avg = totalVel / movingTime;
                    cent_avg = total_cent / movingTime;
                    for (int i = 0; i < vProfile.length; i++) {
                        vProfile[i] /= .01 * movingTime;
                    }
                    for (int i = 0; i < xpProfile.length; i++) {
                        xpProfile[i] /= .01 * movingTime;
                    }
                    avg_xp = total_xp / movingTime;
                    for (int i = 0; i < centProfile.length; i++) {
                        centProfile[i] /= .01 * movingTime;
                    }
                }
                if (accelTime > 0) {
                    accel_avg = totalAccel / accelTime;
                    for (int i = 0; i < aProfile.length; i++) {
                        aProfile[i] /= .01 * accelTime;
                    }
                    for (int i = 0; i < jProfile.length; i++) {
                        jProfile[i] /= .01 * (accelTime + decelTime);
                    }
                }
                if (decelTime > 0) {
                    decel_avg = totalDecel / decelTime;
                    for (int i = 0; i < dProfile.length; i++) {
                        dProfile[i] /= .01 * decelTime;
                    }
                }
                if (dist_total > 0) {
                    stopsPerKm = 1000 * numStops / dist_total;
                    highAccelRate = (float) 1000 * highAccelEvents / dist_total;
                    highDecelRate = (float) 1000 * highDecelEvents / dist_total;
                }
                if (movingTime > 0 || stoppedTime > 0) fractionMoving = (float) movingTime / (movingTime + stoppedTime);
                if (numStops > 0) avgStopTime = stoppedTime / numStops;
                if (num_cent_gt1 > 0) cent_avg_gt1 = total_cent_gt1 / num_cent_gt1;
                if (numAngles > 0) {
                    for (int i = 0; i < anglesCounts.length; i++) {
                        anglePercents[i] = (float) 100 * anglesCounts[i] / numAngles;
                    }
                    avgAngle = (float) totalAngle / numAngles;
                    vSinThetaAvg = (float) total_vSinTheta / numAngles;
                    for (int i = 0; i < vSinThetaProfile.length; i++) {
                        vSinThetaProfile[i] /= .01 * numAngles;
                    }
                }

                //save values in arrays for calculating similarity features later
                if (calculateSimilarities) {
                    agg_vMax[driveNumber-1] = vel_max;
                    agg_vAvg[driveNumber-1] = vel_avg;
                    agg_aMax[driveNumber-1] = accel_max;
                    agg_aAvg[driveNumber-1] = accel_avg;
                    agg_dMax[driveNumber-1] = decel_max;
                    agg_dAvg[driveNumber-1] = decel_avg;
                    agg_jMax[driveNumber-1] = jerk_max;
                    agg_jMin[driveNumber-1] = jerk_min;
                    agg_distMax[driveNumber-1] = dist_max;
                    agg_distTotal[driveNumber-1] = dist_total;
                    agg_movingTime[driveNumber-1] = movingTime;
                    agg_fracMoving[driveNumber-1] = fractionMoving;
                    agg_stopsPerKm[driveNumber-1] = stopsPerKm;
                }
				/*
				for (int i=0; i < anglePercents.length-1; i++) {
					System.out.println(totalAngle + ", " + anglesCounts[i] + ", " + anglePercents[i]);
				}*/
                output.write(drive.getName().split("\\.")[0] + "," + vel_max + "," + vel_avg + "," + accel_max + "," +
                        accel_avg + "," + decel_max + "," + decel_avg + "," + jerk_max + "," + jerk_min + "," +
                        cent_max + "," + cent_avg + "," + cent_avg_gt1 + "," + num_cent_gt1 + "," +
                        dist_max + "," + dist_total + "," + movingTime + "," + stoppedTime + "," + avgStopTime + "," +
                        fractionMoving + "," + numStops + "," + stopsPerKm + "," + numTunnels + "," +
                        highAccelRate + "," + highDecelRate + "," + avgAngle + "," + avg_xp + ",");
                for (int i=0; i < anglePercents.length; i++) {
                    output.write(anglePercents[i] + ",");
                }
                for (int i=0; i < vProfile.length; i++) {
                    output.write(vProfile[i] + ",");
                }
                for (int i=0; i < aProfile.length; i++) {
                    output.write(aProfile[i] + ",");
                }
                for (int i=0; i < dProfile.length; i++) {
                    output.write(dProfile[i] + ",");
                }
                for (int i=0; i < jProfile.length; i++) {
                    output.write(jProfile[i] + ",");
                }
                for (int i=0; i < xpProfile.length; i++) {
                    output.write(xpProfile[i] + ",");
                }
                for (int i=0; i < centProfile.length; i++) {
                    output.write(centProfile[i] + ",");
                }
                for (int i=0; i < vSinThetaProfile.length; i++) {
                    output.write(vSinThetaProfile[i] + ",");
                }
                output.write(vSinThetaMax + "," + vSinThetaAvg + "\n");

                data.close();
            }

            output.close();

            if (calculateSimilarities) {
                int[] sim_vMax = new int[200];
                int[] sim_vAvg = new int[200];
                int[] sim_aMax = new int[200];
                int[] sim_aAvg = new int[200];
                int[] sim_dMax = new int[200];
                int[] sim_dAvg = new int[200];
                int[] sim_jMax = new int[200];
                int[] sim_jMin = new int[200];
                int[] sim_distMax = new int[200];
                int[] sim_distTotal = new int[200];
                int[] sim_movingTime = new int[200];
                int[] sim_fracMoving = new int[200];
                int[] sim_stopsPerKm = new int[200];

                for (int i = 0; i < 200; i++) {
                    for (int j = 0; j < 200; j++) {
                        if (Math.abs(agg_vMax[i] - agg_vMax[j]) <= 2.5) sim_vMax[i] += 1;
                        if (Math.abs(agg_vAvg[i] - agg_vAvg[j]) <= 2.5) sim_vAvg[i] += 1;
                        if (Math.abs(agg_aMax[i] - agg_aMax[j]) <= 2.5) sim_aMax[i] += 1;
                        if (Math.abs(agg_aAvg[i] - agg_aAvg[j]) <= .2) sim_aAvg[i] += 1;
                        if (Math.abs(agg_dMax[i] - agg_dMax[j]) <= 2.5) sim_dMax[i] += 1;
                        if (Math.abs(agg_dAvg[i] - agg_dAvg[j]) <= .2) sim_dAvg[i] += 1;
                        if (Math.abs(agg_jMax[i] - agg_jMax[j]) <= 2.5) sim_jMax[i] += 1;
                        if (Math.abs(agg_jMin[i] - agg_jMin[j]) <= 2.5) sim_jMin[i] += 1;
                        if (Math.abs(agg_distMax[i] - agg_distMax[j]) <= 500) sim_distMax[i] += 1;
                        if (Math.abs(agg_distTotal[i] - agg_distTotal[j]) <= 1000) sim_distTotal[i] += 1;
                        if (Math.abs(agg_movingTime[i] - agg_movingTime[j]) <= 100) sim_movingTime[i] += 1;
                        if (Math.abs(agg_fracMoving[i] - agg_fracMoving[j]) <= .1) sim_fracMoving[i] += 1;
                        if (Math.abs(agg_stopsPerKm[i] - agg_stopsPerKm[j]) <= 20) sim_stopsPerKm[i] += 1;
                    }

                    BufferedWriter similarity = new BufferedWriter(new FileWriter(dir + driver.getName() + "_similarity.csv"));
                    similarity.write("drive,vMax,vAvg,aMax,aAvg,dMax,dAvg,jMax,jMin,distMax,distTotal,movingTime,fracMoving,stopsPerKm,overall\n");

                    for (int k = 0; k < 200; k++) {
                        int overall = sim_vMax[k] + sim_vAvg[k] + sim_aMax[k] + sim_aAvg[k] +
                                sim_dMax[k] + sim_dAvg[k] + sim_jMax[k] + sim_jMin[k] +
                                sim_distMax[k] + sim_distTotal[k] + sim_movingTime[k] +
                                sim_fracMoving[k] + sim_stopsPerKm[k];
                        similarity.write((k+1) + "," + sim_vMax[k] + "," + sim_vAvg[k] + "," + sim_aMax[k] + "," + sim_aAvg[k] + "," +
                                sim_dMax[k] + "," + sim_dAvg[k] + "," + sim_jMax[k] + "," + sim_jMin[k] + "," +
                                sim_distMax[k] + "," + sim_distTotal[k] + "," + sim_movingTime[k] + "," +
                                sim_fracMoving[k] + "," + sim_stopsPerKm[k] + "," + overall + "\n");
                    }

                    similarity.close();
                }


            }

        }

    }
}

