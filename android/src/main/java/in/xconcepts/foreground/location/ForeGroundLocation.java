package in.xconcepts.foreground.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ForeGroundLocation {
    private static final String TAG = "ForeGroundLocation";

    public interface LocationCallback {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(Context context, LocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = 
            LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Log.d(TAG, "Location retrieved: " + location.getLatitude() + ", " + location.getLongitude());
                        callback.onLocationResult(location);
                    } else {
                        callback.onLocationError("Location is null");
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to get location", e);
                    callback.onLocationError(e.getMessage());
                }
            });
    }

    public String formatTimestamp(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timeMillis));
    }
}
