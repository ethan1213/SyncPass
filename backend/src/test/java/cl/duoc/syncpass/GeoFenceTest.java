package cl.duoc.syncpass;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoFenceTest {
 @Test void samePointHasZeroDistance() {
  assertEquals(0,GeoFence.distanceMeters(-33.45,-70.67,-33.45,-70.67),0.001);
 }
 @Test void nearbyPointIsWithinHundredMeters() {
  double distance=GeoFence.distanceMeters(-33.45,-70.67,-33.4505,-70.67);
  assertTrue(distance>50 && distance<60);
 }
 @Test void distantPointIsOutsideRadius() {
  assertTrue(GeoFence.distanceMeters(-33.45,-70.67,-33.46,-70.67)>1000);
 }
}
