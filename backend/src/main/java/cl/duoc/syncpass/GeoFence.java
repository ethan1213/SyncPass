package cl.duoc.syncpass;

/** Calcula la distancia superficial entre dos puntos WGS84 en metros. */
public final class GeoFence {
 private GeoFence() {}
 public static double distanceMeters(double lat1,double lon1,double lat2,double lon2) {
  double a1=Math.toRadians(lat1), a2=Math.toRadians(lat2);
  double dLat=a2-a1, dLon=Math.toRadians(lon2-lon1);
  double h=Math.pow(Math.sin(dLat/2),2)+Math.cos(a1)*Math.cos(a2)*Math.pow(Math.sin(dLon/2),2);
  return 6371000.0*2*Math.atan2(Math.sqrt(h),Math.sqrt(1-h));
 }
}
