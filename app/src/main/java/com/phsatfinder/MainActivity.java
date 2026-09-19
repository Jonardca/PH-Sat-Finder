package com.phsatfinder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;

import java.util.Locale;

public class MainActivity extends Activity {
    private SatFinderView finderView;
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private static final int REQ_LOCATION = 1001;

    private final LocationListener locationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            finderView.setLocation(location);
        }
        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) { }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    };

    private final SensorEventListener sensorListener = new SensorEventListener() {
        private final float[] rotation = new float[9];
        private final float[] orientation = new float[3];
        private final float[] accel = new float[3];
        private final float[] mag = new float[3];
        private boolean haveAccel = false, haveMag = false;

        @Override public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values);
                SensorManager.getOrientation(rotation, orientation);
                updateHeadingAndTilt(orientation[0], orientation[1], orientation[2]);
                return;
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accel, 0, 3);
                haveAccel = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, mag, 0, 3);
                haveMag = true;
            }
            if (haveAccel && haveMag && SensorManager.getRotationMatrix(rotation, null, accel, mag)) {
                SensorManager.getOrientation(rotation, orientation);
                updateHeadingAndTilt(orientation[0], orientation[1], orientation[2]);
            }
        }

        private void updateHeadingAndTilt(float azimuthRad, float pitchRad, float rollRad) {
            float heading = (float) Math.toDegrees(azimuthRad);
            heading = (heading + 360f) % 360f;
            float pitch = (float) Math.toDegrees(pitchRad);
            float roll = (float) Math.toDegrees(rollRad);
            finderView.setSensorValues(heading, pitch, roll);
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(Color.rgb(2, 5, 10));

        finderView = new SatFinderView(this);
        setContentView(finderView);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        finderView.setCallbacks(new SatFinderView.Callbacks() {
            @Override public void onGpsClicked() { startGps(); }
            @Override public void onSensorsClicked() { startSensors(); }
            @Override public void onSatelliteClicked() { showSatelliteMenu(); }
        });

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    private void startGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            finderView.setGpsState("WAIT", "GPS");
            return;
        }
        finderView.setGpsState("WAIT", "GPS…");
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, locationListener);
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) finderView.setLocation(last);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, locationListener);
                Location last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (last != null) finderView.setLocation(last);
            }
        } catch (SecurityException ignored) {
            finderView.setGpsState("ERROR", "CHECK");
        }
    }

    private void startSensors() {
        if (finderView.isSensorStarted()) return;
        finderView.setSensorStarted(true);
        if (rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            if (accelerometer != null) sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            if (magnetometer != null) sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void showSatelliteMenu() {
        PopupMenu menu = new PopupMenu(this, finderView);
        menu.getMenu().add("Cignal — SES-7 / SES-9");
        menu.getMenu().add("GSAT — SES-9");
        menu.getMenu().add("SatLite — Koreasat 7");
        menu.getMenu().add("Sky Direct — JCSAT-4B");
        menu.setOnMenuItemClickListener(item -> {
            finderView.setSatellite(item.getTitle().toString());
            return true;
        });
        menu.show();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                startGps();
            } else {
                finderView.setGpsState("DENIED", "CHECK");
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (finderView != null && finderView.isSensorStarted()) startSensors();
        if (finderView != null && finderView.hasGpsStarted()) startGps();
    }

    @Override protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(sensorListener);
        if (locationManager != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
        }
    }

    @Override protected void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(sensorListener);
        if (locationManager != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
        }
        super.onDestroy();
    }

    private static class SatFinderView extends View {
        interface Callbacks {
            void onGpsClicked();
            void onSensorsClicked();
            void onSatelliteClicked();
        }

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final String[] satNames = {"Cignal — SES-7 / SES-9", "GSAT — SES-9", "SatLite — Koreasat 7", "Sky Direct — JCSAT-4B"};
        private final double[] satLon = {108.2, 108.2, 116.0, 124.0};
        private int satIndex = 0;
        private Location location;
        private float heading = Float.NaN;
        private float smoothedHeading = Float.NaN;
        private float tilt = Float.NaN;
        private boolean sensorStarted = false;
        private boolean gpsStarted = false;
        private String gpsState = "—", gpsBadge = "GPS";
        private String sensorState = "Sensor ready";
        private String compassState = "Keep phone level for compass.";
        private String compassBadge = "READY";
        private Callbacks callbacks;

        private static final int BG = Color.rgb(3,8,18);
        private static final int PANEL = Color.rgb(9,20,36);
        private static final int PANEL2 = Color.rgb(12,26,45);
        private static final int LINE = Color.rgb(18,80,128);
        private static final int TEXT = Color.rgb(245,248,255);
        private static final int MUTED = Color.rgb(143,166,192);
        private static final int CYAN = Color.rgb(56,217,255);
        private static final int GOOD = Color.rgb(32,230,160);
        private static final int GOLD = Color.rgb(255,198,92);
        private static final int RED = Color.rgb(255,49,93);

        SatFinderView(Context context) {
            super(context);
            p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1f);
            setBackgroundColor(BG);
        }

        void setCallbacks(Callbacks c) { callbacks = c; }
        boolean isSensorStarted() { return sensorStarted; }
        void setSensorStarted(boolean b) { sensorStarted = b; sensorState = "Motion sensor ACTIVE"; invalidate(); }
        boolean hasGpsStarted() { return gpsStarted; }

        void setGpsState(String state, String badge) { gpsStarted = true; gpsState = state; gpsBadge = badge; invalidate(); }

        void setLocation(Location loc) {
            location = loc;
            gpsStarted = true;
            gpsState = "LIVE";
            gpsBadge = "GPS LIVE";
            invalidate();
        }

        void setSensorValues(float rawHeading, float pitch, float roll) {
            tilt = pitch;
            if (Math.max(Math.abs(pitch), Math.abs(roll)) > 15f) {
                compassState = "Compass PAUSED • hold phone flatter (≤15°)";
                compassBadge = "PAUSED";
                invalidate();
                return;
            }
            if (Float.isNaN(smoothedHeading)) smoothedHeading = rawHeading;
            float diff = signed180(rawHeading - smoothedHeading);
            smoothedHeading = norm360(smoothedHeading + diff * 0.16f);
            heading = smoothedHeading;
            compassState = "Compass LIVE • native sensor + smoothed";
            compassBadge = dir8(heading);
            invalidate();
        }

        void setSatellite(String title) {
            for (int i = 0; i < satNames.length; i++) {
                if (satNames[i].equals(title)) { satIndex = i; break; }
            }
            invalidate();
        }

        private float norm360(float x) { x %= 360f; return x < 0 ? x + 360f : x; }
        private float signed180(float x) { x = ((x + 180f) % 360f + 360f) % 360f - 180f; return x; }
        private String dir8(float a) { String[] d={"N","NE","E","SE","S","SW","W","NW"}; return d[Math.round(norm360(a)/45f)%8]; }

        private double[] calculate(double lat, double lon, double sat) {
            double Re=6378.137, Rs=42164;
            double latr=Math.toRadians(lat);
            double dlon=Math.toRadians(signed180((float)(sat-lon)));
            double cosPsi=Math.cos(latr)*Math.cos(dlon);
            double az=norm360((float)Math.toDegrees(Math.atan2(Math.sin(dlon), -Math.sin(latr)*Math.cos(dlon))));
            double el=Math.toDegrees(Math.atan2(cosPsi-Re/Rs, Math.sqrt(Math.max(0,1-cosPsi*cosPsi))));
            double skew=Math.toDegrees(Math.atan2(Math.sin(dlon), Math.tan(latr)));
            return new double[]{az,el,skew};
        }

        private String fmt(double v, int n) { return String.format(Locale.US, "%1$."+n+"f", v); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight(), d=density();
            p.setStyle(Paint.Style.FILL);
            p.setColor(BG); c.drawRect(0,0,w,h,p);
            drawHeader(c,w,d);
            drawTopbar(c,w,d);
            float footerH=62*d;
            float dashTop=100*d, dashBottom=h-footerH;
            drawDashboard(c,w,d,dashTop,dashBottom);
            drawFooter(c,w,d,h-footerH,h);
        }

        private float density(){ return getResources().getDisplayMetrics().density; }
        private void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
            p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(align); p.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); c.drawText(s,x,y,p);
        }
        private void panel(Canvas c,float l,float t,float rr,float b){
            p.setStyle(Paint.Style.FILL); p.setColor(PANEL); r.set(l,t,rr,b); c.drawRoundRect(r,12*density(),12*density(),p);
            stroke.setColor(LINE); stroke.setStrokeWidth(1*density()); c.drawRoundRect(r,12*density(),12*density(),stroke);
        }

        private void drawHeader(Canvas c,float w,float d){
            txt(c,"PH Sat Finder",8*d,20*d,18*d,TEXT,Paint.Align.LEFT,true);
            txt(c,"NATIVE ANDROID",8*d,31*d,8*d,CYAN,Paint.Align.LEFT,true);
            drawBadge(c,w-205*d,4*d,62*d,"GPS",gpsState);
            drawBadge(c,w-138*d,4*d,62*d,"COMPASS",Float.isNaN(heading)?compassBadge:Math.round(heading)+"°");
            drawBadge(c,w-71*d,4*d,63*d,"TILT",Float.isNaN(tilt)?"—":Math.round(tilt)+"°");
        }

        private void drawBadge(Canvas c,float x,float y,float bw,String top,String bottom){
            panel(c,x,y,x+bw,y+30*density());
            txt(c,top,x+bw/2,y+11*density(),8*density(),GOOD,Paint.Align.CENTER,true);
            txt(c,bottom,x+bw/2,y+23*density(),9*density(),TEXT,Paint.Align.CENTER,false);
        }

        private void drawTopbar(Canvas c,float w,float d){
            float y=37*d, gap=5*d, total=w-16*d, c1=total*.42f, c2=total*.21f, c3=total*.21f, c4=total*.16f;
            float x=8*d;
            panel(c,x,y,x+c1,y+52*d); txt(c,"SATELLITE",x+7*d,y+11*d,8*d,MUTED,Paint.Align.LEFT,false); txt(c,satNames[satIndex],x+7*d,y+31*d,11*d,TEXT,Paint.Align.LEFT,true); txt(c,"▼",x+c1-9*d,y+31*d,10*d,CYAN,Paint.Align.RIGHT,true);
            drawMini(c,x+c1+gap,y,c2,"AZIMUTH",location==null?"—":fmt(calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex])[0],1)+"°");
            drawMini(c,x+c1+gap+c2+gap,y,c3,"ELEVATION",location==null?"—":fmt(calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex])[1],1)+"°");
            drawMini(c,x+c1+gap+c2+gap+c3+gap,y,c4,"LNB",location==null?"—":fmt(calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex])[2],1)+"°");
        }
        private void drawMini(Canvas c,float x,float y,float bw,String label,String val){panel(c,x,y,x+bw,y+52*density());txt(c,label,x+7*density(),y+12*density(),8*density(),MUTED,Paint.Align.LEFT,false);txt(c,val,x+7*density(),y+31*density(),14*density(),GOOD,Paint.Align.LEFT,true);}

        private void drawDashboard(Canvas c,float w,float d,float top,float bottom){
            float gap=6*d, leftW=w*.60f-gap/2, rightX=w*.60f+gap/2, rightW=w-rightX-8*d;
            panel(c,8*d,top,leftW, bottom-28*d);
            drawCompass(c,8*d,top,leftW,bottom-28*d);
            panel(c,8*d,bottom-23*d,leftW,bottom-2*d);
            drawTurn(c,8*d,bottom-23*d,leftW,bottom-2*d);

            float y=top;
            float ah=94*d; panel(c,rightX,y,rightX+rightW,y+ah); drawAlignment(c,rightX,y,rightW,ah);
            y+=ah+gap; float sh=105*d; panel(c,rightX,y,rightX+rightW,y+sh); drawSensors(c,rightX,y,rightW,sh);
            y+=sh+gap; panel(c,rightX,y,rightX+rightW,bottom-2*d); drawClock(c,rightX,y,rightW,bottom-2*d-y);
        }

        private void drawCompass(Canvas c,float l,float t,float rr,float b){
            float d=density(); float cx=(l+rr)/2, cy=(t+b)/2-7*density();
            float size=Math.min(rr-l-8*density(),b-t-42*density()); size=Math.max(190*density(),Math.min(size,410*density()));
            float rad=size/2;
            Paint gradPaint=new Paint(Paint.ANTI_ALIAS_FLAG); gradPaint.setShader(new RadialGradient(cx,cy,rad,new int[]{Color.rgb(18,36,60),Color.rgb(11,25,45),Color.rgb(2,6,13)},new float[]{0,.60f,1},Shader.TileMode.CLAMP));
            c.drawCircle(cx,cy,rad,gradPaint); gradPaint.setShader(null); stroke.setColor(Color.rgb(41,77,118)); stroke.setStrokeWidth(3*d); c.drawCircle(cx,cy,rad,stroke);
            c.save(); c.rotate(Float.isNaN(heading)?0:-heading,cx,cy);
            stroke.setStrokeWidth(1*d); stroke.setColor(Color.rgb(40,98,142)); c.drawCircle(cx,cy,rad*.91f,stroke); stroke.setColor(Color.rgb(25,75,115)); c.drawCircle(cx,cy,rad*.81f,stroke);
            for(int deg=0;deg<360;deg+=3){double a=Math.toRadians(deg-90);float outer=rad*.96f;float inner=(deg%45==0)?rad*.84f:rad*.91f;stroke.setStrokeWidth(deg%45==0?2*d:1*d);stroke.setColor(deg%45==0?CYAN:Color.rgb(49,191,249));c.drawLine(cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,stroke);}
            String[] dirs={"N","NE","E","SE","S","SW","W","NW"};
            for(int i=0;i<8;i++){double a=Math.toRadians(i*45-90);float tx=cx+(float)Math.cos(a)*rad*.72f, ty=cy+(float)Math.sin(a)*rad*.72f+5*d;txt(c,dirs[i],tx,ty,(i%2==0?22:13)*d,TEXT,Paint.Align.CENTER,true);}
            for(int i=0;i<8;i++){double a=Math.toRadians(i*45-90);float tx=cx+(float)Math.cos(a)*rad*.89f, ty=cy+(float)Math.sin(a)*rad*.89f+4*d;txt(c,(i*45)+"°",tx,ty,9*d,Color.rgb(208,221,236),Paint.Align.CENTER,true);}
            double[] calc=location==null?null:calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex]);
            if(calc!=null){double a=Math.toRadians(calc[0]-90);float mx=cx+(float)Math.cos(a)*rad*.88f,my=cy+(float)Math.sin(a)*rad*.88f;Path tri=new Path();tri.moveTo(mx,my-9*d);tri.lineTo(mx-8*d,my+7*d);tri.lineTo(mx+8*d,my+7*d);tri.close();p.setColor(GOLD);p.setStyle(Paint.Style.FILL);c.drawPath(tri,p);}
            c.restore();
            p.setColor(RED);p.setStyle(Paint.Style.FILL);Path pointer=new Path();pointer.moveTo(cx,cy-rad*.93f);pointer.lineTo(cx-10*d,cy-rad*.93f+25*d);pointer.lineTo(cx+10*d,cy-rad*.93f+25*d);pointer.close();c.drawPath(pointer,p);
            p.setColor(Color.rgb(181,220,255));c.drawCircle(cx,cy,20*d, p);p.setColor(Color.rgb(16,28,46));c.drawCircle(cx,cy,14*d,p);
            String head=Float.isNaN(heading)?"PHONE —° —":"PHONE "+Math.round(heading)+"° "+dir8(heading);txt(c,head,cx,b+15*d,Math.min(18*d,rr-l-20*d),GOOD,Paint.Align.CENTER,true);
        }

        private void drawTurn(Canvas c,float l,float t,float rr,float b){ float d=density();
            double az=location==null?Double.NaN:calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex])[0];
            String arrow="●", text="Start compass";
            if(!Float.isNaN(heading)&&!Double.isNaN(az)){double diff=signed180((float)(az-heading));double ad=Math.abs(diff);if(ad<2){arrow="✓";text="ALIGNED • within ~2°";}else if(diff>0){arrow="→";text="Turn RIGHT / clockwise "+Math.round(ad)+"°";}else{arrow="←";text="Turn LEFT / counterclockwise "+Math.round(ad)+"°";}}
            txt(c,arrow,l+16*d,t+15*d,14*d,GOOD,Paint.Align.CENTER,true);txt(c,text,l+30*d,t+15*d,9*d,TEXT,Paint.Align.LEFT,true);txt(c,"Target "+(Double.isNaN(az)?"—":fmt(az,1)+"° "+dir8((float)az)),rr-7*d,t+15*d,8*d,GOLD,Paint.Align.RIGHT,true);
        }

        private void drawAlignment(Canvas c,float x,float y,float w,float h){ float d=density();
            txt(c,"SATELLITE ALIGNMENT",x+8*d,y+16*d,10*d,TEXT,Paint.Align.LEFT,true);
            double[] v=location==null?null:calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex]);
            float[] xs={x+w*.18f,x+w*.50f,x+w*.76f,x+w*.76f};
            String[] labs={"AZIMUTH","ELEVATION","LNB SKEW","LNB CLOCK"};
            String[] vals={v==null?"—":fmt(v[0],1)+"°",v==null?"—":fmt(v[1],1)+"°",v==null?"—":(v[2]>=0?"+":"")+fmt(v[2],1)+"°",v==null?"—":lnbClock(v[2])};
            for(int i=0;i<4;i++){float xx=(i<2?x+(i+.5f)*w/2:x+(i-1.5f)*w/2);txt(c,labs[i],xx,y+38*d,7*d,MUTED,Paint.Align.CENTER,false);txt(c,vals[i],xx,y+56*d,13*d,i==2||i==3?Color.rgb(169,135,255):GOOD,Paint.Align.CENTER,true);}
        }
        private String lnbClock(double skew){double hour=(6+(-skew)/30)%12;if(hour<0)hour+=12;int h=(int)Math.floor(hour);int min=(int)Math.round((hour-h)*60);if(h==0)h=12;if(min==60){h=h%12+1;min=0;}return h+":"+String.format(Locale.US,"%02d",min);}

        private void drawSensors(Canvas c,float x,float y,float w,float h){ float d=density();
            txt(c,"REAL-TIME SENSORS",x+8*d,y+16*d,10*d,TEXT,Paint.Align.LEFT,true);
            String coords=location==null?"GPS position —":String.format(Locale.US,"%.6f° N • %.6f° E • ±%.1f m",location.getLatitude(),location.getLongitude(),(double)location.getAccuracy());
            txt(c,sensorState,x+8*d,y+36*d,9*d,sensorStarted?GOOD:MUTED,Paint.Align.LEFT,true);
            txt(c,coords,x+8*d,y+52*d,8*d,TEXT,Paint.Align.LEFT,false);
            txt(c,"PHONE TILT / INCLINE",x+8*d,y+73*d,8*d,MUTED,Paint.Align.LEFT,false);
            txt(c,Float.isNaN(tilt)?"—°":fmt(tilt,1)+"°",x+w-8*d,y+73*d,10*d,TEXT,Paint.Align.RIGHT,true);
            txt(c,compassState,x+8*d,y+h-10*d,7*d,compassState.startsWith("Compass LIVE")?GOOD:MUTED,Paint.Align.LEFT,false);
        }

        private void drawClock(Canvas c,float x,float y,float w,float h){ float d=density();
            float cx=x+w/2, cy=y+h/2+4*d, rad=Math.min(w*.36f,h*.43f);stroke.setColor(Color.rgb(41,77,118));stroke.setStrokeWidth(2*d);c.drawCircle(cx,cy,rad,stroke);stroke.setStrokeWidth(1*d);stroke.setColor(Color.rgb(25,75,115));c.drawCircle(cx,cy,rad*.78f,stroke);
            txt(c,"12",cx,cy-rad*.68f+4*d,10*d,TEXT,Paint.Align.CENTER,true);txt(c,"3",cx+rad*.68f,cy+4*d,10*d,TEXT,Paint.Align.CENTER,true);txt(c,"6",cx,cy+rad*.68f+4*d,10*d,TEXT,Paint.Align.CENTER,true);txt(c,"9",cx-rad*.68f,cy+4*d,10*d,TEXT,Paint.Align.CENTER,true);
            for(int i=0;i<12;i++){double a=Math.toRadians(i*30-90);txt(c,(i*30)+"°",cx+(float)Math.cos(a)*rad*.86f,cy+(float)Math.sin(a)*rad*.86f+3*d,6*d,Color.rgb(208,221,236),Paint.Align.CENTER,false);}
            double skew=location==null?0:calculate(location.getLatitude(),location.getLongitude(),satLon[satIndex])[2];double angle=Math.toRadians((180-skew)-90);stroke.setColor(Color.rgb(255,111,156));stroke.setStrokeWidth(3*d);c.drawLine(cx,cy,cx+(float)Math.cos(angle)*rad*.63f,cy+(float)Math.sin(angle)*rad*.63f,stroke);p.setColor(Color.rgb(233,244,255));p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,5*d,p);
            txt(c,location==null?"LNB —":"LNB "+lnbClock(skew),cx,cy+rad*.30f,9*d,CYAN,Paint.Align.CENTER,true);
        }

        private void drawFooter(Canvas c,float w,float d,float top,float bottom){
            float y=top+2*d, bw=(w-21*d)/2;
            button(c,8*d,y,bw,28*d,"📍 START / REFRESH GPS");
            button(c,13*d+bw,y,bw,28*d,"🧭 COMPASS / TILT LIVE");
            txt(c,"Keep phone level for azimuth • away from metal • tilt sensor works separately",w/2,y+38*d,6*d,MUTED,Paint.Align.CENTER,false);
            String target=satNames[satIndex]+" • "+fmt(satLon[satIndex],1)+"°E • Final lock: receiver SIGNAL QUALITY";
            txt(c,"Target: "+target,w/2,y+50*d,6*d,Color.rgb(97,116,140),Paint.Align.CENTER,false);
        }
        private void button(Canvas c,float x,float y,float bw,float bh,String label){panel(c,x,y,x+bw,y+bh);txt(c,label,x+bw/2,y+18*density(),8*density(),TEXT,Paint.Align.CENTER,true);}

        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            float d=density(), x=e.getX(), y=e.getY(), h=getHeight(), w=getWidth();
            if(y>=37*d && y<=89*d && x<=w*.42f){ if(callbacks!=null)callbacks.onSatelliteClicked(); return true; }
            float footerH=62*d;
            if(y>=h-footerH+2*d){
                if(x<w/2){ if(callbacks!=null)callbacks.onGpsClicked(); }
                else { if(callbacks!=null)callbacks.onSensorsClicked(); }
                return true;
            }
            return true;
        }
    }
}
