package com.dji.sdk.sample.demo.accurateLandingController;

/**
 * this class represents a simple coordinate conversion into AR 2D screen,
 * Mainly use for simple Arduino representation of FFP Project.
 * allowing to convert lat/lon cordinations to x,y (pixels / meters).
 * Moreover it allows performing computations in lat / lon in terms of direction (angle),
 * distance (meter)..
 * 
 * Tests are based on the following cordinates:
 * 	p1			p2			p3	
x	35.209687	35.20954	35.210063	lon
y	32.10283	32.10368	32.104084	lat
z	680			685			691	
				
	p1-->p2	p1-->p3	p2-->p3	
	96.5	147	69	65
	351.5	14.1	47	
 */

public class Cords {
	static final double EARTH_RADIUS = 1000*6371;
	static final double NORM_X=2.0;
	static final double NORM_Y=2.0;
	static int screen_X=128;
	static int screen_Y=64;

/**
 * this is the main function converting 3D vector into a 2D screen AR aiming point
 * @param d_xyz
 * @param yaw
 * @param pitch
 * @return
 */
	public static double[] screen_cordes(double[] d_xyz,double yaw, double pitch) {
		double[] ans={-1,-1};
		double[] yaw_pitch = Cords.yaw_pitch(d_xyz);
		 double d_yaw = yaw_pitch[0] - yaw;
		 double d_pitch = yaw_pitch[1] - pitch;
		 if(d_yaw>=180) {
			 d_yaw = (yaw_pitch[0]-360) - yaw;
		 }
		 else {
			 if(d_yaw<=-180) {
				 d_yaw = yaw_pitch[0] - (yaw-360);
			 }
		 }
		 double px = d_yaw*NORM_X;
		 double py = d_pitch*NORM_Y;
		 
		 ans[0] = Cords.screen_X/2+(int)px;
		 ans[1] = Cords.screen_Y/2+(int)py;
		return ans;
	}
	
	/**
	 * this method computes the flat world distance vector between two global points (lat-north, lon-east, alt-above-sea) 
	 * assuming the two points are relatively close .
	 * @param ll1
	 * @param ll2
	 * @return
	 */
	public static double[] flatWorldDist(double[] ll1, double[] ll2) {
		double[] ans = new double[3];
		double dx = ll2[1]-ll1[1]; // delta lon east
		double dy = ll2[0]-ll1[0]; // delta lat north
		double dz = ll2[2]-ll1[2]; // delta alt
		if(Math.abs(dx)>0.1 | Math.abs(dy)>0.1) {return null;}
		double x = EARTH_RADIUS * Math.sin(Math.toRadians(dx)) * Math.cos(Math.toRadians(ll1[0]));
		double y = EARTH_RADIUS * Math.sin(Math.toRadians(dy)); 
		ans[0] = x; ans[1]=y; ans[2] = dz;
		return ans;
	}
	/**
	 * this function computes the azimuth and distance (and dz) in degrees and meters between
	 * two lat/lon points. 
	 * NOTE: this function is used by many navigation higher level function do NOT change it
	 * unless you fully understand - use all Junits - to make sure the change is not adding bugs.
	 *  
	 * @param ll1 first GPS cords
	 * @param ll2 second GPS cords
	 * @return [azm,dist,dz];
	 */
	public static double[] azmDist(double[] ll1, double[] ll2){
		double[] ans = new double[3];
		double[] vec = flatWorldDist(ll1,ll2);
		double dist = Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]); // 2D for now
		double ang = angXY(vec[0], vec[1]);
				
		ans[0] = ang;
		ans [1]= dist;
		ans[2] = vec[2];
		return ans;
	}
	/**
	 * This method gets a Lat-Lon-Alt global coordination and a vector (in meters) <x,y,z> (east, north, up)
	 * it returns a new Lat-Lon-Alt global coordination which is the offset of the inpot point with the vector.
	 * Assumes the vector is smaller than 100km).
	 * @param ll1
	 * @param vec
	 * @return
	 */
	public static double[] offsetLatLonAlt(double[] ll1, double[] vec) {
		double[] ans = new double[3];
		double dz = vec[2]; // delta alt
		if(Math.abs(vec[0])>100000 | Math.abs(vec[1])>100000) {
			//throw new RuntimeException ("ERR: the offset vectr os too big (more than 100km) - can not be assumed as flat world");
			return null;
		}
		double lon = vec[0]/(EARTH_RADIUS * Math.cos(Math.toRadians(ll1[0])));
		double lat = vec[1]/EARTH_RADIUS; 
		ans[1] = ll1[1]+Math.toDegrees(lon); ans[0]=ll1[0]+Math.toDegrees(lat); ans[2] = ll1[2]+dz;
		return ans;
	}
	public static double[] offsetLatLonAzmDist(double[] ll1, double azm, double dist) {
		double[] vec = azmDist2cords(azm,dist);
		return offsetLatLonAlt(ll1,vec);
	}

	
	/**
	 * this static function compute the angle from 0,0 to x,y assuming north is 0, east is 90, south =180, west = 270
	 * @return
	 */
	public static double angXY(double dx,double dy){
		double a0 = Math.atan2(dy, dx);
		//double a1 = Math.toDegrees(a0);
		//double  ans = 90-a1;
		//if(a1>90) ans =450 - a1;
		double ans = rad2Deg(a0);
		return ans;
	}
	
	
	public static double[] yaw_pitch(double[] d_xyz){
		double dx =  d_xyz[0];
		double dy =  d_xyz[1];
		double dz =  d_xyz[2];
		double yaw_rad = Math.atan2(dy,dx);
		double dxy = Math.sqrt(dx*dx+dy*dy);
		double pitch_rad = Math.atan2(dxy,dz);
		double[] ans = new double[2];
		
		ans[0] = rad2Deg(yaw_rad);
		ans[1] = rad2Deg(pitch_rad);
		if(ans[1]>=180) {ans[1] = 360-ans[1];}
		return ans;
	}
	public static double[] azmDist2cords(double azm,double dist){
		//double  a = 90-azm;
		//if(azm>270) a = 450 - azm;
		//a = Math.toRadians(a);
		double  a = deg2Rad(azm);
		double[] ans = new double[3];
		ans[0] = Math.cos(a)*dist;
		ans[1] = Math.sin(a)*dist;
		ans[2] = 0;
		return ans;
	}
	public static double deg2Rad(double deg) {
		double  a = 90-deg;
		if(deg>270) a = 450 - deg;
		a = Math.toRadians(a);
		return a;
	}
	public static double rad2Deg(double rad) {
		double a1 = Math.toDegrees(rad);
		double  ans = 90-a1;
		if(a1>90) ans =450 - a1;
		return ans;
	}
	
	//**************** W2F  & F2W simple transformation
	public static double w2F(double w_width, double f_width) {
		double norm = f_width / w_width;
		return norm;
	}
}




